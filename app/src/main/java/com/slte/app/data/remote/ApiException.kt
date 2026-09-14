package com.slte.app.data.remote

import androidx.annotation.StringRes
import java.io.IOException

/**
 * API 业务异常：服务器返回了错误码或错误消息。
 *
 * 与网络层 IOException 区分，用于将后端错误信息透传到 UI 层展示。
 *
 * @param message 原始错误消息（用于日志和调试）
 * @param stringResId 可选的字符串资源 ID，UI 层可通过 Context 解析为本地化文本
 */
class ApiException(
    override val message: String,
    @StringRes val stringResId: Int? = null,
) : IOException(message)
