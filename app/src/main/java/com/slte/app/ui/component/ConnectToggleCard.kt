package com.slte.app.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.slte.app.R
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.utils.Dimens
import com.slte.app.ui.theme.TextSizes

/**
 * 连接开关卡片：大开关 + 下方状态文字。
 */
@Composable
fun ConnectToggleCard(
    isConnected: Boolean,
    isConnecting: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = Dimens.dashboardToggleCardMinHeight
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = SlteShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BigToggle(
                    isConnected = isConnected,
                    isConnecting = isConnecting,
                    onClick = onToggle
                )
                Spacer(modifier = Modifier.height(Dimens.dashboardToggleGap))
                Text(
                    text = when {
                        isConnecting -> stringResource(R.string.status_connecting)
                        isConnected -> stringResource(R.string.status_connected)
                        else -> stringResource(R.string.status_disconnected)
                    },
                    fontWeight = FontWeight.Medium,
                    fontSize = TextSizes.dashboardToggleStatus,
                    color = when {
                        isConnected -> SlteColors.current.iconGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * 大型开关组件（iOS 风格横向 toggle）。
 */
@Composable
private fun BigToggle(
    isConnected: Boolean,
    isConnecting: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val trackColor = when {
        isConnected -> SlteColors.current.iconGreen
        isConnecting -> SlteColors.current.statusWarning
        else -> SlteColors.current.statusDanger
    }

    val thumbOffset by animateDpAsState(
        targetValue = if (isConnected) Dimens.dashboardToggleThumbOffset else Dimens.dashboardToggleThumbPadding,
        animationSpec = tween(Dimens.dashboardToggleAnimDurationMs),
        label = "toggle_thumb"
    )

    Box(
        modifier = Modifier
            .width(Dimens.dashboardToggleWidth)
            .height(Dimens.dashboardToggleHeight)
            .clip(CircleShape)
            .background(trackColor)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.dashboardToggleThumbSize)
                .offset(x = thumbOffset, y = Dimens.dashboardToggleThumbPadding)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}
