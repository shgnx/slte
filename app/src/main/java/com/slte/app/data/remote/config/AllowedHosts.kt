package com.slte.app.data.remote.config

import com.slte.app.BuildConfig

/**
 * 自有域名白名单：凭据外发的唯一安全边界，添加域名须评审。
 *
 * 白名单 = 内置占位域 + 构建期 `SLTE_ALLOWED_DOMAINS`（构建脚本已自动并入 API 与
 * 远程配置源域名，见 `CONFIG.md`）。新增携带凭据的绝对地址请求时必须同步接入校验：
 * - [RemoteConfig]：远程下发的 API/直连/APK 地址校验；
 * - [com.slte.app.data.remote.AuthInterceptor]：Authorization 只发往白名单域；
 * - [com.slte.app.data.remote.SubscribeSourceImpl]：订阅下载地址校验。
 */
internal object AllowedHosts {
    /** 允许的域后缀：主机等于某项或为其子域才放行 */
    val SUFFIXES: List<String> =
        buildList {
            // 占位域：自有 API 域名后缀在此配置（与 kernel-core process.go directDomains 保持同步）
            add("example.com")
            BuildConfig.ALLOWED_DOMAINS
                .split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .forEach { if (it !in this) add(it) }
        }

    /** 主机是否在白名单内（大小写不敏感） */
    fun isAllowedHost(host: String?): Boolean = !host.isNullOrBlank() && ConfigValidation.isHostAllowed(host.lowercase(), SUFFIXES)

    /** 完整 URL 是否合法且主机在白名单内（要求 https） */
    fun isAllowedUrl(url: String?): Boolean = !url.isNullOrBlank() && ConfigValidation.isValidApiUrl(url, SUFFIXES)
}
