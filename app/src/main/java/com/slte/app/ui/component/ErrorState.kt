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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.Stickers

/**
 * 统一错误状态：动态贴纸 + 错误文案 + 重试按钮（与 EmptyState 同款结构）。
 * 样式集中于此，避免各页单独拼装导致图标尺寸/按钮样式不一致。
 */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedSticker(
            assetPath = Stickers.ERROR,
            modifier = Modifier.size(Dimens.stateStickerSize),
        )
        Spacer(modifier = Modifier.height(Dimens.gap.md))
        Text(
            text = message,
            style = SlteType.body,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(Dimens.gap.lg))
        SlteButton(
            text = stringResource(R.string.notice_retry),
            onClick = onRetry,
            style = SlteButtonStyle.Neutral,
        )
    }
}
