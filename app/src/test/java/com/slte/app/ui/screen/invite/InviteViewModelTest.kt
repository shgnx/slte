package com.slte.app.ui.screen.invite

import com.slte.app.R
import com.slte.app.data.repository.InviteRepository
import com.slte.app.domain.model.InviteInfo
import com.slte.app.domain.model.InviteStat
import com.slte.app.support.MainDispatcherRule
import com.slte.app.ui.component.SubmitTip
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InviteViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val repository = mockk<InviteRepository>(relaxed = true)

    private fun stubPageData(
        methods: Result<List<String>> = Result.success(listOf("USDT")),
        stat: InviteStat = InviteStat(availableBalance = 56_700),
    ) {
        coEvery { repository.fetchInviteInfo() } returns Result.success(InviteInfo(stat = stat))
        coEvery { repository.fetchCommissionRecords(any(), any()) } returns Result.success(emptyList())
        coEvery { repository.fetchWithdrawMethods() } returns methods
    }

    @Test
    fun `弹窗状态互斥：转账与提现不会同时打开`() = runTest(mainRule.dispatcher) {
        stubPageData()
        val vm = InviteViewModel(repository)

        assertEquals(InviteSheet.None, vm.data.value.sheet)
        vm.showTransferSheet()
        assertEquals(InviteSheet.Transfer, vm.data.value.sheet)
        vm.showWithdrawSheet()
        assertEquals(InviteSheet.Withdraw, vm.data.value.sheet)
        vm.hideWithdrawSheet()
        assertEquals(InviteSheet.None, vm.data.value.sheet)
    }

    @Test
    fun `页面数据与提现方式一起预加载`() = runTest(mainRule.dispatcher) {
        stubPageData(methods = Result.success(listOf("USDT", "支付宝")))
        val vm = InviteViewModel(repository)

        vm.enterAndRefresh()
        advanceUntilIdle()

        assertEquals(WithdrawMethodsState.Ready(listOf("USDT", "支付宝")), vm.data.value.withdrawMethods)
        assertEquals(56_700, vm.data.value.stat.availableBalance)
        assertTrue("预加载完成后不再是进入中", !vm.data.value.isEntering)
    }

    @Test
    fun `提现方式拉取失败进入可重试状态`() = runTest(mainRule.dispatcher) {
        stubPageData(methods = Result.failure(IllegalStateException("boom")))
        val vm = InviteViewModel(repository)

        vm.enterAndRefresh()
        advanceUntilIdle()

        assertEquals(WithdrawMethodsState.Failed, vm.data.value.withdrawMethods)

        stubPageData(methods = Result.success(listOf("USDT")))
        vm.retryWithdrawMethods()
        advanceUntilIdle()

        assertEquals(WithdrawMethodsState.Ready(listOf("USDT")), vm.data.value.withdrawMethods)
    }

    @Test
    fun `转账成功后关闭弹窗并提示`() = runTest(mainRule.dispatcher) {
        stubPageData()
        coEvery { repository.transferCommission(any()) } returns Result.success(true)
        val vm = InviteViewModel(repository)
        advanceUntilIdle()

        vm.showTransferSheet()
        vm.transferCommission(56.7)
        advanceUntilIdle()

        assertEquals(InviteSheet.None, vm.data.value.sheet)
        assertEquals(SubmitTip(messageRes = R.string.invite_success_transfer), vm.data.value.tip)
        assertTrue("提交结束后应复位提交中", !vm.data.value.isSubmitting)
    }

    @Test
    fun `转账失败保持弹窗并提示`() = runTest(mainRule.dispatcher) {
        stubPageData()
        coEvery { repository.transferCommission(any()) } returns Result.success(false)
        val vm = InviteViewModel(repository)
        advanceUntilIdle()

        vm.showTransferSheet()
        vm.transferCommission(56.7)
        advanceUntilIdle()

        assertEquals("失败不应关闭弹窗", InviteSheet.Transfer, vm.data.value.sheet)
        assertEquals(SubmitTip(messageRes = R.string.invite_error_transfer), vm.data.value.tip)
    }
}
