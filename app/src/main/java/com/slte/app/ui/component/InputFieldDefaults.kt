package com.slte.app.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.slte.app.ui.theme.SlteColors

/**
 * 统一的输入框配色：白色背景，无边缘描边（聚焦/未聚焦均无），错误时显示错误色指示。
 * 颜色统一走主题扩展色，亮暗主题自动适配。
 */
@Composable
fun InputFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = SlteColors.current.inputFieldBackground,
    unfocusedContainerColor = SlteColors.current.inputFieldBackground,
    disabledContainerColor = SlteColors.current.inputFieldBackground,
    errorContainerColor = SlteColors.current.inputFieldBackground,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = MaterialTheme.colorScheme.error,
    cursorColor = SlteColors.current.iconBlue,
)
