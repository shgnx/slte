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
import com.slte.app.R
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServerViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val subscribeRepository: SubscribeRepository,
    private val kernelProxy: KernelProxy
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
        loadNodes()
        refreshSpecialNodes()
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
                    fallbackNodeCountryCode = countryOf(fallback)
                )
            }
        }
    }

    /** 按节点名在列表内匹配国家码；未知国家（XX）或匹配不到返回 null，UI 回退字母图标 */
    private fun countryOf(nodeName: String?): String? =
        nodeName?.let { name ->
            _data.value.nodes.firstOrNull { it.name == name }?.countryCode?.takeIf { it != "XX" }
        }

    /** 外部编排用（支付完成刷新）：强制拉取服务器列表并更新节点页数据 */
    suspend fun refreshNodesForPurchase() {
        serverRepository.fetchServers(force = true).fold(
            onSuccess = { applyNodes(it) },
            onFailure = { _errorMessageRes.value = ErrorMessages.mapServerError(it.message) }
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
                    _errorMessageRes.value = ErrorMessages.mapServerError(throwable.message)
                }
            )
        }
    }

    private fun applyNodes(
        servers: List<com.slte.app.domain.model.ServerNode>,
        delays: Map<String, Int>? = null
    ) {
        // 已有延迟（缓存或本次测速）在刷新节点列表时保留
        val existing = _data.value.nodes.associate { it.name to it.delay }
        val nodes = servers
            // 上游后端节点 id 可能重复（数据不可控）：按名称去重 + 本地重排 id 保证唯一；
            // 节点选择按名称进行（selectNode(name)），本地 id 仅用于列表 key 与选中态
            .distinctBy { it.name }
            .mapIndexed { index, server ->
                ServerNode(
                    id = index + 1,
                    name = server.name,
                    countryCode = extractCountryCode(server.name),
                    type = server.type.name,
                    host = server.host,
                    delay = delays?.get(server.name) ?: existing[server.name]
                )
            }
        _data.update { it.copy(nodes = nodes, isLoading = false) }
        refreshSpecialNodes()
    }

    /** 选择节点 */
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
            val delays = kernelProxy.speedTestProgressiveAndCache { partial ->
                _data.update { state ->
                    if (partial.isEmpty()) return@update state
                    // 已出真实结果的节点保持首次显示值，测速过程数值稳定，最终统一补齐
                    val nodes = state.nodes.map { node ->
                        val d = partial[node.name]
                        if (d != null && d != 999 && node.name !in state.testedNodes) node.copy(delay = d) else node
                    }
                    // 已出真实结果的节点立即去转圈显示延迟；未出的继续转
                    val tested = partial.filterValues { it != 999 }.keys
                    state.copy(nodes = nodes, testedNodes = state.testedNodes + tested)
                }
            }
            val cached = kernelProxy.cachedSpeedResults()
            val fallbackDelay = kernelProxy.groupByTypeDelay("Fallback")
            _data.update { state ->
                val nodes = state.nodes.map { node ->
                    val d = delays[node.name]
                    val delay = when {
                        d != null && d != 999 -> d
                        d == 999 -> cached?.get(node.name) ?: d // 超时兜底用缓存真实值
                        else -> node.delay
                    }
                    node.copy(delay = delay)
                }
            state.copy(
                nodes = nodes,
                isTesting = false,
                testedNodes = emptySet(),
                kernelFallbackDelay = fallbackDelay
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


    /** 从节点名提取国家代码（如 "香港 01" → "HK"） */
    private fun extractCountryCode(name: String): String {
        val map = mapOf(
            "香港" to "HK", "港" to "HK",
            "新加坡" to "SG", "狮城" to "SG",
            "日本" to "JP", "东京" to "JP", "大阪" to "JP",
            "美国" to "US", "美" to "US", "洛杉矶" to "US", "硅谷" to "US",
            "英国" to "GB", "伦敦" to "GB",
            "德国" to "DE", "法兰克福" to "DE",
            "韩国" to "KR", "首尔" to "KR",
            "台湾" to "TW", "台北" to "TW",
            "印度" to "IN", "孟买" to "IN",
            "巴西" to "BR",
            "澳大利亚" to "AU", "悉尼" to "AU",
            "法国" to "FR", "巴黎" to "FR",
            "俄罗斯" to "RU", "莫斯科" to "RU",
            "加拿大" to "CA",
            "土耳其" to "TR",
            "荷兰" to "NL", "阿姆斯特丹" to "NL",
        )
        for ((key, code) in map) {
            if (name.contains(key)) return code
        }
        return "XX"
    }
}

data class ServerData(
    val nodes: List<ServerNode> = emptyList(),
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
    val fallbackNodeCountryCode: String? = null
) {
    /** 自动选择：延迟最低的节点 */
    val autoDelay: Int?
        get() = nodes.asSequence()
            .mapNotNull { it.delay }
            .filter { it != 999 }
            .minOrNull()

    /** 故障转移：优先显示内核 fallback 分组当前生效节点的延迟（跟随订阅），否则取第二低 */
    val fallbackDelay: Int?
        get() = kernelFallbackDelay ?: nodes.asSequence()
            .mapNotNull { it.delay }
            .filter { it != 999 }
            .sorted()
            .toList()
            .getOrNull(1)
}

/**
 * 节点信息。
 */
data class ServerNode(
    val id: Int = 0,
    val name: String,
    val countryCode: String = "XX",
    val type: String = "",
    val host: String = "",
    val delay: Int? = null
)
