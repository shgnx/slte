package com.slte.app.data.remote

import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.config.AllowedHosts
import com.slte.app.utils.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 认证决策纯逻辑：与 SessionStore / OkHttp / Android 解耦，便于单元测试。
 */
internal object AuthRules {
    /** 认证接口路径前缀：仅其下 403 判为登录态失效，避免业务 403 误登出 */
    const val AUTH_API_PREFIX = ApiPaths.AUTH

    /** 单次认证决策结果 */
    data class Decision(
        /** 是否应追加 Authorization 头 */
        val attachToken: Boolean,
        /** 是否应判定登录态失效并清会话 */
        val clearSession: Boolean,
    )

    /** 是否认证接口路径（决定 403 是否触发登出） */
    fun isAuthPath(encodedPath: String): Boolean = encodedPath.startsWith(AUTH_API_PREFIX)

    /** 响应体文本是否表示登录态失效（空体/无关键词时为 false） */
    fun isAuthFailureBodyText(body: String?): Boolean {
        if (body.isNullOrBlank()) return false
        val lower = body.lowercase()
        return AUTH_FAILURE_KEYWORDS.any { lower.contains(it) }
    }

    /**
     * 403 响应体是否表示登录态失效：空响应体（部分面板对过期凭证返回 403 + 空体，
     * 旧实现正是为此场景而写）或响应体含失效关键词，任一命中即判定失效。
     */
    fun isAuthFailureBody(body: String?): Boolean = body.isNullOrBlank() || isAuthFailureBodyText(body)

    /**
     * 综合判定：
     * - [attachToken]：有 token、主机在会话白名单、且原请求未带 Authorization 时才注入；
     * - [clearSession]：401，或 403 且 [isAuthFailureBody] 为真时清会话（须已有 token）。
     */
    fun decide(
        token: String?,
        isAllowedHost: Boolean,
        hasAuthHeader: Boolean,
        responseCode: Int,
        isAuthFailureBody: Boolean,
    ): Decision {
        val attachToken = token != null && isAllowedHost && !hasAuthHeader
        val authFailed =
            responseCode == 401 ||
                (responseCode == 403 && isAuthFailureBody)
        val clearSession = authFailed && token != null
        return Decision(attachToken = attachToken, clearSession = clearSession)
    }

    internal val AUTH_FAILURE_KEYWORDS: List<String> =
        listOf(
            "未登录",
            "登陆已过期",
            "登录已过期",
            "unauthorized",
            "unauthenticated",
            "token expired",
            "invalid token",
        )
}

/** 认证拦截器：注入 Authorization 头，认证失效自动清会话 */
@Singleton
class AuthInterceptor
@Inject
constructor(
    private val sessionStore: SessionStore,
) : Interceptor {
    private val _authErrorEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 认证失效事件流 */
    val authErrorEvents: SharedFlow<Unit> = _authErrorEvents.asSharedFlow()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = sessionStore.getAuthData()
        // 凭据只发往自有域名白名单（纵深防御）：订阅等 @Url 请求的地址由服务端下发，
        // 即使上层漏检，也不会把 JWT 带到第三方主机
        val canAttachToken = AllowedHosts.isAllowedHost(request.url.host)
        val decision =
            AuthRules.decide(
                token = token,
                isAllowedHost = canAttachToken,
                hasAuthHeader = request.header("Authorization") != null,
                responseCode = 0, // 响应码未知，先只算"是否注入"；清除判定等拿到响应后再算
                isAuthFailureBody = false,
            )
        val authenticated =
            if (decision.attachToken) {
                request
                    .newBuilder()
                    .addHeader("Authorization", token!!)
                    .build()
            } else {
                if (token != null && !canAttachToken) {
                    AppLog.w("SLTE-Api", "非白名单主机，已跳过凭据注入: ${request.url.encodedPath}")
                }
                request
            }
        val response = chain.proceed(authenticated)

        // 认证失效判定：401，或 403 且响应体表示登录态失效（含失效关键词或空响应体）。
        // 403 仅对 /api/v1/user/ 前缀判定会话过期，避免无套餐、订阅过期等业务 403 误杀会话；
        // 且仅当请求携带的 token 仍是当前会话时才清会话，防止旧请求误清新会话。
        val clearSession =
            AuthRules.decide(
                token = token,
                isAllowedHost = canAttachToken,
                hasAuthHeader = request.header("Authorization") != null,
                responseCode = response.code,
                isAuthFailureBody = isAuthFailureResponse(response),
            ).clearSession
        if (clearSession && token == sessionStore.getAuthData()) {
            sessionStore.clear()
            _authErrorEvents.tryEmit(Unit)
        }

        return response
    }

    /** 403 响应是否表示登录态失效：仅认证类接口，且响应体含登录态失效关键词 */
    private fun isAuthFailureResponse(response: Response): Boolean {
        if (!AuthRules.isAuthPath(response.request.url.encodedPath)) {
            return false
        }
        return isAuthFailureBody(response)
    }

    /** 403 响应体是否表示登录态失效（只读取响应体副本，不消费原响应） */
    private fun isAuthFailureBody(response: Response): Boolean {
        val body =
            try {
                response.peekBody(MAX_PEEK_BYTES).string()
            } catch (_: Exception) {
                return false
            }
        return AuthRules.isAuthFailureBody(body)
    }

    private companion object {
        const val MAX_PEEK_BYTES = 4096L
    }
}
