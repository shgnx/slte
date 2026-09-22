package com.slte.app.kernel

import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.TunnelState
import com.slte.app.utils.AppLog
import com.slte.app.utils.Constants
import kotlinx.coroutines.delay

suspend fun KernelProxy.selectNode(name: String): Boolean = safe(false, "selectNode") {
    val clash = manager.clash() ?: return@safe false
    val group = selectorGroup() ?: return@safe false

    val members = clash.queryProxyGroup(group, ProxySort.Default).proxies.filterNot { it.isGroup }
    val resolved = NodeNameResolver.resolve(members.map { it.name }, name)
    if (resolved == null) {
        AppLog.w(
            "SLTE-Kernel",
            "selectNode: 未匹配到节点 group=$group target=$name members=${members.size} " +
                "sample=${members.take(NODE_NAME_SAMPLE).joinToString(",") { it.name }}",
        )
        return@safe false
    }

    val result = clash.patchSelector(group, resolved)
    var now = clash.queryProxyGroup(group, ProxySort.Default).now
    var attempt = 0
    while (now != resolved && attempt < VERIFY_ATTEMPTS) {
        delay(VERIFY_DELAY_MS)
        now = clash.queryProxyGroup(group, ProxySort.Default).now
        attempt++
    }
    AppLog.d("SLTE-Kernel", "selectNode: group=$group proxy=$resolved result=$result now=$now")
    if (now != resolved) {
        AppLog.w("SLTE-Kernel", "selectNode: 切换未生效 group=$group target=$resolved now=$now")
        return@safe false
    }

    patchGlobalIfGlobal(resolved)
    true
}

private const val NODE_NAME_SAMPLE = 5

private const val VERIFY_ATTEMPTS = 2

private const val VERIFY_DELAY_MS = 120L

suspend fun KernelProxy.nodeNames(): List<String> = safe(emptyList(), "nodeNames") {
    val clash = manager.clash() ?: return@safe emptyList()
    clash
        .queryProxyGroupNames(excludeNotSelectable = false)
        .asSequence()
        .filterNot { it == "GLOBAL" }
        .flatMap { group -> clash.queryProxyGroup(group, ProxySort.Default).proxies.asSequence() }
        .filterNot { it.isGroup || it.name == "DIRECT" || it.name == "REJECT" }
        .map { it.name }
        .distinct()
        .toList()
}

suspend fun KernelProxy.selectAuto(): Boolean = safe(false, "selectAuto") {
    val result = selectSpecialGroup("URLTest", "自动", "auto", "url")
    autoGroupName()?.let { patchGlobalIfGlobal(it) }
    result
}

suspend fun KernelProxy.selectFallback(): Boolean = safe(false, "selectFallback") {
    val result = selectSpecialGroup("Fallback", "故障", "fallback")
    fallbackGroupName()?.let { patchGlobalIfGlobal(it) }
    result
}

suspend fun KernelProxy.serverInfo(): KernelServerInfo? = safe(null, "serverInfo") {
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

suspend fun KernelProxy.groupByTypeCurrentNode(type: String): String? = safe(null, "groupByTypeCurrentNode") {
    val clash = manager.clash() ?: return@safe null
    val group = queryGroupByTypeName(type) ?: return@safe null
    clash.queryProxyGroup(group, ProxySort.Default).now.ifBlank { null }
}

suspend fun KernelProxy.groupByTypeDelay(type: String): Int? = safe(null, "groupByTypeDelay") {
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

internal suspend fun KernelProxy.queryGroupByTypeName(type: String): String? {
    val clash = manager.clash() ?: return null
    for (name in clash.queryProxyGroupNames(excludeNotSelectable = false)) {
        val group = clash.queryProxyGroup(name, ProxySort.Default)
        if (group.type.equals(type, ignoreCase = true)) return name
    }
    return null
}

suspend fun KernelProxy.ensureGlobalSelection() = safe(Unit, "ensureGlobalSelection") {
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

internal suspend fun KernelProxy.waitForGroups(): String? {
    repeat(10) {
        val group = selectorGroup()
        if (group != null) return group
        kotlinx.coroutines.delay(300)
    }
    return null
}

internal fun KernelProxy.normalizeDelay(delay: Int): Int = if (delay <= 0 || delay >= Constants.DELAY_INVALID_MAX) Constants.DELAY_TIMEOUT else delay

internal suspend fun KernelProxy.selectorGroup(): String? {
    val clash = manager.clash() ?: return null
    val groups = clash.queryProxyGroupNames(excludeNotSelectable = false)
    groups.forEach { group ->

        if (group == "GLOBAL") return@forEach
        val type = clash.queryProxyGroup(group, ProxySort.Default).type
        AppLog.d("SLTE-Kernel", "selectorGroup: $group type=$type")
        if (type.equals("Selector", ignoreCase = true) || type.equals("URLTest", ignoreCase = true)) {
            return group
        }
    }
    return groups.firstOrNull { it != "GLOBAL" }
}

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

internal suspend fun KernelProxy.patchGlobalIfGlobal(target: String) {
    val clash = manager.clash() ?: return
    if (clash.queryTunnelState().mode != TunnelState.Mode.Global) return
    val result = clash.patchSelector("GLOBAL", target)
    AppLog.d("SLTE-Kernel", "patchGlobalIfGlobal: GLOBAL -> $target result=$result")
}

internal suspend fun KernelProxy.autoGroupName(): String? = queryGroupByTypeName("URLTest") ?: nameMatch("自动", "auto", "url")

internal suspend fun KernelProxy.fallbackGroupName(): String? = queryGroupByTypeName("Fallback") ?: nameMatch("故障", "fallback")

internal suspend fun KernelProxy.nameMatch(vararg keywords: String): String? {
    val clash = manager.clash() ?: return null
    return clash
        .queryProxyGroupNames(excludeNotSelectable = false)
        .firstOrNull { group -> keywords.any { group.contains(it, ignoreCase = true) } }
}
