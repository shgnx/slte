package com.slte.app.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AuthRules 认证决策纯逻辑测试：与 SessionStore / OkHttp / Android 无依赖。
 */
class AuthRulesTest {
    @Test
    fun `认证接口路径判定`() {
        assertTrue(AuthRules.isAuthPath("/api/v1/user/info"))
        assertTrue(AuthRules.isAuthPath("/api/v1/user/subscribe"))
        assertFalse(AuthRules.isAuthPath("/api/v1/subscribe/token"))
        assertFalse(AuthRules.isAuthPath("/api/v1/orders"))
        assertFalse(AuthRules.isAuthPath(""))
    }

    @Test
    fun `响应体文本判定登录态失效`() {
        assertTrue(AuthRules.isAuthFailureBodyText("未登录"))
        assertTrue(AuthRules.isAuthFailureBodyText("登陆已过期"))
        assertTrue(AuthRules.isAuthFailureBodyText("登录已过期"))
        assertTrue(AuthRules.isAuthFailureBodyText("UNAUTHORIZED"))
        assertTrue(AuthRules.isAuthFailureBodyText("TOKEN EXPIRED"))
        assertTrue(AuthRules.isAuthFailureBodyText("{\"message\":\"invalid token\"}"))
        assertFalse(AuthRules.isAuthFailureBodyText(""))
        assertFalse(AuthRules.isAuthFailureBodyText(null))
        assertFalse(AuthRules.isAuthFailureBodyText("无套餐"))
        assertFalse(AuthRules.isAuthFailureBodyText("订阅已过期"))
    }

    @Test
    fun `403空响应体在认证接口也判为登录态失效`() {
        // 部分面板对过期凭证返回 403 + 空响应体，空体与关键词任一命中即判失效
        assertTrue(AuthRules.isAuthFailureBody(""))
        assertTrue(AuthRules.isAuthFailureBody(null))
        assertTrue(AuthRules.isAuthFailureBody("未登录"))
        assertTrue(AuthRules.isAuthFailureBody("{\"message\":\"invalid token\"}"))
        assertFalse(AuthRules.isAuthFailureBody("无套餐"))
    }

    @Test
    fun `注入token：有token处白名单且无Authorization头`() {
        assertTrue(AuthRules.decide("tok", isAllowedHost = true, hasAuthHeader = false, 200, false).attachToken)
        assertFalse(AuthRules.decide(null, true, false, 200, false).attachToken)
        assertFalse(AuthRules.decide("tok", false, false, 200, false).attachToken)
        assertFalse(AuthRules.decide("tok", true, true, 200, false).attachToken)
    }

    @Test
    fun `401始终清会话`() {
        assertTrue(AuthRules.decide("tok", true, false, 401, false).clearSession)
        // 响应体与路径不相关也清
        assertTrue(AuthRules.decide("tok", true, false, 401, true).clearSession)
        // 但无 token 时即便 401 也无会话可清
        assertFalse(AuthRules.decide(null, true, false, 401, false).clearSession)
    }

    @Test
    fun `403仅在响应体含失效关键词时清会话`() {
        assertTrue(AuthRules.decide("tok", true, false, 403, isAuthFailureBody = true).clearSession)
        // 业务 403（无失效关键词）不清会话
        assertFalse(AuthRules.decide("tok", true, false, 403, isAuthFailureBody = false).clearSession)
    }

    @Test
    fun `200及非失效状态不清会话`() {
        assertFalse(AuthRules.decide("tok", true, false, 200, false).clearSession)
        assertFalse(AuthRules.decide("tok", true, false, 400, true).clearSession)
        assertFalse(AuthRules.decide("tok", true, false, 500, false).clearSession)
    }

    @Test
    fun `注入与清会话相互独立`() {
        // 无 token：既不注入也不清会话
        val noToken = AuthRules.decide(null, true, false, 401, false)
        assertFalse(noToken.attachToken)
        assertFalse(noToken.clearSession)

        // 已带 Authorization 的 401：不重复注入但清会话
        val withHeader = AuthRules.decide("tok", true, true, 401, false)
        assertFalse(withHeader.attachToken)
        assertTrue(withHeader.clearSession)

        // 非白名单主机的 401：不注入但仍清会话（登出判定不依赖主机白名单）
        val notAllowedHost = AuthRules.decide("tok", false, false, 401, false)
        assertFalse(notAllowedHost.attachToken)
        assertTrue(notAllowedHost.clearSession)
    }
}
