package com.slte.app.ui.screen.forgot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.ui.component.AnimatedSticker
import com.slte.app.ui.component.LoadingOverlay
import com.slte.app.ui.component.SlteButton
import com.slte.app.ui.component.SlteButtonStyle
import com.slte.app.ui.component.SlteInput
import com.slte.app.ui.component.ToastTip
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.Stickers

/**
 * 忘记密码页：邮箱 → 验证码 → 新密码 → 重置；验证码按钮固定宽度，发送按钮与输入框等高。
 */
@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val form =
        when (val s = state) {
            is ForgotPasswordUiState.Form -> s
            is ForgotPasswordUiState.SendingCode -> s.form
            is ForgotPasswordUiState.Countdown -> s.form
            is ForgotPasswordUiState.Resetting -> s.form
            is ForgotPasswordUiState.ResetSuccess -> s.form
            is ForgotPasswordUiState.Error -> s.form
        }

    val isSendingCode = state is ForgotPasswordUiState.SendingCode
    val isResetting = state is ForgotPasswordUiState.Resetting
    val isCountingDown = state is ForgotPasswordUiState.Countdown
    val countdownSeconds = (state as? ForgotPasswordUiState.Countdown)?.seconds ?: 0
    val errorMessageRes = (state as? ForgotPasswordUiState.Error)?.messageRes
    val isLoading = isSendingCode

    LaunchedEffect(state is ForgotPasswordUiState.ResetSuccess) {
        if (state is ForgotPasswordUiState.ResetSuccess) {
            onResetSuccess()
        }
    }

    Box(
        modifier =
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = Dimens.gap.xxl,
                vertical = Dimens.gap.xxl,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = Dimens.maxContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(Dimens.gap.xxl))

            AnimatedSticker(
                assetPath = Stickers.FORGOT_PASSWORD,
                modifier = Modifier.size(Dimens.logoSize),
            )

            Spacer(modifier = Modifier.height(Dimens.gap.xl))

            Text(
                text = stringResource(R.string.forgot_title),
                style = SlteType.pageTitle,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(modifier = Modifier.height(Dimens.gap.xxl))

            SlteInput(
                value = form.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = stringResource(R.string.error_email_required),
                modifier = Modifier.fillMaxWidth(),
                icon = SlteIcons.Account,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                enabled = !isResetting,
                bordered = false,
            )

            Spacer(modifier = Modifier.height(Dimens.gap.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.gap.md),
            ) {
                SlteInput(
                    value = form.verificationCode,
                    onValueChange = viewModel::onCodeChange,
                    placeholder = stringResource(R.string.error_code_required),
                    modifier = Modifier.weight(1f),
                    icon = SlteIcons.VerificationCode,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    enabled = !isResetting,
                    bordered = false,
                )
                SlteButton(
                    text =
                    if (isCountingDown) {
                        stringResource(R.string.format_countdown_s, countdownSeconds)
                    } else {
                        stringResource(R.string.register_send_code)
                    },
                    onClick = viewModel::sendVerificationCode,
                    modifier = Modifier.width(Dimens.sendCodeButtonWidth),
                    style = SlteButtonStyle.Tonal,
                    enabled = !isCountingDown,
                    loading = isLoading,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.gap.md))

            SlteInput(
                value = form.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                placeholder = stringResource(R.string.error_new_password_required),
                modifier = Modifier.fillMaxWidth(),
                icon = SlteIcons.Password,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                enabled = !isResetting,
                visualTransformation =
                if (form.passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailing = {
                    IconButton(onClick = viewModel::togglePasswordVisible) {
                        Icon(
                            imageVector =
                            if (form.passwordVisible) {
                                SlteIcons.VisibilityOff
                            } else {
                                SlteIcons.VisibilityOn
                            },
                            contentDescription = stringResource(R.string.login_toggle_password),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                bordered = false,
            )

            Spacer(modifier = Modifier.height(Dimens.gap.lg))
            SlteButton(
                text = stringResource(R.string.forgot_reset_button),
                onClick = viewModel::resetPassword,
                modifier = Modifier.fillMaxWidth(),
                style = SlteButtonStyle.Primary,
                loading = isResetting,
            )

            Spacer(modifier = Modifier.height(Dimens.gap.md))

            TextButton(onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onBackToLogin()
            }) {
                Text(stringResource(R.string.register_back_to_login))
            }
        }
    }

    LoadingOverlay(visible = isResetting, onDismiss = viewModel::cancelLoading)

    ToastTip(
        message = errorMessageRes?.let { stringResource(it) },
        onDismiss = viewModel::dismissError,
    )
}
