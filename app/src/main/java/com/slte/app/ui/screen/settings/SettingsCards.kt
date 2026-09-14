package com.slte.app.ui.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.slte.app.ui.component.SlteRowCard
import com.slte.app.ui.component.SlteSwitch

/** 设置行卡片：图标 + 标题 + 可选值 + 箭头，点击震动反馈 */
@Composable
internal fun SettingsRowCard(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
) = SlteRowCard(
    icon = icon,
    title = title,
    value = value,
    chevron = true,
    onClick = onClick,
)

/** 开关行卡片（与个人中心同款样式：左侧图标 + 标题 + 右侧 Switch） */
@Composable
internal fun SettingsSwitchCard(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    SlteRowCard(
        icon = icon,
        title = title,
        trailing = {
            SlteSwitch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { value ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(value)
                },
            )
        },
        // 整行可点：触控目标由 48×28 的开关扩展为整行 56dp（无障碍 + 通用交互习惯）
        onClick =
        if (enabled) {
            {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!checked)
            }
        } else {
            null
        },
    )
}
