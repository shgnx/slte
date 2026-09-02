package com.slte.app.ui.screen.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.domain.model.CheckoutResult
import com.slte.app.domain.model.CreateOrderResult
import com.slte.app.data.repository.OrderRepository
import com.slte.app.domain.model.PlanInfo
import com.slte.app.utils.ErrorMessages
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 购买流程 ViewModel。
 *
 * 流程：选择周期 → 确认警告 → 创建订单 → 选择支付 → 跳转浏览器
 */
@HiltViewModel
class PurchaseViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _step = MutableStateFlow<PurchaseStep>(PurchaseStep.Idle)
    val step: StateFlow<PurchaseStep> = _step.asStateFlow()

    private val _toastRes = MutableStateFlow<Int?>(null)
    val toastRes: StateFlow<Int?> = _toastRes.asStateFlow()

    /** 支付完成事件（余额支付成功 / 轮询确认）：携带订单号，供首页 Loading 等待订单"已开通"后刷新 */
    private val _paymentCompleted = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val paymentCompleted: SharedFlow<String> = _paymentCompleted.asSharedFlow()

    private var pollJob: Job? = null

    /** 开始购买流程 */
    fun startPurchase(plan: PlanInfo) {
        _step.value = PurchaseStep.SelectPeriod(plan = plan)
    }

    /** 选择周期 */
    fun selectPeriod(period: String) {
        val current = _step.value
        if (current is PurchaseStep.SelectPeriod) {
            _step.value = current.copy(selectedPeriod = period)
        }
    }

    /** 更新优惠券码 */
    fun updateCouponCode(code: String) {
        val current = _step.value
        if (current is PurchaseStep.SelectPeriod) {
            _step.value = current.copy(
                couponCode = code,
                couponVerified = false,
                couponDiscount = 0,
                isVerifying = false
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
            orderRepository.checkCoupon(
                code = current.couponCode.trim(),
                planId = current.plan.id.toInt()
            ).fold(
                onSuccess = { result ->
                    val state = _step.value as? PurchaseStep.SelectPeriod
                    if (state == null || state.couponCode != current.couponCode) return@fold
                    val discount = computeCouponDiscount(result.type, result.value, current.priceCents)
                    AppLog.d(TAG, "checkCoupon: type=${result.type} value=${result.value} price=${current.priceCents} discount=$discount")
                    _step.value = state.copy(
                        isVerifying = false,
                        couponVerified = true,
                        couponDiscount = discount
                    )
                    _toastRes.value = R.string.purchase_coupon_applied
                },
                onFailure = { e ->
                    val state = _step.value as? PurchaseStep.SelectPeriod
                    if (state == null || state.couponCode != current.couponCode) return@fold
                    _step.value = state.copy(
                        isVerifying = false,
                        couponVerified = false,
                        couponDiscount = 0
                    )
                    _toastRes.value = ErrorMessages.mapOrderError(e.message)
                }
            )
        }
    }

    /** 点击确认订单 → 显示警告弹窗（叠加在当前抽屉上） */
    fun showConfirmWarning() {
        val current = _step.value
        if (current !is PurchaseStep.SelectPeriod) return
        // 填了优惠券但未验证：不允许进入下一步
        if (current.couponCode.isNotBlank() && !current.couponVerified) {
            _toastRes.value = R.string.purchase_coupon_verify_first
            return
        }
        _step.value = current.copy(showWarning = true)
    }

    /** 取消警告 → 关闭警告，保留抽屉 */
    fun cancelWarning() {
        val current = _step.value
        if (current is PurchaseStep.SelectPeriod) {
            _step.value = current.copy(showWarning = false)
        }
    }

    private var creatingOrder = false

    /** 确认警告 → 创建订单 → 关闭所有 → 返回订单号给调用方 */
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
                orderRepository.createOrder(
                    planId = current.plan.id.toInt(),
                    period = current.selectedPeriod,
                    couponCode = coupon
                ).fold(
                    onSuccess = { result ->
                        _step.value = PurchaseStep.Idle
                        _createdTradeNo.value = result.tradeNo
                    },
                    onFailure = { e ->
                        val msg = e.message ?: ""
                        val errorMessageRes = ErrorMessages.mapOrderError(msg)
                        if (ErrorMessages.isPendingOrderMessage(msg)) {
                            _step.value = PurchaseStep.ExistingOrderError(
                                errorMessageRes = errorMessageRes,
                                plan = current.plan,
                                period = current.selectedPeriod,
                                couponCode = coupon
                            )
                        } else {
                            _step.value = PurchaseStep.OrderCreateError(errorMessageRes = errorMessageRes)
                        }
                    }
                )
            } finally {
                creatingOrder = false
            }
        }
    }

    /** 从未支付订单错误返回选择周期 */
    fun dismissExistingOrderError() {
        val current = _step.value
        if (current is PurchaseStep.ExistingOrderError) {
            _step.value = PurchaseStep.SelectPeriod(
                plan = current.plan,
                selectedPeriod = current.period,
                couponCode = current.couponCode ?: ""
            )
        }
    }

    /** 从创建订单失败返回 */
    fun dismissOrderCreateError() {
        _step.value = PurchaseStep.Idle
    }

    /** 清除已创建的订单号（导航消费后调用） */
    fun clearCreatedTradeNo() {
        _createdTradeNo.value = null
    }

    /** 加载已有订单的支付信息（从订单页「去支付」调用） */
    fun loadPaymentForOrder(tradeNo: String) {
        loadOrderPayment(CreateOrderResult(tradeNo))
    }

    /** 加载订单支付信息 */
    private fun loadOrderPayment(orderResult: CreateOrderResult) {
        viewModelScope.launch {
            val methodsResult = orderRepository.getPaymentMethods()
            val detailResult = orderRepository.getOrderDetail(orderResult.tradeNo)

            val methods = methodsResult.getOrNull() ?: emptyList()
            val detail = detailResult.getOrNull()

            _step.value = PurchaseStep.OrderPayment(
                tradeNo = orderResult.tradeNo,
                planName = detail?.planName ?: "",
                totalAmount = detail?.totalAmount ?: 0,
                balanceAmount = detail?.balanceAmount ?: 0,
                couponDiscount = detail?.discountAmount ?: 0,
                handlingAmount = detail?.handlingAmount ?: 0,
                paymentMethods = methods,
                selectedMethod = methods.firstOrNull()?.id,
                isLoading = false
            )
        }
    }

    /** 选择支付方式 */
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
        val methodId = current.selectedMethod ?: return

        AppLog.d(TAG, "confirmPayment: tradeNo=${current.tradeNo} method=$methodId")
        _step.value = current.copy(isPaying = true)
        viewModelScope.launch {
            orderRepository.checkoutOrder(
                tradeNo = current.tradeNo,
                paymentMethod = methodId
            ).fold(
                onSuccess = { result ->
                    handleCheckoutResult(result)
                },
                onFailure = { e ->
                    AppLog.w(TAG, "checkoutOrder failed: ${sanitizeLog(e.message ?: "Unknown")}")
                    _toastRes.value = R.string.order_pay_failed
                    _step.value = current.copy(isPaying = false)
                }
            )
        }
    }

    private fun handleCheckoutResult(result: CheckoutResult) {
        val current = _step.value as? PurchaseStep.OrderPayment ?: return
        AppLog.d(TAG, "checkout result: tradeNo=${current.tradeNo} type=${result.type} hasRedirect=${result.redirectUrl != null}")
        when (decideCheckoutStep(result)) {
            CheckoutDecision.SUCCESS -> {
                AppLog.i(TAG, "余额支付成功: tradeNo=${current.tradeNo}")
                _toastRes.value = R.string.order_pay_success
                _step.value = PurchaseStep.Idle
                _paymentCompleted.tryEmit(current.tradeNo)
            }
            CheckoutDecision.REDIRECT -> {
                _step.value = PurchaseStep.Paying(redirectUrl = result.redirectUrl!!)
            }
            CheckoutDecision.RETRY -> {
                // redirectUrl 缺失（如 V2Board 表单支付返回对象而非字符串）：
                // 必须复位 isPaying，否则支付按钮永久禁用、流程卡死
                _toastRes.value = R.string.order_pay_failed
                _step.value = current.copy(isPaying = false)
            }
        }
    }

    /** 返回上一步 */
    fun goBack() {
        pollJob?.cancel()
        _step.value = PurchaseStep.Idle
    }

    /** 支付完成回调（从浏览器返回后调用） */
    fun onPaymentReturn() {
        AppLog.d(TAG, "onPaymentReturn: 从浏览器返回，停止轮询")
        pollJob?.cancel()
        _step.value = PurchaseStep.Idle
        // 支付结果由订单列表刷新后根据订单状态判定
    }

    /** 支付等待轮询：每 3 秒检查订单状态，支付完成/取消/超时即结束 */
    fun startOrderPolling(tradeNo: String) {
        AppLog.d(TAG, "startOrderPolling: tradeNo=$tradeNo")
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            var elapsed = 0L
            while (elapsed < POLL_TIMEOUT_MS) {
                delay(POLL_INTERVAL_MS)
                elapsed += POLL_INTERVAL_MS
                val status = orderRepository.getOrderDetail(tradeNo).getOrNull()?.status
                val outcome = pollOutcome(status)
                if (outcome != null) {
                    AppLog.i(TAG, "poll 结束: tradeNo=$tradeNo status=$status")
                    pollJob?.cancel()
                    // 仅支付完成(1/3)触发完成事件；取消(2)/异常终态静默结束，避免误导性刷新
                    if (outcome == PollOutcome.COMPLETED) {
                        _paymentCompleted.tryEmit(tradeNo)
                    }
                    break
                }
            }
        }
    }

    fun clearToast() {
        _toastRes.value = null
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "SLTE-Purchase"
        /** 支付轮询间隔与总超时：限制后台无限轮询的资源消耗 */
        const val POLL_INTERVAL_MS = 3_000L
        const val POLL_TIMEOUT_MS = 300_000L
    }
}

