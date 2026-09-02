package com.slte.app.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.slte.app.R
import com.slte.app.ui.component.AppLocaleContent
import com.slte.app.ui.component.LocalAppLocale
import com.slte.app.ui.component.SlteTextField
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens

/** 修改密码弹窗 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordSheet(
    state: ChangePasswordState,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onToggleOldVisible: () -> Unit,
    onToggleNewVisible: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = {
            if (!state.submitting) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AppLocaleContent(locale = LocalAppLocale.current) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(
                        horizontal = Dimens.inviteSheetPaddingH,
                        vertical = Dimens.inviteSheetPaddingV
                    )
            ) {
            Text(
                text = stringResource(R.string.settings_change_password),
                fontSize = TextSizes.sheetTitle,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            SlteTextField(
                value = state.oldPassword,
                onValueChange = onOldPasswordChange,
                placeholder = stringResource(R.string.settings_change_pwd_old_hint),
                leadingIcon = Icons.Outlined.Lock,
                trailingIcon = {
                    IconButton(onClick = onToggleOldVisible) {
                        Icon(
                            imageVector = if (state.oldPasswordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = stringResource(R.string.settings_toggle_password_visible),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (state.oldPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardType = KeyboardType.Password,
                enabled = !state.submitting
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            SlteTextField(
                value = state.newPassword,
                onValueChange = onNewPasswordChange,
                placeholder = stringResource(R.string.settings_change_pwd_new_hint),
                leadingIcon = Icons.Outlined.Lock,
                trailingIcon = {
                    IconButton(onClick = onToggleNewVisible) {
                        Icon(
                            imageVector = if (state.newPasswordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = stringResource(R.string.settings_toggle_password_visible),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                visualTransformation = if (state.newPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardType = KeyboardType.Password,
                enabled = !state.submitting
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            SlteTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                placeholder = stringResource(R.string.settings_change_pwd_confirm_hint),
                leadingIcon = Icons.Outlined.Lock,
                visualTransformation = if (state.newPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardType = KeyboardType.Password,
                enabled = !state.submitting
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSubmit()
                },
                enabled = !state.submitting,
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                shape = SlteShapes.medium,
            ) {
                if (state.submitting) {
                    LottieLoadingIcon(modifier = Modifier.size(Dimens.loadingAnimSize))
                } else {
                    Text(stringResource(R.string.settings_change_pwd_submit))
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingLg))
            }
        }
    }
}
