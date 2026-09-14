package com.slte.app.ui.screen.main

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.remote.FallbackDns
import com.slte.app.kernel.KernelConfig
import com.slte.app.kernel.KernelManager
import com.slte.app.kernel.KernelProxy
import com.slte.app.kernel.ensureGlobalSelection
import com.slte.app.kernel.fetchPublicIp
import com.slte.app.kernel.runAutoSpeedTest
import com.slte.app.kernel.serverInfo
import com.slte.app.kernel.warmUp
import com.slte.app.utils.AppLog
import com.slte.app.utils.ErrorMessages
import com.slte.app.utils.sanitizeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 首页状态机：内核连接、模式切换、订阅/购买刷新编排。
 * 订阅与购买流程在 [SubscriptionUpdater] 中，本类只做编排与连接管理。
 */
@HiltViewModel
class MainViewModel
@Inject
constructor(
    private val kernelManager: KernelManager,
    private val kernelProxy: KernelProxy,
    private val kernelConfig: KernelConfig,
    private val fallbackDns: FallbackDns,
    private val subscriptionUpdater: SubscriptionUpdater,
) : ViewModel() {
    private val _data = MutableStateFlow(DashboardData())
    val data: StateFlow<DashboardData> = _data.asStateFlow()

    /** 每次应用启动（ViewModel 重建）只自动测速一次 */
    private var autoTested = false

    init {
        // 先显示本地缓存：冷启动/离线时首页不空白
        subscriptionUpdater.applyCached(_data)
        subscriptionUpdater.seedServerName(_data)
        refresh()
        subscriptionUpdater.loadServers(viewModelScope, _data)
        observeKernelState()
        observeProfileLoaded()
        viewModelScope.launch { subscriptionUpdater.maybeSilentUpdate(_data, viewModelScope) }
        // 内核预热：启动即导入并加载活动配置（无需等点击连接）
        viewModelScope.launch {
            repeat(10) {
                if (kernelProxy.warmUp()) return@launch
                delay(1000)
            }
        }
    }

    /** 内核配置（重新）加载完成后同步真实状态：模式切换是异步重载，不能立刻查询 */
    private fun observeProfileLoaded() {
        viewModelScope.launch {
            kernelManager.profileLoaded.collect {
                refreshKernelInfo()
            }
        }
    }

    /** 监听内核连接状态，驱动连接开关 */
    private fun observeKernelState() {
        viewModelScope.launch {
            kernelManager.connected.collect { connected ->
                _data.update { it.copy(isConnected = connected, isConnecting = false) }
                if (connected) {
                    // VPN 建立后清空 DNS 缓存：缓存里是 VPN 前的真实 IP，
                    // 继续使用会让业务流量以纯 IP 流进 TUN，DOMAIN-SUFFIX 直连规则失配
                    fallbackDns.clearCache()
                    if (!autoTested) {
                        autoTested = true
                        viewModelScope.launch {
                            kernelProxy.runAutoSpeedTest()
                            refreshKernelInfo()
                        }
                    } else {
                        refreshKernelInfo()
                    }
                }
            }
        }
    }

    /** 同步内核真实状态：当前策略、代理模式、出口 IP（连接后/回到首页时调用） */
    fun refreshKernelInfo() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                kernelProxy.ensureGlobalSelection()
                // 内核就绪后先同步本地保存的代理模式，再读取展示
                kernelProxy.ensurePersistedMode()
                kernelProxy.serverInfo()?.let { info ->
                    _data.update { state ->
                        state.copy(
                            // 无套餐时不展示内核节点名
                            serverName = if (state.hasPlan) info.node ?: state.serverName else state.serverName,
                        )
                    }
                }
                kernelProxy.proxyMode()?.let { mode ->
                    _data.update { it.copy(proxyMode = mode) }
                }
                if (_data.value.hasPlan) {
                    kernelProxy.fetchPublicIp()?.let { info ->
                        _data.update {
                            it.copy(
                                currentIp = info.ip,
                                ipCountryCode = info.countryCode,
                            )
                        }
                    }
                }
            }
        }
    }

    /** 首页数据刷新（订阅信息，失败保留缓存展示） */
    fun refresh() {
        viewModelScope.launch { subscriptionUpdater.refresh(_data) }
    }

    /** 更新订阅（调用真实 API） */
    fun updateSubscription() {
        viewModelScope.launch { subscriptionUpdater.updateSubscription(_data, viewModelScope) }
    }

    /** 支付/续费成功后的全屏刷新（流程在 SubscriptionUpdater 中） */
    fun refreshAfterPurchase(tradeNo: String? = null): Job = subscriptionUpdater.refreshAfterPurchase(_data, tradeNo, viewModelScope)

    /** 全屏刷新全部完成后关闭 Loading */
    fun finishPurchaseRefresh() {
        subscriptionUpdater.finishPurchaseRefresh(_data)
    }

    /** 切换连接开关：连接前确保订阅已导入内核，断开走内核停止广播 */
    fun toggleConnection() {
        val current = _data.value
        if (current.isConnecting) return

        if (!current.hasPlan) return

        AppLog.i("SLTE-Main", "toggleConnection: connected=${current.isConnected} -> ${!current.isConnected}")
        if (current.isConnected) {
            kernelManager.stopVpn()
        } else {
            _data.update { it.copy(isConnecting = true, errorMessageRes = null) }
            viewModelScope.launch {
                try {
                    val profile = kernelConfig.ensureProfile()
                    if (profile == null) {
                        AppLog.w("SLTE-Main", "toggleConnection: ensureProfile 返回 null，内核不可用")
                        _data.update {
                            it.copy(
                                isConnecting = false,
                                errorMessageRes = R.string.error_vpn_kernel_unavailable,
                            )
                        }
                        return@launch
                    }
                    kernelManager.startVpn()
                } catch (e: Exception) {
                    AppLog.w("SLTE-Main", "toggleConnection: 启动失败 ${sanitizeLog(e.message ?: "Unknown")}")
                    _data.update {
                        it.copy(
                            isConnecting = false,
                            errorMessageRes = ErrorMessages.networkError(),
                        )
                    }
                }
            }
        }
    }

    /**
     * 需要弹 VPN 授权时返回授权 Intent，否则返回 null。
     *
     * VPN 授权判定收敛在内核层（[KernelManager.vpnRequestIntent]），UI 不直接依赖 VpnService——
     * 否则「先授权再 startVpn」这条约束会散落在每个调用入口，漏一处就会在 establish() 失败时
     * 只表现为开关弹回、没有任何提示。
     */
    fun vpnRequestIntent(): Intent? = kernelManager.vpnRequestIntent()

    fun setProxyMode(mode: String) {
        _data.update { it.copy(proxyMode = mode) }
        viewModelScope.launch {
            kernelProxy.setProxyMode(mode)
        }
    }

    /** 清除仪表盘错误提示 */
    fun clearError() {
        _data.update { it.copy(errorMessageRes = null) }
    }

    /** 取消全屏加载遮罩（订阅更新请求在后台继续，仅收起遮罩） */
    fun cancelUpdating() {
        _data.update { it.copy(isUpdating = false) }
    }

    /** 用户拒绝 VPN 授权：复位连接状态并提示 */
    fun onVpnPermissionDenied() {
        AppLog.w("SLTE-Main", "VPN 授权被拒绝，连接未建立")
        _data.update {
            it.copy(isConnecting = false, errorMessageRes = R.string.error_vpn_permission_denied)
        }
    }
}
