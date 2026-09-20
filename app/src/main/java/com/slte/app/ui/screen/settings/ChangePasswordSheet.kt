package com.slte.app.ui.screen.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import com.slte.app.R
import com.slte.app.ui.component.SlteInputSize
import com.slte.app.ui.component.SltePasswordInput
import com.slte.app.ui.component.SlteSubmitSheet
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.utils.Dimens

@Composable
fun ChangePasswordSheet(
    state: ChangePasswordState.Editing,
    onOldPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    SlteSubmitSheet(
        title = stringResource(R.string.settings_change_password),
        submitText = stringResource(R.string.settings_change_pwd_submit),
        submitting = state.submitting,
        onSubmit = onSubmit,
        onDismiss = onDismiss,
    ) {
        SltePasswordInput(
            value = state.form.oldPassword,
            onValueChange = onOldPasswordChange,
            placeholder = stringResource(R.string.settings_change_pwd_old_hint),
            icon = SlteIcons.Password,
            imeAction = ImeAction.Next,
            enabled = !state.submitting,
            size = SlteInputSize.Compact,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        SltePasswordInput(
            value = state.form.newPassword,
            onValueChange = onNewPasswordChange,
            placeholder = stringResource(R.string.settings_change_pwd_new_hint),
            icon = SlteIcons.Password,
            imeAction = ImeAction.Next,
            enabled = !state.submitting,
            size = SlteInputSize.Compact,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        SltePasswordInput(
            value = state.form.confirmPassword,
            onValueChange = onConfirmPasswordChange,
            placeholder = stringResource(R.string.settings_change_pwd_confirm_hint),
            icon = SlteIcons.Password,
            enabled = !state.submitting,
            size = SlteInputSize.Compact,
        )
    }
}
