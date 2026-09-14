package com.slte.app.domain.model

/**
 * 注册页配置：由后台决定注册表单需要哪些字段。
 */
data class RegisterConfig(
    /** 是否启用邮箱验证码注册 */
    val emailVerifyEnabled: Boolean = false,
    /** 是否强制填写邀请码（false 时邀请码为选填） */
    val inviteForceEnabled: Boolean = false,
)
