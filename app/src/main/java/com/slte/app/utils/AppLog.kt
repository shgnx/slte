package com.slte.app.utils

import android.util.Log
import com.slte.app.R
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * 应用日志收集器：所有日志同时输出 logcat 与内存环形缓冲，
 * 供"日志导出"功能导出为 TXT（导出前统一脱敏）。
 */
object AppLog {
    private const val MAX_ENTRIES = 1000
    private val buffer = ArrayDeque<String>()
    /** SimpleDateFormat 非线程安全：每线程独立实例 */
    private val timeFormat = ThreadLocal.withInitial {
        SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)
    }

    fun d(tag: String, msg: String) = log(Log.DEBUG, tag, msg)
    fun i(tag: String, msg: String) = log(Log.INFO, tag, msg)
    fun w(tag: String, msg: String) = log(Log.WARN, tag, msg)
    fun e(tag: String, msg: String) = log(Log.ERROR, tag, msg)

    private fun log(level: Int, tag: String, msg: String) {
        // 统一出口脱敏：logcat 与内存缓冲都不落明文 token/密码/真实域名
        val safe = sanitize(msg)
        Log.println(level, tag, safe)
        val line = "${timeFormat.get().format(Date())} ${levelChar(level)} $tag: $safe"
        synchronized(buffer) {
            buffer.addLast(line)
            while (buffer.size > MAX_ENTRIES) buffer.removeFirst()
        }
    }

    /** 导出全部收集日志（已脱敏），含文件头信息 */
    fun dump(header: String): String {
        val body = synchronized(buffer) { buffer.joinToString("\n") }
        return "$header\n\n$body"
    }

    /** 导出日志到 app 专属 Download 目录，返回导出文件；失败返回 null */
    fun export(context: android.content.Context): java.io.File? {
        val header = buildString {
            appendLine("SLTE 日志导出")
            appendLine("时间: ${timeFormat.get().format(Date())}")
            appendLine("应用版本: ${com.slte.app.BuildConfig.VERSION_NAME} (${com.slte.app.BuildConfig.VERSION_CODE})")
            appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
            appendLine("设备: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
            appendLine("说明: 含应用层 + mihomo 内核日志；已脱敏（token/密码/邮箱等打码），可直接发送给客服")
        }
        val content = sanitize(dump(header))
        return try {
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(dir, "SLTE_log_${System.currentTimeMillis()}.txt")
            file.writeText(content)
            file
        } catch (e: Exception) {
            null
        }
    }

    /** 内核（mihomo）日志：经 ILogObserver 桥接实时收集，随日志导出一起输出 */
    fun kernel(log: com.github.kr328.clash.core.model.LogMessage) {
        val level = when (log.level) {
            com.github.kr328.clash.core.model.LogMessage.Level.Debug -> Log.DEBUG
            com.github.kr328.clash.core.model.LogMessage.Level.Warning -> Log.WARN
            com.github.kr328.clash.core.model.LogMessage.Level.Error -> Log.ERROR
            else -> Log.INFO
        }
        log(level, "Mihomo", log.message)
    }

    /** 脱敏：订阅 token、auth 凭证、密码等敏感内容打码 */
    fun sanitize(text: String): String = text
        .replace(Regex("(?i)token=\\s*[^&\\s\"']+"), "token=***")
        .replace(Regex("(?i)(password|passwd|pwd)([=:\"'])(\\s*)[^\\s\"']+"), "$1$2$3***")
        .replace(Regex("(?i)(authorization|auth_data)([=:\"'])(\\s*)[^\\s\"']+"), "$1$2$3***")
        .replace(Regex("(?i)bearer\\s+[A-Za-z0-9._\\-]+"), "Bearer ***")
        // JSON/引号键形式："token":"xxx"、"subscribe_token": "xxx"
        .replace(Regex("(?i)(\"(?:token|subscribe_token|auth_data|password|passwd|pwd)\"\\s*:\\s*\")[^\"]*(\")"), "$1***$2")
        // 冒号/等号形式：token: xxx、auth_data:xxx
        .replace(Regex("(?i)(token|subscribe_token|auth_data|password|passwd|pwd)([=:])(\\s*)[^\\s,\"'}&]+"), "$1$2$3***")
        // 邮箱脱敏：保留域名，本地部分打码（x***@example.com）
        .replace(EMAIL_PATTERN) { m ->
            val value = m.value
            val at = value.indexOf('@')
            value.take(1) + "***" + value.substring(at)
        }
        // 构建期注入的真实 API/OSS 域名：无论是否带 scheme 一律打码（白名单域同时是日志敏感域）
        .let { out ->
            SENSITIVE_HOST_PATTERNS.fold(out) { acc, pattern -> acc.replace(pattern, "***") }
        }
        // URL 主机打码：https?://host[:port] → https?://***
        .replace(URL_HOST_PATTERN, "$1***")

    private val EMAIL_PATTERN = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

    /** URL 主机（含端口）打码 */
    private val URL_HOST_PATTERN = Regex("(?i)(https?://)([^/\\s\"'<>]+)")

    /** 构建期注入的真实域名列表（API 主域 + OSS 配置源域 + 白名单后缀），日志打码用；测试/占位构建为空或仅占位域 */
    private val SENSITIVE_HOST_PATTERNS: List<Regex> = buildList {
        fun hostOf(url: String): String? =
            url.trim().substringAfter("://", "").substringBefore("/").takeIf { it.isNotBlank() }

        val hosts = mutableListOf<String>()
        runCatching {
            hostOf(com.slte.app.BuildConfig.API_BASE_URL)?.let { hosts.add(it.lowercase()) }
            com.slte.app.BuildConfig.REMOTE_CONFIG_URLS.split(',')
                .mapNotNull { hostOf(it) }
                .forEach { if (it.lowercase() !in hosts) hosts.add(it.lowercase()) }
            com.slte.app.BuildConfig.ALLOWED_DOMAINS.split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .forEach { if (it !in hosts) hosts.add(it) }
        }
        hosts.forEach { add(Regex("(?i)" + Regex.escape(it))) }
    }

    private fun levelChar(level: Int): String = when (level) {
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        else -> "?"
    }
}
