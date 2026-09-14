package com.slte.app.data.remote.api.dto

/**
 * 支付方式（来自 [com.slte.app.data.remote.api.AuthApi.getPaymentMethods]）。
 */
data class PaymentMethodDto(
    val id: Int,
    val name: String,
    val payment: String = "",
    val icon: String? = null,
)

/**
 * 创建订单结果。
 */
data class CreateOrderResultDto(
    val tradeNo: String,
)

/**
 * 支付结算结果。
 *
 * @param type -1=后端免费流程直接成功 0=扫码类（App 暂不支持） 1=跳转链接 2=直付结果
 * @param redirectUrl 支付跳转 URL
 * @param paid type=2 时的直付结果（data=true 即成功）
 */
data class CheckoutResultDto(
    val type: Int,
    val redirectUrl: String? = null,
    val message: String? = null,
    val paid: Boolean = false,
) {
    companion object {
        /** data 可能为布尔（免费/直付）或字符串（跳转 URL），须按原始 JSON 解析 */
        fun fromRawJson(raw: String): CheckoutResultDto? = runCatching {
            val json = org.json.JSONObject(raw)
            CheckoutResultDto(
                type = json.optInt("type", 0),
                redirectUrl = (json.opt("data") as? String)?.takeIf { it.isNotBlank() },
                message = json.optString("message").takeIf { it.isNotBlank() },
                paid = json.optBoolean("data", false),
            )
        }.getOrNull()
    }
}
