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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.ui.component.AnimatedSticker
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.component.LoadingOverlay
import com.slte.app.ui.component.ToastTip
import com.slte.app.ui.component.InputFieldColors
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.utils.Dimens
import com.slte.app.utils.Stickers

/**
 * 忘记密码页：邮箱 → 验证码 → 新密码 → 重置。
 *
 * 验证码按钮固定宽度，
 * 发送按钮与输入框等高。
 */
@Composable
fun ForgotPasswordScreen(
    onBackToLogin: () -> Unit,
    onResetSuccess: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val form = when (val s = state) {
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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(
                horizontal = Dimens.spacingXxl,
                vertical = Dimens.spacingXxl
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = Dimens.maxContentWidth),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            AnimatedSticker(
                assetPath = Stickers.FORGOT_PASSWORD,
                modifier = Modifier.size(Dimens.logoSize)
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            Text(
                text = stringResource(R.string.forgot_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            OutlinedTextField(
                value = form.email,
                onValueChange = viewModel::onEmailChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.error_email_required),
                        color = SlteColors.current.inputFieldPlaceholder
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = SlteShapes.medium,
                colors = InputFieldColors(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                enabled = !isResetting
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
            ) {
                OutlinedTextField(
                    value = form.verificationCode,
                    onValueChange = viewModel::onCodeChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = stringResource(R.string.error_code_required),
                            color = SlteColors.current.inputFieldPlaceholder
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Sms,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = SlteShapes.medium,
                    colors = InputFieldColors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !isResetting
                )

                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.sendVerificationCode()
                    },
                    modifier = Modifier
                        .width(Dimens.sendCodeButtonWidth)
                        .height(Dimens.fieldHeight),
                    shape = SlteShapes.medium,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isCountingDown
                ) {
                    if (isLoading) {
                        LottieLoadingIcon(modifier = Modifier.size(Dimens.loadingIndicatorSize))
                    } else if (isCountingDown) {
                        Text(
                            text = stringResource(R.string.format_countdown_s, countdownSeconds),
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Text(stringResource(R.string.register_send_code))
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            OutlinedTextField(
                value = form.newPassword,
                onValueChange = viewModel::onNewPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.error_new_password_required),
                        color = SlteColors.current.inputFieldPlaceholder
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisible) {
                        Icon(
                            imageVector = if (form.passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = stringResource(R.string.login_toggle_password),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (form.passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                singleLine = true,
                shape = SlteShapes.medium,
                colors = InputFieldColors(),
                enabled = !isResetting
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.resetPassword()
                },
                enabled = !isResetting,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                shape = SlteShapes.medium,
            ) {
                Text(stringResource(R.string.forgot_reset_button))
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

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
        onDismiss = viewModel::dismissError
    )
}
