package com.slte.app.ui.screen.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.data.repository.ServerRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.kernel.KernelProxy
import com.slte.app.kernel.cachedSpeedResults
import com.slte.app.kernel.groupByTypeCurrentNode
import com.slte.app.kernel.groupByTypeDelay
import com.slte.app.kernel.selectAuto
import com.slte.app.kernel.selectFallback
import com.slte.app.kernel.selectNode
import com.slte.app.kernel.speedTestProgressiveAndCache
import com.slte.app.utils.Constants
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
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
    /** 是否有有效订阅（缓存缺失时放行） */
    private fun hasPlan(): Boolean = subscribeRepository.getCachedSubscribeInfo()?.hasPlan ?: true

    private val _data = MutableStateFlow(ServerData())
    val data: StateFlow<ServerData> = _data.asStateFlow()

    private val _errorMessageRes = MutableStateFlow<Int?>(null)
    val errorMessageRes: StateFlow<Int?> = _errorMessageRes.asStateFlow()

    init {
        // 先显示缓存节点 + 上次测速结果
        val cachedDelays = kernelProxy.cachedSpeedResults()
        serverRepository.getCachedServers()?.let { applyNodes(it, cachedDelays) }
        refreshSpecialNodes()
        // 不在 init 中拉取节点：本 VM 会被登录主界面顶部 eager 创建，冷启动即请求会造成无谓网络突发，
        // 且与 Server 页入口 loadNodes() 重复；真正进入节点页时由入口 LaunchedEffect 触发
    }

    /** 同步内核自动选择/故障转移分组的真实生效节点（按分组类型精确匹配，不依赖订阅命名） */
    private fun refreshSpecialNodes() {
        viewModelScope.launch {
            val auto = kernelProxy.groupByTypeCurrentNode("URLTest")
            val fallback = kernelProxy.groupByTypeCurrentNode("Fallback")
            _data.update { state ->
                state.copy(
                    autoNode = auto,
                    fallbackNode = fallback,
                    autoNodeCountryCode = countryOf(auto),
                    fallbackNodeCountryCode = countryOf(fallback),
                )
            }
        }
    }

    /** 按节点名在列表内匹配国家码；未知国家（XX）或匹配不到返回 null，UI 回退字母图标 */
    private fun countryOf(nodeName: String?): String? = nodeName?.let { name ->
        _data.value.nodes
            .firstOrNull { it.name == name }
            ?.countryCode
            ?.takeIf { it != "XX" }
    }

    /** 外部编排用（支付完成刷新）：强制拉取服务器列表并更新节点页数据 */
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

    /** 从 API 加载真实节点数据；force=true 时绕过本地缓存（订阅更新后使用） */
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

    private fun applyNodes(
        servers: List<com.slte.app.domain.model.ServerNode>,
        delays: Map<String, Int>? = null,
    ) {
        // 已有延迟（缓存或本次测速）在刷新节点列表时保留
        val existing = _data.value.nodes.associate { it.name to it.delay }
        val nodes =
            servers
                // 上游后端节点 id 可能重复（数据不可控）：按名称去重 + 本地重排 id 保证唯一；
                // 节点选择按名称进行（selectNode(name)），本地 id 仅用于列表 key 与选中态
                .distinctBy { it.name }
                .mapIndexed { index, server ->
                    NodeItem(
                        id = index + 1,
                        name = server.name,
                        countryCode = extractCountryCode(server.name),
                        type = server.type.name,
                        host = server.host,
                        delay = delays?.get(server.name) ?: existing[server.name],
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
                _data.update { it.copy(selectedNodeId = nodeId) }
                viewModelScope.launch {
                    kernelProxy.selectNode(node.name)
                }
            }
        }
    }

    /** 测速（对所有节点执行健康检查）；无订阅/失败时静默，不弹提示 */
    fun startSpeedTest() {
        if (_data.value.isTesting) return
        if (!hasPlan()) return
        _data.update { it.copy(isTesting = true, testedNodes = emptySet()) }
        _errorMessageRes.value = null
        viewModelScope.launch {
            // 渐进式：先测完的节点先回填，未出结果的保持原值；结束后统一补齐
            val delays =
                kernelProxy.speedTestProgressiveAndCache { partial ->
                    _data.update { state ->
                        if (partial.isEmpty()) return@update state
                        val nodes =
                            state.nodes.map { node ->
                                val d = partial[node.name]
                                if (d != null && d != Constants.DELAY_TIMEOUT && node.name !in state.testedNodes) node.copy(delay = d) else node
                            }
                        // 已出真实结果的节点立即去转圈显示延迟；未出的继续转
                        val tested = partial.filterValues { it != Constants.DELAY_TIMEOUT }.keys
                        state.copy(nodes = nodes, testedNodes = state.testedNodes + tested)
                    }
                }
            val cached = kernelProxy.cachedSpeedResults()
            val fallbackDelay = kernelProxy.groupByTypeDelay("Fallback")
            _data.update { state ->
                val nodes =
                    state.nodes.map { node ->
                        val d = delays[node.name]
                        val delay =
                            when {
                                d != null && d != Constants.DELAY_TIMEOUT -> d
                                d == Constants.DELAY_TIMEOUT -> cached?.get(node.name) ?: d // 超时兜底用缓存真实值
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

    /** 刷新节点列表（默认入口；实际更新订阅由首页统一入口负责，见 ServerScreen.onUpdateSubscription） */
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
    /** 本轮测速已出真实结果的节点名（用于行级"测试中"状态展示） */
    val testedNodes: Set<String> = emptySet(),
    /** 内核“故障转移”分组当前生效节点的延迟（测速后写入） */
    val kernelFallbackDelay: Int? = null,
    /** 内核“自动选择”（URLTest）分组当前生效节点 */
    val autoNode: String? = null,
    /** 内核“故障转移”（Fallback）分组当前生效节点 */
    val fallbackNode: String? = null,
    /** 自动选择生效节点的国家码（无匹配时为 null，显示字母图标） */
    val autoNodeCountryCode: String? = null,
    /** 故障转移生效节点的国家码 */
    val fallbackNodeCountryCode: String? = null,
) {
    /** 自动选择：延迟最低的节点 */
    val autoDelay: Int?
        get() =
            nodes
                .asSequence()
                .mapNotNull { it.delay }
                .filter { it != Constants.DELAY_TIMEOUT }
                .minOrNull()

    /** 故障转移：优先显示内核 fallback 分组当前生效节点的延迟（跟随订阅），否则取第二低 */
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

/**
 * 节点信息（UI 展示层模型，与 domain 层 ServerNode 区分）。
 */
data class NodeItem(
    val id: Int = 0,
    val name: String,
    val countryCode: String = "XX",
    val type: String = "",
    val host: String = "",
    val delay: Int? = null,
)
