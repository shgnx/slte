package com.slte.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.local.LocaleStore
import com.slte.app.data.local.ThemePreference
import com.slte.app.data.repository.AuthRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.kernel.KernelProxy
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 到期/流量邮件提醒开关状态。
 *
 * 默认开启：后台默认 remind_expire/remind_traffic=1，进入页面时再请求服务端确认；
 * remindLoading=true 表示正在请求服务端。
 */
data class SettingsData(
    val expireRemindEnabled: Boolean = true,
    val trafficRemindEnabled: Boolean = true,
    val remindLoading: Boolean = true,
    val remindSaving: Boolean = false,
    val errorMessageRes: Int? = null,
    val tunStackMode: TunStackMode = TunStackMode.DEFAULT,
    /** 每次切换 TUN 堆栈成功后 +1，驱动页面 Toast 提示 */
    val tunStackSwitchCount: Int = 0,
    /** 深色模式（本地偏好，默认关 = 浅色） */
    val darkModeEnabled: Boolean = false,
    /** 当前界面语言（null = 跟随系统） */
    val locale: Locale? = null,
)

data class ChangePasswordState(
    val showChangePasswordSheet: Boolean = false,
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val oldPasswordVisible: Boolean = false,
    val newPasswordVisible: Boolean = false,
    val submitting: Boolean = false,
    val errorMessageRes: Int? = null,
    val success: Boolean = false,
)

