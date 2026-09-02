package com.slte.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.Stickers

/**
 * 页面空状态组件：TGS 动画居中 + 标题 + 副文案 + 可选 CTA。
 * 动画循环播放本地 assets 中的 TGS 贴纸。
 *
 * @param title 主标题（如 "暂无订单"）
 * @param description 副文案，可空
 * @param actionText CTA 按钮文案，null 时不显示按钮
 * @param onAction CTA 点击回调
 */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedSticker(
            assetPath = Stickers.EMPTY,
            modifier = Modifier.size(Dimens.emptyStickerSize)
        )
        Spacer(modifier = Modifier.height(Dimens.spacingXl))
        Text(
            text = title,
            fontSize = TextSizes.dashboardListLabel,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(Dimens.spacingSm))
            Text(
                text = description,
                fontSize = TextSizes.actionSubtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(Dimens.spacingXl))
            OutlinedButton(onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                onAction()
            }) {
                Text(actionText)
            }
        }
    }
}
