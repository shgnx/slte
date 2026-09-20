package com.slte.app.data.remote.api

import com.slte.app.data.remote.api.dto.CheckoutResultDto
import com.slte.app.data.remote.api.dto.CouponCheckResultDto
import com.slte.app.data.remote.api.dto.CreateOrderResultDto
import com.slte.app.data.remote.api.dto.LoginResponseDto
import com.slte.app.data.remote.api.dto.OrderInfoDto
import com.slte.app.data.remote.api.dto.PaymentMethodDto
import com.slte.app.data.remote.api.dto.PlanInfoDto
import com.slte.app.data.remote.api.dto.SubscribeInfoDto
import com.slte.app.data.remote.api.dto.UserInfoDto
import com.slte.app.domain.model.CommissionRecord
import com.slte.app.domain.model.EmailCodePurpose
import com.slte.app.domain.model.InviteInfo
import com.slte.app.domain.model.Notice
import com.slte.app.domain.model.RegisterConfig
import com.slte.app.domain.model.ServerNode

interface AuthApi {
    suspend fun login(
        email: String,
        password: String,
    ): LoginResponseDto

    suspend fun register(
        email: String,
        password: String,
        emailCode: String? = null,
        inviteCode: String? = null,
    ): LoginResponseDto

    suspend fun fetchRegisterConfig(): RegisterConfig

    suspend fun forgotPassword(
        email: String,
        emailCode: String,
        password: String,
    )

    suspend fun sendEmailCode(
        email: String,
        purpose: EmailCodePurpose = EmailCodePurpose.FORGOT_PASSWORD,
    )

    suspend fun revokeActiveSessions(authData: String)

    suspend fun fetchUserInfo(): UserInfoDto

    suspend fun fetchSubscribeInfo(): SubscribeInfoDto

    suspend fun updateRemindExpire(enabled: Boolean)

    suspend fun updateRemindTraffic(enabled: Boolean)

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
    )

    suspend fun fetchOrders(): List<OrderInfoDto>

    suspend fun fetchPlans(): List<PlanInfoDto>

    suspend fun createOrder(
        planId: Int,
        period: String,
        couponCode: String? = null,
    ): CreateOrderResultDto

    suspend fun getOrderDetail(tradeNo: String): OrderInfoDto

    suspend fun checkCoupon(
        code: String,
        planId: Int? = null,
    ): CouponCheckResultDto

    suspend fun checkoutOrder(
        tradeNo: String,
        paymentMethod: Int,
    ): CheckoutResultDto

    suspend fun getPaymentMethods(): List<PaymentMethodDto>

    suspend fun cancelOrder(tradeNo: String)

    suspend fun redeemGiftCard(code: String)

    suspend fun fetchInviteInfo(): InviteInfo

    suspend fun generateInviteCode(): Boolean

    suspend fun fetchCommissionRecords(
        page: Int,
        pageSize: Int,
    ): List<CommissionRecord>

    suspend fun transferCommission(transferAmount: Int): Boolean

    suspend fun withdrawCommission(
        withdrawMethod: String,
        withdrawAccount: String,
    ): Boolean

    suspend fun fetchWithdrawMethods(): List<String>

    suspend fun fetchNotices(
        page: Int = 1,
        pageSize: Int = 20,
    ): List<Notice>

    suspend fun fetchServers(): List<ServerNode>

    suspend fun fetchSubscribeYaml(url: String): okhttp3.ResponseBody?
}
