package com.slte.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.slte.app.ui.theme.SlteColors
import com.slte.app.utils.Dimens

/**
 * 图标按钮：用于 TopBar 操作按钮和返回按钮。
 *
 * @param showBackground true 时显示圆形底衬（操作按钮），false 时仅显示图标（返回按钮）
 */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true
) {
    val hapticFeedback = LocalHapticFeedback.current
    IconButton(
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
    ) {
        if (showBackground) {
            Box(
                modifier = Modifier
                    .size(Dimens.topBarActionBgSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = description,
                    modifier = Modifier.size(Dimens.topBarActionIconSize),
                    tint = SlteColors.current.iconBlue
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = description,
                modifier = Modifier.size(Dimens.topBarActionIconSize),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
