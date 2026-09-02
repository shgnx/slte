package com.slte.app.ui.screen.plans

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.component.LocaleAwareAlertDialog


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfirmWarningDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    LocaleAwareAlertDialog(
        onDismissRequest = onCancel,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(R.string.purchase_warning_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(text = stringResource(R.string.purchase_warning_message))
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onCancel()
            }) {
                Text(stringResource(R.string.purchase_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onConfirm()
            }) {
                Text(stringResource(R.string.purchase_confirm))
            }
        }
    )
}



@Composable
internal fun ExistingOrderErrorDialog(
    errorMessageRes: Int,
    onGoToOrders: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    LocaleAwareAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(R.string.purchase_existing_order_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(text = stringResource(R.string.purchase_existing_order_message))
        },
        dismissButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onDismiss()
            }) {
                Text(stringResource(R.string.purchase_cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onGoToOrders()
            }) {
                Text(stringResource(R.string.order_pay))
            }
        }
    )
}


@Composable
internal fun OrderCreateErrorDialog(
    errorMessageRes: Int,
    onDismiss: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    LocaleAwareAlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = {
            Text(
                text = stringResource(R.string.purchase_error_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(text = stringResource(errorMessageRes))
        },
        confirmButton = {
            TextButton(onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onDismiss()
            }) {
                Text(stringResource(R.string.purchase_confirm))
            }
        }
    )
}
