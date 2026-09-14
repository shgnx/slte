package com.slte.app.kernel

import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.slte.app.utils.AppLog
import com.slte.app.utils.Constants

/**
 * KernelProxy 分组选择扩展：节点切换、自动选择/故障转移、服务器行状态。
 * 与 KernelProxy 同包，访问其 internal 成员。
 */

/** 选择具体节点：在主选择分组中按名称匹配并切换 */
suspend fun KernelProxy.selectNode(name: String): Boolean = safe(false) {
    val clash = manager.clash() ?: return@safe false
    val group = selectorGroup() ?: return@safe false

    val proxy =
        clash
            .queryProxyGroup(group, ProxySort.Default)
            .proxies
            .firstOrNull { it.name == name } ?: return@safe false

    val result = clash.patchSelector(group, proxy.name)
    AppLog.d("SLTE-Kernel", "selectNode: group=$group proxy=${proxy.name} result=$result")
    patchGlobalIfGlobal(proxy.name)
    result
}

/** 选择“自动选择”分组（内核类型 URLTest 优先，名称匹配兜底） */
suspend fun KernelProxy.selectAuto(): Boolean = safe(false) {
    val result = selectSpecialGroup("URLTest", "自动", "auto", "url")
    autoGroupName()?.let { patchGlobalIfGlobal(it) }
    result
}

/** 选择“故障转移”分组（内核类型 Fallback 优先，名称匹配兜底） */
suspend fun KernelProxy.selectFallback(): Boolean = safe(false) {
    val result = selectSpecialGroup("Fallback", "故障", "fallback")
    fallbackGroupName()?.let { patchGlobalIfGlobal(it) }
    result
}

/** 首页服务器行数据：主选择分组当前策略与生效节点（跟随内核真实状态） */
suspend fun KernelProxy.serverInfo(): KernelServerInfo? = safe(null) {
    val clash = manager.clash() ?: return@safe null
    val selector = selectorGroup() ?: return@safe null
    val state = clash.queryProxyGroup(selector, ProxySort.Default)
    val now = state.now
    AppLog.d("SLTE-Kernel", "serverInfo: selector=$selector now=$now type=${state.type}")
    if (now.isBlank()) return@safe KernelServerInfo(null, null)

    val autoGroup = autoGroupName()
    val fallbackGroup = fallbackGroupName()

    val selection =
        when (now) {
            autoGroup -> SelectionType.AUTO
            fallbackGroup -> SelectionType.FALLBACK
            else -> SelectionType.MANUAL
        }
    val node =
        if (state.proxies.any { !it.isGroup && it.name == now }) {
            now
        } else {
            clash.queryProxyGroup(now, ProxySort.Default).now.ifBlank { null }
        }
    AppLog.d("SLTE-Kernel", "serverInfo: selection=$selection node=$node")
    KernelServerInfo(selection, node)
}

/** 按内核分组类型查当前生效节点（URLTest 自动选最低延迟、Fallback 按优先级故障转移），取内核真实选择 */
suspend fun KernelProxy.groupByTypeCurrentNode(type: String): String? = safe(null) {
    val clash = manager.clash() ?: return@safe null
    val group = queryGroupByTypeName(type) ?: return@safe null
    clash.queryProxyGroup(group, ProxySort.Default).now.ifBlank { null }
}

/** 按内核分组类型查当前生效节点延迟（跟随订阅） */
suspend fun KernelProxy.groupByTypeDelay(type: String): Int? = safe(null) {
    val clash = manager.clash() ?: return@safe null
    val group = queryGroupByTypeName(type) ?: return@safe null

    clash.healthCheck(group)
    val state = clash.queryProxyGroup(group, ProxySort.Delay)
    val proxy =
        state.proxies.firstOrNull { it.name == state.now }
            ?: state.proxies.firstOrNull { !it.isGroup }
            ?: return@safe null

    normalizeDelay(proxy.delay)
}

/** 按分组类型精确查找（不依赖订阅分组命名），未找到返回 null */
internal suspend fun KernelProxy.queryGroupByTypeName(type: String): String? {
    val clash = manager.clash() ?: return null
    for (name in clash.queryProxyGroupNames(excludeNotSelectable = false)) {
        val group = clash.queryProxyGroup(name, ProxySort.Default)
        if (group.type.equals(type, ignoreCase = true)) return name
    }
    return null
}

