package com.slte.app.data.remote.adapter.xboard

import com.slte.app.BuildConfig
import com.slte.app.data.remote.ApiException
import com.slte.app.data.remote.api.AuthApi
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
import com.slte.app.utils.ApiErrors
import com.slte.app.utils.AppLog
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Xboard 认证适配器：将 Xboard API 原始响应转换为领域层统一响应。
 *
 * Xboard 与 V2Board 存在格式差异（布尔开关、会话列表数组），
 * 差异已隔离在本包独立 DTO 中，不依赖 xiaov2b 适配器。
 * 鉴权由 OkHttp 拦截器统一注入。
 */
class XboardAuthApi(
    private val authApi: XboardAuthRetrofit,
    private val userApi: XboardUserRetrofit,
    private val userPlanApi: XboardUserPlanRetrofit
) : AuthApi {

    override suspend fun login(email: String, password: String): LoginResponseDto {
        val response = executeXboard {
            authApi.login(XboardLoginRequest(email, password))
        }
        val data = response.data ?: throw ApiException("服务器返回数据为空", ApiErrors.EMPTY_DATA)
        AppLog.i("SLTE-Api", "login success")
        return data.toDomainLoginResponse()
    }

    override suspend fun register(
        email: String,
        password: String,
        emailCode: String?,
        inviteCode: String?
    ): LoginResponseDto {
        val response = executeXboard {
            authApi.register(
                XboardRegisterRequest(
                    email = email,
                    password = password,
                    email_code = emailCode?.takeIf { it.isNotBlank() },
                    invite_code = inviteCode?.takeIf { it.isNotBlank() }
                )
            )
        }
        val data = response.data ?: throw ApiException("服务器返回数据为空", ApiErrors.EMPTY_DATA)
        AppLog.i("SLTE-Api", "register success")
        return data.toDomainLoginResponse()
    }

    override suspend fun fetchRegisterConfig(): RegisterConfig {
        val response = executeXboard { authApi.fetchConfig() }
        val data = response.data ?: throw ApiException("获取注册配置失败", ApiErrors.REGISTER_CONFIG)
        return RegisterConfig(
            emailVerifyEnabled = data.is_email_verify == 1,
            inviteForceEnabled = data.is_invite_force == 1
        )
    }

    override suspend fun forgotPassword(email: String, emailCode: String, password: String) {
        executeXboard {
            authApi.forgotPassword(XboardForgotRequest(email, password, emailCode))
        }
        AppLog.i("SLTE-Api", "forgotPassword success")
    }

    override suspend fun sendEmailCode(email: String, purpose: EmailCodePurpose) {
        val isForget = when (purpose) {
            EmailCodePurpose.REGISTER -> 0
            EmailCodePurpose.FORGOT_PASSWORD -> 1
        }
        executeXboard {
            authApi.sendEmailCode(XboardSendCodeRequest(email, isforget = isForget))
        }
        AppLog.i("SLTE-Api", "sendEmailCode success purpose=$purpose")
    }

    override suspend fun revokeActiveSessions(authData: String) {
        // 显式传 Authorization 头：logout() 先清本地会话，拦截器此时已取不到 token
        val data = executeXboard { userApi.getActiveSessions(authData) }.data
            ?: return
        // 会话列表兼容两种响应：数组（元素含 id 字段）或对象（key 为会话 id），
        // 逐个吊销保证登出后服务端会话失效；解析为空时留痕，便于发现格式不兼容
        val sessionIds = when (data) {
            is JsonArray -> data.mapNotNull { item ->
                (item as? JsonObject)?.get("id")?.let { v -> (v as? JsonPrimitive)?.content }
            }
            is JsonObject -> data.keys.toList()
            else -> emptyList()
        }
        if (sessionIds.isEmpty()) {
            AppLog.w("SLTE-Api", "revokeActiveSessions: 会话列表解析为空，登出后服务端会话可能未吊销")
        }
        sessionIds.forEach { sessionId ->
            executeXboard {
                userApi.removeActiveSession(authData, XboardRemoveSessionRequest(sessionId))
            }
        }
    }

    override suspend fun fetchUserInfo(): UserInfoDto {
        val response = executeXboard { userApi.fetchUserInfo() }
        val data = response.data ?: throw ApiException("获取用户信息失败", ApiErrors.USER_INFO)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchUserInfo: planId=${data.planId}, expiredAt=${data.expiredAt}, transferEnable=${data.transferEnable}")
        }
        return data.toDomainUserInfo()
    }

    override suspend fun updateRemindExpire(enabled: Boolean) {
        executeXboard {
            userApi.updateUserSettings(XboardUpdateUserRequest(remindExpire = if (enabled) 1 else 0))
        }
    }

    override suspend fun updateRemindTraffic(enabled: Boolean) {
        executeXboard {
            userApi.updateUserSettings(XboardUpdateUserRequest(remindTraffic = if (enabled) 1 else 0))
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String) {
        executeXboard {
            userApi.changePassword(XboardChangePasswordRequest(oldPassword, newPassword))
        }
    }

    override suspend fun fetchSubscribeInfo(): SubscribeInfoDto {
        val response = executeXboard { userApi.fetchSubscribe() }
        val data = response.data
        if (data == null) {
            if (BuildConfig.DEBUG) {
                AppLog.d("SLTE-Api", "fetchSubscribeInfo: 无订阅，返回空订阅")
            }
            return SubscribeInfoDto()
        }
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchSubscribeInfo: planId=${data.planId}, planName=${data.plan?.name}, expiredAt=${data.expiredAt}")
        }
        return data.toDomainSubscribeInfo()
    }

    override suspend fun fetchOrders(): List<OrderInfoDto> {
        val response = executeXboard { userApi.fetchOrders() }
        val data = response.data ?: return emptyList()
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchOrders: 共 ${data.size} 条订单")
        }
        return data.map { it.toDomainOrder() }
    }


    override suspend fun fetchPlans(): List<PlanInfoDto> {
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchPlans: 请求 /user/plan/fetch")
        }
        val response = executeXboard { userPlanApi.fetchPlans() }
        val data = response.data
        if (data == null && response.message != null) {
            throw ApiException(response.message)
        }
        if (data == null) return emptyList()
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchPlans: 返回 ${data.size} 条套餐")
        }
        return data.map { it.toDomainPlan() }
    }

    override suspend fun createOrder(
        planId: Int,
        period: String,
        couponCode: String?
    ): CreateOrderResultDto {
        val response = executeXboard {
            userApi.createOrder(XboardCreateOrderRequest(planId, period, couponCode))
        }
        val tradeNo = response.data ?: throw ApiException("创建订单失败", ApiErrors.CREATE_ORDER)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "createOrder success: tradeNo=$tradeNo")
        }
        return CreateOrderResultDto(tradeNo)
    }

    override suspend fun getOrderDetail(tradeNo: String): OrderInfoDto {
        val response = executeXboard { userApi.getOrderDetail(tradeNo) }
        val data = response.data ?: throw ApiException("获取订单详情失败", ApiErrors.ORDER_DETAIL)
        return data.toDomainOrder()
    }

    override suspend fun checkCoupon(code: String, planId: Int?): CouponCheckResultDto {
        val response = executeXboard { userApi.checkCoupon(XboardCouponCheckRequest(code, planId)) }
        val data = response.data ?: throw ApiException("优惠券无效", ApiErrors.COUPON_INVALID)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "checkCoupon raw: type=${data.type} value=${data.value} name=${data.name}")
        }
        return data.toDomainCouponCheck()
    }

    override suspend fun checkoutOrder(
        tradeNo: String,
        paymentMethod: Int
    ): CheckoutResultDto {
        val response = executeXboard {
            userApi.checkoutOrder(XboardCheckoutRequest(tradeNo, paymentMethod))
        }
        val data = response.data ?: throw ApiException("结算失败", ApiErrors.CHECKOUT)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "checkoutOrder: type=${data.type}")
        }
        val redirectUrl = when (val d = data.data) {
            is JsonPrimitive -> d.content
            else -> null
        }
        return CheckoutResultDto(
            type = data.type,
            redirectUrl = redirectUrl,
            message = data.message
        )
    }

    override suspend fun getPaymentMethods(): List<PaymentMethodDto> {
        val response = executeXboard { userApi.getPaymentMethods() }
        val data = response.data ?: return emptyList()
        return data.map { it.toDomainPaymentMethod() }
    }

    override suspend fun cancelOrder(tradeNo: String) {
        executeXboard { userApi.cancelOrder(XboardCancelOrderRequest(tradeNo)) }
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "cancelOrder: tradeNo=$tradeNo")
        }
    }


    override suspend fun fetchInviteInfo(): InviteInfo {
        val response = executeXboard { userApi.fetchInviteInfo() }
        val data = response.data ?: throw ApiException("获取邀请信息失败", ApiErrors.INVITE_INFO)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchInviteInfo: codes=${data.codes.size}, stat=${data.stat}")
        }
        return data.toDomain()
    }

    override suspend fun generateInviteCode(): Boolean {
        val response = executeXboard { userApi.generateInviteCode() }
        return response.data ?: false
    }

    override suspend fun fetchCommissionRecords(page: Int, pageSize: Int): List<CommissionRecord> {
        val response = executeXboard { userApi.fetchCommissionRecords(page, pageSize) }
        val data = response.data ?: return emptyList()
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchCommissionRecords: ${data.size} 条记录")
        }
        return data.map { it.toDomain() }
    }

    override suspend fun transferCommission(transferAmount: Int): Boolean {
        val response = executeXboard {
            userApi.transferCommission(XboardTransferRequest(transferAmount))
        }
        return response.data ?: false
    }

    override suspend fun withdrawCommission(withdrawMethod: String, withdrawAccount: String): Boolean {
        val response = executeXboard {
            userApi.withdrawCommission(XboardWithdrawRequest(withdrawMethod, withdrawAccount))
        }
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "withdrawCommission: method=$withdrawMethod")
        }
        return response.data ?: false
    }


    override suspend fun fetchNotices(page: Int, pageSize: Int): List<Notice> {
        val response = executeXboard { userApi.fetchNotices(page, pageSize) }
        val data = response.data ?: return emptyList()
        return data.map { it.toDomain() }
    }


    override suspend fun fetchServers(): List<ServerNode> {
        val response = executeXboard { userApi.fetchServers() }
        val data = response.data ?: return emptyList()
        return data.map { it.toServerNode() }
    }

    override suspend fun fetchSubscribeYaml(token: String): okhttp3.ResponseBody? =
        userApi.fetchSubscribeYaml(token)
}
