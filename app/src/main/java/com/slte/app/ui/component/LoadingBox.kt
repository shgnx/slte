package com.slte.app.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

/**
 * 全局 Loading 组件。
 *
 * 白色圆角卡片 + 阴影，从灰色遮罩中浮出；尺寸/圆角/阴影统一走 Dimens 与 SlteShapes。
 */
@Composable
fun LoadingBox(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.loading),
) {
    Card(
        modifier = modifier.size(Dimens.loadingBoxSize),
        shape = SlteShapes.large,
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation =
        CardDefaults.cardElevation(
            defaultElevation = Dimens.loadingBoxElevation,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                LottieLoadingIcon(modifier = Modifier.size(Dimens.loadingAnimSize))
                Spacer(modifier = Modifier.height(Dimens.loadingTextGap))
                Text(
                    text = message,
                    style = SlteType.bodySmall,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 全屏 Loading 遮罩：点击空白或系统返回可取消，淡入淡出过渡。 */
@Composable
fun LoadingOverlay(
    visible: Boolean,
    message: String = stringResource(R.string.loading),
    onDismiss: (() -> Unit)? = null,
) {
    // 系统返回手势/返回键可取消全屏加载（需调用方传入 onDismiss）
    if (visible && onDismiss != null) {
        BackHandler(onBack = onDismiss)
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(initialAlpha = 0f),
        exit = fadeOut(targetAlpha = 0f),
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = Dimens.loadingScrimAlpha))
                .clickable(
                    interactionSource = null,
                    indication = null,
                ) { onDismiss?.invoke() },
            contentAlignment = Alignment.Center,
        ) {
            LoadingBox(message = message)
        }
    }
}
