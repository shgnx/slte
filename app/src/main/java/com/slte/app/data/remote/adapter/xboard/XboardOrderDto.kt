package com.slte.app.data.remote.adapter.xboard

import com.slte.app.data.remote.api.dto.CheckoutResultDto
import com.slte.app.data.remote.api.dto.CouponCheckResultDto
import com.slte.app.data.remote.api.dto.OrderInfoDto
import com.slte.app.data.remote.api.dto.PaymentMethodDto
import com.slte.app.data.remote.api.dto.PlanInfoDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive


@Serializable
data class XboardCreateOrderRequest(
    @SerialName("plan_id")
    val planId: Int,
    val period: String,
    @SerialName("coupon_code")
    val couponCode: String? = null
)

@Serializable
data class XboardCouponCheckRequest(
    val code: String,
    @SerialName("plan_id")
    val planId: Int? = null
)

@Serializable
data class XboardCheckoutRequest(
    @SerialName("trade_no")
    val tradeNo: String,
    val method: Int
)

@Serializable
data class XboardCancelOrderRequest(
    @SerialName("trade_no")
    val tradeNo: String
)


@Serializable
data class XboardPlanData(
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
    val show: Boolean = true,
    val renew: Boolean = true,
    @SerialName("capacity_limit")
    val capacityLimit: Int? = null,
    @SerialName("group_id")
    val groupId: Int? = null,
    val sort: Int? = null,
    @SerialName("transfer_enable")
    val transferEnable: Int = 0
)

fun XboardPlanData.toDomainPlan() = PlanInfoDto(
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
    show = show,
    renew = renew,
    sort = sort
)

@Serializable
data class XboardOrderData(
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
    val plan: XboardPlanData? = null
)

fun XboardOrderData.toDomainOrder() = OrderInfoDto(
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

@Serializable
data class XboardOrderDetailData(
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
    val plan: XboardPlanData? = null
)

fun XboardOrderDetailData.toDomainOrder() = OrderInfoDto(
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

@Serializable
data class XboardCouponData(
    val id: Int? = null,
    val code: String? = null,
    val name: String? = null,
    val type: Int? = null,
    val value: Int? = null,
    @SerialName("plan_id")
    val planId: Int? = null
)

fun XboardCouponData.toDomainCouponCheck() = CouponCheckResultDto(
    name = name ?: code ?: "",
    type = type ?: 2,
    value = value ?: 0
)

/** 后端可能返回数字、JSON null 或字符串 "null"，统一容错为 Int? */
private fun JsonElement?.jsonIntOrNull(): Int? = when (this) {
    is JsonPrimitive -> content.toIntOrNull()
    else -> null
}

@Serializable
data class XboardCheckoutData(
    val type: Int = 0,
    val data: JsonElement? = null,
    val message: String? = null
)

@Serializable
data class XboardPaymentMethodData(
    val id: Int = 0,
    val name: String = "",
    val payment: String = "",
    val icon: String? = null
)

fun XboardPaymentMethodData.toDomainPaymentMethod() = PaymentMethodDto(
    id = id,
    name = name,
    payment = payment,
    icon = icon
)
