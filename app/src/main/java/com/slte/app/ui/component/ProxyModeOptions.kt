package com.slte.app.ui.component

import com.slte.app.R
import com.slte.app.utils.Constants

/** 代理模式选项：mode 为内核/持久化层的稳定标识（不随语言变化），文案经资源按语言解析 */
internal data class ProxyModeOption(
    val mode: String,
    val labelRes: Int,
    val descRes: Int,
)

/** 代理模式选项列表（规则/全局） */
internal val PROXY_MODE_OPTIONS =
    listOf(
        ProxyModeOption(
            Constants.DEFAULT_PROXY_MODE,
            R.string.dashboard_proxy_rule,
            R.string.proxy_rule_desc,
        ),
        ProxyModeOption(
            Constants.PROXY_MODE_GLOBAL,
            R.string.dashboard_proxy_global,
            R.string.proxy_global_desc,
        ),
    )

/** 代理模式稳定标识 → 显示文案资源；未知标识返回 null（调用方兜底展示原值） */
internal fun proxyModeLabelRes(mode: String): Int? = when (mode) {
    Constants.DEFAULT_PROXY_MODE -> R.string.dashboard_proxy_rule
    Constants.PROXY_MODE_GLOBAL -> R.string.dashboard_proxy_global
    Constants.PROXY_MODE_DIRECT -> R.string.dashboard_proxy_direct
    Constants.PROXY_MODE_SCRIPT -> R.string.dashboard_proxy_script
    else -> null
}
