package com.slte.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

/** 代理模式选择弹窗（选择结果由调用方写入内核） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyModeSheet(
    currentMode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    SlteSheet(
        title = stringResource(R.string.action_proxy_mode),
        onDismiss = onDismiss,
    ) {
        PROXY_MODE_OPTIONS.forEach { option ->
            // 勾选与选择都用内核稳定的模式标识，避免显示文案随语言变化导致状态失效
            val selected = currentMode == option.mode
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSelect(option.mode)
                            onDismiss()
                        },
                    ).padding(vertical = Dimens.gap.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors =
                    RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Column(
                    modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = Dimens.gap.sm),
                ) {
                    Text(
                        text = stringResource(option.labelRes),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        style = SlteType.title,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(option.descRes),
                        style = SlteType.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
