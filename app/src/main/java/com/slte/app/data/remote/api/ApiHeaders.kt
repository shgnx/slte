package com.slte.app.data.remote.api

/**
 * 面板 API 请求头常量。
 *
 * 这些值此前在 Xboard 与 V2Board 两个 Retrofit 接口里各写了一份字面量，
 * 而 `X-SLTE-No-Failover` 的名字在 [com.slte.app.data.remote.config.ApiFailoverInterceptor]
 * 里还有第三份（拦截器读它决定是否跳过 failover）。三份字符串靠人眼保持同步，
 * 改名时漏一处就会让「禁止 failover」静默失效——本文件是唯一真源。
 */
object ApiHeaders {
    /**
     * 对外伪装的内核 UA：部分面板会按 UA 做兼容判断。
     * 升级 mihomo 内核时应同步此处，否则会与真实内核版本漂移。
     */
    const val USER_AGENT_NAME = "User-Agent"
    const val USER_AGENT_VALUE = "ClashMetaForAndroid/2.11.32"

    /** 标记「本请求跳过地址 failover」（如订阅下载走固定地址） */
    const val NO_FAILOVER_NAME = "X-SLTE-No-Failover"

    /** Retrofit `@Headers` 需要完整的 "Name: Value" 行，故在此拼好 */
    const val USER_AGENT_HEADER = "$USER_AGENT_NAME: $USER_AGENT_VALUE"

    const val NO_FAILOVER_HEADER = "$NO_FAILOVER_NAME: 1"
}
