package com.slte.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.Stickers

/**
 * 页面空状态组件：居中 TGS 动画 + 标题 + 副文案 + 可选 CTA。
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
    onAction: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedSticker(
            assetPath = Stickers.EMPTY,
            modifier = Modifier.size(Dimens.stateStickerSize),
        )
        Spacer(modifier = Modifier.height(Dimens.gap.xl))
        Text(
            text = title,
            style = SlteType.body,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(Dimens.gap.sm))
            Text(
                text = description,
                style = SlteType.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(Dimens.gap.xl))
            SlteButton(
                text = actionText,
                onClick = onAction,
                style = SlteButtonStyle.Neutral,
            )
        }
    }
}
