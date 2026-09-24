package com.slte.app.ui.screen.profile

import com.slte.app.data.local.SessionManager
import com.slte.app.data.repository.AuthRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.domain.model.SessionState
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.model.User
import com.slte.app.domain.usecase.DaysUntilExpiryUseCase
import com.slte.app.support.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val subscribeRepository = mockk<SubscribeRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val expiryUseCase = mockk<DaysUntilExpiryUseCase>(relaxed = true)

    private val subscribeFlow = MutableStateFlow<SubscribeInfo?>(null)

    private fun user() = User(id = "1", displayName = "测试", email = "a@b.c", balance = "12.34")

    private fun subscribe(expiredAt: Long = 0L) = SubscribeInfo(planName = "进阶套餐", transferEnable = 400L, usedTraffic = 65L, expiredAt = expiredAt, planId = 7)

    private fun publish(info: SubscribeInfo): Result<SubscribeInfo> {
        subscribeFlow.value = info
        return Result.success(info)
    }

    private fun viewModel(
        cachedUser: User? = null,
        cachedSubscribe: SubscribeInfo? = null,
    ): ProfileViewModel {
        every { sessionManager.sessionState } returns MutableStateFlow(SessionState.LoggedOut)
        every { subscribeRepository.getCachedUserInfo() } returns cachedUser
        every { subscribeRepository.subscribeInfo } returns subscribeFlow
        every { expiryUseCase(any()) } returns 49
        subscribeFlow.value = cachedSubscribe
        return ProfileViewModel(subscribeRepository, sessionManager, authRepository, expiryUseCase)
    }

    @Test
    fun `有本地缓存时先展示缓存不显示加载`() = runTest(mainRule.dispatcher) {
        val vm = viewModel(cachedUser = user(), cachedSubscribe = subscribe(expiredAt = 1_800_000_000L))
        advanceUntilIdle()

        val data = vm.data.value
        assertEquals("a@b.c", data.email)
        assertEquals("12.34", data.balance)
        assertEquals(49, data.daysUntilExpired)
        assertTrue("有缓存不应再显示全屏加载", !data.isLoading)
    }

    @Test
    fun `刷新成功更新邮箱与套餐信息`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo() } returns Result.success(user())
        coEvery { subscribeRepository.fetchSubscribeInfo() } coAnswers { publish(subscribe()) }
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        val data = vm.data.value
        assertEquals("a@b.c", data.email)
        assertEquals("进阶套餐", data.subscribeInfo?.planName)
        assertTrue(!data.isLoading)
    }

    @Test
    fun `刷新失败且无缓存时提示错误`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo() } returns Result.failure(java.io.IOException("boom"))
        coEvery { subscribeRepository.fetchSubscribeInfo() } returns Result.failure(java.io.IOException("boom"))
        val vm = viewModel()

        vm.retry()
        advanceUntilIdle()

        assertTrue(vm.errorMessageRes.value != null)
        assertTrue(!vm.data.value.isLoading)
    }

    @Test
    fun `有缓存时刷新失败不清空内容也不弹全屏错误`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo() } returns Result.failure(java.io.IOException("boom"))
        coEvery { subscribeRepository.fetchSubscribeInfo() } returns Result.failure(java.io.IOException("boom"))
        val vm = viewModel(cachedUser = user(), cachedSubscribe = subscribe())

        vm.refresh()
        advanceUntilIdle()

        assertEquals("进阶套餐", vm.data.value.subscribeInfo?.planName)
        assertEquals("失败时保留缓存内容", null, vm.errorMessageRes.value)
    }

    @Test
    fun `订阅信息从共享流更新时个人页同步刷新`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        subscribeFlow.value = subscribe(expiredAt = 1_800_000_000L)
        advanceUntilIdle()

        val data = vm.data.value
        assertEquals("进阶套餐", data.subscribeInfo?.planName)
        assertEquals("共享流带来的到期天数应同步", 49, data.daysUntilExpired)
        assertTrue("拿到订阅后应结束加载态", !data.isLoading)
    }

    @Test
    fun `无缓存时初始空订阅不结束加载态`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertTrue("尚无任何订阅数据时应保持加载态", vm.data.value.isLoading)
        assertEquals(null, vm.data.value.subscribeInfo)
    }

    @Test
    fun `登出委托给鉴权仓库`() = runTest(mainRule.dispatcher) {
        val vm = viewModel()

        vm.logout()
        advanceUntilIdle()

        verify { authRepository.logout() }
    }

    @Test
    fun `套餐卡片字段与首页同一套计算`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo() } returns Result.success(user())
        coEvery { subscribeRepository.fetchSubscribeInfo() } coAnswers {
            publish(subscribe(expiredAt = 1_800_000_000L))
        }
        val vm = viewModel()

        vm.refresh()
        advanceUntilIdle()

        val data = vm.data.value
        assertEquals("进阶套餐", data.planName)
        assertEquals(65L, data.usedBytes)
        assertEquals(400L, data.totalBytes)
        assertTrue("有套餐应标记 hasPlan", data.hasPlan)
        assertTrue("有效期内的套餐应为有效", data.isValid)
        assertEquals(1_800_000_000L, data.expiredAt)
        assertEquals(49, data.daysUntilExpired)
    }
}
