package com.slte.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.slte.app.ui.theme.SlteColors
import com.slte.app.utils.Dimens

/**
 * 带底衬的图标徽章：首页信息行专用结构（蓝底 + 蓝图标）。
 *
 * 规范（见 SlteIcons KDoc）：底衬 accentInteractiveBg、图标 accentInteractive，
 * 尺寸固定 34dp 徽章 / 10dp 圆角 / 20dp 图标。
 * 不要在导航行（个人中心/设置/关于）使用本组件，那里按规范用无底衬的灰色图标
 * （SlteRowCard 默认色）。
 */
@Composable
fun SlteIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = SlteColors.current.accentInteractive,
    background: Color = SlteColors.current.accentInteractiveBg,
    size: androidx.compose.ui.unit.Dp = Dimens.iconBadgeSize,
) {
    Box(
        modifier =
        modifier
            .size(size)
            .clip(RoundedCornerShape(Dimens.iconBadgeRadius))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(Dimens.icon.md),
            tint = tint,
        )
    }
}
