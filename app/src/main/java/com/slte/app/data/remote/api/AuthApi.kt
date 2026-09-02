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

/** 后端适配器契约，Repository 只依赖此接口 */
interface AuthApi {

    suspend fun login(email: String, password: String): LoginResponseDto

    suspend fun register(
        email: String,
        password: String,
        emailCode: String? = null,
        inviteCode: String? = null
    ): LoginResponseDto

    suspend fun fetchRegisterConfig(): RegisterConfig

    suspend fun forgotPassword(email: String, emailCode: String, password: String)

    suspend fun sendEmailCode(email: String, purpose: EmailCodePurpose = EmailCodePurpose.FORGOT_PASSWORD)

    /** 吊销当前登录会话（登出时调用，尽力而为） */
    suspend fun revokeActiveSessions(authData: String)

    suspend fun fetchUserInfo(): UserInfoDto

    suspend fun fetchSubscribeInfo(): SubscribeInfoDto

    /** 更新到期邮件提醒开关（服务端下发邮件） */
    suspend fun updateRemindExpire(enabled: Boolean)

    /** 更新流量邮件提醒开关（服务端下发邮件） */
    suspend fun updateRemindTraffic(enabled: Boolean)

    /** 修改密码（成功后服务端清除所有活跃会话） */
    suspend fun changePassword(oldPassword: String, newPassword: String)

    suspend fun fetchOrders(): List<OrderInfoDto>


    suspend fun fetchPlans(): List<PlanInfoDto>

    suspend fun createOrder(planId: Int, period: String, couponCode: String? = null): CreateOrderResultDto

    suspend fun getOrderDetail(tradeNo: String): OrderInfoDto

    /** 校验优惠券，返回折扣信息 */
    suspend fun checkCoupon(code: String, planId: Int? = null): CouponCheckResultDto

    suspend fun checkoutOrder(tradeNo: String, paymentMethod: Int): CheckoutResultDto

    suspend fun getPaymentMethods(): List<PaymentMethodDto>

    suspend fun cancelOrder(tradeNo: String)


    suspend fun fetchInviteInfo(): InviteInfo

    suspend fun generateInviteCode(): Boolean

    suspend fun fetchCommissionRecords(page: Int, pageSize: Int): List<CommissionRecord>

    suspend fun transferCommission(transferAmount: Int): Boolean

    suspend fun withdrawCommission(withdrawMethod: String, withdrawAccount: String): Boolean


    suspend fun fetchNotices(page: Int = 1, pageSize: Int = 20): List<Notice>

    /** 服务器节点列表（UI 展示用；连接配置由订阅 YAML 提供） */
    suspend fun fetchServers(): List<ServerNode>

    /** 下载订阅 Clash YAML（文本响应；非 2xx 抛 HttpException） */
    suspend fun fetchSubscribeYaml(token: String): okhttp3.ResponseBody?
}
