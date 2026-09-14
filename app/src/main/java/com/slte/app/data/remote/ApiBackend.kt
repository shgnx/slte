package com.slte.app.data.remote

/**
 * API 路径前缀的唯一定义处，其余用法一律从 [PREFIX] 派生。
 *
 * 此前前缀在 [AuthRules.AUTH_API_PREFIX]、RemoteConfig.PROBE_PATH 与构建脚本的
 * SLTE_SUBSCRIBE_PATH 各写一份，面板升级 v2 时漏改任一处都是静默故障：例如认证前缀不匹配
 * 会让「凭证过期后自动登出」失效（403 判定永远落空）。故统一收敛到本对象。
 */
object ApiPaths {
    /** API 版本前缀（[ApiBackend] 的默认值） */
    const val PREFIX = "/api/v1"

    /** 认证接口前缀：仅该前缀下的 403 才判为登录态失效，避免业务 403 误登出 */
    const val AUTH = "$PREFIX/user/"

    /** 游客探活路径：多地址竞速前用它确认某个候选地址真的可用 */
    const val GUEST_CONFIG = "$PREFIX/guest/comm/config"
}

data class ApiBackend(
    val type: String,
    val baseUrl: String,
    val apiPrefix: String = ApiPaths.PREFIX,
)
