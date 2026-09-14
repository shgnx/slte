package com.slte.app.ui.screen.register

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
 * 注册页：邮箱 + 密码 + 验证码（可选）+ 邀请码（可选）→ 注册。
 *
 * 验证码和邀请码的启用/必填由登录页加载配置后注入，
 * 注册页本身不发起网络请求获取配置。
 */
@Composable
fun RegisterScreen(
    emailVerifyEnabled: Boolean,
    inviteForceEnabled: Boolean,
    onBackToLogin: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    LaunchedEffect(emailVerifyEnabled, inviteForceEnabled) {
        viewModel.initConfig(emailVerifyEnabled, inviteForceEnabled)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    val form =
        when (val s = state) {
            is RegisterUiState.Form -> s
            is RegisterUiState.SendingCode -> s.form
            is RegisterUiState.Countdown -> s.form
            is RegisterUiState.Registering -> s.form
            is RegisterUiState.RegisterSuccess -> s.form
            is RegisterUiState.Error -> s.form
        }

    val isSendingCode = state is RegisterUiState.SendingCode
    val isRegistering = state is RegisterUiState.Registering
    val countdownSeconds = (state as? RegisterUiState.Countdown)?.seconds ?: 0
    val errorMessageRes = (state as? RegisterUiState.Error)?.messageRes
    val isCountingDown = state is RegisterUiState.Countdown
    val isLoading = isSendingCode

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
                assetPath = Stickers.REGISTER,
                modifier = Modifier.size(Dimens.logoSize),
            )

            Spacer(modifier = Modifier.height(Dimens.gap.xl))

            Text(
                text = stringResource(R.string.register_title),
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
                enabled = !isRegistering,
                bordered = false,
            )

            Spacer(modifier = Modifier.height(Dimens.gap.md))

            if (emailVerifyEnabled) {
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
                        enabled = !isRegistering,
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
            }

            SlteInput(
                value = form.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = stringResource(R.string.error_password_required),
                modifier = Modifier.fillMaxWidth(),
                icon = SlteIcons.Password,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                enabled = !isRegistering,
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

            Spacer(modifier = Modifier.height(Dimens.gap.md))

            SlteInput(
                value = form.inviteCode,
                onValueChange = viewModel::onInviteCodeChange,
                placeholder =
                if (inviteForceEnabled) {
                    stringResource(R.string.register_invite_hint)
                } else {
                    stringResource(R.string.register_invite_optional)
                },
                modifier = Modifier.fillMaxWidth(),
                icon = SlteIcons.InviteCode,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                enabled = !isRegistering,
                bordered = false,
            )

            Spacer(modifier = Modifier.height(Dimens.gap.lg))
            SlteButton(
                text = stringResource(R.string.register_button),
                onClick = viewModel::register,
                modifier = Modifier.fillMaxWidth(),
                style = SlteButtonStyle.Primary,
                loading = isRegistering,
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

    LoadingOverlay(visible = isRegistering, onDismiss = viewModel::cancelLoading)

    ToastTip(
        message = errorMessageRes?.let { stringResource(it) },
        onDismiss = viewModel::dismissError,
    )
}
