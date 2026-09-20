package com.slte.app.utils

import com.slte.app.R
import com.slte.app.data.remote.ApiException

object ErrorMessages {

    fun forLogin(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapLoginError(e.message)
        else -> R.string.error_network
    }

    fun forRegister(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapRegisterError(e.message)
        else -> R.string.error_network
    }

    fun forForgot(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapForgotError(e.message)
        else -> R.string.error_network
    }

    fun forSendCode(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapSendCodeError(e.message)
        else -> R.string.error_network
    }

    fun forOrder(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapOrderError(e.message)
        else -> R.string.error_order_failed
    }

    fun forSubscribe(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapSubscribeError(e.message)
        else -> R.string.error_network
    }

    fun forServer(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapServerError(e.message)
        else -> R.string.error_server_load
    }

    fun mapLoginError(backendMessage: String): Int = when {
        backendMessage.contains("不存在", ignoreCase = true) ||
            backendMessage.contains("not found", ignoreCase = true) ||
            backendMessage.contains("未注册", ignoreCase = true) ||
            backendMessage.contains("not registered", ignoreCase = true) -> R.string.error_login_not_found

        backendMessage.contains("密码", ignoreCase = true) ||
            backendMessage.contains("password", ignoreCase = true) ||
            backendMessage.contains("账号", ignoreCase = true) ||
            backendMessage.contains("account", ignoreCase = true) ||
            backendMessage.contains("邮箱", ignoreCase = true) ||
            backendMessage.contains("email", ignoreCase = true) -> R.string.error_login_invalid

        else -> R.string.error_login_failed
    }

    fun mapRegisterError(backendMessage: String): Int = when {
        backendMessage.contains("验证码", ignoreCase = true) ||
            backendMessage.contains("code", ignoreCase = true) ||
            backendMessage.contains("verification", ignoreCase = true) -> R.string.error_code_required

        else -> R.string.error_register_failed
    }

    fun mapForgotError(backendMessage: String): Int = when {
        backendMessage.contains("验证码", ignoreCase = true) ||
            backendMessage.contains("code", ignoreCase = true) ||
            backendMessage.contains("verification", ignoreCase = true) -> R.string.error_code_required

        else -> R.string.error_forgot_failed
    }

    fun mapSendCodeError(backendMessage: String): Int = R.string.error_email_send_failed

    fun isPendingOrderMessage(backendMessage: String?): Boolean = backendMessage != null &&
        (
            backendMessage.contains("未完成", ignoreCase = true) ||
                backendMessage.contains("未支付", ignoreCase = true) ||
                backendMessage.contains("pending", ignoreCase = true) ||
                backendMessage.contains("unpaid", ignoreCase = true)
            )

    fun mapOrderError(backendMessage: String?): Int = when {
        backendMessage == null -> R.string.error_order_failed

        isPendingOrderMessage(backendMessage) -> R.string.purchase_existing_order_message

        backendMessage.contains("优惠", ignoreCase = true) ||
            backendMessage.contains("coupon", ignoreCase = true) -> R.string.error_coupon_invalid

        backendMessage.contains("订单", ignoreCase = true) ||
            backendMessage.contains("order", ignoreCase = true) -> R.string.error_order_failed

        else -> R.string.error_order_failed
    }

    fun mapSubscribeError(backendMessage: String?): Int = when {
        backendMessage == null -> R.string.api_error_subscribe_info

        backendMessage.contains("网络", ignoreCase = true) ||
            backendMessage.contains("network", ignoreCase = true) ||
            backendMessage.contains("连接", ignoreCase = true) -> R.string.error_network

        else -> R.string.api_error_subscribe_info
    }

    fun mapServerError(backendMessage: String?): Int = R.string.error_server_load

    fun giftCardMessageRes(backendMessage: String?): Int? = when {
        backendMessage == null -> null

        backendMessage.contains("已使用", ignoreCase = true) ||
            backendMessage.contains("被使用", ignoreCase = true) ||
            backendMessage.contains("使用过", ignoreCase = true) ||
            backendMessage.contains("already been used", ignoreCase = true) ||
            backendMessage.contains("already used", ignoreCase = true) -> R.string.error_gift_card_used

        backendMessage.contains("使用限制", ignoreCase = true) ||
            backendMessage.contains("使用条件", ignoreCase = true) ||
            backendMessage.contains("不满足", ignoreCase = true) ||
            backendMessage.contains("limit", ignoreCase = true) ||
            backendMessage.contains("not suitable", ignoreCase = true) ||
            backendMessage.contains("eligible", ignoreCase = true) ||
            backendMessage.contains("condition", ignoreCase = true) -> R.string.error_gift_card_unavailable

        backendMessage.contains("不存在", ignoreCase = true) ||
            backendMessage.contains("不可用", ignoreCase = true) ||
            backendMessage.contains("停用", ignoreCase = true) ||
            backendMessage.contains("无效", ignoreCase = true) ||
            backendMessage.contains("过期", ignoreCase = true) ||
            backendMessage.contains("长度", ignoreCase = true) ||
            backendMessage.contains("gift card does not exist", ignoreCase = true) ||
            backendMessage.contains("gift card has expired", ignoreCase = true) ||
            backendMessage.contains("invalid", ignoreCase = true) ||
            backendMessage.contains("expired", ignoreCase = true) ||
            backendMessage.contains("length", ignoreCase = true) -> R.string.error_gift_card_invalid

        backendMessage.contains("unknown", ignoreCase = true) ||
            backendMessage.contains("save failed", ignoreCase = true) -> R.string.error_gift_card_failed

        else -> null
    }

    fun networkError(): Int = R.string.error_network
}
