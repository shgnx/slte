package com.slte.app.data.remote.config

import okhttp3.Response

/**
 * API failover 判定规则：可重试方法、故障状态码、响应是否为 JSON。
 * 纯函数便于单测；HTTP 响应读取由拦截器负责。
 */
object FailoverPolicy {
    /** 允许 failover 重放的幂等方法：POST 在"连接超时但已落库"窗口下重复发送会导致重复下单/结算 */
    val RETRYABLE_METHODS = setOf("GET", "HEAD", "OPTIONS")

    /**
     * 视为地址故障的状态码：超时/限流/服务端 5xx/网关类错误。
     * 4xx 中仅 408/425/429 属于瞬时故障可切换；其余 4xx 是业务拒绝，切换地址无意义。
     */
    val FAILURE_CODES = setOf(408, 425, 429, 500, 502, 503, 504, 522, 524, 530)

    /** 是否允许对该方法做 failover 重放 */
    fun isRetryableMethod(method: String): Boolean = method in RETRYABLE_METHODS

    /** 是否属于可触发故障切换的状态码 */
    fun isFailureCode(code: Int): Boolean = code in FAILURE_CODES

    /**
     * 伪成功响应判定：响应声明为 JSON，但首字节不是 JSON 起始（{ 或 [）。
     * 用于识别"HTTP 200 但返回劫持页/网关默认页/被篡改内容"的假成功；
     * 非 JSON 声明（如订阅 YAML 的 text/plain）不参与判定，避免误伤合法响应。
     */
    fun isJsonMismatch(
        contentType: String?,
        firstByte: Int?,
    ): Boolean {
        val declaresJson = contentType?.lowercase()?.contains("json") == true
        if (!declaresJson || firstByte == null) return false
        return firstByte != '{'.code && firstByte != '['.code
    }

    /** 从响应读取用于 JSON 判定的首字节；不消费响应体（peek 语义） */
    fun firstByteOf(response: Response): Int? = try {
        response
            .peekBody(1)
            .byteStream()
            .read()
            .takeIf { it >= 0 }
    } catch (_: Exception) {
        null
    }
}
