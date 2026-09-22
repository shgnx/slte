package com.slte.app.ui.screen.server

import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.github.kr328.clash.service.remote.IClashManager
import com.slte.app.R
import com.slte.app.data.repository.ServerRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.ServerType
import com.slte.app.kernel.KernelManager
import com.slte.app.kernel.KernelProxy
import com.slte.app.support.MainDispatcherRule
import com.slte.app.support.stubKernelBridge
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ServerViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val serverRepository = mockk<ServerRepository>(relaxed = true)
    private val subscribeRepository = mockk<SubscribeRepository>(relaxed = true)
    private val kernelProxy = mockk<KernelProxy>(relaxed = true)

    private val kernelGroup = "节点选择"

    private var kernelNow = ""

    private fun kernelNode(name: String) = Proxy(
        name = name,
        title = name,
        subtitle = "vless",
        type = "Vless",
        delay = 100,
        isGroup = false,
    )

    private fun viewModel(): ServerViewModel {
        kernelProxy.stubKernelBridge()
        every { subscribeRepository.getCachedSubscribeInfo() } returns null
        return ServerViewModel(serverRepository, subscribeRepository, kernelProxy)
    }

    private fun stubKernelNodes(vararg names: String): IClashManager {
        kernelNow = names.firstOrNull().orEmpty()
        val clash = mockk<IClashManager>(relaxed = true)
        val manager = mockk<KernelManager>(relaxed = true)
        every { kernelProxy.manager } returns manager
        every { manager.clash() } returns clash
        every { clash.queryTunnelState() } returns TunnelState(TunnelState.Mode.Rule)
        every { clash.queryProxyGroupNames(any()) } returns listOf(kernelGroup)
        every { clash.queryProxyGroup(kernelGroup, ProxySort.Default) } answers {
            ProxyGroup(type = "Selector", proxies = names.map(::kernelNode), now = kernelNow)
        }
        every { clash.patchSelector(kernelGroup, any()) } answers {
            kernelNow = secondArg()
            true
        }
        return clash
    }

    private fun node(
        name: String,
        id: Int = 1,
        type: ServerType = ServerType.VMESS,
    ) = ServerNode(id = id, name = name, type = type, host = "h.example.com", port = 443)

    @Test
    fun `加载成功后填充节点并标注国家码`() = runTest(mainRule.dispatcher) {
        coEvery { serverRepository.fetchServers(any()) } returns
            Result.success(listOf(node("香港01", 1), node("美国01", 2), node("香港01", 3)))
        val vm = viewModel()

        vm.loadNodes(force = true)
        advanceUntilIdle()

        val nodes = vm.data.value.nodes
        assertEquals("同名节点应去重", 2, nodes.size)
        assertEquals("HK", nodes.first { it.name == "香港01" }.countryCode)
        assertEquals("US", nodes.first { it.name == "美国01" }.countryCode)
        assertTrue(!vm.data.value.isLoading)
    }

    @Test
    fun `加载失败提示节点错误`() = runTest(mainRule.dispatcher) {
        coEvery { serverRepository.fetchServers(any()) } returns Result.failure(java.io.IOException("boom"))
        val vm = viewModel()

        vm.retry()
        advanceUntilIdle()

        assertEquals(R.string.error_server_load, vm.errorMessageRes.value)
        assertTrue(!vm.data.value.isLoading)
    }

    @Test
    fun `选中普通节点调用内核选择并更新选中项`() = runTest(mainRule.dispatcher) {
        coEvery { serverRepository.fetchServers(any()) } returns Result.success(listOf(node("香港01", 1)))
        val vm = viewModel()
        stubKernelNodes("香港01")
        vm.loadNodes(force = true)
        advanceUntilIdle()

        val target = vm.data.value.nodes.first()
        vm.selectNode(target.id)
        advanceUntilIdle()

        assertEquals(target.id, vm.data.value.selectedNodeId)
    }

    @Test
    fun `内核未确认切换时不改动选中项`() = runTest(mainRule.dispatcher) {
        coEvery { serverRepository.fetchServers(any()) } returns Result.success(listOf(node("香港01", 1)))
        val vm = viewModel()
        stubKernelNodes("日本01")
        vm.loadNodes(force = true)
        advanceUntilIdle()

        vm.selectNode(vm.data.value.nodes.first().id)
        advanceUntilIdle()

        assertEquals(0, vm.data.value.selectedNodeId)
    }

    @Test
    fun `内核名字带协议前缀时按内核名字回填延迟`() = runTest(mainRule.dispatcher) {
        coEvery { serverRepository.fetchServers(any()) } returns Result.success(listOf(node("香港01", 1)))
        every { kernelProxy.speedResultStore.getSpeedResults() } returns mapOf("[vless]香港01" to 123)
        val vm = viewModel()
        stubKernelNodes("[vless]香港01")
        vm.loadNodes(force = true)
        advanceUntilIdle()

        val item = vm.data.value.nodes.first()
        assertEquals("[vless]香港01", item.proxyName)
        assertEquals(123, item.delay)
    }

    @Test
    fun `选中项按内核名字下发切换`() = runTest(mainRule.dispatcher) {
        coEvery { serverRepository.fetchServers(any()) } returns Result.success(listOf(node("香港01", 1)))
        val vm = viewModel()
        val clash = stubKernelNodes("[vless]香港01")
        vm.loadNodes(force = true)
        advanceUntilIdle()

        vm.selectNode(vm.data.value.nodes.first().id)
        advanceUntilIdle()

        verify { clash.patchSelector(kernelGroup, "[vless]香港01") }
    }

    @Test
    fun `同名多协议节点按协议标签消歧`() = runTest(mainRule.dispatcher) {
        coEvery { serverRepository.fetchServers(any()) } returns
            Result.success(listOf(node("香港01", 1, ServerType.VLESS)))
        every { kernelProxy.speedResultStore.getSpeedResults() } returns mapOf("[vless]香港01" to 88)
        val vm = viewModel()
        stubKernelNodes("[trojan]香港01", "[vless]香港01")
        vm.loadNodes(force = true)
        advanceUntilIdle()

        val item = vm.data.value.nodes.first()
        assertEquals("[vless]香港01", item.proxyName)
        assertEquals(88, item.delay)
    }

    @Test
    fun `装饰前缀不一致时选中项跟随内核`() = runTest(mainRule.dispatcher) {
        coEvery { serverRepository.fetchServers(any()) } returns Result.success(listOf(node("新加坡01", 1)))
        val vm = viewModel()
        stubKernelNodes("[vless]新加坡01")
        vm.loadNodes(force = true)
        advanceUntilIdle()

        val target = vm.data.value.nodes.first()
        vm.selectNode(target.id)
        advanceUntilIdle()

        assertEquals(target.id, vm.data.value.selectedNodeId)
    }

    @Test
    fun `选中自动选择走专用内核入口`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()

        vm.selectNode(0)
        advanceUntilIdle()

        assertEquals(0, vm.data.value.selectedNodeId)
    }

    @Test
    fun `测速中状态标记进行中，重复触发不叠加`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()

        vm.startSpeedTest()
        assertTrue("测速应标记进行中", vm.data.value.isTesting)

        vm.startSpeedTest()
        assertTrue("重复触发不应改变状态", vm.data.value.isTesting)

        advanceUntilIdle()
        assertTrue("测速结束后应复位", !vm.data.value.isTesting)
    }
}
