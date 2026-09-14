package com.slte.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 已登录用户信息。
 *
 * @param id 用户唯一标识（复用 JWT token）
 * @param email 登录邮箱，用于 UI 展示
 * @param authData JWT 认证令牌，用于认证后 API 请求
 * @param subscribeToken 订阅 token，用于订阅链接和会话身份标识
 * @param balance 账户余额（元）
 * @param remindExpire 到期邮件提醒（0=关闭, 1=开启）
 * @param remindTraffic 流量邮件提醒（0=关闭, 1=开启）
 */
@Serializable
data class User(
    val id: String,
    val email: String = "",
    val authData: String = "",
    val subscribeToken: String = "",
    val balance: String = "0.00",
    val remindExpire: Int = 0,
    val remindTraffic: Int = 0,
)
