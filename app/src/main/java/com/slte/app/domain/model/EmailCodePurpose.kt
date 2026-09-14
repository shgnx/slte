package com.slte.app.domain.model

/**
 * 邮箱验证码用途：区分验证码是用于注册还是忘记密码。
 */
enum class EmailCodePurpose {
    REGISTER,
    FORGOT_PASSWORD,
}
