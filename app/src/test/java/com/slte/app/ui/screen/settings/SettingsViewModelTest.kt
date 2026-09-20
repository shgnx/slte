package com.slte.app.ui.screen.settings

import com.slte.app.R
import com.slte.app.data.local.LocaleStore
import com.slte.app.data.local.ThemePreference
import com.slte.app.data.repository.AuthRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.domain.model.User
import com.slte.app.kernel.KernelProxy
import com.slte.app.support.MainDispatcherRule
import com.slte.app.ui.component.SubmitTip
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val subscribeRepository = mockk<SubscribeRepository>(relaxed = true)
    private val kernelProxy = mockk<KernelProxy>(relaxed = true)
    private val themePreference = mockk<ThemePreference>(relaxed = true)
    private val localeStore = mockk<LocaleStore>(relaxed = true)

    private fun viewModel(): SettingsViewModel {
        every { themePreference.dark } returns MutableStateFlow(false)
        every { localeStore.locale } returns MutableStateFlow(null)
        coEvery { kernelProxy.tunStackMode() } returns "system"
        return SettingsViewModel(authRepository, subscribeRepository, kernelProxy, themePreference, localeStore)
    }

    private fun user(
        remindExpire: Int = 0,
        remindTraffic: Int = 0,
    ) = User(id = "1", displayName = "测试", remindExpire = remindExpire, remindTraffic = remindTraffic)

    @Test
    fun `加载成功后提醒开关采用服务端值`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo(force = true) } returns
            Result.success(user(remindExpire = 1, remindTraffic = 0))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(RemindSync.Idle, vm.data.value.remindSync)
        assertTrue(vm.data.value.expireRemindEnabled)
        assertFalse(vm.data.value.trafficRemindEnabled)
    }

    @Test
    fun `加载失败也回到 Idle 并保持默认开启`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo(force = true) } returns Result.failure(IllegalStateException("boom"))

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(RemindSync.Idle, vm.data.value.remindSync)
        assertTrue(vm.data.value.expireRemindEnabled)
        assertTrue(vm.data.value.trafficRemindEnabled)
    }

    @Test
    fun `保存期间拒绝再次切换，完成后回到 Idle`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo(force = true) } returns Result.success(user(remindExpire = 1, remindTraffic = 1))
        coEvery { authRepository.updateRemindExpire(any()) } returns Result.success(Unit)

        val vm = viewModel()
        advanceUntilIdle()

        vm.setExpireRemind(false)
        assertEquals(RemindSync.Saving, vm.data.value.remindSync)
        vm.setTrafficRemind(false)
        assertTrue("保存中不应接受第二次切换", vm.data.value.trafficRemindEnabled)

        advanceUntilIdle()
        assertEquals(RemindSync.Idle, vm.data.value.remindSync)
        assertFalse(vm.data.value.expireRemindEnabled)
        coVerify(exactly = 1) { authRepository.updateRemindExpire(false) }
    }

    @Test
    fun `保存失败回滚开关并提示`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo(force = true) } returns Result.success(user(remindExpire = 1, remindTraffic = 1))
        coEvery { authRepository.updateRemindExpire(any()) } returns Result.failure(IllegalStateException("boom"))

        val vm = viewModel()
        advanceUntilIdle()
        vm.setExpireRemind(false)
        advanceUntilIdle()

        assertTrue("失败应回滚为开启", vm.data.value.expireRemindEnabled)
        assertEquals(R.string.settings_remind_save_failed, vm.data.value.errorMessageRes)
    }

    @Test
    fun `修改密码：本地校验拦住不一致输入且不发请求`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo(force = true) } returns Result.success(user())
        val vm = viewModel()
        advanceUntilIdle()

        vm.showChangePassword()
        vm.onOldPasswordChange("oldpass12")
        vm.onNewPasswordChange("newpass1234")
        vm.onConfirmPasswordChange("different99")
        vm.submitChangePassword()
        advanceUntilIdle()

        val state = vm.changePasswordState.value as ChangePasswordState.Editing
        assertEquals(SubmitTip(messageRes = R.string.settings_change_pwd_mismatch), vm.tip.value)
        assertFalse(state.submitting)
        coVerify(exactly = 0) { authRepository.changePassword(any(), any()) }
    }

    @Test
    fun `修改密码：成功后关闭弹窗并提示成功`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo(force = true) } returns Result.success(user())
        coEvery { authRepository.changePassword(any(), any()) } returns Result.success(Unit)
        val vm = viewModel()
        advanceUntilIdle()

        vm.showChangePassword()
        vm.onOldPasswordChange("oldpass12")
        vm.onNewPasswordChange("newpass1234")
        vm.onConfirmPasswordChange("newpass1234")
        vm.submitChangePassword()
        advanceUntilIdle()

        assertEquals(ChangePasswordState.Closed, vm.changePasswordState.value)
        assertEquals(SubmitTip(messageRes = R.string.settings_change_pwd_success), vm.tip.value)

        vm.clearTip()
        assertNull(vm.tip.value)
    }

    @Test
    fun `修改密码：服务端失败回到编辑态并带错误`() = runTest(mainRule.dispatcher) {
        coEvery { subscribeRepository.fetchUserInfo(force = true) } returns Result.success(user())
        coEvery { authRepository.changePassword(any(), any()) } returns Result.failure(IllegalStateException("boom"))
        val vm = viewModel()
        advanceUntilIdle()

        vm.showChangePassword()
        vm.onOldPasswordChange("oldpass12")
        vm.onNewPasswordChange("newpass1234")
        vm.onConfirmPasswordChange("newpass1234")
        vm.submitChangePassword()
        advanceUntilIdle()

        val state = vm.changePasswordState.value as ChangePasswordState.Editing
        assertEquals(SubmitTip(messageRes = R.string.settings_change_pwd_failed), vm.tip.value)
        assertFalse(state.submitting)
    }
}
