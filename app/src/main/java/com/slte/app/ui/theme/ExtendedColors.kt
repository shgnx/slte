package com.slte.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Telegram 源码扩展色（来源 ThemeColors.java）：输入框/图标/状态/延迟分档等语义色。 */
data class ExtendedColors(
    // 场景色：可操作（按钮、可点入口）
    val accentInteractive: Color,
    val accentInteractiveBg: Color,
    /** 文本选中底色：选中区域与光标把手共用，与 accentInteractive 成对 */
    val textSelectionBg: Color,
    // 状态色：前景与底色成对使用
    val statusSuccess: Color,
    val statusSuccessBg: Color,
    val statusWarning: Color,
    val statusWarningBg: Color,
    val statusDanger: Color,
    val statusDangerBg: Color,
    val statusNeutral: Color,
    val statusNeutralBg: Color,
    // 延迟分档：蓝=中等，黄=慢（节点页专用）
    val statusInfo: Color,
    val statusSlow: Color,
)

private val BrandGreenLight = Color(0xFF4BCB1C)
private val BrandGreenDark = Color(0xFF6DC26D)

/** 深色主题图标亮蓝：与按钮底（深蓝 primary）区分，图标/装饰保持高亮 */
private val BrandBlueDark = Color(0xFF6CB2F1)

val LightExtendedColors =
    ExtendedColors(
        accentInteractive = md_light_primary,
        accentInteractiveBg = Color(0x1A229AF0),
        textSelectionBg = Color(0x66229AF0),
        statusSuccess = BrandGreenLight,
        statusSuccessBg = Color(0x1A4BCB1C),
        statusWarning = Color(0xFFFFAB40),
        statusWarningBg = Color(0x1AFFAB40),
        statusDanger = md_light_error,
        statusDangerBg = md_light_errorContainer,
        statusNeutral = Color(0xFF999999),
        statusNeutralBg = Color(0x14999999),
        statusInfo = Color(0xFF2196F3),
        statusSlow = Color(0xFFFFC107),
    )

val DarkExtendedColors =
    ExtendedColors(
        accentInteractive = BrandBlueDark,
        accentInteractiveBg = Color(0x1A6CB2F1),
        textSelectionBg = Color(0x666CB2F1),
        statusSuccess = BrandGreenDark,
        statusSuccessBg = Color(0x1A6DC26D),
        statusWarning = Color(0xFFFFCC80),
        statusWarningBg = Color(0x1AFFCC80),
        statusDanger = md_dark_error,
        statusDangerBg = md_dark_errorContainer,
        statusNeutral = md_dark_onSurfaceVariant,
        statusNeutralBg = Color(0x14A8A8A8),
        statusInfo = Color(0xFF64B5F6),
        statusSlow = Color(0xFFFFD54F),
    )

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
