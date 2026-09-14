package com.slte.app.data.remote.api.dto

/**
 * 登录/注册接口统一响应。
 */
data class LoginResponseDto(
    val token: String,
    val authData: String,
)
