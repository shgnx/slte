package com.slte.app.ui.screen.settings

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
import com.slte.app.ui.component.SlteSheet
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

/** TUN 堆栈模式选项（顺序：System 默认 / Gvisor / Mixed），value 与内核 TUNStack 小写枚举一致 */
enum class TunStackMode(
    val value: String,
    val labelRes: Int,
    val descRes: Int,
) {
    SYSTEM("system", R.string.settings_tun_stack_system, R.string.settings_tun_stack_system_desc),
    GVISOR("gvisor", R.string.settings_tun_stack_gvisor, R.string.settings_tun_stack_gvisor_desc),
    MIXED("mixed", R.string.settings_tun_stack_mixed, R.string.settings_tun_stack_mixed_desc),
    ;

    companion object {
        val DEFAULT = SYSTEM

        /** 按内核取值（小写）解析，未知值回退默认 */
        fun fromValue(value: String): TunStackMode = entries.firstOrNull { it.value == value } ?: DEFAULT
    }
}

/** TUN 堆栈模式选择弹窗（仅 UI 状态，不写内核） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TunStackModeSheet(
    currentMode: TunStackMode,
    onDismiss: () -> Unit,
    onSelect: (TunStackMode) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    SlteSheet(
        title = stringResource(R.string.settings_tun_stack),
        onDismiss = onDismiss,
    ) {
        TunStackMode.entries.forEach { mode ->
            val selected = currentMode == mode
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSelect(mode)
                        },
                    ).padding(vertical = Dimens.gap.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors =
                    RadioButtonDefaults.colors(
                        selectedColor = SlteColors.current.accentInteractive,
                    ),
                )
                Column(
                    modifier =
                    Modifier
                        .weight(1f)
                        .padding(start = Dimens.gap.sm),
                ) {
                    Text(
                        text = stringResource(mode.labelRes),
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        style = SlteType.title,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(mode.descRes),
                        style = SlteType.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
