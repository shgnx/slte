package com.slte.app.utils

import com.slte.app.R
import com.slte.app.data.remote.ApiException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 错误提示映射测试：优先采用异常自带的 [ApiException.stringResId]，缺失时回退后端文案关键词。
 * 用户可见的唯一错误出口，曾被「非 2xx 一律请求失败」掩盖真实原因，故两条路径均需锁定。
 */
class ErrorMessagesTest {
    @Test
    fun `优先采用异常自带的资源ID`() {
        val withResId = ApiException("服务器响应格式异常", ApiErrors.SERIALIZATION)

        assertEquals(R.string.api_error_bad_response, ErrorMessages.forLogin(withResId))
        assertEquals(R.string.error_network, ErrorMessages.forOrder(ApiException("x", ApiErrors.NETWORK)))
        assertEquals(R.string.error_coupon_invalid, ErrorMessages.forOrder(ApiException("x", ApiErrors.COUPON_INVALID)))
    }

    @Test
    fun `无资源ID时回退到后端文案关键词`() {
        // 面板真实返回：{"message":"邮箱或密码错误"}
        assertEquals(R.string.error_login_invalid, ErrorMessages.forLogin(ApiException("邮箱或密码错误")))
        assertEquals(R.string.error_login_not_found, ErrorMessages.forLogin(ApiException("该邮箱未注册")))
        assertEquals(R.string.error_login_not_found, ErrorMessages.forLogin(ApiException("User not found")))
    }

    @Test
    fun `非法输入文案不会被误判为账号不存在`() {
        // Laravel 校验错误的通用句：不能因为它含 "invalid" 就报"未注册"
        assertEquals(R.string.error_login_failed, ErrorMessages.forLogin(ApiException("The given data was invalid.")))
        assertEquals(
            R.string.error_login_invalid,
            ErrorMessages.forLogin(ApiException("密码必须大于 8 个字符")),
        )
    }

    @Test
    fun `非API异常一律按网络问题处理`() {
        assertEquals(R.string.error_network, ErrorMessages.forLogin(IOException("timeout")))
        assertEquals(R.string.error_network, ErrorMessages.forLogin(null))
        assertEquals(R.string.error_network, ErrorMessages.forSubscribe(null))
        assertEquals(R.string.error_order_failed, ErrorMessages.forOrder(null))
        assertEquals(R.string.error_server_load, ErrorMessages.forServer(null))
    }

    @Test
    fun `存在未支付订单的识别`() {
        assertTrue(ErrorMessages.isPendingOrderMessage("您有未完成的订单"))
        assertTrue(ErrorMessages.isPendingOrderMessage("存在未支付订单"))
        assertTrue(ErrorMessages.isPendingOrderMessage("pending order exists"))
        assertFalse(ErrorMessages.isPendingOrderMessage(null))
        assertFalse(ErrorMessages.isPendingOrderMessage("余额不足"))

        // 识别出来就应映射到"存在未支付订单"的专属提示，而不是通用订单错误
        assertEquals(
            R.string.purchase_existing_order_message,
            ErrorMessages.forOrder(ApiException("您有未完成的订单")),
        )
    }

    @Test
    fun `注册与找回不透露邮箱是否已注册`() {
        // 这两种场景必须统一文案，避免账号可枚举
        assertEquals(R.string.error_register_failed, ErrorMessages.forRegister(ApiException("该邮箱已注册")))
        assertEquals(R.string.error_forgot_failed, ErrorMessages.forForgot(ApiException("该邮箱未注册")))
        assertEquals(R.string.error_email_send_failed, ErrorMessages.forSendCode(ApiException("该邮箱未注册")))
    }
}
