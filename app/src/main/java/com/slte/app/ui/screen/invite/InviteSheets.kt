package com.slte.app.ui.screen.invite
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.slte.app.R
import com.slte.app.ui.component.SlteInput
import com.slte.app.ui.component.SlteInputSize
import com.slte.app.ui.component.SlteSubmitSheet
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSheet(
    availableBalance: Int,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    SlteSubmitSheet(
        title = stringResource(R.string.invite_transfer_title),
        subtitle = stringResource(R.string.invite_transfer_subtitle, stringResource(R.string.app_name)),
        submitText = stringResource(R.string.invite_transfer_confirm),
        submitting = isSubmitting,
        submitEnabled = amountText.toDoubleOrNull()?.let { it > 0 } == true,
        onSubmit = { amountText.toDoubleOrNull()?.let { onConfirm(it) } },
        onDismiss = onDismiss,
    ) {
        ReadOnlyAmountField(cents = availableBalance)

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        SlteInput(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = stringResource(R.string.invite_transfer_amount_hint),
            icon = SlteIcons.Amount,
            keyboardType = KeyboardType.Decimal,
            size = SlteInputSize.Compact,
        )
    }
}
