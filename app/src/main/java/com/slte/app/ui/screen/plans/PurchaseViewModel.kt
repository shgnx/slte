package com.slte.app.ui.screen.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.remote.ApiException
import com.slte.app.data.repository.OrderRepository
import com.slte.app.domain.model.CheckoutResult
import com.slte.app.domain.model.CreateOrderResult
import com.slte.app.domain.model.OrderStatus
import com.slte.app.domain.model.PlanInfo
import com.slte.app.utils.AppLog
import com.slte.app.utils.ErrorMessages
import com.slte.app.utils.sanitizeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 购买流程 ViewModel。
 *
 * 流程：选择周期 → 确认警告 → 创建订单 → 选择支付 → 跳转浏览器
 */
@HiltViewModel
class PurchaseViewModel
@Inject
constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val _step = MutableStateFlow<PurchaseStep>(PurchaseStep.Idle)
    val step: StateFlow<PurchaseStep> = _step.asStateFlow()

    private val _toastRes = MutableStateFlow<Int?>(null)
    val toastRes: StateFlow<Int?> = _toastRes.asStateFlow()

    /**
     * 支付完成事件（余额支付成功 / 轮询确认）：携带订单号，供首页等待订单"已开通"后刷新。
     *
     * `replay = 1`：事件产生时若尚无订阅者（进程重建、账号切换重建 VM、订阅方被长任务占住）
     * 也不会丢失。订阅方处理完必须调用 [ackPaymentCompleted] 清空回放缓存，否则
     * 重组（如旋转屏幕重新挂载 effect）会重放该事件、触发一次多余的整屏刷新。
     */
    private val _paymentCompleted = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val paymentCompleted: SharedFlow<String> = _paymentCompleted.asSharedFlow()

    private var pollJob: Job? = null

    /** 当前轮询对应的订单号，用于 [startOrderPolling] 去重（同一订单重复请求直接忽略） */
    private var pollingTradeNo: String? = null

    /**
     * 消费并清空支付完成事件（订阅方处理完毕后调用，避免重组重放）。
     *
     * `resetReplayCache` 目前仍是实验性 API（`@ExperimentalCoroutinesApi`），
     * 但它是 SharedFlow 上唯一能清空回放缓存的官方手段，且行为稳定；此处显式 opt-in
     * 而不是改用「自己维护一个已消费标记」这类绕法。
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun ackPaymentCompleted() {
        _paymentCompleted.resetReplayCache()
    }

    fun startPurchase(plan: PlanInfo) {
        _step.value = PurchaseStep.SelectPeriod(plan = plan)
    }

    fun selectPeriod(period: String) {
        val current = _step.value
        if (current is PurchaseStep.SelectPeriod) {
            _step.value = current.copy(selectedPeriod = period)
        }
    }

    fun updateCouponCode(code: String) {
        val current = _step.value
        if (current is PurchaseStep.SelectPeriod) {
            _step.value =
                current.copy(
                    couponCode = code,
                    couponVerified = false,
                    couponDiscount = 0,
                    isVerifying = false,
                )
        }
    }

    /** 验证优惠券：真实调用后端 check 接口，成功才允许下单 */
    fun verifyCoupon() {
        val current = _step.value
        if (current !is PurchaseStep.SelectPeriod) return
        if (current.couponCode.isBlank()) return
        if (current.isVerifying) return

        _step.value = current.copy(isVerifying = true)
        viewModelScope.launch {
            orderRepository
                .checkCoupon(
                    code = current.couponCode.trim(),
                    planId = current.plan.id.toInt(),
                ).fold(
                    onSuccess = { result ->
                        val state = _step.value as? PurchaseStep.SelectPeriod
                        if (state == null || state.couponCode != current.couponCode) return@fold
                        val discount = computeCouponDiscount(result.type, result.value, current.priceCents)
                        AppLog.d(TAG, "checkCoupon: type=${result.type} value=${result.value} price=${current.priceCents} discount=$discount")
                        _step.value =
                            state.copy(
                                isVerifying = false,
                                couponVerified = true,
                                couponDiscount = discount,
                            )
                        _toastRes.value = R.string.purchase_coupon_applied
                    },
                    onFailure = { e ->
                        val state = _step.value as? PurchaseStep.SelectPeriod
                        if (state == null || state.couponCode != current.couponCode) return@fold
                        _step.value =
                            state.copy(
                                isVerifying = false,
                                couponVerified = false,
                                couponDiscount = 0,
                            )
                        _toastRes.value = ErrorMessages.forOrder(e)
                    },
                )
        }
    }

    fun showConfirmWarning() {
        val current = _step.value
        if (current !is PurchaseStep.SelectPeriod) return
        if (current.couponCode.isNotBlank() && !current.couponVerified) {
            _toastRes.value = R.string.purchase_coupon_verify_first
            return
        }
        _step.value = current.copy(showWarning = true)
    }

    fun cancelWarning() {
        val current = _step.value
        if (current is PurchaseStep.SelectPeriod) {
            _step.value = current.copy(showWarning = false)
        }
    }

    private var creatingOrder = false

    /** 确认警告：创建订单，成功后结束流程并将订单号回传给调用方。 */
    private val _createdTradeNo = MutableStateFlow<String?>(null)
    val createdTradeNo: StateFlow<String?> = _createdTradeNo.asStateFlow()

    fun confirmWarning() {
        if (creatingOrder) return
        val current = _step.value
        if (current !is PurchaseStep.SelectPeriod) return

        val coupon = current.couponCode.takeIf { it.isNotBlank() && current.couponVerified }
        if (current.couponCode.isNotBlank() && !current.couponVerified) {
            _toastRes.value = R.string.purchase_coupon_verify_first
            return
        }
        creatingOrder = true
        viewModelScope.launch {
            try {
                orderRepository
                    .createOrder(
                        planId = current.plan.id.toInt(),
                        period = current.selectedPeriod,
                        couponCode = coupon,
                    ).fold(
                        onSuccess = { result ->
                            _step.value = PurchaseStep.Idle
                            _createdTradeNo.value = result.tradeNo
                        },
                        onFailure = { e ->
                            val msg = e.message ?: ""
                            val errorMessageRes = ErrorMessages.forOrder(e)
                            if (ErrorMessages.isPendingOrderMessage(msg)) {
                                _step.value =
                                    PurchaseStep.ExistingOrderError(
                                        errorMessageRes = errorMessageRes,
                                        plan = current.plan,
                                        period = current.selectedPeriod,
                                        couponCode = coupon,
                                    )
                            } else {
                                _step.value = PurchaseStep.OrderCreateError(errorMessageRes = errorMessageRes)
                            }
                        },
                    )
            } finally {
                creatingOrder = false
            }
        }
    }

    fun clearCreatedTradeNo() {
        _createdTradeNo.value = null
    }

    /** 加载已有订单的支付信息（从订单页「去支付」调用） */
    fun loadPaymentForOrder(tradeNo: String) {
        loadOrderPayment(CreateOrderResult(tradeNo))
    }

    private fun loadOrderPayment(orderResult: CreateOrderResult) {
        viewModelScope.launch {
            val methodsResult = orderRepository.getPaymentMethods()
            val detailResult = orderRepository.getOrderDetail(orderResult.tradeNo)

            val methods = methodsResult.getOrNull() ?: emptyList()
            val detail = detailResult.getOrNull()
            if (detail == null) {
                _toastRes.value = ErrorMessages.forOrder(detailResult.exceptionOrNull())
                return@launch
            }
            when (OrderStatus.from(detail.status)) {
                OrderStatus.COMPLETED -> {
                    _toastRes.value = R.string.order_already_paid
                    return@launch
                }
                OrderStatus.CANCELLED -> {
                    _toastRes.value = R.string.order_status_cancelled
                    return@launch
                }
                else -> Unit
            }

            _step.value =
                PurchaseStep.OrderPayment(
                    tradeNo = orderResult.tradeNo,
                    planName = detail.planName,
                    totalAmount = detail.totalAmount,
                    balanceAmount = detail.balanceAmount,
                    couponDiscount = detail.discountAmount,
                    surplusAmount = detail.surplusAmount,
                    refundAmount = detail.refundAmount,
                    handlingAmount = detail.handlingAmount ?: 0,
                    paymentMethods = methods,
                    selectedMethod = methods.firstOrNull()?.id,
                    isLoading = false,
                )
        }
    }

    fun selectPaymentMethod(methodId: Int) {
        val current = _step.value
        if (current is PurchaseStep.OrderPayment) {
            _step.value = current.copy(selectedMethod = methodId)
        }
    }

    /** 确认支付 → 结算订单 → 跳转浏览器 */
    fun confirmPayment() {
        val current = _step.value
        // isPaying 期间拒绝重入：UI 守卫读组合期快照，同帧双击可绕过，此处做权威拦截
        if (current !is PurchaseStep.OrderPayment || current.isPaying) return
        val methodId = if (current.zeroPayable) 0 else current.selectedMethod ?: return

        AppLog.d(TAG, "confirmPayment: tradeNo=${current.tradeNo} method=$methodId")
        _step.value = current.copy(isPaying = true)
        viewModelScope.launch {
            orderRepository
                .checkoutOrder(
                    tradeNo = current.tradeNo,
                    paymentMethod = methodId,
                ).fold(
                    onSuccess = { result ->
                        handleCheckoutResult(tradeNo = current.tradeNo, result = result)
                    },
                    onFailure = { e ->
                        AppLog.w(TAG, "checkoutOrder failed: ${sanitizeLog(e.message ?: "Unknown")}")
                        // 优先用适配器给出的资源 ID（空数据/响应格式异常/网络…）；
                        // 兜底用「支付失败」而非通用订单错误，结算场景更贴切
                        _toastRes.value = (e as? ApiException)?.stringResId ?: R.string.order_pay_failed
                        // 只复位 isPaying，绝不整体回写请求前快照：
                        // 请求飞行期间用户仍可切换支付方式（UI 只禁用支付按钮），
                        // 回写快照会把用户的切换静默改回旧值。
                        _step.update { s ->
                            if (s is PurchaseStep.OrderPayment) s.copy(isPaying = false) else s
                        }
                    },
                )
        }
    }

    private fun handleCheckoutResult(
        tradeNo: String,
        result: CheckoutResult,
    ) {
        AppLog.d(TAG, "checkout result: tradeNo=$tradeNo type=${result.type} hasRedirect=${result.redirectUrl != null}")
        when (decideCheckoutStep(result)) {
            CheckoutDecision.SUCCESS -> {
                // 结算结果不可丢：用户可能在请求飞行中点「返回」（此时 step 已不是 OrderPayment），
                // 但余额已经扣掉 —— 仍须提示并发完成事件驱动首页刷新。
                AppLog.i(TAG, "余额支付成功: tradeNo=$tradeNo")
                _toastRes.value = R.string.order_pay_success
                _step.update { s -> if (s is PurchaseStep.OrderPayment) PurchaseStep.Idle else s }
                _paymentCompleted.tryEmit(tradeNo)
            }
            CheckoutDecision.REDIRECT -> {
                _step.update { s ->
                    if (s is PurchaseStep.OrderPayment) PurchaseStep.Paying(redirectUrl = result.redirectUrl!!) else s
                }
            }
            CheckoutDecision.RETRY -> {
                // redirectUrl 缺失（如 V2Board 表单支付返回对象而非字符串）：
                // 必须复位 isPaying，否则支付按钮永久禁用、流程卡死
                _toastRes.value = R.string.order_pay_failed
                _step.update { s -> if (s is PurchaseStep.OrderPayment) s.copy(isPaying = false) else s }
            }
        }
    }

    /**
     * 返回上一步。
     *
     * 错误态下必须回到用户刚才的选择（套餐/周期/优惠券），而不是直接结束流程——
     * 原实现把错误弹窗的 onDismiss 一律接到「结束流程」，用户从「有未支付订单」提示返回后
     * 先前选的套餐与优惠券会全部丢失，只能从头再选一遍。
     */
    fun goBack() {
        pollJob?.cancel()
        pollingTradeNo = null
        _step.value =
            when (val current = _step.value) {
                is PurchaseStep.ExistingOrderError ->
                    PurchaseStep.SelectPeriod(
                        plan = current.plan,
                        selectedPeriod = current.period,
                        couponCode = current.couponCode ?: "",
                    )
                else -> PurchaseStep.Idle
            }
    }

    /** 中止当前购买流程回到初始态（导航离开购买流程时调用，避免残留的 step 在其它页面渲染） */
    fun abortFlow() {
        pollJob?.cancel()
        pollingTradeNo = null
        _step.value = PurchaseStep.Idle
    }

    /** 支付完成回调（从浏览器返回后调用） */
    fun onPaymentReturn() {
        AppLog.d(TAG, "onPaymentReturn: 从浏览器返回，停止轮询")
        pollJob?.cancel()
        pollingTradeNo = null
        _step.value = PurchaseStep.Idle
        // 支付结果由订单列表刷新后根据订单状态判定
    }

    /** 支付等待轮询：每 3 秒检查订单状态，支付完成/取消/超时即结束 */
    fun startOrderPolling(tradeNo: String) {
        // 幂等：同一订单已有在跑的轮询则忽略（该入口由 composable effect 驱动，可能重复触发）
        if (pollJob?.isActive == true && pollingTradeNo == tradeNo) return
        AppLog.d(TAG, "startOrderPolling: tradeNo=$tradeNo")
        pollJob?.cancel()
        pollingTradeNo = tradeNo
        pollJob =
            viewModelScope.launch {
                var elapsed = 0L
                while (elapsed < POLL_TIMEOUT_MS) {
                    delay(POLL_INTERVAL_MS)
                    elapsed += POLL_INTERVAL_MS
                    val status = orderRepository.getOrderDetail(tradeNo).getOrNull()?.status
                    val outcome = pollOutcome(status)
                    if (outcome != null) {
                        AppLog.i(TAG, "poll 结束: tradeNo=$tradeNo status=$status")
                        // 仅「已开通/已完成」触发完成事件；取消/异常终态静默结束，避免误导性刷新
                        if (outcome == PollOutcome.COMPLETED) {
                            _paymentCompleted.tryEmit(tradeNo)
                        }
                        return@launch
                    }
                }
            }
    }

    fun clearToast() {
        _toastRes.value = null
    }

    override fun onCleared() {
        pollJob?.cancel()
        pollingTradeNo = null
        super.onCleared()
    }

    private companion object {
        const val TAG = "SLTE-Purchase"

        /** 支付轮询间隔与总超时：限制后台无限轮询的资源消耗 */
        const val POLL_INTERVAL_MS = 3_000L
        const val POLL_TIMEOUT_MS = 300_000L
    }
}
