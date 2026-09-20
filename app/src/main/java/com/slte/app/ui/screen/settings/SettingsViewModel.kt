package com.slte.app.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.local.LocaleStore
import com.slte.app.data.local.ThemePreference
import com.slte.app.data.repository.AuthRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.kernel.KernelProxy
import com.slte.app.ui.component.SubmitTip
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class RemindSync { Loading, Idle, Saving }

data class SettingsData(
    val expireRemindEnabled: Boolean = true,
    val trafficRemindEnabled: Boolean = true,
    val remindSync: RemindSync = RemindSync.Loading,
    val errorMessageRes: Int? = null,
    val tunStackMode: TunStackMode = TunStackMode.DEFAULT,

    val tunStackSwitchCount: Int = 0,

    val darkModeEnabled: Boolean = false,

    val locale: Locale? = null,
)

data class ChangePasswordForm(
    val oldPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
)

sealed interface ChangePasswordState {
    object Closed : ChangePasswordState

    data class Editing(
        val form: ChangePasswordForm = ChangePasswordForm(),
        val submitting: Boolean = false,
    ) : ChangePasswordState
}

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

    private val _changePasswordState = MutableStateFlow<ChangePasswordState>(ChangePasswordState.Closed)
    val changePasswordState: StateFlow<ChangePasswordState> = _changePasswordState.asStateFlow()

    private val _tip = MutableStateFlow<SubmitTip?>(null)
    val tip: StateFlow<SubmitTip?> = _tip.asStateFlow()

    init {
        loadRemindSettings()
        loadTunStackMode()
    }

    fun setDarkMode(enabled: Boolean) {
        themePreference.setDark(enabled)
        _data.value = _data.value.copy(darkModeEnabled = enabled)
    }

    fun setLocale(locale: Locale?) {
        localeStore.setLocale(locale)
        _data.value = _data.value.copy(locale = locale)
    }

    private fun loadTunStackMode() {
        viewModelScope.launch {
            val mode = TunStackMode.fromValue(kernelProxy.tunStackMode())
            _data.value = _data.value.copy(tunStackMode = mode)
        }
    }

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

    fun loadRemindSettings() {
        viewModelScope.launch {
            subscribeRepository
                .fetchUserInfo(force = true)
                .onSuccess { user ->
                    _data.value =
                        _data.value.copy(
                            expireRemindEnabled = user.remindExpire == 1,
                            trafficRemindEnabled = user.remindTraffic == 1,
                            remindSync = RemindSync.Idle,
                            errorMessageRes = null,
                        )
                }.onFailure {
                    _data.value = _data.value.copy(remindSync = RemindSync.Idle)
                }
        }
    }

    fun setExpireRemind(enabled: Boolean) {
        if (_data.value.remindSync != RemindSync.Idle) return
        _data.value = _data.value.copy(expireRemindEnabled = enabled, remindSync = RemindSync.Saving, errorMessageRes = null)
        viewModelScope.launch {
            authRepository
                .updateRemindExpire(enabled)
                .onSuccess {
                    _data.value = _data.value.copy(remindSync = RemindSync.Idle)
                }.onFailure {
                    _data.value =
                        _data.value.copy(
                            expireRemindEnabled = !enabled,
                            remindSync = RemindSync.Idle,
                            errorMessageRes = R.string.settings_remind_save_failed,
                        )
                }
        }
    }

    fun setTrafficRemind(enabled: Boolean) {
        if (_data.value.remindSync != RemindSync.Idle) return
        _data.value = _data.value.copy(trafficRemindEnabled = enabled, remindSync = RemindSync.Saving, errorMessageRes = null)
        viewModelScope.launch {
            authRepository
                .updateRemindTraffic(enabled)
                .onSuccess {
                    _data.value = _data.value.copy(remindSync = RemindSync.Idle)
                }.onFailure {
                    _data.value =
                        _data.value.copy(
                            trafficRemindEnabled = !enabled,
                            remindSync = RemindSync.Idle,
                            errorMessageRes = R.string.settings_remind_save_failed,
                        )
                }
        }
    }

    fun showChangePassword() {
        _changePasswordState.value = ChangePasswordState.Editing()
    }

    fun dismissChangePassword() {
        _changePasswordState.value = ChangePasswordState.Closed
    }

    fun onOldPasswordChange(value: String) {
        _changePasswordState.updateEditing { it.copy(oldPassword = value) }
    }

    fun onNewPasswordChange(value: String) {
        _changePasswordState.updateEditing { it.copy(newPassword = value) }
    }

    fun onConfirmPasswordChange(value: String) {
        _changePasswordState.updateEditing { it.copy(confirmPassword = value) }
    }

    private fun MutableStateFlow<ChangePasswordState>.updateEditing(transform: (ChangePasswordForm) -> ChangePasswordForm) {
        val current = value as? ChangePasswordState.Editing ?: return
        value = current.copy(form = transform(current.form))
    }

    fun submitChangePassword() {
        val state = _changePasswordState.value as? ChangePasswordState.Editing ?: return
        if (state.submitting) return
        val form = state.form
        val error =
            when {
                form.oldPassword.isBlank() -> R.string.settings_change_pwd_old_required
                form.newPassword.length < 8 -> R.string.settings_change_pwd_too_short
                form.newPassword != form.confirmPassword -> R.string.settings_change_pwd_mismatch
                else -> null
            }
        if (error != null) {
            _tip.value = SubmitTip(messageRes = error)
            return
        }
        _changePasswordState.value = state.copy(submitting = true)
        viewModelScope.launch {
            authRepository
                .changePassword(form.oldPassword, form.newPassword)
                .onSuccess {
                    _changePasswordState.value = ChangePasswordState.Closed
                    _tip.value = SubmitTip(messageRes = R.string.settings_change_pwd_success)
                }.onFailure {
                    val editing = _changePasswordState.value as? ChangePasswordState.Editing
                    if (editing != null) {
                        _changePasswordState.value = editing.copy(submitting = false)
                    }
                    _tip.value = SubmitTip(messageRes = R.string.settings_change_pwd_failed)
                }
        }
    }

    fun clearTip() {
        _tip.value = null
    }
}
