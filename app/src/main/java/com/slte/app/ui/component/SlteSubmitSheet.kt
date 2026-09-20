package com.slte.app.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.slte.app.utils.Dimens
import kotlinx.coroutines.delay

data class SubmitTip(
    @StringRes val messageRes: Int? = null,
    val message: String? = null,
)

@Composable
fun SlteSubmitSheet(
    title: String,
    submitText: String,
    submitting: Boolean,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    submitEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    SlteSheet(
        title = title,
        subtitle = subtitle,
        header = header,
        modifier = modifier,
        onDismiss = onDismiss,
    ) {
        content()

        Spacer(modifier = Modifier.height(Dimens.gap.lg))

        SlteButton(
            text = submitText,
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            style = SlteButtonStyle.Primary,
            enabled = submitEnabled,
            loading = submitting,
        )
    }
}

@Composable
fun SubmitTipHost(
    tip: SubmitTip?,
    onTipShown: () -> Unit,
) {
    var delayedTip by remember { mutableStateOf<SubmitTip?>(null) }
    LaunchedEffect(tip) {
        delayedTip = null
        if (tip != null) {
            delay(TIP_DELAY_MS)
            delayedTip = tip
        }
    }
    ToastTip(
        message = delayedTip?.messageRes?.let { stringResource(it) } ?: delayedTip?.message,
        onDismiss = onTipShown,
    )
}

private const val TIP_DELAY_MS = 250L
