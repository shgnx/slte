package com.slte.app.ui.screen.profile

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.slte.app.R
import com.slte.app.ui.component.SlteButton
import com.slte.app.ui.component.SlteButtonStyle
import com.slte.app.ui.component.SlteSheet
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LogoutConfirmSheet(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = LocalHapticFeedback.current

    SlteSheet(
        title = stringResource(R.string.profile_logout),
        onDismiss = onDismiss,
    ) {
        Text(
            text = stringResource(R.string.logout_confirm_message),
            style = SlteType.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.lg))

        SlteButton(
            text = stringResource(R.string.profile_logout),
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            style = SlteButtonStyle.Danger,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        SlteButton(
            text = stringResource(R.string.purchase_cancel),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            style = SlteButtonStyle.Neutral,
        )
    }
}
