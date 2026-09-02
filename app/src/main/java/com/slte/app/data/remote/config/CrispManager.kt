package com.slte.app.data.remote.config

import android.content.Context
import android.content.Intent
import android.util.Patterns
import im.crisp.client.external.ChatActivity
import im.crisp.client.external.Crisp
import im.crisp.client.external.EventsCallback
import im.crisp.client.external.data.message.Message
import javax.inject.Inject
import javax.inject.Singleton
import com.slte.app.utils.AppLog

/** Crisp 客服管理器：初始化 SDK、同步用户信息、打开客服页面。 */
@Singleton
class CrispManager @Inject constructor() {

    private var config: CrispConfig? = null
    private var initialized = false
    private var lastEmail: String? = null

    /** 会话就绪后重新同步邮箱 */
    private val sessionCallback = object : EventsCallback {
        override fun onSessionLoaded(sessionId: String) {
            val email = lastEmail ?: return
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Crisp.setUserEmail(email)
            }
        }

        override fun onChatOpened() = Unit
        override fun onChatClosed() = Unit
        override fun onMessageSent(message: Message) = Unit
        override fun onMessageReceived(message: Message) = Unit
        override fun onNotificationReceived(notification: Map<String, String>) = Unit
    }

    /**
     * 初始化/更新客服配置：可重复调用，远程配置就绪或更新后自动生效。
     * 配置不可用或与当前一致时跳过；websiteId 变化时重新配置 SDK。
     */
    fun init(context: Context, config: CrispConfig) {
        if (!config.enabled || config.websiteId.isNotBlank().not()) return
        if (initialized && this.config?.websiteId == config.websiteId) return
        this.config = config
        if (initialized) {
            // websiteId 变化：重置旧会话后重新配置，避免串到旧网站
            clearUser()
        }
        Crisp.configure(context.applicationContext, config.websiteId)
        if (!initialized) {
            Crisp.addCallback(sessionCallback)
        }
        initialized = true
    }

    fun setUser(email: String?, nickname: String?) {
        if (!initialized) return
        if (!email.isNullOrBlank()) {
            val normalized = email.trim()
            lastEmail = normalized
            // SDK 内部校验邮箱格式，先规范化 + 预检
            if (Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) {
                val ok = Crisp.setUserEmail(normalized)
                if (!ok) {
                    AppLog.w(TAG, "Crisp setUserEmail 返回 false（会话可能未就绪，会在会话就绪/打开聊天时重试）")
                }
            } else {
                AppLog.w(TAG, "Crisp 邮箱格式校验失败，跳过")
            }
        }
        nickname?.let { Crisp.setUserNickname(it) }
    }

    fun openChat(context: Context, email: String? = null) {
        if (!initialized) return
        // 打开聊天前再次同步登录邮箱
        email?.let { setUser(it, null) }
        context.startActivity(Intent(context, ChatActivity::class.java))
    }

    fun clearUser() {
        if (!initialized) return
        try {
            Crisp.setUserEmail("")
            Crisp.setUserNickname("")
        } catch (_: Exception) {}
    }

    fun isEnabled(): Boolean = config?.enabled == true && initialized

    private companion object {
        const val TAG = "SLTE-Crisp"
    }
}
