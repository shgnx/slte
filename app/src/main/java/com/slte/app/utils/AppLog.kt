package com.slte.app.utils

import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque
import java.util.Locale

object AppLog {
    private const val MAX_ENTRIES = 1000
    private val buffer = ArrayDeque<String>()

    private val timeFormat = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS", Locale.US)

    private fun now(): String = LocalDateTime.now().format(timeFormat)

    fun d(
        tag: String,
        msg: String,
    ) = log(Log.DEBUG, tag, msg)

    fun i(
        tag: String,
        msg: String,
    ) = log(Log.INFO, tag, msg)

    fun w(
        tag: String,
        msg: String,
    ) = log(Log.WARN, tag, msg)

    fun e(
        tag: String,
        msg: String,
    ) = log(Log.ERROR, tag, msg)

    private fun log(
        level: Int,
        tag: String,
        msg: String,
    ) {
        val safe = sanitize(msg)
        Log.println(level, tag, safe)
        val line = "${now()} ${levelChar(level)} $tag: $safe"
        synchronized(buffer) {
            buffer.addLast(line)
            while (buffer.size > MAX_ENTRIES) buffer.removeFirst()
        }
    }

    fun dump(header: String): String {
        val body = synchronized(buffer) { buffer.joinToString("\n") }
        return "$header\n\n$body"
    }

    fun export(context: android.content.Context): java.io.File? {
        val header =
            buildString {
                appendLine("SLTE 日志导出")
                appendLine("时间: ${now()}")
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

    fun kernel(log: com.github.kr328.clash.core.model.LogMessage) {
        val level =
            when (log.level) {
                com.github.kr328.clash.core.model.LogMessage.Level.Debug -> Log.DEBUG
                com.github.kr328.clash.core.model.LogMessage.Level.Warning -> Log.WARN
                com.github.kr328.clash.core.model.LogMessage.Level.Error -> Log.ERROR
                else -> Log.INFO
            }
        log(level, "Mihomo", log.message)
    }

    fun sanitize(text: String): String = text
        .replace(TOKEN_QUERY_PATTERN, "token=***")
        .replace(BEARER_PATTERN, "Bearer ***")
        .replace(KEY_VALUE_PATTERN) { m ->
            val quote = m.groupValues[2]
            val masked = if (m.groups[2] != null) quote + "***" + quote else "***"
            m.groupValues[1] + masked
        }
        .replace(URL_CREDENTIALS_PATTERN, "$1***@")
        .replace(PATH_TOKEN_PATTERN) { m -> "/" + m.groupValues[1] + "/***" }
        .replace(PATH_LONG_SEGMENT_PATTERN) { m -> m.groupValues[1] + "/***" }
        .replace(EMAIL_PATTERN) { m ->
            val value = m.value
            val at = value.indexOf('@')
            value.take(1) + "***" + value.substring(at)
        }
        .let { out ->
            SENSITIVE_HOST_PATTERNS.fold(out) { acc, pattern -> acc.replace(pattern, "***") }
        }
        .replace(URL_HOST_PATTERN, "$1***")

    private const val SENSITIVE_KEYS =
        "subscribe_token|access_token|refresh_token|auth_data|authorization|token|password|passwd|pwd"

    private val TOKEN_QUERY_PATTERN = Regex("(?i)token=\\s*[^&\\s\"'}\\]]+")

    private val BEARER_PATTERN = Regex("(?i)bearer\\s+[A-Za-z0-9._\\-]+")

    private val KEY_VALUE_PATTERN =
        Regex(
            "(?i)((?:$SENSITIVE_KEYS)\\s*[\"']?\\s*[=:]\\s*)" +
                "(?:Bearer\\s+|Basic\\s+)?(?:([\"'])(.*?)\\2|([^\\s\"',;&{}\\]]+))",
        )

    private val URL_CREDENTIALS_PATTERN = Regex("(?i)\\b([a-z][a-z0-9+.\\-]*://)([^\\s/]+)@")

    private val PATH_TOKEN_PATTERN =
        Regex("(?i)/(s|sub|subs|subscribe|link|token|t)/([^/\\s\"'?#]{12,})")

    private val PATH_LONG_SEGMENT_PATTERN =
        Regex("(^|[^:/])/([^/\\s\"'?#.{}]{16,})(?=/|\\?|#|\\s|$)")

    private val EMAIL_PATTERN = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")

    private val URL_HOST_PATTERN = Regex("(?i)((?:https?|wss?)://)([^/\\s\"'<>]+)")

    private val SENSITIVE_HOST_PATTERNS: List<Regex> =
        buildList {
            fun hostOf(url: String): String? = url
                .trim()
                .substringAfter("://", "")
                .substringBefore("/")
                .takeIf { it.isNotBlank() }

            val hosts = mutableListOf<String>()
            runCatching {
                hostOf(com.slte.app.BuildConfig.API_BASE_URL)?.let { hosts.add(it.lowercase()) }
                com.slte.app.BuildConfig.REMOTE_CONFIG_URLS
                    .split(',')
                    .mapNotNull { hostOf(it) }
                    .forEach { if (it.lowercase() !in hosts) hosts.add(it.lowercase()) }
                com.slte.app.BuildConfig.ALLOWED_DOMAINS
                    .split(',')
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
