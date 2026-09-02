package com.slte.app.data.remote.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** 单地址健康快照（对外只读） */
data class EndpointSnapshot(
    val url: String,
    val state: HealthState,
    val consecutiveFailures: Int,
    val lastLatencyMs: Long
)

/** 选择器整体状态：当前主地址 + 各候选健康快照（UI/日志可订阅） */
data class EndpointSelectionState(
    val primary: String?,
    val endpoints: List<EndpointSnapshot>
)

/**
 * API 端点选择器：维护各候选地址的健康状态，提供候选排序与主地址粘滞选择。
 *
 * 熔断后自动退避（指数 + 抖动），到期进入半开允许探测恢复；
 * 主地址优先粘滞，仅当新地址明显更快或当前主地址不健康时才切换。
 * 状态经 [state] 流对外通知，切换不触发上层崩溃。
 */
@Singleton
class EndpointSelector @Inject constructor() {

    private val healthMap = ConcurrentHashMap<String, EndpointHealth>()

    private val _state = MutableStateFlow(EndpointSelectionState(null, emptyList()))
    /** 当前主地址与各候选健康状态 */
    val state: StateFlow<EndpointSelectionState> = _state.asStateFlow()

    /** 记录请求成功：清零失败计数并记录延迟 */
    fun recordSuccess(url: String, latencyMs: Long) {
        val now = System.currentTimeMillis()
        healthMap[url] = EndpointHealthRules.onSuccess(
            healthMap[url] ?: EndpointHealth(url),
            latencyMs,
            now
        )
        refreshState()
    }

    /** 记录请求/探测失败：累计连续失败，达到阈值进入熔断（日志由调用方负责） */
    fun recordFailure(url: String) {
        val now = System.currentTimeMillis()
        healthMap[url] = EndpointHealthRules.onFailure(
            healthMap[url] ?: EndpointHealth(url),
            now
        )
        refreshState()
    }

    /** 启动探测结果（探活成功）：仅记录延迟与健康，不切换主地址 */
    fun recordProbe(url: String, latencyMs: Long) {
        healthMap[url] = EndpointHealthRules.onSuccess(
            healthMap[url] ?: EndpointHealth(url),
            latencyMs,
            System.currentTimeMillis()
        )
        refreshState()
    }

    /** 是否处于熔断退避期（此时不派发常规请求） */
    fun isOpen(url: String, now: Long = System.currentTimeMillis()): Boolean =
        healthMap[url]?.let { EndpointHealthRules.isOpen(it, now) } ?: false

    /**
     * 半开候选：退避期已过、允许放行恢复探测的地址。
     * 由独立探测调度（RemoteConfig.probeLoop）定期消费，实现熔断后的及时恢复，
     * 不依赖下一次配置刷新。
     */
    fun halfOpenCandidates(now: Long = System.currentTimeMillis()): List<String> =
        healthMap.values
            .filter { EndpointHealthRules.state(it, now) == HealthState.HALF_OPEN }
            .map { it.url }
            .sorted()

    /**
     * 候选排序：主地址在前（粘滞），其余按健康度排列，熔断中的排最后。
     * 半开（退避期已过）的地址保留在候选内，允许后续探测恢复。
     */
    fun candidateOrder(primary: String, candidates: List<String>): List<String> {
        val now = System.currentTimeMillis()
        val rest = candidates.filter { it != primary }
        val ordered = rest.sortedWith(
            compareBy { healthRank(healthMap[it], now) }
        )
        return listOf(primary) + ordered
    }

    /**
     * 主地址粘滞选择：
     * - 当前主地址仍健康（未熔断）且探测延迟已知 → 保持粘滞，不因轻微波动切换；
     * - 当前主地址不健康（熔断/失败）→ 在其余候选中选延迟最小者；
     * - 其余候选比当前主地址快 [EndpointHealthRules.STICKY_IMPROVE_RATIO] 以上才切换。
     */
    fun pickPrimary(
        candidates: List<String>,
        probes: Map<String, Long>,
        currentPrimary: String?,
        now: Long = System.currentTimeMillis()
    ): String? {
        if (candidates.isEmpty()) return null
        val healthy = probes.filterKeys { it in candidates }
        if (healthy.isEmpty()) return currentPrimary?.takeIf { it in candidates } ?: candidates.first()

        val currentHealth = currentPrimary?.let { healthMap[it] }
        val currentOpen = currentPrimary != null && EndpointHealthRules.isOpen(currentHealth ?: EndpointHealth(currentPrimary), now)
        val currentLatency = currentPrimary?.let { healthy[it] }

        if (!currentOpen && currentLatency != null && currentPrimary in candidates) {
            val faster = healthy.entries
                .filter { it.key != currentPrimary }
                .filter { EndpointHealthRules.shouldSwitchPrimary(currentLatency, it.value) }
                .minByOrNull { it.value }
            return faster?.key ?: currentPrimary
        }

        return healthy.minByOrNull { it.value }?.key ?: candidates.first()
    }

    private fun healthRank(health: EndpointHealth?, now: Long): Int = when {
        health == null -> 0
        EndpointHealthRules.state(health, now) == HealthState.OPEN -> 3
        EndpointHealthRules.state(health, now) == HealthState.HALF_OPEN -> 2
        EndpointHealthRules.state(health, now) == HealthState.DEGRADED -> 1
        else -> 0
    }

    private fun refreshState() {
        val now = System.currentTimeMillis()
        _state.value = EndpointSelectionState(
            primary = _state.value.primary,
            endpoints = healthMap.entries.sortedBy { it.key }.map { (url, h) ->
                EndpointSnapshot(
                    url = url,
                    state = EndpointHealthRules.state(h, now),
                    consecutiveFailures = h.consecutiveFailures,
                    lastLatencyMs = h.lastLatencyMs
                )
            }
        )
    }

    /** 供 RemoteConfig 在竞速选主后更新对外状态 */
    internal fun updatePrimary(primary: String?) {
        _state.value = _state.value.copy(primary = primary)
    }
}
