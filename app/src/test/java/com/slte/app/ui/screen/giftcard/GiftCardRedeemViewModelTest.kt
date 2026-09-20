package com.slte.app.ui.screen.giftcard

import com.slte.app.R
import com.slte.app.data.remote.ApiException
import com.slte.app.data.repository.GiftCardRepository
import com.slte.app.support.MainDispatcherRule
import com.slte.app.ui.component.SubmitTip
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GiftCardRedeemViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<GiftCardRepository>(relaxed = true)

    @Test
    fun `打开弹窗进入输入态`() = runTest(mainRule.dispatcher) {
        val vm = GiftCardRedeemViewModel(repository)

        vm.open()

        val state = vm.state.value
        assertTrue("打开后弹窗应可见", state.visible)
        assertEquals("", state.code)
        assertTrue("打开时不应处于提交中", !state.submitting)
        assertNull(vm.tip.value)
    }

    @Test
    fun `兑换成功关闭弹窗并提示礼品卡兑换成功`() = runTest(mainRule.dispatcher) {
        coEvery { repository.redeem("SLTE2026") } returns Result.success(Unit)

        val vm = GiftCardRedeemViewModel(repository)
        val events = mutableListOf<Unit>()
        val collector = launch { vm.redeemed.collect { events += it } }

        vm.open()
        vm.updateCode(" SLTE2026 ")
        vm.submit()
        advanceUntilIdle()

        assertEquals(GiftCardRedeemState(), vm.state.value)
        assertEquals(SubmitTip(messageRes = R.string.gift_card_success_toast), vm.tip.value)
        assertEquals("兑换成功应触发一次账号刷新", 1, events.size)
        collector.cancel()
    }

    @Test
    fun `兑换失败把常见后端文案映射成本地化提示`() = runTest(mainRule.dispatcher) {
        coEvery { repository.redeem("BAD") } returns Result.failure(ApiException("兑换码不存在"))

        val vm = GiftCardRedeemViewModel(repository)
        vm.open()
        vm.updateCode("BAD")
        vm.submit()
        advanceUntilIdle()

        assertEquals(GiftCardRedeemState(visible = true, code = "BAD"), vm.state.value)
        assertEquals(SubmitTip(messageRes = R.string.error_gift_card_invalid), vm.tip.value)
    }

    @Test
    fun `未识别的后端文案原样透传`() = runTest(mainRule.dispatcher) {
        coEvery { repository.redeem("BAD") } returns Result.failure(ApiException("未知的后端提示"))

        val vm = GiftCardRedeemViewModel(repository)
        vm.open()
        vm.updateCode("BAD")
        vm.submit()
        advanceUntilIdle()

        assertEquals(SubmitTip(message = "未知的后端提示"), vm.tip.value)
    }

    @Test
    fun `本地错误走本地化文案`() = runTest(mainRule.dispatcher) {
        coEvery { repository.redeem("BAD") } returns
            Result.failure(ApiException("请求失败，请检查网络连接", R.string.error_network))

        val vm = GiftCardRedeemViewModel(repository)
        vm.open()
        vm.updateCode("BAD")
        vm.submit()
        advanceUntilIdle()

        assertEquals(SubmitTip(messageRes = R.string.error_network), vm.tip.value)
    }

    @Test
    fun `提交中忽略重复提交`() = runTest(mainRule.dispatcher) {
        coEvery { repository.redeem(any()) } returns Result.success(Unit)

        val vm = GiftCardRedeemViewModel(repository)
        vm.open()
        vm.updateCode("SLTE2026")
        vm.submit()
        vm.submit()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.redeem("SLTE2026") }
        assertTrue("完成后应关闭弹窗", !vm.state.value.visible)
    }

    @Test
    fun `提交中关闭弹窗，结果仍以提示返回`() = runTest(mainRule.dispatcher) {
        coEvery { repository.redeem(any()) } returns Result.failure(ApiException("兑换失败"))

        val vm = GiftCardRedeemViewModel(repository)
        vm.open()
        vm.updateCode("SLTE2026")
        vm.submit()
        vm.dismiss()

        assertTrue("关闭后弹窗应复位", !vm.state.value.visible)

        advanceUntilIdle()

        assertTrue("关闭弹窗后不应再打开", !vm.state.value.visible)
        assertEquals(SubmitTip(message = "兑换失败"), vm.tip.value)
    }

    @Test
    fun `未输入兑换码不发起请求`() = runTest(mainRule.dispatcher) {
        val vm = GiftCardRedeemViewModel(repository)
        vm.open()

        vm.submit()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.redeem(any()) }
        assertTrue("空兑换码时弹窗保持打开", vm.state.value.visible)
    }

    @Test
    fun `兑换码超长截断且关闭后状态复位`() = runTest(mainRule.dispatcher) {
        val vm = GiftCardRedeemViewModel(repository)

        vm.open()
        vm.updateCode("A".repeat(40))
        assertEquals(32, vm.state.value.code.length)

        vm.dismiss()

        assertEquals(GiftCardRedeemState(), vm.state.value)
    }
}
