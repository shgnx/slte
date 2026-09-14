package com.slte.app.utils

import com.slte.app.R
import com.slte.app.data.remote.ApiException

/**
 * 后端异常消息 → 友好提示资源 ID。
 *
 * 每组提供两类入口：
 * - `forXxx(Throwable?)`：ViewModel 统一用它。**优先采用 [ApiException.stringResId]**——适配器在
 *   抛出点就知道语义（空数据/优惠券无效/响应格式异常…），比按文案猜类型可靠；非 ApiException
 *   一律按网络问题处理。
 * - `mapXxx(String?)`：仅按 message 关键词匹配，作为兜底。
 *
 * 关键词兜底本身很脆（后端改文案即失效、且无法本地化），因此只在适配器没给出资源 ID 时才走。
 */
object ErrorMessages {
    /** 登录失败 */
    fun forLogin(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapLoginError(e.message)
        else -> R.string.error_network
    }

    /** 注册失败 */
    fun forRegister(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapRegisterError(e.message)
        else -> R.string.error_network
    }

    /** 忘记密码失败 */
    fun forForgot(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapForgotError(e.message)
        else -> R.string.error_network
    }

    /** 发送邮箱验证码失败 */
    fun forSendCode(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapSendCodeError(e.message)
        else -> R.string.error_network
    }

    /** 订单相关失败 */
    fun forOrder(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapOrderError(e.message)
        else -> R.string.error_order_failed
    }

    /** 订阅信息失败 */
    fun forSubscribe(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapSubscribeError(e.message)
        else -> R.string.error_network
    }

    /** 节点列表失败 */
    fun forServer(e: Throwable?): Int = when (e) {
        is ApiException -> e.stringResId ?: mapServerError(e.message)
        else -> R.string.error_server_load
    }

    fun mapLoginError(backendMessage: String): Int = when {
        // "未注册/不存在"先判断：后端消息如"该邮箱未注册"同时含邮箱与未注册关键词
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

    /** 注册错误：不向未认证用户透露邮箱是否已注册，避免可枚举性 */
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

    /** 发送验证码错误：统一提示发送失败，不向未认证用户透露邮箱注册状态 */
    fun mapSendCodeError(backendMessage: String): Int = R.string.error_email_send_failed

    /** 是否为"存在未支付订单"类错误（创建订单被既有未支付订单拦截） */
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

    /** 节点/服务器错误（统一提示加载失败） */
    fun mapServerError(backendMessage: String?): Int = R.string.error_server_load

    /** 网络异常兜底 */
    fun networkError(): Int = R.string.error_network
}
