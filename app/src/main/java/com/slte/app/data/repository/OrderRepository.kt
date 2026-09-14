package com.slte.app.data.repository

import com.slte.app.data.remote.api.AuthApi
import com.slte.app.data.remote.api.dto.PlanInfoDto
import com.slte.app.domain.model.CheckoutResult
import com.slte.app.domain.model.CouponCheckResult
import com.slte.app.domain.model.CreateOrderResult
import com.slte.app.domain.model.OrderInfo
import com.slte.app.domain.model.PaymentMethod
import com.slte.app.domain.model.PlanInfo
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 订单仓库：套餐查询、下单、支付、取消。
 *
 * 鉴权由 OkHttp 拦截器统一注入。
 */
@Singleton
class OrderRepository
@Inject
constructor(
    private val authApi: AuthApi,
) {
    suspend fun fetchPlans(): Result<List<PlanInfo>> = runApi {
        authApi.fetchPlans().map { it.toDomainPlanInfo() }
    }

    suspend fun createOrder(
        planId: Int,
        period: String,
        couponCode: String? = null,
    ): Result<CreateOrderResult> = runApi {
        authApi.createOrder(planId, period, couponCode).let {
            CreateOrderResult(tradeNo = it.tradeNo)
        }
    }

    suspend fun getOrderDetail(tradeNo: String): Result<OrderInfo> = runApi {
        authApi.getOrderDetail(tradeNo).toDomain()
    }

    suspend fun checkCoupon(
        code: String,
        planId: Int?,
    ): Result<CouponCheckResult> = runApi {
        authApi.checkCoupon(code, planId).let {
            CouponCheckResult(name = it.name, type = it.type, value = it.value)
        }
    }

    suspend fun checkoutOrder(
        tradeNo: String,
        paymentMethod: Int,
    ): Result<CheckoutResult> = runApi {
        authApi.checkoutOrder(tradeNo, paymentMethod).let {
            CheckoutResult(
                type = it.type,
                redirectUrl = it.redirectUrl,
                message = it.message,
                paid = it.paid,
            )
        }
    }

    suspend fun getPaymentMethods(): Result<List<PaymentMethod>> = runApi {
        authApi.getPaymentMethods().map {
            PaymentMethod(id = it.id, name = it.name, payment = it.payment, icon = it.icon)
        }
    }

    suspend fun cancelOrder(tradeNo: String): Result<Unit> = runApi {
        authApi.cancelOrder(tradeNo)
    }

    suspend fun fetchOrders(): Result<List<OrderInfo>> = runApi {
        authApi.fetchOrders().map { it.toDomain() }
    }.onFailure {
        AppLog.w("SLTE-Repo", "fetchOrders failed: ${sanitizeLog(it.message ?: "Unknown")}")
    }

    private fun com.slte.app.data.remote.api.dto.OrderInfoDto.toDomain() = OrderInfo(
        id = id,
        tradeNo = tradeNo,
        planName = planName,
        totalAmount = totalAmount,
        balanceAmount = balanceAmount,
        discountAmount = discountAmount,
        surplusAmount = surplusAmount,
        refundAmount = refundAmount,
        handlingAmount = handlingAmount,
        status = status,
        period = period,
        createdAt = createdAt,
        expiredAt = expiredAt,
    )

    private fun PlanInfoDto.toDomainPlanInfo() = PlanInfo(
        id = id.toLong(),
        name = name,
        transferEnable = transferEnable,
        speedLimit = speedLimit,
        deviceLimit = deviceLimit,
        content = content,
        show = show,
        periodPrices =
        listOfNotNull(
            monthPrice?.takeIf { it > 0 }?.let { PlanInfo.PeriodPrice("month_price", it.toString()) },
            quarterPrice?.takeIf { it > 0 }?.let { PlanInfo.PeriodPrice("quarter_price", it.toString()) },
            halfYearPrice?.takeIf { it > 0 }?.let { PlanInfo.PeriodPrice("half_year_price", it.toString()) },
            yearPrice?.takeIf { it > 0 }?.let { PlanInfo.PeriodPrice("year_price", it.toString()) },
            twoYearPrice?.takeIf { it > 0 }?.let { PlanInfo.PeriodPrice("two_year_price", it.toString()) },
            threeYearPrice?.takeIf { it > 0 }?.let { PlanInfo.PeriodPrice("three_year_price", it.toString()) },
            onetimePrice?.takeIf { it > 0 }?.let { PlanInfo.PeriodPrice("onetime_price", it.toString()) },
        ),
    )
}
