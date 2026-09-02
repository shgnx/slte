package com.slte.app.utils

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object Dimens {

    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 12.dp
    val spacingLg = 16.dp
    val spacingXl = 24.dp
    val spacingXxl = 32.dp

    val buttonHeight = 48.dp
    val fieldHeight = 56.dp
    val logoSize = 96.dp
    /** 周期网格条目上下内边距（小屏防溢出） */
    val periodGridItemPaddingV = 20.dp
    /** 支付方式单元格高度（两列网格） */
    val paymentMethodCellHeight = 44.dp

    // Stroke / border
    val strokeMedium = 1.5.dp
    val strokeThick = 2.dp
    val dividerThickness = 1.dp

    // Screen
    val maxContentWidth = 480.dp

    // Text / Loading
    val loadingIndicatorSize = 24.dp

    // LoadingBox
    val loadingBoxSize = 76.dp
    val loadingAnimSize = 32.dp
    val loadingTextGap = 5.dp
    val loadingScrimAlpha = 0.3f
    val loadingBoxElevation = 8.dp

    // Error / Empty state
    val errorIconSize = 48.dp
    val emptyStickerSize = 160.dp

    // SlteSwitch 胶囊开关
    val switchTrackWidth = 48.dp
    val switchTrackHeight = 28.dp
    val switchThumbSize = 20.dp
    val switchThumbPadding = 4.dp

    // Flag
    val flagSize = 40.dp
    /** IP 行小国旗尺寸（4:3） */
    val flagSizeSmall = 16.dp
    /** 非圆形展示时的国旗圆角 */
    val flagCornerRadius = 4.dp

    // Button widths
    val sendCodeButtonWidth = 110.dp

    // Top bar
    val topBarActionBgSize = 36.dp
    val topBarActionIconSize = 24.dp

    // Card
    val cardContentPadding = 16.dp
    val cardElevation = 0.dp

    // Plan card
    val planStatusPaddingH = 16.dp
    val planStatusPaddingV = 4.dp
    val planStatusChipCornerRadius = 50
    val planRenewButtonPaddingH = 16.dp
    val planRenewButtonPaddingV = 6.dp

    // Connect button
    val connectButtonAlpha = 0.06f

    // Action card
    val actionRowHeight = 56.dp
    val actionIconBgSize = 36.dp
    val actionIconSize = 24.dp

    // Icon bg alpha
    val iconBgAlphaMedium = 0.12f

    // Alpha
    val inviteCodeItemBgAlpha = 0.5f
    val noticeTimeAlpha = 0.75f
    val paymentMethodDotAlpha = 0.4f
    val dividerAlpha = 0.5f
    val disabledAlpha = 0.4f

    // Dashboard
    val dashboardScreenPaddingH = 14.dp
    val dashboardScreenPaddingV = 10.dp
    val dashboardCardSpacing = 10.dp
    /** 小屏（真机）首页压缩：低于此高度启用紧凑布局，保证首屏完整可见 */
    val dashboardCompactBreakpoint = 840.dp
    val dashboardScreenPaddingVCompact = 6.dp
    val dashboardCardSpacingCompact = 6.dp
    val dashboardUsageBarHeight = 6.dp
    val dashboardUsageBarRadius = 3.dp
    val dashboardIconBgSize = 34.dp
    val dashboardIconBgRadius = 10.dp
    val dashboardIconSize = 20.dp
    val dashboardListRowHeight = 56.dp
    val dashboardListRowPaddingH = 18.dp
    val dashboardListRowGap = 12.dp
    val dashboardListValueMaxWidth = 180.dp
    val dashboardChevronSize = 20.dp
    val dashboardChevronGap = 2.dp
    val dashboardActionIconSize = 20.dp
    val dashboardActionIconGap = 8.dp
    val dashboardToggleWidth = 100.dp
    val dashboardToggleHeight = 52.dp
    /** 连接开关卡片最小高度：卡片加高、内容垂直居中 */
    val dashboardToggleCardMinHeight = 240.dp
    /** 小屏压缩后的连接开关卡片最小高度 */
    val dashboardToggleCardMinHeightCompact = 170.dp
    val dashboardToggleThumbSize = 44.dp
    val dashboardToggleThumbOffset = 52.dp
    val dashboardToggleThumbPadding = 4.dp
    val dashboardToggleGap = 10.dp
    val dashboardToggleAnimDurationMs = 350
    val dashboardActionBtnHeight = 48.dp

    // Invite
    val inviteStatCardPaddingH = 16.dp
    val inviteStatCardPaddingV = 20.dp
    val inviteStatGridSpacing = 16.dp
    val inviteStickerSize = 100.dp
    val inviteCodeCardPaddingH = 16.dp
    val inviteCodeCardPaddingV = 16.dp
    val inviteCodeItemHeight = 52.dp
    val inviteCodeItemPaddingH = 16.dp
    val inviteCodeCopyIconSize = 18.dp
    val inviteCodeCopyBtnSize = 48.dp
    val inviteRecordItemHeight = 56.dp
    val inviteActionButtonHeight = 54.dp

    // Invite Sheet
    val inviteSheetPaddingH = 24.dp
    val inviteSheetPaddingV = 8.dp
    val inviteSheetBalancePaddingH = 16.dp
    val inviteSheetBalancePaddingV = 12.dp

    // 通用零散
    val spacingTiny = 6.dp
    val radioDotSize = 18.dp
    val radioDotInnerSize = 9.dp
    val radioDotGap = 5.dp
    val paymentMethodDotSize = 14.dp

    // Notice
    val noticeCardPaddingH = 16.dp
    val noticeCardPaddingV = 16.dp
    val noticeTagPaddingH = 8.dp
    val noticeTagSpacing = 6.dp
    val noticeBodyMaxLines = 2
    val noticeSheetPaddingH = 24.dp
    val noticeSheetPaddingV = 8.dp
    val noticeSheetContentPaddingV = 16.dp
}

object VerificationCodeConfig {
    const val countdownSeconds = 60
    const val countdownIntervalMs = 1000L
}
