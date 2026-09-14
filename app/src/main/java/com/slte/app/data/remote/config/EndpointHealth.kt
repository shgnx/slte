package com.slte.app.data.remote.config

/**
 * API 地址健康状态。
 */
enum class HealthState {
    /** 健康：可正常派发请求 */
    HEALTHY,

    /** 曾失败但未达熔断阈值，继续使用但降低优先级 */
    DEGRADED,

    /** 熔断打开：退避期内不派发请求 */
    OPEN,

    /** 熔断期结束：允许放行探测请求验证是否恢复 */
    HALF_OPEN,
}

/**
 * 单个 API 地址的健康记录。
 */
data class EndpointHealth(
    val url: String,
    /** 连续失败次数（成功即清零） */
    val consecutiveFailures: Int = 0,
    /** 最近成功时间（毫秒）；0 表示从未成功 */
    val lastSuccessAt: Long = 0L,
    /** 熔断打开时间（毫秒）；0 表示未熔断 */
    val openedAt: Long = 0L,
    /** 本次熔断退避时长（打开时一次性算好，判定只读固定值，避免每次判定抖动） */
    val backoffMs: Long = 0L,
    /** 最近一次成功延迟（毫秒） */
    val lastLatencyMs: Long = 0L,
)

/**
 * 健康判定规则：熔断阈值、指数退避 + 抖动、半开恢复、主地址粘滞。
 * 纯函数，不依赖 Android 环境，便于单元测试。
 */
object EndpointHealthRules {
    /** 连续失败达到该次数即熔断 */
    const val FAILURE_THRESHOLD = 3

    /** 基础退避时长（毫秒） */
    const val BASE_BACKOFF_MS = 5_000L

    /** 退避上限（毫秒） */
    const val MAX_BACKOFF_MS = 5 * 60_000L

    /** 抖动比例：实际退避在期望值的 ±20% 内随机，避免多端同步恢复造成惊群 */
    const val JITTER_RATIO = 0.2

    /** 新地址需比当前主地址快该比例（30%）才切换，防止轻微波动导致主地址抖动 */
    const val STICKY_IMPROVE_RATIO = 0.3

    /** 当前健康状态：达到阈值且仍在退避期 = 熔断；达到阈值且退避期已过 = 半开可探测 */
    fun state(
        health: EndpointHealth,
        now: Long,
    ): HealthState = when {
        health.consecutiveFailures >= FAILURE_THRESHOLD &&
            now < health.openedAt + health.backoffMs -> HealthState.OPEN
        health.consecutiveFailures >= FAILURE_THRESHOLD -> HealthState.HALF_OPEN
        health.consecutiveFailures > 0 -> HealthState.DEGRADED
        else -> HealthState.HEALTHY
    }

    /** 是否处于熔断退避期（此时不派发常规请求） */
    fun isOpen(
        health: EndpointHealth,
        now: Long,
    ): Boolean = state(health, now) == HealthState.OPEN

    /** 请求成功：清零失败计数，记录成功时间与延迟 */
    fun onSuccess(
        health: EndpointHealth,
        latencyMs: Long,
        now: Long,
    ): EndpointHealth = EndpointHealth(
        url = health.url,
        lastSuccessAt = now,
        lastLatencyMs = latencyMs,
    )

    /** 请求失败：累计连续失败；达到阈值时记录熔断打开时间 */
    fun onFailure(
        health: EndpointHealth,
        now: Long,
    ): EndpointHealth {
        val failures = health.consecutiveFailures + 1
        if (failures < FAILURE_THRESHOLD) {
            return health.copy(consecutiveFailures = failures)
        }
        // 达到阈值：打开熔断并一次性确定退避时长（含抖动），后续判定不再重新随机
        return health.copy(
            consecutiveFailures = failures,
            openedAt = now,
            backoffMs = computeBackoffMs(failures),
        )
    }

    /** 指数退避 + 随机抖动：5s * 2^(失败数-阈值)，上限 5 分钟，±20% 抖动 */
    private fun computeBackoffMs(failures: Int): Long {
        val exponent = (failures - FAILURE_THRESHOLD).coerceAtLeast(0)
        val base = (BASE_BACKOFF_MS shl exponent.coerceAtMost(8)).coerceAtMost(MAX_BACKOFF_MS)
        val jitter = (base * JITTER_RATIO).toLong()
        val jittered = (base - jitter) + (Math.random() * 2 * jitter).toLong()
        // 抖动可能越过上限，整体收敛到 [0, 上限] 保证退避时间可控
        return jittered.coerceIn(0, MAX_BACKOFF_MS)
    }

    /**
     * 主地址粘滞判断：仅当候选延迟比当前主地址快 [STICKY_IMPROVE_RATIO] 以上才切换。
     * 轻微波动（如 <30%）不切换，避免每次刷新都在两个地址间抖动。
     */
    fun shouldSwitchPrimary(
        currentLatencyMs: Long,
        candidateLatencyMs: Long,
    ): Boolean = candidateLatencyMs < currentLatencyMs * (1 - STICKY_IMPROVE_RATIO)
}
