package com.slte.app.domain.model

import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.AuthInterceptor
import com.slte.app.data.remote.FallbackDns
import com.slte.app.data.remote.config.CrispManager
import com.slte.app.kernel.KernelConfig
import com.slte.app.kernel.KernelManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 会话管理器：登录态的唯一权威来源。
 *
 * 持有 [sessionState]、统一登录/登出/401 入口、发布 [logoutEvents] 供数据域清缓存；
 * 会话状态不归 AuthRepository 或 SubscribeRepository 所有。
 */
@Singleton
class SessionManager
@Inject
constructor(
    private val sessionStore: SessionStore,
    private val authInterceptor: AuthInterceptor,
    private val crispManager: CrispManager,
    private val kernelManager: KernelManager,
    private val kernelConfig: KernelConfig,
    private val fallbackDns: FallbackDns,
) {
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    /** 登出事件（登出/401/会话过期均触发）：数据域监听后清理各自缓存 */
    private val _logoutEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val logoutEvents: SharedFlow<Unit> = _logoutEvents.asSharedFlow()

    /** 会话过期自动登出事件：UI 提示用户重新登录（手动登出不发） */
    private val _sessionExpiredEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvents: SharedFlow<Unit> = _sessionExpiredEvents.asSharedFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        restoreSession()
        observeAuthErrors()
    }

    /** 恢复登录会话：存储的用户信息与订阅 token 不匹配时，用基础字段重建（旧数据自愈） */
    private fun restoreSession() {
        if (sessionStore.hasSession()) {
            val authData = sessionStore.getAuthData()
            val email = sessionStore.getEmail()
            val subscribeToken = sessionStore.getSubscribeToken()
            if (authData != null && email != null && subscribeToken != null) {
                val user =
                    sessionStore.getUserInfo()?.takeIf { it.subscribeToken == subscribeToken }
                        ?: User(
                            id = subscribeToken,
                            email = email,
                            authData = authData,
                            subscribeToken = subscribeToken,
                        )
                _sessionState.value = SessionState.LoggedIn(user)
            } else {
                sessionStore.clear()
                _sessionState.value = SessionState.LoggedOut
            }
        } else {
            sessionStore.clear()
            _sessionState.value = SessionState.LoggedOut
        }
    }

    /** 认证失效自动登出（与手动登出走同一清除路径，额外通知 UI 会话已过期） */
    private fun observeAuthErrors() {
        scope.launch {
            authInterceptor.authErrorEvents.collect {
                clearSession(expired = true)
            }
        }
    }

    /** 登录/注册成功后写入会话并持久化 */
    fun setLoggedIn(user: User) {
        _sessionState.value = SessionState.LoggedIn(user)
        sessionStore.save(user.authData, user.email, user.subscribeToken)
        sessionStore.saveUserInfo(user)
    }

    /** 用户信息刷新后更新会话内用户（余额等展示数据） */
    fun updateUser(user: User) {
        if (_sessionState.value is SessionState.LoggedIn) {
            _sessionState.value = SessionState.LoggedIn(user)
        }
    }

    /**
     * 登出：删除内核配置、停止内核、清空 DNS 与本地会话，并发布登出事件。
     * 记住的密码保留，供下次登录预填。
     *
     * @param expired true 表示会话失效被动登出（UI 提示重新登录）
     */
    fun clearSession(expired: Boolean = false) {
        // 邮箱同步读取（会话清空后不可恢复）；内核清理与停服务在 IO 上顺序执行
        val email = sessionStore.getEmail()
        scope.launch {
            kernelConfig.deleteAccountProfiles(email)
            kernelManager.stopVpn()
            fallbackDns.clearCache()
        }
        _sessionState.value = SessionState.LoggedOut
        sessionStore.clear()
        crispManager.clearUser()
        _logoutEvents.tryEmit(Unit)
        if (expired) _sessionExpiredEvents.tryEmit(Unit)
    }
}
