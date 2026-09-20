package com.slte.app.ui.screen.invite
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.slte.app.R
import com.slte.app.ui.component.SlteInput
import com.slte.app.ui.component.SlteInputSize
import com.slte.app.ui.component.SlteSubmitSheet
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSheet(
    methodsState: WithdrawMethodsState,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onRetryMethods: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMethod by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val methods = (methodsState as? WithdrawMethodsState.Ready)?.methods.orEmpty()
    val isLoadingMethods = methodsState is WithdrawMethodsState.Loading
    val methodsFailed = methodsState is WithdrawMethodsState.Failed

    LaunchedEffect(methods) {
        if (selectedMethod !in methods) {
            selectedMethod = methods.firstOrNull().orEmpty()
        }
    }

    SlteSubmitSheet(
        title = stringResource(R.string.invite_withdraw_title),
        subtitle = stringResource(R.string.invite_withdraw_subtitle),
        submitText = stringResource(R.string.invite_withdraw_confirm),
        submitting = isSubmitting,
        submitEnabled = selectedMethod.isNotBlank() && account.isNotBlank(),
        onSubmit = { onConfirm(selectedMethod, account) },
        onDismiss = onDismiss,
    ) {
        WithdrawMethodField(
            methods = methods,
            selected = selectedMethod,
            isLoading = isLoadingMethods,
            failed = methodsFailed,
            onRetry = onRetryMethods,
            onSelect = { method ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedMethod = method
            },
        )

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        SlteInput(
            value = account,
            onValueChange = { input ->
                account = input.filter { it.isLetterOrDigit() || it in "@.-_+" }.take(100)
            },
            placeholder = stringResource(R.string.invite_withdraw_account_hint),
            icon = SlteIcons.AtSign,
            iconDesc = stringResource(R.string.invite_withdraw_account_label),
            size = SlteInputSize.Compact,
        )
    }
}
