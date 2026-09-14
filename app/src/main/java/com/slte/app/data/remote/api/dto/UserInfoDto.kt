package com.slte.app.data.remote.api.dto

/**
 * 用户信息（来自 `/user/info` 接口）。
 *
 * @param email 用户邮箱
 * @param balance 账户余额，单位：分（应用层自行转换为元展示）
 * @param planId 当前套餐 ID（0 表示无套餐）
 * @param expiredAt 套餐到期时间戳（秒），0 表示无套餐
 * @param transferEnable 总可用流量（字节）
 * @param remindExpire 到期邮件提醒（0=关闭, 1=开启）
 * @param remindTraffic 流量邮件提醒（0=关闭, 1=开启）
 */
data class UserInfoDto(
    val email: String,
    val balance: Int,
    val planId: Int = 0,
    val expiredAt: Long = 0L,
    val transferEnable: Long = 0L,
    val remindExpire: Int = 0,
    val remindTraffic: Int = 0,
)
