package com.slte.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

/**
 * 按钮预设档位（详见 docs/BUTTON-STYLES.md）。
 *
 * 档位仅为默认值，调用处可通过 [SlteButton] 对应参数逐项覆盖；覆盖值必须取主题令牌，禁止字面量。
 */
enum class SlteButtonStyle {
    /** 蓝色填充，页面级主操作（登录、注册、支付、立即更新） */
    Primary,

    /** 蓝描边蓝字，拉新/转化类推进操作（创建账号），全项目应保持稀有 */
    Secondary,

    /** 灰描边灰字，默认中性次要操作（取消、返回、稍后提醒、重试、空态动作） */
    Neutral,

    /** 白底蓝字无边框，输入行内按钮（发送验证码），默认与 Hero 输入框等高 */
    Tonal,

    /** 蓝色填充紧凑档，卡片内 CTA（续费、购买） */
    Medium,

    /** 红色填充，危险操作（退出登录） */
    Danger,
}

/**
 * 项目统一按钮组件：固定圆角、加载动画与触觉反馈，其余可调。
 *
 * @param style 预设档位，见 [SlteButtonStyle]
 * @param enabled 是否可用（置灰且不可点）
 * @param loading 提交中：内容替换为加载动画，按钮本色不变灰，点击被忽略（防重复提交）
 * @param containerColor 覆盖容器色，默认取档位预设
 * @param contentColor 覆盖文字/内容色，默认取档位预设
 * @param borderColor 覆盖描边色；非空即渲染描边
 * @param borderWidth 覆盖描边宽度，默认取档位预设（蓝描边 1.5dp / 灰描边 1dp）
 * @param height 覆盖高度，默认取档位预设（Tonal 56dp / Medium 40dp / 其余 48dp）
 */
@Composable
fun SlteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: SlteButtonStyle = SlteButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null,
    borderColor: Color? = null,
    borderWidth: Dp? = null,
    height: Dp? = null,
) {
    val haptic = LocalHapticFeedback.current
    val presetHeight =
        when (style) {
            SlteButtonStyle.Medium -> Dimens.size.buttonMd
            SlteButtonStyle.Tonal -> Dimens.size.row
            else -> Dimens.size.button
        }
    val effHeight = height ?: presetHeight
    val effContainer =
        containerColor ?: when (style) {
            SlteButtonStyle.Secondary, SlteButtonStyle.Neutral -> Color.Transparent
            SlteButtonStyle.Tonal -> MaterialTheme.colorScheme.surface
            SlteButtonStyle.Danger -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
    val effContent =
        contentColor ?: when (style) {
            SlteButtonStyle.Secondary, SlteButtonStyle.Tonal -> MaterialTheme.colorScheme.primary
            SlteButtonStyle.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
            SlteButtonStyle.Danger -> MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.onPrimary
        }
    val effBorder =
        borderColor?.let { BorderStroke(borderWidth ?: Dimens.strokeMedium, it) }
            ?: when (style) {
                SlteButtonStyle.Secondary ->
                    BorderStroke(Dimens.strokeMedium, MaterialTheme.colorScheme.primary)
                SlteButtonStyle.Neutral ->
                    BorderStroke(Dimens.dividerThickness, MaterialTheme.colorScheme.outline)
                else -> null
            }
    val action = {
        if (enabled && !loading) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        }
    }
    val textStyle =
        if (style == SlteButtonStyle.Medium) {
            SlteType.body.copy(fontWeight = FontWeight.Medium)
        } else {
            SlteType.field
        }
    val contentPadding =
        if (style == SlteButtonStyle.Medium) {
            PaddingValues(horizontal = Dimens.gap.lg)
        } else {
            ButtonDefaults.ContentPadding
        }
    val content: @Composable () -> Unit = {
        if (loading) {
            LottieLoadingIcon(modifier = Modifier.size(Dimens.icon.lg))
        } else {
            Text(text = text, style = textStyle)
        }
    }

    if (effBorder != null) {
        OutlinedButton(
            onClick = action,
            enabled = enabled,
            modifier = modifier.height(effHeight),
            shape = SlteShapes.medium,
            contentPadding = contentPadding,
            colors =
            ButtonDefaults.outlinedButtonColors(
                containerColor = effContainer,
                contentColor = effContent,
            ),
            border = effBorder,
        ) { content() }
    } else {
        Button(
            onClick = action,
            enabled = enabled,
            modifier = modifier.height(effHeight),
            shape = SlteShapes.medium,
            contentPadding = contentPadding,
            colors =
            ButtonDefaults.buttonColors(
                containerColor = effContainer,
                contentColor = effContent,
            ),
        ) { content() }
    }
}
