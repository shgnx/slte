package com.slte.app.domain.model

/** 订单状态分类 */
enum class OrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED,
    ABNORMAL,
    ;

    companion object {
        /** 后端状态码 → 分类：0=待支付 1=开通中 2=已取消 3=已完成 4=已折抵 */
        fun from(code: Int): OrderStatus = when (code) {
            0 -> PENDING
            1, 3, 4 -> COMPLETED
            2 -> CANCELLED
            else -> ABNORMAL
        }
    }
}

/**
 * 订单状态码是否代表「已开通/已完成」。
 *
 * 唯一判定入口：支付轮询（[com.slte.app.ui.screen.plans.pollOutcome]）与支付后刷新
 * （SubscriptionUpdater）必须共用本函数。此前支付轮询硬编码 `setOf(3)`，把常见的
 * 「开通中(=1)」判为未完成，导致支付成功后空等并误报「开通超时」。
 */
fun isOrderActivated(code: Int): Boolean = OrderStatus.from(code) == OrderStatus.COMPLETED

data class OrderInfo(
    val id: Int,
    /** 订单号 */
    val tradeNo: String,
    /** 套餐名称 */
    val planName: String,
    /** 金额（分） */
    val totalAmount: Int,
    /** 余额抵扣（分） */
    val balanceAmount: Int = 0,
    /** 优惠券抵扣（分，含会员折扣） */
    val discountAmount: Int = 0,
    /** 旧套餐折抵（分，换购单） */
    val surplusAmount: Int = 0,
    /** 换购多退金额（分，开通后退回余额） */
    val refundAmount: Int = 0,
    /** 手续费（分），无手续费为 null */
    val handlingAmount: Int? = null,
    /** 订单状态原始码（分类见 [OrderStatus.from]） */
    val status: Int,
    /** 周期标识：month_price / quarter_price / year_price 等 */
    val period: String = "",
    /** 创建时间戳（秒） */
    val createdAt: Long,
    /** 到期时间戳（秒），0 表示无到期时间 */
    val expiredAt: Long,
) {
    /** 状态分类（UI 展示语义） */
    val statusClass: OrderStatus get() = OrderStatus.from(status)
}

data class PaymentMethod(
    val id: Int,
    val name: String,
    val payment: String = "",
    val icon: String? = null,
)

data class CreateOrderResult(
    val tradeNo: String,
)

data class CheckoutResult(
    /** -1=后端免费流程直接成功 0=扫码类（暂不支持） 1=跳转链接 2=直付结果（data=true 即成功） */
    val type: Int,
    val redirectUrl: String? = null,
    val message: String? = null,
    val paid: Boolean = false,
)

data class CouponCheckResult(
    val name: String,
    /** 2=百分比折扣，1=固定金额减扣 */
    val type: Int,
    /** 百分比时表示折扣百分数，固定金额时表示减扣金额（分） */
    val value: Int,
)
