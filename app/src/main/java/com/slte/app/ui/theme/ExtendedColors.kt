package com.slte.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Telegram 源码扩展色（来源 ThemeColors.java）：输入框/图标/状态/延迟分档等语义色。 */
data class ExtendedColors(
    val inputFieldBackground: Color,
    val inputFieldPlaceholder: Color,
    // 图标色：蓝=主操作，绿=成功/在线，灰=次要信息
    val iconBlue: Color,
    val iconBlueBg: Color,
    val iconGreen: Color,
    val iconGreenBg: Color,
    // 状态色
    val statusSuccess: Color,
    val statusWarning: Color,
    val statusDanger: Color,
    val statusNeutral: Color,
    // 延迟分档：蓝=中等，黄=慢（节点页专用）
    val statusInfo: Color,
    val statusSlow: Color,
)

private val BrandGreenLight = Color(0xFF4BCB1C)
private val BrandGreenDark = Color(0xFF6DC26D)

/** 深色主题图标亮蓝：与按钮底（深蓝 primary）区分，图标/装饰保持高亮 */
private val BrandBlueDark = Color(0xFF6CB2F1)

val LightExtendedColors = ExtendedColors(
    inputFieldBackground = Color(0xFFFFFFFF),
    inputFieldPlaceholder = Color(0xFFA8A8A8),
    iconBlue = md_light_primary,
    iconBlueBg = Color(0x1A229AF0),
    iconGreen = BrandGreenLight,
    iconGreenBg = Color(0x1A4BCB1C),
    statusSuccess = BrandGreenLight,
    statusWarning = Color(0xFFFFAB40),
    statusDanger = md_light_error,
    statusNeutral = Color(0xFF999999),
    statusInfo = Color(0xFF2196F3),
    statusSlow = Color(0xFFFFC107),
)

val DarkExtendedColors = ExtendedColors(
    inputFieldBackground = Color(0xFF1F2936),
    inputFieldPlaceholder = Color(0xFF5A6175),
    iconBlue = BrandBlueDark,
    iconBlueBg = Color(0x1A6CB2F1),
    iconGreen = BrandGreenDark,
    iconGreenBg = Color(0x1A6DC26D),
    statusSuccess = BrandGreenDark,
    statusWarning = Color(0xFFFFCC80),
    statusDanger = md_dark_error,
    statusNeutral = md_dark_onSurfaceVariant,
    statusInfo = Color(0xFF64B5F6),
    statusSlow = Color(0xFFFFD54F),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
