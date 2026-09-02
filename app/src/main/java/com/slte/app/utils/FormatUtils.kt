package com.slte.app.utils

import android.content.Context
import com.slte.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** 数值 → UI 文本格式化 */
object FormatUtils {

    /** 余额（分）→ 元字符串，如 1250 → "12.50" */
    fun balance(balanceCents: Int): String {
        val yuan = balanceCents.toDouble() / 100.0
        return "%.2f".format(yuan)
    }

    fun compactIp(ip: String): String {
        if (ip.length <= 20 || !ip.contains(':')) return ip
        return "${ip.take(13)}…${ip.takeLast(6)}"
    }

    /**
     * 字节 → 自适应单位文本（B / KB / MB / GB / TB）。
     * 保留两位小数，去除尾部多余的零。
     */
    fun traffic(bytes: Long): String {
        if (bytes <= 0L) return "0B"
        return when {
            bytes >= 1024L * 1024 * 1024 * 1024 -> tb(bytes)
            bytes >= 1024L * 1024 * 1024 -> gb(bytes)
            bytes >= 1024L * 1024 -> mb(bytes)
            bytes >= 1024 -> kb(bytes)
            else -> "${bytes}B"
        }
    }

    private fun tb(bytes: Long): String {
        val v = bytes.toDouble() / (1024.0 * 1024 * 1024 * 1024)
        return "%.2f".format(v).trimEnd('0').trimEnd('.') + "TB"
    }

    private fun gb(bytes: Long): String {
        val v = bytes.toDouble() / (1024.0 * 1024 * 1024)
        return "%.2f".format(v).trimEnd('0').trimEnd('.') + "GB"
    }

    private fun mb(bytes: Long): String {
        val v = bytes.toDouble() / (1024.0 * 1024)
        return "%.2f".format(v).trimEnd('0').trimEnd('.') + "MB"
    }

    private fun kb(bytes: Long): String {
        val v = bytes.toDouble() / 1024.0
        return "%.2f".format(v).trimEnd('0').trimEnd('.') + "KB"
    }

    /** 时间戳（秒）→ 日期字符串，如 1735689600 → "2026-01-01" */
    fun formatDate(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return ""
        return Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    /** 时间戳（秒）→ 点分日期，如 1735689600 → "2026.01.01"（到期展示用） */
    fun formatExpiryDate(epochSeconds: Long): String {
        if (epochSeconds <= 0L) return ""
        return Instant.ofEpochSecond(epochSeconds)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    }

    /**
     * 周期标识 → 本地化标签。
     *
     * V2Board 后端 period 字段枚举值映射为展示文本：
     * month_price → 月付、quarter_price → 季付、year_price → 年付 等。
     */
    fun periodLabel(period: String, context: Context): String {
        val resId = when (period) {
            "month_price" -> R.string.period_month
            "quarter_price" -> R.string.period_quarter
            "half_year_price" -> R.string.period_half_year
            "year_price" -> R.string.period_year
            "two_year_price" -> R.string.period_two_year
            "three_year_price" -> R.string.period_three_year
            "onetime_price" -> R.string.period_onetime
            "reset_price" -> R.string.period_reset
            else -> return period
        }
        return context.getString(resId)
    }
}
