package com.slte.app.ui.screen.main

import com.slte.app.R
import com.slte.app.data.remote.ApiException
import com.slte.app.data.repository.OrderRepository
import com.slte.app.data.repository.ServerRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.usecase.DaysUntilExpiryUseCase
import com.slte.app.kernel.KernelConfig
import com.slte.app.kernel.KernelManager
import com.slte.app.kernel.KernelProxy
import com.slte.app.kernel.speedTestUntilReady
import com.slte.app.utils.AppLog
import com.slte.app.utils.Constants
import com.slte.app.utils.ErrorMessages
import com.slte.app.utils.sanitizeLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * 订阅更新流程（手动/静默/支付后刷新）与状态写回。
 *
 * 只操作传入的 [DashboardData] 流与调用方提供的 scope，不持有 ViewModel 生命周期；
 * 手动/静默/支付刷新三个入口通过 [updateMutex] 串行化。
 */
class SubscriptionUpdater @Inject constructor(
    private val subscribeRepository: SubscribeRepository,
    private val kernelConfig: KernelConfig,
    private val serverRepository: ServerRepository,
    private val kernelProxy: KernelProxy,
    private val kernelManager: KernelManager,
    private val orderRepository: OrderRepository,
    private val daysUntilExpiryUseCase: DaysUntilExpiryUseCase,
) {

    /** 订阅更新互斥：手动/静默/支付刷新三个入口串行化 */
    private val updateMutex = Mutex()

    /** 静默更新进行中标志（不展示 UI Loading） */
    private var silentUpdating = false

    /** 更新订阅（调用真实 API） */
    suspend fun updateSubscription(data: MutableStateFlow<DashboardData>, scope: CoroutineScope) {
        // 检查+置位原子化：三个入口互斥，避免并发双跑
        val proceed = updateMutex.withLock {
            if (data.value.isUpdating || !data.value.hasPlan) false
            else {
                data.update { it.copy(isUpdating = true) }
                true
            }
        }
        if (!proceed) return
        subscribeRepository.fetchSubscribeInfo(force = true).fold(
            onSuccess = {
                // 等待内核配置更新完成后再提示，Loading 期间按钮保持转圈
                val kernelOk = kernelConfig.updateProfile()
                if (kernelOk) {
                    // 订阅已更新：清掉节点缓存并重新拉取
                    serverRepository.invalidateCache()
                    loadServers(scope, data)
                    // 测速后台执行，不阻塞订阅更新完成（节点不可达时不卡 UI）
                    scope.launch { autoSpeedTestAfterUpdate() }
                }
                applySubscribeInfo(
                    data,
                    it,
                    errorMessageRes = if (kernelOk) {
                        R.string.dashboard_refresh_done
                    } else {
                        R.string.api_error_subscribe_info
                    }
                )
            },
            onFailure = { e ->
                val resId = if (e is ApiException) {
                    ErrorMessages.mapSubscribeError(e.message)
                } else {
                    ErrorMessages.networkError()
                }
                data.update { it.copy(isUpdating = false, errorMessageRes = resId) }
            }
        )
    }

    /** 每次进入软件自动更新订阅：失败静默（保留缓存展示，不弹错误） */
    suspend fun maybeSilentUpdate(data: MutableStateFlow<DashboardData>, scope: CoroutineScope) {
        val proceed = updateMutex.withLock {
            if (data.value.isUpdating || silentUpdating) false
            else {
                silentUpdating = true
                true
            }
        }
        if (!proceed) return
        try {
            subscribeRepository.fetchSubscribeInfo(force = true).fold(
                onSuccess = { info ->
                    // 无套餐不更新内核（空订阅没有可导入的节点）
                    if (!info.hasPlan) return@fold
                    val ok = kernelConfig.updateProfile()
                    if (ok) {
                        serverRepository.invalidateCache()
                        loadServers(scope, data)
                        scope.launch { autoSpeedTestAfterUpdate() }
                        applySubscribeInfo(data, info, errorMessageRes = null)
                    }
                },
                onFailure = { e ->
                    // 静默失败：不打扰用户，留痕供排查，下次进入再试
                    AppLog.w("SLTE-Main", "silent subscription update failed: ${sanitizeLog(e.message ?: "Unknown")}")
                }
            )
        } finally {
            silentUpdating = false
        }
    }

    /** 首页节点名填充：先用缓存中的第一个节点填充信息行 */
    fun seedServerName(data: MutableStateFlow<DashboardData>) {
        val cached = serverRepository.getCachedServers()
        if (!cached.isNullOrEmpty()) {
            data.update { it.copy(serverName = cached.first().name) }
        }
    }

    /** 拉取服务器列表更新首页节点名；空列表时清除已失效的展示数据 */
    fun loadServers(scope: CoroutineScope, data: MutableStateFlow<DashboardData>) {
        scope.launch {
            serverRepository.fetchServers().fold(
                onSuccess = { servers ->
                    if (servers.isNotEmpty()) {
                        data.update { it.copy(serverName = servers.first().name) }
                    } else {
                        data.update {
                            it.copy(
                                serverName = Constants.PLACEHOLDER_DASH,
                                currentIp = Constants.PLACEHOLDER_DASH
                            )
                        }
                    }
                },
                onFailure = { e ->
                    AppLog.w("SLTE-Main", "loadServers failed: ${sanitizeLog(e.message ?: "Unknown")}")
                }
            )
        }
    }

    /** 支付/续费成功后的全屏刷新：订阅信息、用户信息、内核订阅、首页信息；
     * 完成后由调用方调用 [finishPurchaseRefresh] 关闭 Loading（配合服务器节点刷新）。已有更新先等待其结束；以订单开通（status=1/3）为准轮询；超时未开通明确提示；测速与内核热身后台执行。
     */
    fun refreshAfterPurchase(
        data: MutableStateFlow<DashboardData>,
        tradeNo: String? = null,
        scope: CoroutineScope,
    ): Job = scope.launch {
        if (data.value.isUpdating) {
            withTimeoutOrNull(PURCHASE_REFRESH_TIMEOUT_MS) { data.first { !it.isUpdating } }
        }
        data.update { it.copy(isUpdating = true) }
        val deadline = System.currentTimeMillis() + PURCHASE_REFRESH_TIMEOUT_MS
        var info = subscribeRepository.fetchSubscribeInfo(force = true).getOrNull()
        var activated = true
        if (tradeNo != null) {
            activated = false
            while (System.currentTimeMillis() < deadline && !activated) {
                activated = orderRepository.getOrderDetail(tradeNo).getOrNull()?.status in ACTIVATED_ORDER_STATUSES
                if (!activated) {
                    delay(3000)
                    info = subscribeRepository.fetchSubscribeInfo(force = true).getOrNull()
                }
            }
        } else {
            while (System.currentTimeMillis() < deadline && info?.hasPlan != true) {
                delay(3000)
                info = subscribeRepository.fetchSubscribeInfo(force = true).getOrNull()
            }
        }
        subscribeRepository.fetchUserInfo()
        val hasPlan = info?.hasPlan == true
        val kernelOk = if (hasPlan) kernelConfig.updateProfile() else false
        serverRepository.invalidateCache()
        var servers = serverRepository.fetchServers(force = true).getOrNull().orEmpty()
        if (servers.isEmpty() && hasPlan) {
            delay(3000)
            servers = serverRepository.fetchServers(force = true).getOrNull().orEmpty()
        }
        if (servers.isNotEmpty()) {
            data.update { it.copy(serverName = servers.first().name) }
        } else {
            data.update {
                it.copy(
                    serverName = Constants.PLACEHOLDER_DASH,
                    currentIp = Constants.PLACEHOLDER_DASH
                )
            }
        }
        applySubscribeInfo(
            data,
            info ?: subscribeRepository.getCachedSubscribeInfo(),
            errorMessageRes = if (!activated && tradeNo != null) {
                R.string.purchase_activation_timeout
            } else {
                null
            }
        )
        if (kernelOk) scope.launch { autoSpeedTestAfterUpdate() }
    }

    /** 全屏刷新全部完成后关闭 Loading */
    fun finishPurchaseRefresh(data: MutableStateFlow<DashboardData>) {
        data.update { it.copy(isUpdating = false, isRefreshing = false, dataLoaded = true) }
    }

    /** 首页数据刷新：拉取订阅信息并应用（失败保留缓存展示） */
    suspend fun refresh(data: MutableStateFlow<DashboardData>) {
        data.update { it.copy(isRefreshing = true) }
        subscribeRepository.fetchSubscribeInfo().fold(
            onSuccess = { applySubscribeInfo(data, it, errorMessageRes = null) },
            onFailure = { e ->
                val resId = if (e is ApiException) {
                    ErrorMessages.mapSubscribeError(e.message)
                } else {
                    ErrorMessages.networkError()
                }
                data.update {
                    it.copy(
                        isRefreshing = false,
                        dataLoaded = true,
                        errorMessageRes = resId
                    )
                }
            }
        )
    }

    /** 用本地缓存填充仪表盘（冷启动/离线时首页不空白） */
    fun applyCached(data: MutableStateFlow<DashboardData>) {
        subscribeRepository.getCachedSubscribeInfo()?.let {
            applySubscribeInfo(data, it, errorMessageRes = null)
        }
    }

    /**
     * 更新订阅成功后自动测速并缓存结果：
     * 已连接时先等内核配置重载完成（profileLoaded 递增）；
     * 未连接时直接测速（speedTest 内部会确保配置已加载）。
     */
    private suspend fun autoSpeedTestAfterUpdate() {
        if (kernelManager.connected.value) {
            val before = kernelManager.profileLoaded.value
            withTimeoutOrNull(SPEED_TEST_WAIT_MS) {
                kernelManager.profileLoaded.first { it > before }
            }
        }
        kernelProxy.speedTestUntilReady()
    }

    /** 将订阅信息填充到仪表盘，并保留本地缓存逻辑 */
    private fun applySubscribeInfo(data: MutableStateFlow<DashboardData>, info: SubscribeInfo?, errorMessageRes: Int?) {
        val planValid = info?.hasPlan == true && !info.expired
        // 套餐失效（过期/无套餐）时清掉节点缓存
        if (!planValid) serverRepository.invalidateCache()
        val expAt = info?.expiredAt ?: 0L
        data.update {
            it.copy(
                usedBytes = info?.usedTraffic ?: 0L,
                totalBytes = info?.transferEnable ?: 0L,
                isValid = planValid,
                hasPlan = info?.hasPlan == true,
                planName = info?.planName ?: "",
                daysUntilExpired = daysUntilExpiryUseCase(expAt),
                expiredAt = expAt,
                isRefreshing = false,
                isUpdating = false,
                dataLoaded = true,
                errorMessageRes = errorMessageRes,
            )
        }
    }

    companion object {
        /** 等待内核配置重载的最长时间（毫秒） */
        private const val SPEED_TEST_WAIT_MS = 10_000L

        /** 支付后等待订单开通的最长时间（毫秒） */
        private const val PURCHASE_REFRESH_TIMEOUT_MS = 60_000L

        /** 订单已开通状态：1=已支付，3=已开通（与订单页状态展示一致） */
        private val ACTIVATED_ORDER_STATUSES = setOf(1, 3)
    }
}
