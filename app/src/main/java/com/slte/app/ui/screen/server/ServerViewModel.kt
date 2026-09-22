package com.slte.app.ui.screen.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.data.repository.ServerRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.kernel.KernelProxy
import com.slte.app.kernel.KernelServerInfo
import com.slte.app.kernel.NodeNameResolver
import com.slte.app.kernel.SelectionType
import com.slte.app.kernel.cachedSpeedResults
import com.slte.app.kernel.groupByTypeCurrentNode
import com.slte.app.kernel.groupByTypeDelay
import com.slte.app.kernel.nodeNames
import com.slte.app.kernel.selectAuto
import com.slte.app.kernel.selectFallback
import com.slte.app.kernel.selectNode
import com.slte.app.kernel.serverInfo
import com.slte.app.kernel.speedTestProgressiveAndCache
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

    private val kernelTagToType =
        mapOf(
            "vless" to "vless",
            "vmess" to "vmess",
            "trojan" to "trojan",
            "ss" to "shadowsocks",
            "hy" to "hysteria",
            "hy2" to "hysteria2",
            "tuic" to "tuic",
            "anytls" to "anytls",
            "socks" to "socks",
        )

    init {

        serverRepository.getCachedServers()?.let { applyNodes(it) }
        refreshSpecialNodes()
    }

    private fun refreshSpecialNodes() {
        val seq = refreshSeq.incrementAndGet()
        viewModelScope.launch {
            val auto = kernelProxy.groupByTypeCurrentNode("URLTest")
            val fallback = kernelProxy.groupByTypeCurrentNode("Fallback")
            val info = kernelProxy.serverInfo()
            val kernelNames = kernelProxy.nodeNames()
            val cachedDelays = kernelProxy.cachedSpeedResults()
            if (seq != refreshSeq.get()) return@launch
            _data.update { state ->
                val index = kernelNameIndex(kernelNames)
                val nodes =
                    state.nodes.map { node ->
                        val proxyName = resolveKernelName(index, node)
                        node.copy(
                            proxyName = proxyName,
                            delay = proxyName?.let { cachedDelays?.get(it) } ?: node.delay,
                        )
                    }
                state.copy(
                    nodes = nodes,
                    autoNode = auto?.let(NodeNameResolver::displayName),
                    fallbackNode = fallback?.let(NodeNameResolver::displayName),
                    autoNodeCountryCode = countryOf(nodes, auto),
                    fallbackNodeCountryCode = countryOf(nodes, fallback),
                    selectedNodeId = selectedNodeIdOf(nodes, info) ?: state.selectedNodeId,
                )
            }
        }
    }

    private fun kernelNameIndex(kernelNames: List<String>): Map<String, List<String>> = kernelNames
        .groupBy { NodeNameResolver.of(it) }
        .filterKeys { it.isNotEmpty() }

    private fun resolveKernelName(
        index: Map<String, List<String>>,
        node: NodeItem,
    ): String? {
        val candidates = index[NodeNameResolver.of(node.name)] ?: return null
        candidates.singleOrNull()?.let { return it }

        val type = node.type.lowercase()
        return candidates
            .filter { candidate -> NodeNameResolver.protocolTag(candidate)?.let(kernelTagToType::get) == type }
            .singleOrNull()
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
        val key = NodeNameResolver.of(name)
        if (key.isEmpty()) return null
        return nodes.filter { NodeNameResolver.of(it.name) == key }.singleOrNull()
    }

    private fun countryOf(
        nodes: List<NodeItem>,
        nodeName: String?,
    ): String? = nodeName?.let { name ->
        nodes
            .firstOrNull { it.proxyName == name || it.name == name }
            ?.countryCode
            ?.takeIf { it != "XX" }
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
        val nodes =
            servers
                .distinctBy { it.name }
                .mapIndexed { index, server ->
                    NodeItem(
                        id = index + 1,
                        name = server.name,
                        countryCode = extractCountryCode(server.name),
                        type = server.type.name,
                        host = server.host,
                        delay = existing[server.name],
                    )
                }
        _data.update { it.copy(nodes = nodes, isLoading = false) }
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
            val delays =
                kernelProxy.speedTestProgressiveAndCache { partial ->
                    _data.update { state ->
                        if (partial.isEmpty()) return@update state

                        val nodes =
                            state.nodes.map { node ->
                                val d = partial[node.proxyName ?: node.name]
                                if (d != null && d != Constants.DELAY_TIMEOUT && node.name !in state.testedNodes) node.copy(delay = d) else node
                            }

                        val tested =
                            state.nodes.mapNotNull { node ->
                                val d = partial[node.proxyName ?: node.name]
                                node.name.takeIf { d != null && d != Constants.DELAY_TIMEOUT }
                            }
                        state.copy(nodes = nodes, testedNodes = state.testedNodes + tested)
                    }
                }
            val cached = kernelProxy.cachedSpeedResults()
            val fallbackDelay = kernelProxy.groupByTypeDelay("Fallback")
            _data.update { state ->
                val nodes =
                    state.nodes.map { node ->
                        val key = node.proxyName ?: node.name
                        val d = delays[key]
                        val delay =
                            when {
                                d != null && d != Constants.DELAY_TIMEOUT -> d
                                d == Constants.DELAY_TIMEOUT -> cached?.get(key) ?: d
                                else -> node.delay
                            }
                        node.copy(delay = delay)
                    }
                state.copy(
                    nodes = nodes,
                    isTesting = false,
                    testedNodes = emptySet(),
                    kernelFallbackDelay = fallbackDelay,
                )
            }
            refreshSpecialNodes()
        }
    }

    fun updateSubscription() {
        if (!hasPlan()) return
        loadNodes(force = true)
    }
}

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
) {

    val autoDelay: Int?
        get() =
            nodes
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
    val host: String = "",
    val delay: Int? = null,
    val proxyName: String? = null,
)
