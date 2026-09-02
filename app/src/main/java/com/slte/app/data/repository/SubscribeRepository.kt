package com.slte.app.data.repository

import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.data.remote.api.dto.SubscribeInfoDto as ApiSubscribeInfo
import com.slte.app.domain.model.Notice
import com.slte.app.domain.model.SessionManager
import com.slte.app.domain.model.SessionState
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.model.User
import com.slte.app.utils.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 订阅/用户/公告数据仓库：会话数据获取 + 缓存（30s TTL + 磁盘兜底）。
 *
 * 会话状态不归本类所有（见 [SessionManager]）；登出事件触发缓存清理。
 */
@Singleton
class SubscribeRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
    private val sessionManager: SessionManager,
) {
    private var cachedSubscribeInfo: SubscribeInfo? = null
    private var subscribeInfoTimestamp: Long = 0L
    private var cachedUserInfo: User? = null
    private var userInfoTimestamp: Long = 0L
    private companion object {
        const val CACHE_TTL_MS = 30_000L
    }

    private val subscribeMutex = Mutex()
    private val userMutex = Mutex()

    init {
        sessionManager.logoutEvents
            .onEach { invalidateCache() }
            .launchIn(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
    }

    /** 订阅信息：30s 内存 TTL + 磁盘缓存离线兜底 */
    suspend fun fetchSubscribeInfo(force: Boolean = false): Result<SubscribeInfo> {
        if (sessionManager.sessionState.value !is SessionState.LoggedIn) {
            return Result.failure(IllegalStateException("未登录"))
        }
        val now = System.currentTimeMillis()
        val cached = cachedSubscribeInfo
        if (!force && cached != null && now - subscribeInfoTimestamp < CACHE_TTL_MS) {
            return Result.success(cached)
        }
        return subscribeMutex.withLock {
            // 双重检查：获取锁后再次验证缓存
            val recheckCached = cachedSubscribeInfo
            if (!force && recheckCached != null && System.currentTimeMillis() - subscribeInfoTimestamp < CACHE_TTL_MS) {
                return@withLock Result.success(recheckCached)
            }
            val result = runApi {
                val session = sessionManager.sessionState.value as? SessionState.LoggedIn
                    ?: error("会话已失效")
                val info = authApi.fetchSubscribeInfo().toDomainSubscribeInfo()
                val current = sessionManager.sessionState.value as? SessionState.LoggedIn
                    ?: error("会话已失效")
                check(current.user.authData == session.user.authData) { "会话已切换" }
                cachedSubscribeInfo = info
                subscribeInfoTimestamp = System.currentTimeMillis()
                sessionStore.saveSubscribeInfo(info)
                info
            }
            if (result.isSuccess) result
            // 手动强制更新失败时不静默回退，让 UI 明确提示失败；
            // 自动刷新失败时回退磁盘缓存，保证离线可见
            else if (force) result
            else sessionStore.getSubscribeInfo()?.let { Result.success(it) } ?: result
        }
    }

    fun getCachedSubscribeInfo(): SubscribeInfo? = sessionStore.getSubscribeInfo()

    /** 上次订阅成功更新时间戳（毫秒），0 表示从未更新 */
    fun getSubscriptionUpdatedAt(): Long = sessionStore.getSubscriptionUpdatedAt()

    /** 用户信息：同订阅缓存策略；force=true 时强制请求服务端最新值（设置页开关用） */
    suspend fun fetchUserInfo(force: Boolean = false): Result<User> {
        if (sessionManager.sessionState.value !is SessionState.LoggedIn) {
            return Result.failure(IllegalStateException("未登录"))
        }
        val now = System.currentTimeMillis()
        val cached = cachedUserInfo
        if (!force && cached != null && now - userInfoTimestamp < CACHE_TTL_MS) {
            return Result.success(cached)
        }
        return userMutex.withLock {
            // 双重检查：获取锁后再次验证缓存
            val recheckCached = cachedUserInfo
            if (!force && recheckCached != null && System.currentTimeMillis() - userInfoTimestamp < CACHE_TTL_MS) {
                return@withLock Result.success(recheckCached)
            }
            val result = runApi {
                val session = sessionManager.sessionState.value as? SessionState.LoggedIn
                    ?: error("会话已失效")
                val info = authApi.fetchUserInfo()
                val current = sessionManager.sessionState.value as? SessionState.LoggedIn
                    ?: error("会话已失效")
                check(current.user.authData == session.user.authData) { "会话已切换" }
                val user = session.user.copy(
                    email = info.email,
                    balance = FormatUtils.balance(info.balance),
                    remindExpire = info.remindExpire,
                    remindTraffic = info.remindTraffic
                )
                cachedUserInfo = user
                userInfoTimestamp = System.currentTimeMillis()
                sessionStore.saveUserInfo(user)
                sessionManager.updateUser(user)
                user
            }
            if (result.isSuccess || force) result
            else sessionStore.getUserInfo()?.let { Result.success(it) } ?: result
        }
    }

    /** 读取本地持久化的用户信息缓存，离线时展示 */
    fun getCachedUserInfo(): User? = sessionStore.getUserInfo()

    /** 同步更新内存/磁盘中的用户信息缓存（开关状态变更后调用） */
    fun updateCachedUserInfo(user: User) {
        cachedUserInfo = user
        sessionStore.saveUserInfo(user)
    }

    /** 公告列表：每次强制请求最新内容，不做缓存 */
    suspend fun fetchNotices(): Result<List<Notice>> {
        if (sessionManager.sessionState.value !is SessionState.LoggedIn) {
            return Result.failure(IllegalStateException("未登录"))
        }
        return runApi {
            authApi.fetchNotices()
        }
    }

    /** 清空全部缓存（登录/切换账号/登出时调用）：内存 + 磁盘 */
    internal fun invalidateCache() {
        cachedSubscribeInfo = null
        subscribeInfoTimestamp = 0L
        cachedUserInfo = null
        userInfoTimestamp = 0L
        sessionStore.clearDataCache()
    }

    private fun ApiSubscribeInfo.toDomainSubscribeInfo() = SubscribeInfo(
        planName = planName,
        planId = planId,
        transferEnable = transferEnable,
        usedTraffic = upload + download,
        expiredAt = expiredAt,
        resetDay = resetDay,
    )
}
