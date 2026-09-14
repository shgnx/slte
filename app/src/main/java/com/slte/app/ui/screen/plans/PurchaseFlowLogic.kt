package com.slte.app.ui.screen.plans

import com.slte.app.domain.model.CheckoutResult
import com.slte.app.domain.model.isOrderActivated

/** 结算结果 → 流程决策 */
enum class CheckoutDecision {
    /** 支付成功（余额/免费），直接结束流程 */
    SUCCESS,

    /** 需要跳转浏览器支付 */
    REDIRECT,

    /** 支付未完成，复位支付按钮等待重试 */
    RETRY,
}

/** 支付轮询单次状态判定：null=继续等待；TERMINATED=终态停止（取消/异常，不发完成事件）；COMPLETED=支付完成 */
enum class PollOutcome { TERMINATED, COMPLETED }

/** 根据订单状态码决定轮询走向（纯函数，供 ViewModel 与单测复用） */
fun pollOutcome(status: Int?): PollOutcome? = when {
    status == null || status == 0 -> null
    isOrderActivated(status) -> PollOutcome.COMPLETED
    else -> PollOutcome.TERMINATED
}

/** 根据结算结果决定下一步（纯函数，供 ViewModel 与单测复用） */
fun decideCheckoutStep(result: CheckoutResult): CheckoutDecision = when (result.type) {
    -1 -> CheckoutDecision.SUCCESS
    1 -> if (result.redirectUrl != null) CheckoutDecision.REDIRECT else CheckoutDecision.RETRY
    2 -> if (result.paid) CheckoutDecision.SUCCESS else CheckoutDecision.RETRY
    else -> CheckoutDecision.RETRY
}

/** 优惠券折扣计算：type=2 为百分比折扣（value=折扣百分数），type=1 为固定金额（分）；最低 0 */
internal fun computeCouponDiscount(
    type: Int,
    value: Int,
    priceCents: Int,
): Int = when (type) {
    2 -> priceCents * value / 100
    else -> value
}.coerceAtLeast(0)

/** 优惠后应付（分）：原价减优惠券，最低 0 */
internal fun finalPriceCents(
    priceCents: Int,
    couponDiscount: Int,
): Int = (priceCents - couponDiscount).coerceAtLeast(0)
