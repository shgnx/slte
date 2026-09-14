package com.slte.app.domain.model

/**
 * 全局会话状态。
 * - [SessionState.Loading]: 启动中，正在恢复会话
 * - [SessionState.LoggedOut]: 未登录
 * - [SessionState.LoggedIn]: 已登录，携带当前用户
 */
sealed interface SessionState {
    data object Loading : SessionState

    data object LoggedOut : SessionState

    data class LoggedIn(
        val user: User,
    ) : SessionState
}
