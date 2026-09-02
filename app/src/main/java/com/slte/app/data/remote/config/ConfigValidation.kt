package com.slte.app.data.remote.config

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * 配置校验与择优规则：URL/域名白名单校验、版本比较、多源择优。
 * 纯函数便于单测。
 */
internal object ConfigValidation {

    /** API 地址合法：https 且主机在自有域名白名单内（凭据只发往受信域） */
    fun isValidApiUrl(value: String, allowedSuffixes: List<String>): Boolean {
        val url = value.trim().toHttpUrlOrNull() ?: return false
        if (url.scheme != "https") return false
        val host = url.host.lowercase()
        return allowedSuffixes.any { host == it || host.endsWith(".$it") }
    }

    /** 直连域名合法：注册域名格式（两段以上 label）且在自有域名白名单内 */
    fun isValidDomain(value: String, allowedSuffixes: List<String>): Boolean {
        val host = value.trim().lowercase().trimEnd('.')
        if (host.isEmpty() || host.length > 253) return false
        val labels = host.split(".")
        if (labels.size < 2 || labels.any { it.isEmpty() || it.length > 63 }) return false
        return allowedSuffixes.any { host == it || host.endsWith(".$it") }
    }

    /**
     * 配置版本比较：数字分段逐段比较（如 "1.10" > "1.9"、"2.0" > "1.99"）。
     * 非数字段按 0 处理；空串视为最低版本。
     */
    fun compareVersions(a: String, b: String): Int {
        val pa = a.trim().split('.', '-').map { it.toIntOrNull() ?: 0 }
        val pb = b.trim().split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }

    /** 缓存是否处于短缓存窗口内（窗口内非强制刷新直接复用，避免重复拉取） */
    fun isCacheFresh(fetchedAt: Long, now: Long, ttlMs: Long): Boolean =
        fetchedAt > 0 && now - fetchedAt < ttlMs

    /**
     * API 候选解码：Base64 编码的地址解码后返回；非 Base64 或解码结果不是 URL 时返回原文。
     * 明文 https URL 含 ':' '/' '.' 等非 Base64 字符，解码必然失败，天然不会被误解码；
     * 解码结果必须是 http(s) URL 才采用，防止任意 Base64 字符串被当地址。
     */
    fun decodeApiCandidate(raw: String): String {
        val trimmed = raw.trim()
        val decoded = try {
            String(java.util.Base64.getDecoder().decode(trimmed), Charsets.UTF_8).trim()
        } catch (_: Exception) {
            return trimmed
        }
        return if (decoded.startsWith("https://") || decoded.startsWith("http://")) decoded else trimmed
    }

    /** 多源择优：版本最高者胜出；版本相同取延迟最小（最先完成的镜像） */
    fun pickBest(configs: List<FetchedConfig>): FetchedConfig? =
        configs.minWithOrNull { a, b ->
            val v = compareVersions(b.version, a.version)
            if (v != 0) v else a.latencyMs.compareTo(b.latencyMs)
        }
}
