package com.slte.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

/**
 * 项目统一行卡片：图标 + 标题（+ 副标题 / 值 / 尾部内容 / 箭头）。
 *
 * 高度、内边距、图标尺寸与配色固定，保证全项目列表行规格一致。
 *
 * @param iconTint 图标配色：展示/导航类用灰（onSurfaceVariant，默认），可操作类用 accentInteractive（蓝）
 * @param chevron 是否显示行尾箭头（跳转提示）
 * @param trailing 尾部自定义内容（开关、按钮等），与 chevron 二选一
 */
@Composable
fun SlteRowCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current

    @Composable
    fun Content() {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(Dimens.size.row)
                .padding(horizontal = Dimens.gap.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.icon.lg),
                tint = iconTint,
            )
            Spacer(modifier = Modifier.width(Dimens.gap.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = SlteType.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(Dimens.gap.xs))
                    Text(
                        text = subtitle,
                        style = SlteType.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (value != null) {
                Text(
                    text = value,
                    style = SlteType.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing?.invoke()
            if (chevron) {
                Spacer(modifier = Modifier.width(Dimens.gap.xs))
                Icon(
                    imageVector = SlteIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.icon.md),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = modifier.fillMaxWidth(),
            shape = SlteShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
        ) { Content() }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = SlteShapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
        ) { Content() }
    }
}
