package com.slte.app.data.remote.api.dto

/**
 * 订单信息（来自 `/user/order/fetch` 接口）。
 */
data class OrderInfoDto(
    val id: Int,
    val tradeNo: String,
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
    /** 订单状态：0=待支付 1=开通中 2=已取消 3=已完成 4=已折抵 */
    val status: Int,
    /** 周期标识：month_price / quarter_price / year_price 等 */
    val period: String = "",
    /** 创建时间戳（秒） */
    val createdAt: Long,
    /** 到期时间戳（秒），0 表示无到期时间 */
    val expiredAt: Long,
)
