package com.slte.app.ui.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.slte.app.R
import com.slte.app.ui.component.SlteButton
import com.slte.app.ui.component.SlteButtonStyle
import com.slte.app.ui.component.SlteInput
import com.slte.app.ui.component.SlteInputSize
import com.slte.app.ui.component.SlteSheet
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.utils.Dimens

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
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    SlteSheet(
        title = stringResource(R.string.settings_change_password),
        onDismiss = { if (!state.submitting) onDismiss() },
    ) {
        SlteInput(
            value = state.oldPassword,
            onValueChange = onOldPasswordChange,
            placeholder = stringResource(R.string.settings_change_pwd_old_hint),
            icon = SlteIcons.Password,
            trailing = {
                IconButton(onClick = onToggleOldVisible) {
                    Icon(
                        imageVector =
                        if (state.oldPasswordVisible) {
                            SlteIcons.VisibilityOff
                        } else {
                            SlteIcons.VisibilityOn
                        },
                        contentDescription = stringResource(R.string.settings_toggle_password_visible),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            visualTransformation =
            if (state.oldPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardType = KeyboardType.Password,
            enabled = !state.submitting,
            size = SlteInputSize.Compact,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        SlteInput(
            value = state.newPassword,
            onValueChange = onNewPasswordChange,
            placeholder = stringResource(R.string.settings_change_pwd_new_hint),
            icon = SlteIcons.Password,
            trailing = {
                IconButton(onClick = onToggleNewVisible) {
                    Icon(
                        imageVector =
                        if (state.newPasswordVisible) {
                            SlteIcons.VisibilityOff
                        } else {
                            SlteIcons.VisibilityOn
                        },
                        contentDescription = stringResource(R.string.settings_toggle_password_visible),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            visualTransformation =
            if (state.newPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardType = KeyboardType.Password,
            enabled = !state.submitting,
            size = SlteInputSize.Compact,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        SlteInput(
            value = state.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = stringResource(R.string.settings_change_pwd_confirm_hint),
            icon = SlteIcons.Password,
            visualTransformation =
            if (state.newPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardType = KeyboardType.Password,
            enabled = !state.submitting,
            size = SlteInputSize.Compact,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.lg))

        SlteButton(
            text = stringResource(R.string.settings_change_pwd_submit),
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            style = SlteButtonStyle.Primary,
            loading = state.submitting,
        )
    }
}