/**
 * 其他设置页 ViewModel：加载并更新到期/流量邮件提醒开关。
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val subscribeRepository: SubscribeRepository,
    private val kernelProxy: KernelProxy,
    private val themePreference: ThemePreference,
    private val localeStore: LocaleStore,
) : ViewModel() {
    private val _data =
        MutableStateFlow(
            SettingsData(
                darkModeEnabled = themePreference.dark.value,
                locale = localeStore.locale.value,
            ),
        )
    val data: StateFlow<SettingsData> = _data.asStateFlow()

    private val _changePasswordState = MutableStateFlow(ChangePasswordState())
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState.asStateFlow()

    init {
        loadRemindSettings()
        loadTunStackMode()
    }

    /** 切换深色模式并持久化 */
    fun setDarkMode(enabled: Boolean) {
        themePreference.setDark(enabled)
        _data.value = _data.value.copy(darkModeEnabled = enabled)
    }

    /** 切换界面语言并持久化；null = 跟随系统，根组件监听后全树热切换 */
    fun setLocale(locale: Locale?) {
        localeStore.setLocale(locale)
        _data.value = _data.value.copy(locale = locale)
    }

    /** 进入页面时读取内核当前 TUN 堆栈模式（后台进程 ServiceStore 为权威值） */
    private fun loadTunStackMode() {
        viewModelScope.launch {
            val mode = TunStackMode.fromValue(kernelProxy.tunStackMode())
            _data.value = _data.value.copy(tunStackMode = mode)
        }
    }

    /** 切换 TUN 堆栈模式：写内核并持久化；运行中会自动原地重启 TUN 生效 */
    fun setTunStackMode(mode: TunStackMode) {
        if (_data.value.tunStackMode == mode) return
        viewModelScope.launch {
            kernelProxy.setTunStack(mode.value)
            _data.value =
                _data.value.copy(
                    tunStackMode = mode,
                    tunStackSwitchCount = _data.value.tunStackSwitchCount + 1,
                )
        }
    }

    fun consumeTunStackSwitch() {
        _data.value = _data.value.copy(tunStackSwitchCount = 0)
    }

    /** 进入页面时直接请求服务端 remind_expire/remind_traffic 覆盖默认值 */
    fun loadRemindSettings() {
        viewModelScope.launch {
            subscribeRepository
                .fetchUserInfo(force = true)
                .onSuccess { user ->
                    _data.value =
                        _data.value.copy(
                            expireRemindEnabled = user.remindExpire == 1,
                            trafficRemindEnabled = user.remindTraffic == 1,
                            remindLoading = false,
                            errorMessageRes = null,
                        )
                }.onFailure {
                    // 请求失败保持默认开启，不打扰用户
                    _data.value = _data.value.copy(remindLoading = false)
                }
        }
    }

    /** 切换到期提醒并写入服务端；失败时回滚并提示 */
    fun setExpireRemind(enabled: Boolean) {
        if (_data.value.remindSaving) return
        _data.value = _data.value.copy(expireRemindEnabled = enabled, remindSaving = true, errorMessageRes = null)
        viewModelScope.launch {
            authRepository
                .updateRemindExpire(enabled)
                .onSuccess {
                    _data.value = _data.value.copy(remindSaving = false)
                }.onFailure {
                    // 回滚开关状态
                    _data.value =
                        _data.value.copy(
                            expireRemindEnabled = !enabled,
                            remindSaving = false,
                            errorMessageRes = R.string.settings_remind_save_failed,
                        )
                }
        }
    }

    /** 切换流量提醒并写入服务端；失败时回滚并提示 */
    fun setTrafficRemind(enabled: Boolean) {
        if (_data.value.remindSaving) return
        _data.value = _data.value.copy(trafficRemindEnabled = enabled, remindSaving = true, errorMessageRes = null)
        viewModelScope.launch {
            authRepository
                .updateRemindTraffic(enabled)
                .onSuccess {
                    _data.value = _data.value.copy(remindSaving = false)
                }.onFailure {
                    _data.value =
                        _data.value.copy(
                            trafficRemindEnabled = !enabled,
                            remindSaving = false,
                            errorMessageRes = R.string.settings_remind_save_failed,
                        )
                }
        }
    }

    fun showChangePassword() {
        _changePasswordState.value = ChangePasswordState(showChangePasswordSheet = true)
    }

    fun dismissChangePassword() {
        _changePasswordState.value = _changePasswordState.value.copy(showChangePasswordSheet = false)
    }

    fun onOldPasswordChange(value: String) {
        _changePasswordState.value = _changePasswordState.value.copy(oldPassword = value, errorMessageRes = null)
    }

    fun onNewPasswordChange(value: String) {
        _changePasswordState.value = _changePasswordState.value.copy(newPassword = value, errorMessageRes = null)
    }

    fun onConfirmPasswordChange(value: String) {
        _changePasswordState.value = _changePasswordState.value.copy(confirmPassword = value, errorMessageRes = null)
    }

    fun toggleOldPasswordVisible() {
        _changePasswordState.value = _changePasswordState.value.copy(oldPasswordVisible = !_changePasswordState.value.oldPasswordVisible)
    }

    fun toggleNewPasswordVisible() {
        _changePasswordState.value = _changePasswordState.value.copy(newPasswordVisible = !_changePasswordState.value.newPasswordVisible)
    }

    /** 提交修改密码：本地校验 → 服务端 → 成功后关闭弹窗并提示 */
    fun submitChangePassword() {
        val state = _changePasswordState.value
        if (state.submitting) return
        val error =
            when {
                state.oldPassword.isBlank() -> R.string.settings_change_pwd_old_required
                state.newPassword.length < 8 -> R.string.settings_change_pwd_too_short
                state.newPassword != state.confirmPassword -> R.string.settings_change_pwd_mismatch
                else -> null
            }
        if (error != null) {
            _changePasswordState.value = state.copy(errorMessageRes = error)
            return
        }
        _changePasswordState.value = state.copy(submitting = true, errorMessageRes = null)
        viewModelScope.launch {
            authRepository
                .changePassword(state.oldPassword, state.newPassword)
                .onSuccess {
                    _changePasswordState.value =
                        _changePasswordState.value.copy(
                            submitting = false,
                            success = true,
                            showChangePasswordSheet = false,
                        )
                }.onFailure {
                    _changePasswordState.value =
                        _changePasswordState.value.copy(
                            submitting = false,
                            errorMessageRes = R.string.settings_change_pwd_failed,
                        )
                }
        }
    }

    fun consumeChangePasswordSuccess() {
        _changePasswordState.value = _changePasswordState.value.copy(success = false)
    }

    fun consumeChangePasswordError() {
        _changePasswordState.value = _changePasswordState.value.copy(errorMessageRes = null)
    }
}
