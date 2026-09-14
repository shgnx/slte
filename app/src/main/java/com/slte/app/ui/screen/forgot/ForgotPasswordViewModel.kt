package com.slte.app.ui.screen.forgot

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

sealed interface ForgotPasswordUiState {
    data class Form(
        val email: String = "",
        val verificationCode: String = "",
        val newPassword: String = "",
        val passwordVisible: Boolean = false,
    ) : ForgotPasswordUiState

    data class SendingCode(
        val form: Form,
    ) : ForgotPasswordUiState

    data class Countdown(
        val form: Form,
        val seconds: Int,
    ) : ForgotPasswordUiState

    data class Resetting(
        val form: Form,
    ) : ForgotPasswordUiState

    data class ResetSuccess(
        val form: Form,
    ) : ForgotPasswordUiState

    data class Error(
        val form: Form,
        val messageRes: Int,
    ) : ForgotPasswordUiState
}

@HiltViewModel
class ForgotPasswordViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val countdownUseCase: CountdownUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Form())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var resetJob: Job? = null

    private fun currentForm() = when (val s = _uiState.value) {
        is ForgotPasswordUiState.Form -> s
        is ForgotPasswordUiState.SendingCode -> s.form
        is ForgotPasswordUiState.Countdown -> s.form
        is ForgotPasswordUiState.Resetting -> s.form
        is ForgotPasswordUiState.ResetSuccess -> s.form
        is ForgotPasswordUiState.Error -> s.form
    }

    private val isLoadingOrResetting: Boolean
        get() =
            _uiState.value is ForgotPasswordUiState.SendingCode ||
                _uiState.value is ForgotPasswordUiState.Resetting

    private val isCountingDown: Boolean
        get() = _uiState.value is ForgotPasswordUiState.Countdown

    /** 更新表单字段时保留当前状态类型（倒计时/错误等不被输入覆盖） */
    private fun updateForm(transform: (ForgotPasswordUiState.Form) -> ForgotPasswordUiState.Form) {
        when (val s = _uiState.value) {
            is ForgotPasswordUiState.Form -> _uiState.value = transform(s)
            is ForgotPasswordUiState.SendingCode -> _uiState.value = ForgotPasswordUiState.SendingCode(transform(s.form))
            is ForgotPasswordUiState.Countdown -> _uiState.value = ForgotPasswordUiState.Countdown(transform(s.form), s.seconds)
            is ForgotPasswordUiState.Resetting -> _uiState.value = ForgotPasswordUiState.Resetting(transform(s.form))
            is ForgotPasswordUiState.ResetSuccess -> _uiState.value = ForgotPasswordUiState.ResetSuccess(transform(s.form))
            is ForgotPasswordUiState.Error -> _uiState.value = ForgotPasswordUiState.Error(transform(s.form), s.messageRes)
        }
    }

    fun onEmailChange(email: String) = updateForm { it.copy(email = email) }

    fun onCodeChange(code: String) = updateForm { it.copy(verificationCode = code) }

    fun onNewPasswordChange(password: String) = updateForm { it.copy(newPassword = password) }

    fun togglePasswordVisible() = updateForm { it.copy(passwordVisible = !it.passwordVisible) }

    fun dismissError() {
        val f = currentForm()
        _uiState.value =
            ForgotPasswordUiState.Form(
                email = f.email,
                verificationCode = f.verificationCode,
                newPassword = f.newPassword,
                passwordVisible = f.passwordVisible,
            )
    }

    fun sendVerificationCode() {
        if (isLoadingOrResetting || isCountingDown) return
        val f = currentForm()
        if (f.email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error(f, R.string.error_email_required)
            return
        }

        _uiState.value = ForgotPasswordUiState.SendingCode(f)

        viewModelScope.launch {
            val result = authRepository.sendEmailCode(f.email, EmailCodePurpose.FORGOT_PASSWORD)
            result.fold(
                onSuccess = { startCountdown() },
                onFailure = { e ->
                    val f2 = currentForm()
                    val resId =
                        ErrorMessages.forSendCode(e)
                    _uiState.value = ForgotPasswordUiState.Error(f2, resId)
                },
            )
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        val f = currentForm()
        _uiState.value = ForgotPasswordUiState.Countdown(f, 0)

        countdownJob =
            viewModelScope.launch {
                countdownUseCase().collect { seconds ->
                    val f2 = currentForm()
                    _uiState.value = ForgotPasswordUiState.Countdown(f2, seconds)
                }
            }
    }

    fun resetPassword() {
        if (isLoadingOrResetting) return
        val f = currentForm()
        if (f.email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error(f, R.string.error_email_required)
            return
        }
        if (f.verificationCode.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error(f, R.string.error_code_required)
            return
        }
        if (f.newPassword.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error(f, R.string.error_new_password_required)
            return
        }

        _uiState.value = ForgotPasswordUiState.Resetting(f)

        resetJob?.cancel()
        resetJob =
            viewModelScope.launch {
                val f2 = currentForm()
                val result = authRepository.forgotPassword(f2.email, f2.verificationCode, f2.newPassword)
                result.fold(
                    onSuccess = { _uiState.value = ForgotPasswordUiState.ResetSuccess(f2) },
                    onFailure = { e ->
                        val f3 = currentForm()
                        val resId =
                            ErrorMessages.forForgot(e)
                        _uiState.value = ForgotPasswordUiState.Error(f3, resId)
                    },
                )
            }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        resetJob?.cancel()
    }

    /** 取消全屏加载（系统返回/点击遮罩）：取消在途请求与倒计时，复位表单 */
    fun cancelLoading() {
        resetJob?.cancel()
        countdownJob?.cancel()
        val f = currentForm()
        _uiState.value =
            ForgotPasswordUiState.Form(
                email = f.email,
                verificationCode = f.verificationCode,
                newPassword = f.newPassword,
                passwordVisible = f.passwordVisible,
            )
    }
}
