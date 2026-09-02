package com.slte.app.ui.screen.login

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import com.slte.app.ui.component.LoadingOverlay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.ui.component.AnimatedSticker
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.component.SlteTextField
import com.slte.app.ui.component.ToastTip
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.utils.Dimens
import com.slte.app.utils.Stickers

/**
 * 登录页：邮箱 + 密码登录 + 记住密码 + 忘记密码 + 创建账号入口。
 *
 * 点击"创建账号"先向后端请求注册配置（验证码/邀请码），
 * 按钮显示 loading 动画，完成后跳转注册页。
 */
@Composable
fun LoginScreen(
    onForgotPassword: () -> Unit = {},
    onCreateAccount: (emailVerify: Boolean, inviteForce: Boolean) -> Unit = { _, _ -> },
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    val form = when (val s = state) {
        is LoginUiState.Form -> s
        is LoginUiState.LoggingIn -> s.form
        is LoginUiState.CheckingRegisterConfig -> s.form
        is LoginUiState.LoginSuccess -> s.form
        is LoginUiState.RegisterConfigReady -> s.form
        is LoginUiState.Error -> s.form
    }

    val isLoading = state is LoginUiState.LoggingIn
    val isCheckingRegisterConfig = state is LoginUiState.CheckingRegisterConfig
    val errorMessageRes = (state as? LoginUiState.Error)?.messageRes

    LaunchedEffect(state is LoginUiState.LoginSuccess) {
        if (state is LoginUiState.LoginSuccess) {
            viewModel.onNavigatedToLoginSuccess()
        }
    }

    LaunchedEffect(state is LoginUiState.RegisterConfigReady) {
        val s = state
        if (s is LoginUiState.RegisterConfigReady) {
            viewModel.onNavigatedToRegister()
            onCreateAccount(s.config.emailVerifyEnabled, s.config.inviteForceEnabled)
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
                assetPath = Stickers.LOGIN,
                modifier = Modifier.size(Dimens.logoSize)
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            Text(
                text = stringResource(R.string.login_welcome),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXxl))

            SlteTextField(
                value = form.account,
                onValueChange = viewModel::onAccountChange,
                placeholder = stringResource(R.string.login_account_hint),
                leadingIcon = Icons.Outlined.Email,
                keyboardType = KeyboardType.Email,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            SlteTextField(
                value = form.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = stringResource(R.string.login_password_hint),
                leadingIcon = Icons.Outlined.Lock,
                trailingIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        viewModel.togglePasswordVisible()
                    }) {
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
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                enabled = !isLoading
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    viewModel.login()
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                shape = SlteShapes.medium,
            ) {
                if (isLoading) {
                    LottieLoadingIcon(modifier = Modifier.size(Dimens.loadingIndicatorSize))
                } else {
                    Text(stringResource(R.string.login_button))
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = form.rememberMe,
                        onCheckedChange = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            viewModel.toggleRememberMe()
                        },
                        enabled = !isLoading
                    )
                    Text(
                        text = stringResource(R.string.login_remember_me),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onForgotPassword()
                }) {
                    Text(stringResource(R.string.login_forgot_password))
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            OutlinedButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    viewModel.checkRegisterConfig()
                },
                enabled = !isCheckingRegisterConfig,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                shape = SlteShapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                border = BorderStroke(
                    width = Dimens.strokeMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.register_title))
            }
        }
    }

    LoadingOverlay(
        visible = isLoading || isCheckingRegisterConfig,
        onDismiss = viewModel::cancelLoading
    )

    ToastTip(
        message = errorMessageRes?.let { stringResource(it) },
        onDismiss = viewModel::dismissError
    )
}
