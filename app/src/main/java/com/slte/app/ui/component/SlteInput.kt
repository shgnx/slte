package com.slte.app.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

/**
 * 输入框档位：按"任务主次"选择，不按页面选择。
 *
 * - [Hero]：页面唯一任务（登录/注册/忘记密码）—— 56dp、16sp、24dp 图标
 * - [Compact]：次要表面（弹窗内表单、行内）—— 48dp、14sp、20dp 图标
 */
enum class SlteInputSize { Hero, Compact }

/**
 * 项目统一输入框：描边容器 + 左图标 + 框内占位符（无 label 浮动）。
 *
 * 全项目唯一输入框实现，禁止在页面内直接使用 OutlinedTextField 或另写 BasicTextField。
 * 文字与占位符共用同一排版角色，保证行高与字距一致。
 */
@Composable
fun SlteInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconDesc: String? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    bordered: Boolean = true,
    size: SlteInputSize = SlteInputSize.Hero,
) {
    val compact = size == SlteInputSize.Compact
    val fieldHeight = if (compact) Dimens.size.button else Dimens.size.row
    val fieldText = if (compact) SlteType.body else SlteType.field
    val fieldIcon = if (compact) Dimens.icon.md else Dimens.icon.lg
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SlteShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border =
        when {
            focused -> BorderStroke(Dimens.strokeMedium, MaterialTheme.colorScheme.primary)
            bordered -> BorderStroke(Dimens.dividerThickness, MaterialTheme.colorScheme.outline)
            else -> null
        },
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(fieldHeight)
                .padding(horizontal = Dimens.gap.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = iconDesc,
                    modifier = Modifier.size(fieldIcon),
                    tint = SlteColors.current.accentInteractive,
                )
                Spacer(modifier = Modifier.width(Dimens.gap.sm))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                interactionSource = interactionSource,
                enabled = enabled,
                readOnly = readOnly,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                visualTransformation = visualTransformation,
                textStyle = fieldText.copy(color = MaterialTheme.colorScheme.onSurface),
                // BasicTextField 默认光标是写死的黑色，必须显式走主题色
                cursorBrush = SolidColor(SlteColors.current.accentInteractive),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = fieldText.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                },
            )
            trailing?.invoke()
        }
    }
}
