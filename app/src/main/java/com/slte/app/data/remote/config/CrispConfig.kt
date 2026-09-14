package com.slte.app.data.remote.config

/**
 * Crisp 客服 SDK 配置。
 *
 * @param websiteId Crisp 网站 ID，从 Crisp 后台获取
 * @param enabled 是否启用客服功能
 */
data class CrispConfig(
    val websiteId: String,
    val enabled: Boolean,
)
