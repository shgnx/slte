package com.slte.app.utils

import androidx.compose.ui.unit.dp

object Dimens {
    /** 间距刻度（6 档，4dp 基准） */
    object gap {
        val xs = 4.dp
        val sm = 8.dp
        val md = 12.dp
        val lg = 16.dp
        val xl = 24.dp
        val xxl = 32.dp
    }

    /** 图标尺寸（3 档） */
    object icon {
        val sm = 16.dp
        val md = 20.dp
        val lg = 24.dp
    }

    /** 图标徽章（首页信息行等「展示·可操作」场景的蓝底衬） */
    val iconBadgeSize = 34.dp
    val iconBadgeRadius = 10.dp

    /** 组件高度（3 档） */
    object size {
        val button = 48.dp
        val buttonMd = 40.dp
        val row = 56.dp
    }

    val logoSize = 96.dp

    /** 周期网格条目上下内边距（小屏防溢出） */
    val periodGridItemPaddingV = 20.dp

    /** 支付方式单元格高度（两列网格）：对齐无障碍最小触控高度 */
    val paymentMethodCellHeight = 48.dp

    val strokeMedium = 1.5.dp
    val strokeThick = 2.dp
    val dividerThickness = 1.dp

    val maxContentWidth = 480.dp

    val loadingBoxSize = 76.dp
    val loadingAnimSize = 32.dp
    val loadingTextGap = 5.dp
    val loadingScrimAlpha = 0.3f
    val loadingBoxElevation = 8.dp

    /** 状态贴纸统一尺寸 */
    val stateStickerSize = 80.dp

    val switchTrackWidth = 48.dp
    val switchTrackHeight = 28.dp

    /** 开关触控区高度：视觉轨道 28dp，命中区需满足无障碍最小 48dp */
    val switchTouchHeight = 48.dp
    val switchThumbSize = 20.dp
    val switchThumbPadding = 4.dp

    /** IP 行小国旗尺寸（4:3） */
    val flagSize = 40.dp

    /** 非圆形展示时的国旗圆角 */
    val flagCornerRadius = 4.dp

    val sendCodeButtonWidth = 110.dp

    val topBarActionBgSize = 36.dp

    val cardElevation = 0.dp

    val planStatusPaddingV = 4.dp
    val planStatusChipCornerRadius = 50

    val inviteCodeItemBgAlpha = 0.5f
    val noticeTimeAlpha = 0.75f
    val paymentMethodDotAlpha = 0.4f
    val dividerAlpha = 0.5f
    val disabledAlpha = 0.4f

    val dashboardScreenPaddingH = gap.lg
    val dashboardScreenPaddingV = 10.dp
    val dashboardCardSpacing = 10.dp

    /** 小屏（真机）首页压缩：低于此高度启用紧凑布局，保证首屏完整可见 */
    val dashboardCompactBreakpoint = 840.dp
    val dashboardScreenPaddingVCompact = 6.dp
    val dashboardCardSpacingCompact = 6.dp
    val dashboardUsageBarHeight = 6.dp
    val dashboardUsageBarRadius = 3.dp
    val dashboardListValueMaxWidth = 180.dp
    val dashboardChevronGap = 2.dp
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
    val dashboardActionBtnHeight = size.button

    val inviteStatCardPaddingV = 20.dp
    val inviteStickerSize = 100.dp
    val inviteCodeItemHeight = 52.dp
    val inviteCodeItemPaddingH = 16.dp
    val inviteCodeCopyIconSize = 18.dp
    val inviteCodeCopyBtnSize = 48.dp

    val sheetPaddingH = gap.xl
    val sheetPaddingV = gap.sm

    /** 下拉浮层 */
    val inviteMethodListMaxHeight = 240.dp
    val popupShadowElevation = 8.dp

    val radioDotSize = 18.dp
    val radioDotInnerSize = 9.dp
    val radioDotGap = 5.dp
    val paymentMethodDotSize = 14.dp

    val noticeTagPaddingH = 8.dp
    val noticeTagSpacing = 6.dp
    val noticeBodyMaxLines = 2
}

object VerificationCodeConfig {
    const val countdownSeconds = 60
    const val countdownIntervalMs = 1000L
}
