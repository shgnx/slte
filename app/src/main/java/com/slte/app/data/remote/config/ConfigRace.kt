package com.slte.app.data.remote.config

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * 单个配置源的成功结果。
 *
 * @param url 配置源地址
 * @param raw 原始响应体
 * @param version 配置版本号（空 = 未声明版本）
 * @param latencyMs 拉取耗时（毫秒）
 * @param notModified 是否命中 ETag/Last-Modified 返回 304（内容未变，应复用本地缓存）
 */
internal data class FetchedConfig(
    val url: String,
    val raw: String,
    val version: String,
    val latencyMs: Long,
    val notModified: Boolean = false,
)

/** 多源并发竞速结果 */
internal data class RaceResult(
    /** 择优选中的配置；null = 全部源失败或非法 */
    val chosen: FetchedConfig?,
    /** 返回合法配置（含 304 命中）的源地址；用于记录"上次成功地址" */
    val lastUrl: String?,
)

/**
 * 多配置源并发竞速：并发发起所有源，收集合法结果后按版本择优。
 *
 * 任一源失败/非法不影响其他源；整体耗时受调用方 withTimeout 约束，超时/取消时
 * 全部子请求随结构化并发一并取消。fetch 由调用方注入，便于解耦单元测试。
 */
internal object ConfigRace {
    suspend fun race(
        urls: List<String>,
        fetch: suspend (String) -> FetchedConfig?,
    ): RaceResult {
        if (urls.isEmpty()) return RaceResult(null, null)
        return coroutineScope {
            val results = urls.map { url -> async { url to fetch(url) } }.awaitAll()
            val valid = results.mapNotNull { (url, cfg) -> cfg?.copy(url = url) }
            val chosen = ConfigValidation.pickBest(valid)
            RaceResult(
                chosen = chosen,
                lastUrl = chosen?.url,
            )
        }
    }
}
