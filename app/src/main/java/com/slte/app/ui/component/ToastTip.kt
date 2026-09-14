package com.slte.app.ui.component

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/**
 * 原生 Toast 轻提示（String 版本）。
 *
 * 以 message 为 LaunchedEffect key：参数变化即重启协程读取新值并弹出。
 */
@Composable
fun ToastTip(
    message: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }
}

/**
 * 原生 Toast 轻提示（resId 版本）。
 */
@Composable
fun ToastTip(
    messageRes: Int?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(messageRes) {
        val res = messageRes
        if (res != null) {
            Toast.makeText(context, context.getString(res), Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }
}
