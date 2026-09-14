package com.slte.app.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.model.User
import com.slte.app.kernel.SpeedResultStore
import com.slte.app.utils.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 加密持久化登录会话与订阅/节点/测速缓存。
 *
 * 依赖收窄为 [SharedPreferences]：Android 侧由 DI 注入加密实现（见 StorageModule），
 * 单元测试注入内存实现即可覆盖全部读写/容错分支。
 */
@Singleton
class SessionStore
@Inject
constructor(
    @SessionPrefs private val prefs: SharedPreferences,
) : SpeedResultStore {

    fun hasSession(): Boolean = prefs.contains(KEY_AUTH_DATA) && prefs.contains(KEY_SUBSCRIBE_TOKEN)

    fun getAuthData(): String? = prefs.getString(KEY_AUTH_DATA, null)

    fun getEmail(): String? = prefs.getString(KEY_EMAIL, null)

    fun getSubscribeToken(): String? = prefs.getString(KEY_SUBSCRIBE_TOKEN, null)

    fun getSubscribeUrl(): String? = prefs.getString(KEY_SUBSCRIBE_URL, null)

    fun saveSubscribeUrl(url: String) {
        prefs.edit { putString(KEY_SUBSCRIBE_URL, url) }
    }

    /** 保存登录会话 */
    fun save(
        authData: String,
        email: String,
        subscribeToken: String,
    ) {
        prefs.edit {
            putString(KEY_AUTH_DATA, authData)
            putString(KEY_EMAIL, email)
            putString(KEY_SUBSCRIBE_TOKEN, subscribeToken)
        }
    }

    /** 读取缓存并反序列化；解析失败（缓存损坏）时留痕并清除损坏键。 */
    private inline fun <reified T> readCached(
        key: String,
        decode: (String) -> T,
    ): T? {
        val raw = prefs.getString(key, null) ?: return null
        return try {
            decode(raw)
        } catch (e: Exception) {
            AppLog.w("SLTE-Session", "SessionStore: 缓存解析失败 key=$key: ${e.message}")
            prefs.edit { remove(key) }
            null
        }
    }

    /** 保存订阅信息缓存（流量/到期时间，网络不可用时兜底显示） */
    fun saveSubscribeInfo(info: SubscribeInfo) {
        prefs.edit {
            putString(KEY_SUBSCRIBE_INFO, Json.encodeToString(info))
        }
    }

    /** 读取订阅信息缓存；无缓存或解析失败返回 null */
    fun getSubscribeInfo(): SubscribeInfo? = readCached(KEY_SUBSCRIBE_INFO) { Json.decodeFromString<SubscribeInfo>(it) }

    /** 保存用户信息缓存（邮箱/余额），离线时展示 */
    fun saveUserInfo(user: User) {
        prefs.edit {
            putString(KEY_USER_INFO, Json.encodeToString(user))
        }
    }

    /** 读取用户信息缓存；无缓存或解析失败返回 null */
    fun getUserInfo(): User? = readCached(KEY_USER_INFO) { Json.decodeFromString<User>(it) }

    /** 记录订阅 YAML 成功更新时间戳 */
    fun saveSubscriptionUpdatedAt(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit { putLong(KEY_SUBSCRIPTION_UPDATED_AT, timestamp) }
    }

    /** 上次订阅 YAML 成功更新时间戳（毫秒），0 表示从未更新 */
    fun getSubscriptionUpdatedAt(): Long = prefs.getLong(KEY_SUBSCRIPTION_UPDATED_AT, 0L)

    /** 节点缓存写入时间戳（毫秒）；无缓存返回 0 */
    fun getServerNodesFetchedAt(): Long = prefs.getLong(KEY_SERVER_NODES_FETCHED_AT, 0L)

    /** 保存节点列表缓存（含密码等敏感字段，必须加密存储） */
    fun saveServerNodes(nodes: List<ServerNode>) {
        prefs.edit {
            putString(KEY_SERVER_NODES, Json.encodeToString(nodes))
            putLong(KEY_SERVER_NODES_FETCHED_AT, System.currentTimeMillis())
        }
    }

    /** 读取节点列表缓存；无缓存或解析失败返回 null */
    fun getServerNodes(): List<ServerNode>? = readCached(KEY_SERVER_NODES) { Json.decodeFromString<List<ServerNode>>(it) }

    /** 清除节点缓存（订阅更新后调用，强制下次拉取最新节点） */
    fun clearServerNodes() {
        prefs.edit {
            remove(KEY_SERVER_NODES)
            remove(KEY_SERVER_NODES_FETCHED_AT)
        }
    }

    /** 保存测速结果缓存（节点名 → 延迟毫秒），重启后仍可展示 */
    override fun saveSpeedResults(results: Map<String, Int>) {
        prefs.edit {
            putString(KEY_SPEED_RESULTS, Json.encodeToString(results))
        }
    }

    /** 读取测速结果缓存；无缓存或解析失败返回 null */
    override fun getSpeedResults(): Map<String, Int>? = readCached(KEY_SPEED_RESULTS) { Json.decodeFromString<Map<String, Int>>(it) }

    /** 清除登录会话（退出登录时调用） */
    fun clear() {
        prefs.edit {
            remove(KEY_AUTH_DATA)
            remove(KEY_EMAIL)
            remove(KEY_SUBSCRIBE_TOKEN)
            remove(KEY_SUBSCRIBE_URL)
            remove(KEY_SUBSCRIBE_INFO)
            remove(KEY_SUBSCRIPTION_UPDATED_AT)
            remove(KEY_USER_INFO)
            remove(KEY_SERVER_NODES)
            remove(KEY_SERVER_NODES_FETCHED_AT)
            remove(KEY_SPEED_RESULTS)
        }
    }

    /** 清除数据缓存（订阅/用户/节点），保留会话凭证；登录/改密重登后强制下次拉取新数据 */
    fun clearDataCache() {
        prefs.edit {
            remove(KEY_SUBSCRIBE_URL)
            remove(KEY_SUBSCRIBE_INFO)
            remove(KEY_SUBSCRIPTION_UPDATED_AT)
            remove(KEY_USER_INFO)
            remove(KEY_SERVER_NODES)
            remove(KEY_SERVER_NODES_FETCHED_AT)
            remove(KEY_SPEED_RESULTS)
        }
    }

    internal companion object {
        /** 加密偏好文件名（DI 侧用，测试注入内存实现时无需关心） */
        internal const val PREFS_NAME = "slte_session"
        internal const val KEY_ALIAS = "slte_session_master_key"
        private const val KEY_AUTH_DATA = "auth_data"
        private const val KEY_EMAIL = "email"
        private const val KEY_SUBSCRIBE_TOKEN = "subscribe_token"
        private const val KEY_SUBSCRIBE_URL = "subscribe_url"
        private const val KEY_SUBSCRIBE_INFO = "subscribe_info"
        private const val KEY_SUBSCRIPTION_UPDATED_AT = "subscription_updated_at"
        private const val KEY_USER_INFO = "user_info"
        private const val KEY_SERVER_NODES = "server_nodes"
        private const val KEY_SERVER_NODES_FETCHED_AT = "server_nodes_fetched_at"
        private const val KEY_SPEED_RESULTS = "speed_results"
    }
}
