package com.slte.app.data.remote.adapter.xiaov2b

import com.slte.app.data.remote.api.dto.OrderInfoDto
import com.slte.app.data.remote.api.dto.CouponCheckResultDto
import com.slte.app.data.remote.api.dto.PaymentMethodDto
import com.slte.app.data.remote.api.dto.PlanInfoDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class XiaoV2bCreateOrderRequest(
    @SerialName("plan_id")
    val planId: Int,
    val period: String,
    @SerialName("coupon_code")
    val couponCode: String? = null
)

@Serializable
data class XiaoV2bCouponCheckRequest(
    val code: String,
    @SerialName("plan_id")
    val planId: Int? = null
)

@Serializable
data class XiaoV2bCheckoutRequest(
    @SerialName("trade_no")
    val tradeNo: String,
    val method: Int
)

@Serializable
data class XiaoV2bCancelOrderRequest(
    @SerialName("trade_no")
    val tradeNo: String
)

@Serializable
data class XiaoV2bOrderData(
    val id: Int = 0,
    @SerialName("trade_no")
    val tradeNo: String = "",
    @SerialName("total_amount")
    val totalAmount: Int = 0,
    @SerialName("balance_amount")
    val balanceAmount: JsonElement? = null,
    @SerialName("discount_amount")
    val discountAmount: JsonElement? = null,
    @SerialName("handling_amount")
    val handlingAmount: JsonElement? = null,
    val status: Int = 0,
    val period: String = "",
    @SerialName("created_at")
    val createdAt: Long = 0L,
    @SerialName("expired_at")
    val expiredAt: Long = 0L,
    val plan: XiaoV2bPlanData? = null
) {
    fun toDomainOrder() = OrderInfoDto(
        id = id,
        tradeNo = tradeNo,
        planName = plan?.name ?: "",
        totalAmount = totalAmount,
        balanceAmount = balanceAmount.jsonIntOrNull() ?: 0,
        discountAmount = discountAmount.jsonIntOrNull() ?: 0,
        handlingAmount = handlingAmount.jsonIntOrNull(),
        status = status,
        period = period,
        createdAt = createdAt,
        expiredAt = expiredAt
    )
}


@Serializable
data class XiaoV2bCouponData(
    val id: Int? = null,
    val code: String? = null,
    val name: String? = null,
    val type: Int? = null,
    val value: Int? = null,
    @SerialName("plan_id")
    val planId: Int? = null
) {
    fun toDomainCouponCheck() = CouponCheckResultDto(
        name = name ?: code ?: "",
        type = type ?: 2,
        value = value ?: 0
    )
}

@Serializable
data class XiaoV2bPlanData(
    val id: Int = 0,
    val name: String = "",
    val description: String? = null,
    @SerialName("month_price")
    val monthPrice: Long? = null,
    @SerialName("quarter_price")
    val quarterPrice: Long? = null,
    @SerialName("half_year_price")
    val halfYearPrice: Long? = null,
    @SerialName("year_price")
    val yearPrice: Long? = null,
    @SerialName("two_year_price")
    val twoYearPrice: Long? = null,
    @SerialName("three_year_price")
    val threeYearPrice: Long? = null,
    @SerialName("onetime_price")
    val onetimePrice: Long? = null,
    @SerialName("reset_price")
    val resetPrice: Long? = null,
    @SerialName("speed_limit")
    val speedLimit: Int? = null,
    @SerialName("device_limit")
    val deviceLimit: Int? = null,
    val content: String? = null,
    val show: Int = 1,
    val renew: Int = 1,
    @SerialName("capacity_limit")
    val capacityLimit: Int? = null,
    @SerialName("group_id")
    val groupId: Int? = null,
    val sort: Int? = null,
    @SerialName("transfer_enable")
    val transferEnable: Int = 0
) {
    fun toDomainPlan() = PlanInfoDto(
        id = id,
        name = name,
        monthPrice = monthPrice,
        quarterPrice = quarterPrice,
        halfYearPrice = halfYearPrice,
        yearPrice = yearPrice,
        twoYearPrice = twoYearPrice,
        threeYearPrice = threeYearPrice,
        onetimePrice = onetimePrice,
        resetPrice = resetPrice,
        speedLimit = speedLimit,
        deviceLimit = deviceLimit,
        content = content,
        transferEnable = transferEnable,
        show = show == 1,
        renew = renew == 1,
        sort = sort
    )
}

@Serializable
data class XiaoV2bOrderDetailData(
    val id: Int = 0,
    @SerialName("trade_no")
    val tradeNo: String = "",
    @SerialName("total_amount")
    val totalAmount: Int = 0,
    @SerialName("balance_amount")
    val balanceAmount: JsonElement? = null,
    @SerialName("discount_amount")
    val discountAmount: JsonElement? = null,
    @SerialName("handling_amount")
    val handlingAmount: JsonElement? = null,
    val status: Int = 0,
    val period: String = "",
    @SerialName("created_at")
    val createdAt: Long = 0L,
    @SerialName("expired_at")
    val expiredAt: Long = 0L,
    val plan: XiaoV2bPlanData? = null
) {
    fun toDomainOrder() = OrderInfoDto(
        id = id,
        tradeNo = tradeNo,
        planName = plan?.name ?: "",
        totalAmount = totalAmount,
        balanceAmount = balanceAmount.jsonIntOrNull() ?: 0,
        discountAmount = discountAmount.jsonIntOrNull() ?: 0,
        handlingAmount = handlingAmount.jsonIntOrNull(),
        status = status,
        period = period,
        createdAt = createdAt,
        expiredAt = expiredAt
    )
}

/** 后端可能返回数字、JSON null 或字符串 "null"，统一容错为 Int? */
private fun JsonElement?.jsonIntOrNull(): Int? = when (this) {
    is kotlinx.serialization.json.JsonPrimitive -> content.toIntOrNull()
    else -> null
}

@Serializable
data class XiaoV2bCheckoutData(
    val type: Int = 0,
    val data: JsonElement? = null,
    val message: String? = null
)

@Serializable
data class XiaoV2bPaymentMethodData(
    val id: Int = 0,
    val name: String = "",
    val payment: String = "",
    val icon: String? = null
) {
    fun toDomainPaymentMethod() = PaymentMethodDto(
        id = id,
        name = name,
        payment = payment,
        icon = icon
    )
}
