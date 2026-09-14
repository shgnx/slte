package com.slte.app.ui.screen.plans

import com.slte.app.domain.model.CheckoutResult
import com.slte.app.domain.model.OrderStatus
import com.slte.app.domain.model.PlanInfo
import com.slte.app.domain.model.isOrderActivated
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 支付流程决策与订单状态分类（纯函数）测试。
 */
class PurchaseFlowLogicTest {
    @Test
    fun `结算类型负一判定支付成功`() {
        assertEquals(
            CheckoutDecision.SUCCESS,
            decideCheckoutStep(CheckoutResult(type = -1)),
        )
    }

    @Test
    fun `结算类型一且带跳转地址判定跳转`() {
        assertEquals(
            CheckoutDecision.REDIRECT,
            decideCheckoutStep(CheckoutResult(type = 1, redirectUrl = "https://pay.example")),
        )
    }

    @Test
    fun `结算类型零扫码类判定重试`() {
        assertEquals(
            CheckoutDecision.RETRY,
            decideCheckoutStep(CheckoutResult(type = 0, redirectUrl = "https://pay.example")),
        )
    }

    @Test
    fun `结算类型二按直付结果判定`() {
        assertEquals(
            CheckoutDecision.SUCCESS,
            decideCheckoutStep(CheckoutResult(type = 2, paid = true)),
        )
        assertEquals(
            CheckoutDecision.RETRY,
            decideCheckoutStep(CheckoutResult(type = 2, paid = false)),
        )
    }

    @Test
    fun `结算类型一缺跳转地址判定重试`() {
        assertEquals(
            CheckoutDecision.RETRY,
            decideCheckoutStep(CheckoutResult(type = 1, redirectUrl = null)),
        )
    }

    @Test
    fun `未知结算类型判定重试`() {
        assertEquals(
            CheckoutDecision.RETRY,
            decideCheckoutStep(CheckoutResult(type = 99)),
        )
    }

    @Test
    fun `订单状态码映射到分类`() {
        assertEquals(OrderStatus.PENDING, OrderStatus.from(0))
        assertEquals(OrderStatus.COMPLETED, OrderStatus.from(1))
        assertEquals(OrderStatus.CANCELLED, OrderStatus.from(2))
        assertEquals(OrderStatus.COMPLETED, OrderStatus.from(3))
        assertEquals(OrderStatus.COMPLETED, OrderStatus.from(4))
        assertEquals(OrderStatus.ABNORMAL, OrderStatus.from(-1))
        assertEquals(OrderStatus.ABNORMAL, OrderStatus.from(9))
    }

    @Test
    fun `应付金额为净值加手续费`() {
        val step =
            PurchaseStep.OrderPayment(
                tradeNo = "T",
                planName = "P",
                totalAmount = 100,
                balanceAmount = 200,
                couponDiscount = 30,
                handlingAmount = 20,
            )
        assertEquals(120, step.payAmount)
        assertEquals(false, step.zeroPayable)
    }

    @Test
    fun `应付金额为零时判定免费开通`() {
        val step =
            PurchaseStep.OrderPayment(
                tradeNo = "T",
                planName = "P",
                totalAmount = 0,
                balanceAmount = 200,
                couponDiscount = 0,
                handlingAmount = 0,
            )
        assertEquals(0, step.payAmount)
        assertEquals(true, step.zeroPayable)
    }

    @Test
    fun `优惠后应付金额钳制为非负`() {
        val step =
            PurchaseStep.SelectPeriod(
                plan =
                PlanInfo(
                    id = 1,
                    name = "P",
                    periodPrices = listOf(PlanInfo.PeriodPrice(period = "month", price = "100")),
                ),
                selectedPeriod = "month",
                couponDiscount = 500,
            )
        assertEquals(0, step.finalPrice)
    }

    @Test
    fun `轮询状态待支付或缺失时继续等待`() {
        assertNull(pollOutcome(null))
        assertNull(pollOutcome(0))
    }

    @Test
    fun `轮询状态支付完成判定完成`() {
        assertEquals(PollOutcome.COMPLETED, pollOutcome(1))
        assertEquals(PollOutcome.COMPLETED, pollOutcome(3))
        assertEquals(PollOutcome.COMPLETED, pollOutcome(4))
    }

    @Test
    fun `轮询状态取消或异常判定终态停止`() {
        assertEquals(PollOutcome.TERMINATED, pollOutcome(2))
        assertEquals(PollOutcome.TERMINATED, pollOutcome(-1))
        assertEquals(PollOutcome.TERMINATED, pollOutcome(9))
    }

    /**
     * 状态码语义单一来源回归：轮询（[pollOutcome]）与开通判定（[isOrderActivated]）
     * 对同一状态码必须一致。此前开通判定硬编码 `setOf(3)`，把「开通中(=1)」判为未完成，
     * 导致余额支付成功后空等 60 秒误报「开通超时」并清空节点缓存，本用例锁死两侧对齐。
     */
    @Test
    fun `轮询与开通判定对同一状态码结论一致`() {
        listOf(-1, 0, 1, 2, 3, 4, 9, 99).forEach { code ->
            assertEquals(
                "状态码 $code 的「已完成」判定在轮询与开通等待之间不一致",
                pollOutcome(code) == PollOutcome.COMPLETED,
                isOrderActivated(code),
            )
        }
    }

    @Test
    fun `开通中与已折抵都算已开通`() {
        // 1=开通中、3=已完成、4=已折抵：三者都是「后端已受理」，必须一致对待
        assertEquals(true, isOrderActivated(1))
        assertEquals(true, isOrderActivated(3))
        assertEquals(true, isOrderActivated(4))
        assertEquals(false, isOrderActivated(0))
        assertEquals(false, isOrderActivated(2))
    }
}
