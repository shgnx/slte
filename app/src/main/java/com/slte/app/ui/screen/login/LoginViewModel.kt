package com.slte.app.ui.screen.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.repository.AuthRepository
import com.slte.app.domain.model.RegisterConfig
import com.slte.app.domain.model.SessionState
import com.slte.app.domain.model.User
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data class Form(
        val account: String = "",
        val password: String = "",
        val passwordVisible: Boolean = false,
        val rememberMe: Boolean = false,
    ) : LoginUiState

    data class LoggingIn(
        val form: Form,
    ) : LoginUiState

    data class CheckingRegisterConfig(
        val form: Form,
    ) : LoginUiState

    data class LoginSuccess(
        val form: Form,
        val user: User,
    ) : LoginUiState

    data class RegisterConfigReady(
        val form: Form,
        val config: RegisterConfig,
    ) : LoginUiState

    data class Error(
        val form: Form,
        val messageRes: Int,
    ) : LoginUiState
}

@HiltViewModel
class LoginViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Form())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var loginJob: Job? = null
    private var registerConfigJob: Job? = null

    private fun currentForm() = when (val s = _uiState.value) {
        is LoginUiState.Form -> s
        is LoginUiState.LoggingIn -> s.form
        is LoginUiState.CheckingRegisterConfig -> s.form
        is LoginUiState.LoginSuccess -> s.form
        is LoginUiState.RegisterConfigReady -> s.form
        is LoginUiState.Error -> s.form
    }

    private val isLoading: Boolean
        get() =
            _uiState.value is LoginUiState.LoggingIn ||
                _uiState.value is LoginUiState.CheckingRegisterConfig

    /** 启动时检查是否有已保存的账号，有则自动填入邮箱和密码。 */
    private fun loadSavedCredentials() {
        val savedEmail = authRepository.savedEmail()
        if (savedEmail != null) {
            val f = currentForm()
            _uiState.value =
                LoginUiState.Form(
                    account = savedEmail,
                    password = authRepository.savedPassword() ?: "",
                    passwordVisible = f.passwordVisible,
                    rememberMe = true,
                )
        }
    }

    init {
        loadSavedCredentials()
        // 登出/会话切换后重新读取保存的凭证
        viewModelScope.launch {
            authRepository.sessionState.collect { state ->
                if (state is SessionState.LoggedOut) {
                    loadSavedCredentials()
                }
            }
        }
    }

    fun onAccountChange(value: String) {
        val f = currentForm()
        _uiState.value = f.copy(account = value)
    }

    fun onPasswordChange(value: String) {
        val f = currentForm()
        _uiState.value = f.copy(password = value)
    }

    fun togglePasswordVisible() {
        val f = currentForm()
        _uiState.value = f.copy(passwordVisible = !f.passwordVisible)
    }

    fun toggleRememberMe() {
        val f = currentForm()
        val newValue = !f.rememberMe
        if (!newValue) {
            // 用户手动取消记住密码时清除已保存的凭证
            authRepository.clearCredentials()
        }
        _uiState.value = f.copy(rememberMe = newValue)
    }

    fun dismissError() {
        val f = currentForm()
        _uiState.value =
            LoginUiState.Form(
                account = f.account,
                password = f.password,
                passwordVisible = f.passwordVisible,
                rememberMe = f.rememberMe,
            )
    }

    fun login() {
        if (isLoading) return
        val f = currentForm()
        if (f.account.isBlank()) {
            _uiState.value = LoginUiState.Error(f, R.string.error_email_required)
            return
        }
        if (f.password.isBlank()) {
            _uiState.value = LoginUiState.Error(f, R.string.error_password_required)
            return
        }

        _uiState.value = LoginUiState.LoggingIn(f)
        loginJob?.cancel()
        loginJob =
            viewModelScope.launch {
                val f2 = currentForm()
                val result = authRepository.login(f2.account.trim(), f2.password)
                result.fold(
                    onSuccess = { user ->
                        // 记住当前账号（覆盖上一个），不勾选则清除
                        if (f2.rememberMe) {
                            authRepository.saveCredentials(f2.account.trim(), f2.password)
                        } else {
                            authRepository.clearCredentials()
                        }
                        _uiState.value = LoginUiState.LoginSuccess(f2, user)
                    },
                    onFailure = { e ->
                        val f3 = currentForm()
                        val resId =
                            ErrorMessages.forLogin(e)
                        _uiState.value = LoginUiState.Error(f3, resId)
                    },
                )
            }
    }

    /**
     * 点击"创建账号"时调用：向后端请求注册配置（验证码/邀请码是否启用），
     * 加载期间按钮显示 loading 动画，完成后通过 registerConfig 驱动导航。
     *
     * 每次点击都会重新请求，确保二次返回后的配置是最新的。
     */
    fun checkRegisterConfig() {
        if (isLoading) return
        val f = currentForm()
        _uiState.value = LoginUiState.CheckingRegisterConfig(f)
        registerConfigJob?.cancel()
        registerConfigJob =
            viewModelScope.launch {
                val f2 = currentForm()
                val result = authRepository.fetchRegisterConfig()
                result.fold(
                    onSuccess = { config ->
                        _uiState.value = LoginUiState.RegisterConfigReady(f2, config)
                    },
                    onFailure = { e ->
                        val f3 = currentForm()
                        val resId =
                            ErrorMessages.forRegister(e)
                        _uiState.value = LoginUiState.Error(f3, resId)
                    },
                )
            }
    }

    fun onNavigatedToRegister() {
        val f = currentForm()
        _uiState.value =
            LoginUiState.Form(
                account = f.account,
                password = f.password,
                passwordVisible = f.passwordVisible,
                rememberMe = f.rememberMe,
            )
    }

    fun onNavigatedToLoginSuccess() {
        val f = currentForm()
        _uiState.value =
            LoginUiState.Form(
                account = f.account,
                password = f.password,
                passwordVisible = f.passwordVisible,
                rememberMe = f.rememberMe,
            )
    }

    /** 取消全屏加载（系统返回/点击遮罩）：取消在途请求并复位表单 */
    fun cancelLoading() {
        loginJob?.cancel()
        registerConfigJob?.cancel()
        val f = currentForm()
        _uiState.value =
            LoginUiState.Form(
                account = f.account,
                password = f.password,
                passwordVisible = f.passwordVisible,
                rememberMe = f.rememberMe,
            )
    }
}
