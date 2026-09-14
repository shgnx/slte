package com.slte.app.kernel

import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.service.remote.IClashManager
import com.slte.app.utils.AppLog
import com.slte.app.utils.Constants
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/**
 * KernelProxy 测速扩展：节点测速、预热、自动测速。
 * 与 KernelProxy 同包，访问其 internal 成员。
 */

/** 节点测速：健康检查后返回 节点名 → 延迟毫秒（未测通=DELAY_TIMEOUT） */
suspend fun KernelProxy.speedTest(): Map<String, Int> = safe(emptyMap()) {
    val clash = manager.clash()
    if (clash == null) {
        AppLog.d("SLTE-Kernel", "speedTest: clash=null")
        return@safe emptyMap()
    }
    if (selectorGroup() == null) {
        // 未连接 VPN 时内核未加载配置：导入订阅 + 后台加载后即可测速
        config.ensureProfile()
        clash.loadActiveProfile()
        if (waitForGroups() == null) return@safe emptyMap()
    }

    clash.healthCheckAll()

    var result = queryAllGroupDelays(clash)
    repeat(10) {
        if (result.values.all { it != Constants.DELAY_TIMEOUT }) return@safe result
        delay(500)
        result = queryAllGroupDelays(clash)
    }
    AppLog.d("SLTE-Kernel", "speedTest: result=$result")
    result
}

/**
 * 渐进式测速：触发全组健康检查后轮询读取已完成节点，每轮经 [onProgress] 回传部分结果；
 * 全部完成返回最终延迟表（先测完的节点先展示）。
 */
suspend fun KernelProxy.speedTestProgressive(
    onProgress: (Map<String, Int>) -> Unit,
): Map<String, Int> = safe(emptyMap()) {
    val clash = manager.clash()
    if (clash == null) {
        AppLog.d("SLTE-Kernel", "speedTestProgressive: clash=null")
        return@safe emptyMap()
    }
    if (selectorGroup() == null) {
        config.ensureProfile()
        clash.loadActiveProfile()
        if (waitForGroups() == null) return@safe emptyMap()
    }

    clash.healthCheckAll()

    var result = queryAllGroupDelays(clash)
    onProgress(result)
    repeat(20) {
        if (result.values.all { it != Constants.DELAY_TIMEOUT }) return@safe result
        delay(PROGRESS_POLL_INTERVAL_MS)
        result = queryAllGroupDelays(clash)
        if (result.isNotEmpty()) onProgress(result)
    }
    result
}

/** 渐进式测速并缓存结果：全超时（未完成）时不覆盖已有缓存，与 [speedTestAndCache] 策略一致 */
suspend fun KernelProxy.speedTestProgressiveAndCache(
    onProgress: (Map<String, Int>) -> Unit,
): Map<String, Int> {
    val delays = speedTestProgressive(onProgress)
    if (delays.isNotEmpty() && delays.values.any { it != Constants.DELAY_TIMEOUT }) {
        speedResultStore.saveSpeedResults(delays)
    }
    return delays
}

/** 查询全部分组节点延迟快照（跨组去重合并），供测速轮询与结果收集复用 */
private fun KernelProxy.queryAllGroupDelays(clash: IClashManager): Map<String, Int> = clash
    .queryProxyGroupNames(excludeNotSelectable = false)
    .asSequence()
    .filter { it != "GLOBAL" }
    .flatMap { group ->
        clash.queryProxyGroup(group, ProxySort.Delay).proxies.asSequence()
    }.filter { !it.isGroup && it.name != "DIRECT" && it.name != "REJECT" }
    .fold(mutableMapOf()) { acc, proxy ->
        val delay = normalizeDelay(proxy.delay)
        // 组间测速进度不同步：同节点优先保留真实延迟，避免未完成组的超时哨兵覆盖已完成值
        val existing = acc[proxy.name]
        if (existing == null || existing == Constants.DELAY_TIMEOUT) acc[proxy.name] = delay
        acc
    }

/** 渐进式轮询间隔：测速期间按此频率读取已完成节点 */
private const val PROGRESS_POLL_INTERVAL_MS = 500L

/** 等待真实延迟的最大轮数（每轮含一次全量健康检查 + 2s 间隔） */
private const val MAX_SPEED_TEST_ATTEMPTS = 5

/** 测速并缓存结果：本地持久化，重启后仍可排序展示 */
suspend fun KernelProxy.speedTestAndCache(): Map<String, Int> = safe(emptyMap()) {
    val delays = speedTest()
    // 全超时（热身未完成）时不覆盖已有缓存
    if (delays.isNotEmpty() && delays.values.any { it != Constants.DELAY_TIMEOUT }) {
        speedResultStore.saveSpeedResults(delays)
    }
    delays
}

/** 应用启动时预热内核：导入并加载活动配置（分组/节点立即可用，无需等点击连接） */
suspend fun KernelProxy.warmUp(): Boolean = safe(false) {
    val clash = manager.clash() ?: return@safe false
    config.ensureProfile()
    clash.loadActiveProfile()
    true
}

/**
 * 测速并等待真实延迟（非超时哨兵）：支付/更新后的内核热身。
 * 最多重试 [MAX_SPEED_TEST_ATTEMPTS] 轮，避免节点全部不可达时反复全量健康检查。
 */
suspend fun KernelProxy.speedTestUntilReady(maxDurationMs: Long = 60_000L): Map<String, Int> = safe(emptyMap()) {
    var delays = speedTestAndCache()
    try {
        // 整体（含单轮内部的同步 Binder 调用与轮询）受 deadline 约束，不再只查轮间时刻
        withTimeout(maxDurationMs) {
            var attempts = 1
            while (attempts < MAX_SPEED_TEST_ATTEMPTS &&
                (delays.isEmpty() || delays.values.all { it == Constants.DELAY_TIMEOUT })
            ) {
                delay(2000)
                delays = speedTestAndCache()
                attempts++
            }
        }
    } catch (e: TimeoutCancellationException) {
        AppLog.d("SLTE-Kernel", "speedTestUntilReady: ${maxDurationMs}ms 截止，返回当前结果")
    }
    delays
}

/** 读取上次测速结果缓存（无网络请求） */
fun KernelProxy.cachedSpeedResults(): Map<String, Int>? = speedResultStore.getSpeedResults()

/** 启动自动测速：测速 + 缓存 + 自动选择（手动选择优先） */
suspend fun KernelProxy.runAutoSpeedTest(): Map<String, Int> = safe(emptyMap()) {
    AppLog.d("SLTE-Kernel", "runAutoSpeedTest: start")
    var delays = speedTestAndCache()
    // 内核配置加载是异步的：连接广播先于策略组就绪，分组为空时重试
    repeat(5) { attempt ->
        if (delays.isNotEmpty()) return@repeat
        AppLog.d("SLTE-Kernel", "runAutoSpeedTest: 分组未就绪，第 ${attempt + 1} 次重试")
        delay(1000)
        delays = speedTestAndCache()
    }
    AppLog.d("SLTE-Kernel", "runAutoSpeedTest: delays=$delays")
    if (delays.isNotEmpty()) {
        val info = serverInfo()
        if (info?.selection == null ||
            info.selection == SelectionType.AUTO ||
            info.selection == SelectionType.FALLBACK
        ) {
            selectAuto()
        }
    }
    delays
}
