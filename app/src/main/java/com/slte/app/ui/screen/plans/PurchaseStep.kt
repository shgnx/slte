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
        val showWarning: Boolean = false
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
        val handlingAmount: Int,
        val paymentMethods: List<PaymentMethod> = emptyList(),
        val selectedMethod: Int? = null,
        val isLoading: Boolean = true,
        val isPaying: Boolean = false
    ) : PurchaseStep {
        /** 实际应付金额（分） */
        val payAmount: Int
            get() = payAmountCents(totalAmount, handlingAmount, couponDiscount, balanceAmount)
    }

    /** 支付完成，跳转浏览器 */
    data class Paying(val redirectUrl: String) : PurchaseStep

    /** 创建订单失败，有未支付订单 */
    data class ExistingOrderError(
        val errorMessageRes: Int,
        val plan: PlanInfo,
        val period: String,
        val couponCode: String?
    ) : PurchaseStep

    /** 其他创建订单失败 */
    data class OrderCreateError(val errorMessageRes: Int) : PurchaseStep

    /** 空状态 */
    data object Idle : PurchaseStep
}
