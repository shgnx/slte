package com.slte.app.ui.screen.main

import com.slte.app.utils.Constants

/** 首页仪表盘数据 */
data class DashboardData(
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isValid: Boolean = false,
    val hasPlan: Boolean = false,
    val planName: String = "",
    val daysUntilExpired: Int = 0,
    val expiredAt: Long = 0L,
    val serverName: String = Constants.PLACEHOLDER_DASH,
    val serverSelection: String = Constants.SELECTION_AUTO,
    val proxyMode: String = Constants.DEFAULT_PROXY_MODE,
    val currentIp: String = Constants.PLACEHOLDER_DASH,
    /** 当前出口 IP 对应国家 ISO 码（小写），null 表示未识别 */
    val ipCountryCode: String? = null,
    /** 当前出口 IP 归属地中文名（由国家码映射），空表示未识别 */
    val ipRegion: String = "",
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isRefreshing: Boolean = false,
    val dataLoaded: Boolean = false,
    val errorMessageRes: Int? = null,
    val isUpdating: Boolean = false,
)

