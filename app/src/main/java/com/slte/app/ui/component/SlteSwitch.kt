package com.slte.app.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.slte.app.R
import com.slte.app.utils.Dimens

/**
 * 干净胶囊开关：无阴影、无边框。
 * 关闭 = 灰胶囊 + 白圆；开启 = 淡蓝胶囊 + 主题蓝圆，点击左右平滑切换。
 * 全部取主题色（primary/primaryContainer/outlineVariant/surface），自动适配暗色主题。
 */
@Composable
fun SlteSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val trackOff = scheme.outlineVariant
    val trackOn = scheme.primaryContainer
    val thumbOff = scheme.surface
    val thumbOn = scheme.primary
    val stateDesc = if (checked) stringResource(R.string.switch_state_on) else stringResource(R.string.switch_state_off)

    val offset by animateDpAsState(
        targetValue = if (checked) Dimens.switchTrackWidth - Dimens.switchThumbSize - Dimens.switchThumbPadding * 2 else 0.dp,
        animationSpec = tween(180),
        label = "slte-switch-offset"
    )

    Box(
        modifier = modifier
            .size(width = Dimens.switchTrackWidth, height = Dimens.switchTrackHeight)
            .clip(RoundedCornerShape(Dimens.switchTrackHeight / 2))
            .background(if (checked) trackOn else trackOff)
            .semantics {
                role = Role.Switch
                stateDescription = stateDesc
            }
            .toggleable(value = checked, enabled = enabled, role = Role.Switch) { onCheckedChange(it) }
            .padding(Dimens.switchThumbPadding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .size(Dimens.switchThumbSize)
                .clip(CircleShape)
                .background(if (checked) thumbOn else thumbOff)
        )
    }
}
