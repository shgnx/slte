package com.slte.app.data.remote.api.dto

/**
 * 用户订阅/套餐信息（来自 `/user/getSubscribe` 接口）。
 *
 * @param planId 套餐 ID（0 表示无套餐）
 * @param planName 套餐名称
 * @param expiredAt 套餐到期时间戳（秒）
 * @param transferEnable 总可用流量（字节）
 * @param upload 已上传流量（字节）
 * @param download 已下载流量（字节）
 */
data class SubscribeInfoDto(
    val planId: Int = 0,
    val planName: String = "",
    val expiredAt: Long = 0L,
    val transferEnable: Long = 0L,
    val upload: Long = 0L,
    val download: Long = 0L,
    val subscribeUrl: String? = null,
)
