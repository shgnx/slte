package com.slte.app.ui.screen.server

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.domain.model.SpecialNodeSnapshot
import com.slte.app.domain.repository.ServerRepository
import com.slte.app.domain.repository.SubscribeRepository
import com.slte.app.kernel.KernelProxy
import com.slte.app.kernel.LIVE_SELECTION_BUSY_POLL_MS
import com.slte.app.kernel.NodeNameResolver
import com.slte.app.kernel.api.KernelServerInfo
import com.slte.app.kernel.api.SelectionGroupType
import com.slte.app.kernel.api.SelectionType
import com.slte.app.kernel.cachedSpeedResults
import com.slte.app.kernel.nodeLatencyStream
import com.slte.app.utils.Constants
import com.slte.app.utils.ErrorMessages
import com.slte.app.utils.extractCountryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ServerViewModel
@Inject
constructor(
    private val serverRepository: ServerRepository,
    private val subscribeRepository: SubscribeRepository,
    private val kernelProxy: KernelProxy,
) : ViewModel() {

    private fun hasPlan(): Boolean = subscribeRepository.getCachedSubscribeInfo()?.hasPlan ?: true

    private val _data = MutableStateFlow(ServerData())
    val data: StateFlow<ServerData> = _data.asStateFlow()

    private val _errorMessageRes = MutableStateFlow<Int?>(null)
    val errorMessageRes: StateFlow<Int?> = _errorMessageRes.asStateFlow()

    private val refreshSeq = AtomicInteger()

    init {

        serverRepository.getCachedServers()?.let { applyNodes(it) }
        applySpecialNodeSnapshots()
        refreshSpecialNodes()
    }

    private fun refreshSpecialNodes() {
        val seq = refreshSeq.incrementAndGet()
        viewModelScope.launch {
            val auto = kernelProxy.groupByTypeCurrentNode(SelectionGroupType.AUTO)
            val fallback = kernelProxy.groupByTypeCurrentNode(SelectionGroupType.FALLBACK)
            val info = kernelProxy.serverInfo()
            val kernelNames = kernelProxy.nodeNames()
            val cachedDelays = kernelProxy.cachedSpeedResults()
            if (seq != refreshSeq.get()) return@launch
            val autoCurrent = auto ?: info?.takeIf { it.selection == SelectionType.AUTO }?.node
            val fallbackCurrent = fallback ?: info?.takeIf { it.selection == SelectionType.FALLBACK }?.node
            _data.update { state ->
                val index = NodeNameMatcher.index(kernelNames)
                // 内核已加载但面板节点匹配不上：订阅没下发，按超时展示而非留空白
                val kernelReady = kernelNames.isNotEmpty()
                val panelNodes = state.nodes.filterNot { it.fromKernel }
                val resolved =
                    panelNodes.map { node ->
                        val proxyName = resolveKernelName(index, node)
                        node.copy(
                            proxyName = proxyName,
                            delay =
                            proxyName?.let { cachedDelays?.get(it) }
                                ?: if (kernelReady && proxyName == null) Constants.DELAY_TIMEOUT else node.delay,
                        )
                    }
                val nodes = resolved + kernelOnlyNodes(resolved, kernelNames, cachedDelays)
                // 自动/故障转移两行直接读测速缓存，不等整轮测速结束
                val autoDelay = cachedDelays?.get(autoCurrent)?.takeIf { it != Constants.DELAY_TIMEOUT }
                val fallbackDelay = cachedDelays?.get(fallbackCurrent)?.takeIf { it != Constants.DELAY_TIMEOUT }
                state.copy(
                    nodes = nodes,
                    autoNode = autoCurrent?.let(NodeNameResolver::displayName) ?: state.autoNode,
                    fallbackNode = fallbackCurrent?.let(NodeNameResolver::displayName) ?: state.fallbackNode,
                    autoNodeCountryCode = countryOf(nodes, autoCurrent) ?: state.autoNodeCountryCode,
                    fallbackNodeCountryCode = countryOf(nodes, fallbackCurrent) ?: state.fallbackNodeCountryCode,
                    autoNodeDelay = autoDelay ?: state.autoNodeDelay,
                    kernelFallbackDelay = fallbackDelay ?: state.kernelFallbackDelay,
                    selectedNodeId = selectedNodeIdOf(nodes, info) ?: state.selectedNodeId,
                )
            }
            persistSpecialNodeSnapshots(autoCurrent, fallbackCurrent)
        }
    }

    /** 实时值落盘成快照，下次冷启动内核还没上报时，两行先用上次的成员和国旗垫显示。 */
    private fun persistSpecialNodeSnapshots(auto: String?, fallback: String?) {
        val state = _data.value
        auto?.let {
            serverRepository.saveAutoNodeSnapshot(
                SpecialNodeSnapshot(
                    kernelName = it,
                    displayName = state.autoNode.orEmpty(),
                    countryCode = state.autoNodeCountryCode.orEmpty(),
                    delay = state.autoNodeDelay,
                ),
            )
        }
        fallback?.let {
            serverRepository.saveFallbackNodeSnapshot(
                SpecialNodeSnapshot(
                    kernelName = it,
                    displayName = state.fallbackNode.orEmpty(),
                    countryCode = state.fallbackNodeCountryCode.orEmpty(),
                    delay = state.kernelFallbackDelay,
                ),
            )
        }
    }

    /** 内核还没上报时，先用快照把两行填上；实时值到达后由 [refreshSpecialNodes] 覆盖。 */
    private fun applySpecialNodeSnapshots() {
        val auto = serverRepository.getAutoNodeSnapshot()
        val fallback = serverRepository.getFallbackNodeSnapshot()
        _data.update { state ->
            // 无快照（首次使用）时用订阅首个节点兜底，保证未连接也能显示国旗
            val fallbackNode = state.nodes.firstOrNull()
            state.copy(
                autoNode = auto?.displayName ?: state.autoNode ?: fallbackNode?.name,
                autoNodeCountryCode =
                auto?.countryCode?.takeIf { it.isNotEmpty() }
                    ?: state.autoNodeCountryCode
                    ?: fallbackNode?.countryCode?.takeIf { it != "XX" },
                autoNodeDelay = auto?.delay ?: state.autoNodeDelay,
                fallbackNode = fallback?.displayName ?: state.fallbackNode ?: fallbackNode?.name,
                fallbackNodeCountryCode =
                fallback?.countryCode?.takeIf { it.isNotEmpty() }
                    ?: state.fallbackNodeCountryCode
                    ?: fallbackNode?.countryCode?.takeIf { it != "XX" },
                kernelFallbackDelay = fallback?.delay ?: state.kernelFallbackDelay,
            )
        }
    }

    private fun resolveKernelName(
        index: Map<String, List<String>>,
        node: NodeItem,
    ): String? = NodeNameMatcher.resolveKernelName(index, node.name, node.type)

    private fun kernelOnlyNodes(
        panelNodes: List<NodeItem>,
        kernelNames: List<String>,
        cachedDelays: Map<String, Int>?,
    ): List<NodeItem> {
        if (kernelNames.isEmpty()) return emptyList()
        val taken = panelNodes.mapNotNullTo(HashSet<String>()) { it.proxyName }
        return kernelNames
            .asSequence()
            .distinct()
            .filterNot { it in taken }
            .mapIndexed { index, name ->
                NodeItem(
                    id = panelNodes.size + index + 1,
                    name = name,
                    countryCode = extractCountryCode(name),
                    delay = cachedDelays?.get(name),
                    proxyName = name,
                    fromKernel = true,
                )
            }
            .toList()
    }

    private fun selectedNodeIdOf(
        nodes: List<NodeItem>,
        info: KernelServerInfo?,
    ): Int? {
        val current = info?.node
        return when (info?.selection) {
            SelectionType.AUTO -> 0
            SelectionType.FALLBACK -> -1
            SelectionType.MANUAL ->
                current?.let { name ->
                    nodes.firstOrNull { it.proxyName == name }?.id
                        ?: nodes.firstOrNull { it.name == name }?.id
                        ?: matchedNode(nodes, name)?.id
                }
            null -> null
        }
    }

    private fun matchedNode(
        nodes: List<NodeItem>,
        name: String,
    ): NodeItem? {
        val key = NodeNameMatcher.matchKey(name)
        if (key.isEmpty()) return null
        return nodes.filter { NodeNameMatcher.matchKey(it.name) == key }.singleOrNull()
    }

    private fun countryOf(
        nodes: List<NodeItem>,
        nodeName: String?,
    ): String? = nodeName?.let { name ->
        val node = nodes.firstOrNull { it.proxyName == name || it.name == name } ?: matchedNode(nodes, name)
        node?.countryCode?.takeIf { it != "XX" } ?: extractCountryCode(name).takeIf { it != "XX" }
    }

    suspend fun refreshNodesForPurchase() {
        serverRepository.fetchServers(force = true).fold(
            onSuccess = { applyNodes(it) },
            onFailure = { _errorMessageRes.value = ErrorMessages.forServer(it) },
        )
    }

    fun retry() {
        loadNodes()
    }

    fun dismissError() {
        _errorMessageRes.value = null
    }

    fun loadNodes(force: Boolean = false) {
        _errorMessageRes.value = null
        _data.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            serverRepository.fetchServers(force = force).fold(
                onSuccess = ::applyNodes,
                onFailure = { throwable ->
                    _data.update { it.copy(isLoading = false) }
                    _errorMessageRes.value = ErrorMessages.forServer(throwable)
                },
            )
        }
    }

    private fun applyNodes(servers: List<com.slte.app.domain.model.ServerNode>) {
        val existing = _data.value.nodes.associate { it.name to it.delay }
        val panel =
            servers
                .distinctBy { it.name }
                .mapIndexed { index, server ->
                    NodeItem(
                        id = index + 1,
                        name = server.name,
                        countryCode = extractCountryCode(server.name),
                        type = server.type.name,
                        delay = existing[server.name],
                    )
                }
        val kept = _data.value.nodes.filter { it.fromKernel }.mapIndexed { index, node -> node.copy(id = panel.size + index + 1) }
        _data.update { it.copy(nodes = panel + kept, isLoading = false) }
        applySpecialNodeSnapshots()
        refreshSpecialNodes()
    }

    fun selectNode(nodeId: Int) {
        when (nodeId) {
            0 -> {
                _data.update { it.copy(selectedNodeId = 0) }
                viewModelScope.launch {
                    kernelProxy.selectAuto()
                    refreshSpecialNodes()
                }
            }
            -1 -> {
                _data.update { it.copy(selectedNodeId = -1) }
                viewModelScope.launch {
                    kernelProxy.selectFallback()
                    refreshSpecialNodes()
                }
            }
            else -> {
                val node = _data.value.nodes.firstOrNull { it.id == nodeId } ?: return
                viewModelScope.launch {
                    if (kernelProxy.selectNode(node.proxyName ?: node.name)) {
                        _data.update { it.copy(selectedNodeId = nodeId) }
                    }
                    refreshSpecialNodes()
                }
            }
        }
    }

    fun startSpeedTest() {
        if (_data.value.isTesting) return
        if (!hasPlan()) return
        _data.update { it.copy(isTesting = true, testedNodes = emptySet()) }
        _errorMessageRes.value = null
        viewModelScope.launch {
            var latest: Map<String, Int> = emptyMap()
            var lastSpecialRefresh = 0L
            kernelProxy.nodeLatencyStream().collect { update ->
                latest = update.all
                if (update.fresh.isNotEmpty()) {
                    // 谁先出结果就先落到列表里
                    applyFreshDelays(update.fresh)
                    // 自动/故障转移两行跟随内核实时切换，限频避免刷爆 IPC
                    val now = System.currentTimeMillis()
                    if (now - lastSpecialRefresh >= LIVE_SELECTION_BUSY_POLL_MS) {
                        lastSpecialRefresh = now
                        refreshSpecialNodes()
                    }
                }
            }
            finishSpeedTest(latest)
        }
    }

    /** 增量落结果：只用本次新出的延迟覆盖对应节点，不覆盖用户手动设置过的值。 */
    private fun applyFreshDelays(fresh: Map<String, Int>) {
        _data.update { state ->
            val nodes =
                state.nodes.map { node ->
                    val delay = fresh[node.proxyName ?: node.name]
                    if (delay != null && node.name !in state.testedNodes) node.copy(delay = delay) else node
                }
            val tested =
                state.nodes.mapNotNull { node ->
                    node.name.takeIf { fresh[node.proxyName ?: node.name] != null }
                }
            state.copy(nodes = nodes, testedNodes = state.testedNodes + tested)
        }
    }

    /** 收尾：没测到结果的节点回退到缓存值，测过但失败的标"超时"，并刷新自动/故障转移两行。 */
    private suspend fun finishSpeedTest(latest: Map<String, Int>) {
        val cached = kernelProxy.cachedSpeedResults()
        _data.update { state ->
            val nodes =
                state.nodes.map { node ->
                    val key = node.proxyName ?: node.name
                    val measured = latest[key]?.takeIf { it != Constants.DELAY_TIMEOUT }
                    // 测过但没出延迟：按超时收尾
                    val failed =
                        latest.isNotEmpty() &&
                            node.proxyName != null &&
                            (latest[key] == null || latest[key] == Constants.DELAY_TIMEOUT)
                    node.copy(
                        delay = measured ?: cached?.get(key) ?: if (failed) Constants.DELAY_TIMEOUT else node.delay,
                    )
                }
            state.copy(
                nodes = nodes,
                isTesting = false,
                testedNodes = emptySet(),
            )
        }
        refreshSpecialNodes()
    }

    fun updateSubscription() {
        if (!hasPlan()) return
        loadNodes(force = true)
    }
}

