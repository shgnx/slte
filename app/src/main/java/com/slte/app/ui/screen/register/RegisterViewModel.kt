package com.slte.app.ui.screen.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.repository.AuthRepository
import com.slte.app.domain.model.EmailCodePurpose
import com.slte.app.domain.usecase.CountdownUseCase
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RegisterUiState {
    data class Form(
        val email: String = "",
        val password: String = "",
        val passwordVisible: Boolean = false,
        val verificationCode: String = "",
        val inviteCode: String = "",
        val emailVerifyEnabled: Boolean = false,
        val inviteForceEnabled: Boolean = false,
    ) : RegisterUiState

    data class SendingCode(
        val form: Form,
    ) : RegisterUiState

    data class Countdown(
        val form: Form,
        val seconds: Int,
    ) : RegisterUiState

    data class Registering(
        val form: Form,
    ) : RegisterUiState

    data class RegisterSuccess(
        val form: Form,
    ) : RegisterUiState

    data class Error(
        val form: Form,
        val messageRes: Int,
    ) : RegisterUiState
}

/** 注册配置由登录页加载后通过 initConfig() 注入。 */
@HiltViewModel
class RegisterViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val countdownUseCase: CountdownUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Form())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var registerJob: Job? = null

    private fun currentForm() = when (val s = _uiState.value) {
        is RegisterUiState.Form -> s
        is RegisterUiState.SendingCode -> s.form
        is RegisterUiState.Countdown -> s.form
        is RegisterUiState.Registering -> s.form
        is RegisterUiState.RegisterSuccess -> s.form
        is RegisterUiState.Error -> s.form
    }

    private val isLoadingOrRegistering: Boolean
        get() =
            _uiState.value is RegisterUiState.SendingCode ||
                _uiState.value is RegisterUiState.Registering

    private val isCountingDown: Boolean
        get() = _uiState.value is RegisterUiState.Countdown

    /**
     * 由 RegisterScreen 在组合时调用，注入登录页已加载好的配置。
     * 注册页本身不发起网络请求获取配置。
     */
    fun initConfig(
        emailVerifyEnabled: Boolean,
        inviteForceEnabled: Boolean,
    ) {
        val f = currentForm()
        if (f.emailVerifyEnabled == emailVerifyEnabled && f.inviteForceEnabled == inviteForceEnabled) return
        _uiState.value =
            RegisterUiState.Form(
                email = f.email,
                password = f.password,
                passwordVisible = f.passwordVisible,
                verificationCode = f.verificationCode,
                inviteCode = f.inviteCode,
                emailVerifyEnabled = emailVerifyEnabled,
                inviteForceEnabled = inviteForceEnabled,
            )
    }

    /** 更新表单字段时保留当前状态类型（倒计时/错误等不被输入覆盖） */
    private fun updateForm(transform: (RegisterUiState.Form) -> RegisterUiState.Form) {
        when (val s = _uiState.value) {
            is RegisterUiState.Form -> _uiState.value = transform(s)
            is RegisterUiState.SendingCode -> _uiState.value = RegisterUiState.SendingCode(transform(s.form))
            is RegisterUiState.Countdown -> _uiState.value = RegisterUiState.Countdown(transform(s.form), s.seconds)
            is RegisterUiState.Registering -> _uiState.value = RegisterUiState.Registering(transform(s.form))
            is RegisterUiState.RegisterSuccess -> _uiState.value = RegisterUiState.RegisterSuccess(transform(s.form))
            is RegisterUiState.Error -> _uiState.value = RegisterUiState.Error(transform(s.form), s.messageRes)
        }
    }

    fun onEmailChange(email: String) = updateForm { it.copy(email = email) }

    fun onPasswordChange(password: String) = updateForm { it.copy(password = password) }

    fun togglePasswordVisible() = updateForm { it.copy(passwordVisible = !it.passwordVisible) }

    fun onCodeChange(code: String) = updateForm { it.copy(verificationCode = code) }

    fun onInviteCodeChange(code: String) = updateForm { it.copy(inviteCode = code) }

    fun dismissError() {
        val f = currentForm()
        _uiState.value =
            RegisterUiState.Form(
                email = f.email,
                password = f.password,
                passwordVisible = f.passwordVisible,
                verificationCode = f.verificationCode,
                inviteCode = f.inviteCode,
                emailVerifyEnabled = f.emailVerifyEnabled,
                inviteForceEnabled = f.inviteForceEnabled,
            )
    }

    fun sendVerificationCode() {
        if (isLoadingOrRegistering || isCountingDown) return
        val f = currentForm()
        if (f.email.isBlank()) {
            _uiState.value = RegisterUiState.Error(f, R.string.error_email_required)
            return
        }

        _uiState.value = RegisterUiState.SendingCode(f)

        viewModelScope.launch {
            val result = authRepository.sendEmailCode(f.email, EmailCodePurpose.REGISTER)
            result.fold(
                onSuccess = { startCountdown() },
                onFailure = { e ->
                    val f2 = currentForm()
                    val resId =
                        ErrorMessages.forSendCode(e)
                    _uiState.value = RegisterUiState.Error(f2, resId)
                },
            )
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        val f = currentForm()
        _uiState.value = RegisterUiState.Countdown(f, 0)

        countdownJob =
            viewModelScope.launch {
                countdownUseCase().collect { seconds ->
                    val f2 = currentForm()
                    _uiState.value = RegisterUiState.Countdown(f2, seconds)
                }
            }
    }

    fun register() {
        if (isLoadingOrRegistering) return
        val f = currentForm()
        if (f.email.isBlank()) {
            _uiState.value = RegisterUiState.Error(f, R.string.error_email_required)
            return
        }
        if (f.password.isBlank()) {
            _uiState.value = RegisterUiState.Error(f, R.string.error_password_required)
            return
        }
        if (f.emailVerifyEnabled && f.verificationCode.isBlank()) {
            _uiState.value = RegisterUiState.Error(f, R.string.error_code_required)
            return
        }
        if (f.inviteForceEnabled && f.inviteCode.isBlank()) {
            _uiState.value = RegisterUiState.Error(f, R.string.error_invite_required)
            return
        }

        _uiState.value = RegisterUiState.Registering(f)

        registerJob?.cancel()
        registerJob =
            viewModelScope.launch {
                val f2 = currentForm()
                val result =
                    authRepository.register(
                        email = f2.email,
                        password = f2.password,
                        emailCode = if (f2.emailVerifyEnabled) f2.verificationCode else null,
                        inviteCode = if (f2.inviteForceEnabled || f2.inviteCode.isNotBlank()) f2.inviteCode else null,
                    )
                result.fold(
                    onSuccess = { _uiState.value = RegisterUiState.RegisterSuccess(f2) },
                    onFailure = { e ->
                        val f3 = currentForm()
                        val resId =
                            ErrorMessages.forRegister(e)
                        _uiState.value = RegisterUiState.Error(f3, resId)
                    },
                )
            }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        registerJob?.cancel()
    }

    /** 取消全屏加载（系统返回/点击遮罩）：取消在途请求与倒计时，复位表单 */
    fun cancelLoading() {
        registerJob?.cancel()
        countdownJob?.cancel()
        val f = currentForm()
        _uiState.value =
            RegisterUiState.Form(
                email = f.email,
                password = f.password,
                passwordVisible = f.passwordVisible,
                verificationCode = f.verificationCode,
                inviteCode = f.inviteCode,
                emailVerifyEnabled = f.emailVerifyEnabled,
                inviteForceEnabled = f.inviteForceEnabled,
            )
    }
}
