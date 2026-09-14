package com.slte.app.data.remote

import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.data.remote.config.AllowedHosts
import com.slte.app.data.remote.config.RemoteConfig
import com.slte.app.kernel.SubscribeSource
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.ResponseBody

/** 订阅源实现：内存 token + 通过 AuthApi 契约下载订阅 YAML（与具体后端无关） */
@Singleton
class SubscribeSourceImpl
@Inject
constructor(
    private val sessionStore: SessionStore,
    private val authApi: AuthApi,
    private val remoteConfig: RemoteConfig,
) : SubscribeSource {
    override fun getEmail(): String? = sessionStore.getEmail()

    override suspend fun fetchSubscribeYaml(): ResponseBody? {
        val url =
            trustedSubscribeUrl()
                ?: sessionStore.getSubscribeToken()?.let { subscribeFetchUrl(remoteConfig.data.apiBaseUrl, it) }
                ?: return null
        return authApi.fetchSubscribeYaml(url)
    }

    /**
     * 面板下发的 subscribe_url 属服务端数据，须与远程配置同等对待：
     * 仅当为 https 且主机在自有域名白名单内才采用，否则回退到「构建期订阅路径 + 内存 token」。
     * 该地址会随请求携带凭据，非白名单地址可能导致凭证外泄。
     */
    private fun trustedSubscribeUrl(): String? {
        val raw = sessionStore.getSubscribeUrl()?.takeIf { it.isNotBlank() } ?: return null
        if (AllowedHosts.isAllowedUrl(raw)) return raw.trim()
        AppLog.w(
            "SLTE-Subscribe",
            "订阅地址被拒（须 https 且主机在编译期白名单内）: ${sanitizeLog(raw)}",
        )
        return null
    }

    override fun saveSubscriptionUpdatedAt() = sessionStore.saveSubscriptionUpdatedAt()
}
