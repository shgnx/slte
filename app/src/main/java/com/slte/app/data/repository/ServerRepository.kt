package com.slte.app.data.repository

import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.domain.model.ServerNode
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import com.slte.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 服务器节点仓库：拉取节点列表并缓存，网络失败时回退缓存。
 */
@Singleton
class ServerRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore
) {
    private val CACHE_TTL_MS = 30 * 60_000L

    suspend fun fetchServers(force: Boolean = false): Result<List<ServerNode>> = runApi {
        val cached = sessionStore.getServerNodes()
        val fetchedAt = sessionStore.getServerNodesFetchedAt()
        if (!force && cached != null && fetchedAt > 0L && System.currentTimeMillis() - fetchedAt < CACHE_TTL_MS) {
            debugLog("fetchServers: 缓存未过期，直接返回 ${cached.size} 个节点")
            return@runApi cached
        }

        debugLog("fetchServers: 开始请求 API...")
        val nodes = authApi.fetchServers()
        debugLog("fetchServers: 返回 ${nodes.size} 个节点")
        if (nodes.isNotEmpty()) {
            sessionStore.saveServerNodes(nodes)
        }
        nodes
    }.recoverCatching { e ->
        if (force) throw e
        val cached = sessionStore.getServerNodes()
        if (cached != null) {
            AppLog.w("SLTE-Repo", "fetchServers: 网络失败,使用本地缓存 ${cached.size} 个节点: ${sanitizeLog(e.message ?: "")}")
            cached
        } else {
            throw e
        }
    }

    /** 读取本地缓存的节点列表（不触发网络请求） */
    fun getCachedServers(): List<ServerNode>? = sessionStore.getServerNodes()

    /** 清除节点缓存，保证下次展示最新节点 */
    fun invalidateCache() {
        sessionStore.clearServerNodes()
    }

    /** 节点元数据日志仅 debug 构建输出 */
    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Repo", message)
        }
    }
}
