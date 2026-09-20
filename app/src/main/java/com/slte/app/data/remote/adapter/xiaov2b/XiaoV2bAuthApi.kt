package com.slte.app.data.remote.adapter.xiaov2b

import com.slte.app.BuildConfig
import com.slte.app.data.remote.ApiException
import com.slte.app.data.remote.adapter.AdapterExecute
import com.slte.app.data.remote.adapter.orEmptyLogged
import com.slte.app.data.remote.adapter.orFalseLogged
import com.slte.app.data.remote.adapter.orNullLogged
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
import kotlinx.coroutines.CancellationException

class XiaoV2bAuthApi(
    private val authApi: XiaoV2bAuthRetrofit,
    private val userApi: XiaoV2bUserRetrofit,
    private val userPlanApi: XiaoV2bUserPlanRetrofit,
) : AuthApi {
    override suspend fun login(
        email: String,
        password: String,
    ): LoginResponseDto {
        val response = AdapterExecute.typed { authApi.login(XiaoV2bLoginRequest(email, password)) }
        val data = response.data ?: throw ApiException("服务器返回数据为空", ApiErrors.EMPTY_DATA)
        AppLog.i("SLTE-Api", "login success")
        return data.toDomainLoginResponse()
    }

    override suspend fun register(
        email: String,
        password: String,
        emailCode: String?,
        inviteCode: String?,
    ): LoginResponseDto {
        val response =
            AdapterExecute.typed {
                authApi.register(
                    XiaoV2bRegisterRequest(
                        email = email,
                        password = password,
                        email_code = emailCode?.takeIf { it.isNotBlank() },
                        invite_code = inviteCode?.takeIf { it.isNotBlank() },
                    ),
                )
            }
        val data = response.data ?: throw ApiException("服务器返回数据为空", ApiErrors.EMPTY_DATA)
        AppLog.i("SLTE-Api", "register success")
        return data.toDomainLoginResponse()
    }

    override suspend fun fetchRegisterConfig(): RegisterConfig {
        val response = AdapterExecute.typed { authApi.fetchConfig() }
        val data = response.data ?: throw ApiException("获取注册配置失败", ApiErrors.REGISTER_CONFIG)
        return RegisterConfig(
            emailVerifyEnabled = data.is_email_verify == 1,
            inviteForceEnabled = data.is_invite_force == 1,
        )
    }

    override suspend fun forgotPassword(
        email: String,
        emailCode: String,
        password: String,
    ) {
        AdapterExecute.typed {
            authApi.forgotPassword(XiaoV2bForgotRequest(email, password, emailCode))
        }
        AppLog.i("SLTE-Api", "forgotPassword success")
    }

    override suspend fun sendEmailCode(
        email: String,
        purpose: EmailCodePurpose,
    ) {
        val isForget =
            when (purpose) {
                EmailCodePurpose.REGISTER -> 0
                EmailCodePurpose.FORGOT_PASSWORD -> 1
            }
        AdapterExecute.typed {
            authApi.sendEmailCode(XiaoV2bSendCodeRequest(email, isforget = isForget))
        }
        AppLog.i("SLTE-Api", "sendEmailCode success purpose=$purpose")
    }

    override suspend fun revokeActiveSessions(authData: String) {
        val sessions = AdapterExecute.typed { userApi.getActiveSessions(authData) }.data ?: return

        sessions.keys.forEach { sessionId ->
            try {
                AdapterExecute.typed {
                    userApi.removeActiveSession(authData, XiaoV2bRemoveSessionRequest(sessionId))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("SLTE-Api", "revokeActiveSessions: 会话吊销失败，继续吊销其余会话")
            }
        }
    }

    override suspend fun fetchUserInfo(): UserInfoDto {
        val response = AdapterExecute.typed { userApi.fetchUserInfo() }
        val data = response.data ?: throw ApiException("获取用户信息失败", ApiErrors.USER_INFO)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchUserInfo: planId=${data.planId}, expiredAt=${data.expiredAt}, transferEnable=${data.transferEnable}")
        }
        return data.toDomainUserInfo()
    }

    override suspend fun updateRemindExpire(enabled: Boolean) {
        AdapterExecute.typed {
            userApi.updateUserSettings(XiaoV2bUpdateUserRequest(remindExpire = if (enabled) 1 else 0))
        }
    }

    override suspend fun updateRemindTraffic(enabled: Boolean) {
        AdapterExecute.typed {
            userApi.updateUserSettings(XiaoV2bUpdateUserRequest(remindTraffic = if (enabled) 1 else 0))
        }
    }

    override suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
    ) {
        AdapterExecute.typed {
            userApi.changePassword(XiaoV2bChangePasswordRequest(oldPassword, newPassword))
        }
    }

    override suspend fun fetchSubscribeInfo(): SubscribeInfoDto {
        val response = AdapterExecute.typed { userApi.fetchSubscribe() }
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
        val response = AdapterExecute.typed { userApi.fetchOrders() }
        val data = response.data.orEmptyLogged("fetchOrders")
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchOrders: 共 ${data.size} 条订单")
        }
        return data.map { it.toDomainOrder() }
    }

    override suspend fun fetchPlans(): List<PlanInfoDto> {
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchPlans: 请求 /user/plan/fetch")
        }

        val response = AdapterExecute.typed { userPlanApi.fetchPlans() }
        val data = response.data.orEmptyLogged("fetchPlans")
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchPlans: 返回 ${data.size} 条套餐")
        }
        return data.map { it.toDomainPlan() }
    }

    override suspend fun createOrder(
        planId: Int,
        period: String,
        couponCode: String?,
    ): CreateOrderResultDto {
        val response =
            AdapterExecute.typed {
                userApi.createOrder(XiaoV2bCreateOrderRequest(planId, period, couponCode))
            }
        val tradeNo = response.data ?: throw ApiException("创建订单失败", ApiErrors.CREATE_ORDER)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "createOrder success: tradeNo=$tradeNo")
        }
        return CreateOrderResultDto(tradeNo)
    }

    override suspend fun getOrderDetail(tradeNo: String): OrderInfoDto {
        val response = AdapterExecute.typed { userApi.getOrderDetail(tradeNo) }
        val data = response.data ?: throw ApiException("获取订单详情失败", ApiErrors.ORDER_DETAIL)
        return data.toDomainOrder()
    }

    override suspend fun checkCoupon(
        code: String,
        planId: Int?,
    ): CouponCheckResultDto {
        val response = AdapterExecute.typed { userApi.checkCoupon(XiaoV2bCouponCheckRequest(code, planId)) }
        val data = response.data ?: throw ApiException("优惠券无效", ApiErrors.COUPON_INVALID)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "checkCoupon raw: type=${data.type} value=${data.value} name=${data.name}")
        }
        return data.toDomainCouponCheck()
    }

    override suspend fun checkoutOrder(
        tradeNo: String,
        paymentMethod: Int,
    ): CheckoutResultDto {
        val body =
            AdapterExecute.raw {
                userApi.checkoutOrder(XiaoV2bCheckoutRequest(tradeNo, paymentMethod))
            }
        return body.use { CheckoutResultDto.fromRawJson(it.string()) }
            ?: throw ApiException("结算响应无法解析", ApiErrors.CHECKOUT)
    }

    override suspend fun getPaymentMethods(): List<PaymentMethodDto> {
        val response = AdapterExecute.typed { userApi.getPaymentMethods() }
        return response.data.orEmptyLogged("getPaymentMethods").map { it.toDomainPaymentMethod() }
    }

    override suspend fun cancelOrder(tradeNo: String) {
        AdapterExecute.typed { userApi.cancelOrder(XiaoV2bCancelOrderRequest(tradeNo)) }
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "cancelOrder: tradeNo=$tradeNo")
        }
    }

    override suspend fun redeemGiftCard(code: String) {
        val response =
            AdapterExecute.typed {
                userApi.redeemGiftCard(XiaoV2bGiftCardRedeemRequest(code))
            }
        if (response.data != true) throw ApiException(response.message ?: "兑换失败", ApiErrors.GIFT_CARD)
        AppLog.i("SLTE-Api", "redeemGiftCard success")
    }

    override suspend fun fetchInviteInfo(): InviteInfo {
        val response = AdapterExecute.typed { userApi.fetchInviteInfo() }
        val data = response.data ?: throw ApiException("获取邀请信息失败", ApiErrors.INVITE_INFO)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchInviteInfo: codes=${data.codes.size}, stat=${data.stat}")
        }
        return data.toDomain()
    }

    override suspend fun generateInviteCode(): Boolean {
        val response = AdapterExecute.typed { userApi.generateInviteCode() }
        return response.data.orFalseLogged("generateInviteCode")
    }

    override suspend fun fetchCommissionRecords(
        page: Int,
        pageSize: Int,
    ): List<CommissionRecord> {
        val response = AdapterExecute.typed { userApi.fetchCommissionRecords(page, pageSize) }
        val data = response.data.orEmptyLogged("fetchCommissionRecords")
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchCommissionRecords: ${data.size} 条记录")
        }
        return data.map { it.toDomain() }
    }

    override suspend fun transferCommission(transferAmount: Int): Boolean {
        val response =
            AdapterExecute.typed {
                userApi.transferCommission(XiaoV2bTransferRequest(transferAmount))
            }
        return response.data.orFalseLogged("transferCommission")
    }

    override suspend fun withdrawCommission(
        withdrawMethod: String,
        withdrawAccount: String,
    ): Boolean {
        val response =
            AdapterExecute.typed {
                userApi.withdrawCommission(XiaoV2bWithdrawRequest(withdrawMethod, withdrawAccount))
            }
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "withdrawCommission: method=$withdrawMethod")
        }
        return response.data.orFalseLogged("withdrawCommission")
    }

    override suspend fun fetchWithdrawMethods(): List<String> {
        val response = AdapterExecute.typed { userApi.fetchUserCommConfig() }
        val data = response.data.orNullLogged("fetchWithdrawMethods") ?: return emptyList()
        if (data.withdrawClose == 1) return emptyList()
        return data.withdrawMethods.orEmpty()
    }

    override suspend fun fetchNotices(
        page: Int,
        pageSize: Int,
    ): List<Notice> {
        val response = AdapterExecute.typed { userApi.fetchNotices(page, pageSize) }
        return response.data.orEmptyLogged("fetchNotices").map { it.toDomain() }
    }

    override suspend fun fetchServers(): List<ServerNode> {
        val response = AdapterExecute.typed { userApi.fetchServers() }
        return response.data.orEmptyLogged("fetchServers").map { it.toServerNode() }
    }

    override suspend fun fetchSubscribeYaml(url: String): okhttp3.ResponseBody? = userApi.fetchSubscribeYaml(url)
}
