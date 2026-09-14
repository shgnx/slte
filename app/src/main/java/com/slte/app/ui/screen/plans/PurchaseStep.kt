package com.slte.app.ui.screen.plans

import com.slte.app.domain.model.PaymentMethod
import com.slte.app.domain.model.PlanInfo

/** 购买流程步骤状态 */
sealed interface PurchaseStep {
    /** 选择周期 + 输入优惠券 + 可选警告弹窗叠加 */
    data class SelectPeriod(
        val plan: PlanInfo,
        val selectedPeriod: String = plan.periodPrices.firstOrNull()?.period ?: "",
        val couponCode: String = "",
        val couponDiscount: Int = 0,
        val couponVerified: Boolean = false,
        val isVerifying: Boolean = false,
        val showWarning: Boolean = false,
    ) : PurchaseStep {
        /** 原价（分） */
        val priceCents: Int
            get() = plan.priceForPeriod(selectedPeriod)?.toIntOrNull() ?: 0

        /** 优惠后应付（分） */
        val finalPrice: Int
            get() = finalPriceCents(priceCents, couponDiscount)
    }

    /** 订单详情 + 选择支付方式（在我的订单页弹出） */
    data class OrderPayment(
        val tradeNo: String,
        val planName: String,
        val totalAmount: Int,
        val balanceAmount: Int,
        val couponDiscount: Int,
        val surplusAmount: Int = 0,
        val refundAmount: Int = 0,
        val handlingAmount: Int,
        val paymentMethods: List<PaymentMethod> = emptyList(),
        val selectedMethod: Int? = null,
        val isLoading: Boolean = true,
        val isPaying: Boolean = false,
    ) : PurchaseStep {
        /** 产品原价（分）：后端 total_amount 为扣减后净值，按各抵扣字段还原 */
        val productPrice: Int
            get() = (totalAmount + couponDiscount + surplusAmount + balanceAmount - refundAmount).coerceAtLeast(0)

        /** 实际应付金额（分）：后端 total_amount 已是券/余额抵扣后的净值，仅叠加手续费 */
        val payAmount: Int
            get() = (totalAmount + handlingAmount).coerceAtLeast(0)

        /** 应付为 0：后端在下单时已全额抵扣，结算走免费流程 */
        val zeroPayable: Boolean
            get() = payAmount <= 0
    }

    data class Paying(
        val redirectUrl: String,
    ) : PurchaseStep

    /** 创建订单失败，有未支付订单 */
    data class ExistingOrderError(
        val errorMessageRes: Int,
        val plan: PlanInfo,
        val period: String,
        val couponCode: String?,
    ) : PurchaseStep

    /** 其他创建订单失败 */
    data class OrderCreateError(
        val errorMessageRes: Int,
    ) : PurchaseStep

    data object Idle : PurchaseStep
}