@Immutable
data class ServerData(
    val nodes: List<NodeItem> = emptyList(),
    val selectedNodeId: Int = 0,
    val isLoading: Boolean = false,
    val isTesting: Boolean = false,

    val testedNodes: Set<String> = emptySet(),

    val kernelFallbackDelay: Int? = null,

    val autoNode: String? = null,

    val fallbackNode: String? = null,

    val autoNodeCountryCode: String? = null,

    val fallbackNodeCountryCode: String? = null,

    /** 自动选择组当前成员的延迟（来自测速流缓存）。 */
    val autoNodeDelay: Int? = null,
) {

    val autoDelay: Int?
        get() = autoNodeDelay ?: nodes
            .asSequence()
            .mapNotNull { it.delay }
            .filter { it != Constants.DELAY_TIMEOUT }
            .minOrNull()

    val fallbackDelay: Int?
        get() =
            kernelFallbackDelay ?: nodes
                .asSequence()
                .mapNotNull { it.delay }
                .filter { it != Constants.DELAY_TIMEOUT }
                .sorted()
                .toList()
                .getOrNull(1)
}

data class NodeItem(
    val id: Int = 0,
    val name: String,
    val countryCode: String = "XX",
    val type: String = "",
    val delay: Int? = null,
    val proxyName: String? = null,
    val fromKernel: Boolean = false,
)