/** 全局模式下确保 GLOBAL 组不指向 DIRECT/REJECT */
suspend fun KernelProxy.ensureGlobalSelection() = safe(Unit) {
    val clash = manager.clash() ?: return@safe
    if (clash.queryTunnelState().mode != TunnelState.Mode.Global) return@safe
    val now = clash.queryProxyGroup("GLOBAL", ProxySort.Default).now
    AppLog.d("SLTE-Kernel", "ensureGlobalSelection: GLOBAL now=$now")
    if (now.isBlank() || now == "DIRECT" || now == "REJECT") {
        val target = autoGroupName() ?: return@safe
        val result = clash.patchSelector("GLOBAL", target)
        AppLog.d("SLTE-Kernel", "ensureGlobalSelection: GLOBAL -> $target result=$result")
    }
}

/** 等待内核配置加载完成（分组出现），最多约 3 秒 */
internal suspend fun KernelProxy.waitForGroups(): String? {
    repeat(10) {
        val group = selectorGroup()
        if (group != null) return group
        kotlinx.coroutines.delay(300)
    }
    return null
}

internal fun KernelProxy.normalizeDelay(delay: Int): Int = if (delay <= 0 || delay >= Constants.DELAY_INVALID_MAX) Constants.DELAY_TIMEOUT else delay

/** 主选择分组：优先按内核类型识别 Selector，其次 URLTest；无类型匹配时取首个非 GLOBAL 组 */
internal suspend fun KernelProxy.selectorGroup(): String? {
    val clash = manager.clash() ?: return null
    val groups = clash.queryProxyGroupNames(excludeNotSelectable = false)
    groups.forEach { group ->
        // 全局模式会生成 GLOBAL 分组，它不是订阅里的节点选择组
        if (group == "GLOBAL") return@forEach
        val type = clash.queryProxyGroup(group, ProxySort.Default).type
        AppLog.d("SLTE-Kernel", "selectorGroup: $group type=$type")
        if (type.equals("Selector", ignoreCase = true) || type.equals("URLTest", ignoreCase = true)) {
            return group
        }
    }
    return groups.firstOrNull { it != "GLOBAL" }
}

/** 把主选择分组切换到指定类型的分组（自动选择/故障转移）；类型缺失时按常见命名兜底 */
internal suspend fun KernelProxy.selectSpecialGroup(
    type: String,
    vararg nameKeywords: String,
): Boolean {
    val clash = manager.clash() ?: return false
    val selector = selectorGroup() ?: return false
    val target = queryGroupByTypeName(type) ?: nameMatch(*nameKeywords) ?: return false

    val result = clash.patchSelector(selector, target)
    AppLog.d("SLTE-Kernel", "selectSpecial($type): selector=$selector target=$target result=$result")
    return result
}

/** 全局模式下把 GLOBAL 组同步切到目标（节点/自动选择/故障转移） */
internal suspend fun KernelProxy.patchGlobalIfGlobal(target: String) {
    val clash = manager.clash() ?: return
    if (clash.queryTunnelState().mode != TunnelState.Mode.Global) return
    val result = clash.patchSelector("GLOBAL", target)
    AppLog.d("SLTE-Kernel", "patchGlobalIfGlobal: GLOBAL -> $target result=$result")
}

/**
 * 自动选择分组：内核类型 URLTest 精确识别优先；
 * 名称关键词匹配仅作兜底（演进方向：订阅统一为标准分组类型后移除）。
 */
internal suspend fun KernelProxy.autoGroupName(): String? = queryGroupByTypeName("URLTest") ?: nameMatch("自动", "auto", "url")

/**
 * 故障转移分组：内核类型 Fallback 精确识别优先；
 * 名称关键词匹配仅作兜底（演进方向：订阅统一为标准分组类型后移除）。
 */
internal suspend fun KernelProxy.fallbackGroupName(): String? = queryGroupByTypeName("Fallback") ?: nameMatch("故障", "fallback")

internal suspend fun KernelProxy.nameMatch(vararg keywords: String): String? {
    val clash = manager.clash() ?: return null
    return clash
        .queryProxyGroupNames(excludeNotSelectable = false)
        .firstOrNull { group -> keywords.any { group.contains(it, ignoreCase = true) } }
}
