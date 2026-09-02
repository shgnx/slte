package com.slte.app.data.remote.adapter.xiaov2b

import com.slte.app.BuildConfig
import com.slte.app.data.remote.ApiException
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.data.remote.api.dto.CheckoutResultDto
import com.slte.app.data.remote.api.dto.CouponCheckResultDto
import com.slte.app.data.remote.api.dto.LoginResponseDto
import com.slte.app.domain.model.CommissionRecord
import com.slte.app.data.remote.api.dto.CreateOrderResultDto
import com.slte.app.domain.model.EmailCodePurpose
import com.slte.app.domain.model.InviteInfo
import com.slte.app.domain.model.Notice
import com.slte.app.data.remote.api.dto.OrderInfoDto
import com.slte.app.data.remote.api.dto.PaymentMethodDto
import com.slte.app.data.remote.api.dto.PlanInfoDto
import com.slte.app.domain.model.RegisterConfig
import com.slte.app.domain.model.ServerNode
import com.slte.app.data.remote.api.dto.SubscribeInfoDto
import com.slte.app.data.remote.api.dto.UserInfoDto
import com.slte.app.utils.ApiErrors
import com.slte.app.utils.sanitizeLog
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException
import java.io.IOException
import com.slte.app.utils.AppLog

/**
 * XiaoV2b 认证适配器：将 V2Board API 原始响应转换为领域层统一响应。
 *
 * 鉴权由 OkHttp 拦截器统一注入。
 */
class XiaoV2bAuthApi(
    private val authApi: XiaoV2bAuthRetrofit,
    private val userApi: XiaoV2bUserRetrofit,
    private val userPlanApi: XiaoV2bUserPlanRetrofit
) : AuthApi {

    override suspend fun login(email: String, password: String): LoginResponseDto {
        val response = execute {
            authApi.login(XiaoV2bLoginRequest(email, password))
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
        val response = execute {
            authApi.register(
                XiaoV2bRegisterRequest(
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
        val response = execute { authApi.fetchConfig() }
        val data = response.data ?: throw ApiException("获取注册配置失败", ApiErrors.REGISTER_CONFIG)
        return RegisterConfig(
            emailVerifyEnabled = data.is_email_verify == 1,
            inviteForceEnabled = data.is_invite_force == 1
        )
    }

    override suspend fun forgotPassword(email: String, emailCode: String, password: String) {
        execute {
            authApi.forgotPassword(XiaoV2bForgotRequest(email, password, emailCode))
        }
        AppLog.i("SLTE-Api", "forgotPassword success")
    }

    override suspend fun sendEmailCode(email: String, purpose: EmailCodePurpose) {
        val isForget = when (purpose) {
            EmailCodePurpose.REGISTER -> 0
            EmailCodePurpose.FORGOT_PASSWORD -> 1
        }
        execute {
            authApi.sendEmailCode(XiaoV2bSendCodeRequest(email, isforget = isForget))
        }
        AppLog.i("SLTE-Api", "sendEmailCode success purpose=$purpose")
    }

    override suspend fun revokeActiveSessions(authData: String) {
        // 显式传 Authorization 头：logout() 先清本地会话，拦截器此时已取不到 token
        val sessions = execute { userApi.getActiveSessions(authData) }.data ?: return
        // 会话条目无法按 authData 可靠匹配当前会话，遍历全部会话逐个吊销，保证登出后服务端 JWT 失效
        sessions.keys.forEach { sessionId ->
            execute {
                userApi.removeActiveSession(authData, XiaoV2bRemoveSessionRequest(sessionId))
            }
        }
    }

    override suspend fun fetchUserInfo(): UserInfoDto {
        val response = execute { userApi.fetchUserInfo() }
        val data = response.data ?: throw ApiException("获取用户信息失败", ApiErrors.USER_INFO)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchUserInfo: planId=${data.planId}, expiredAt=${data.expiredAt}, transferEnable=${data.transferEnable}")
        }
        return data.toDomainUserInfo()
    }

    override suspend fun updateRemindExpire(enabled: Boolean) {
        execute {
            userApi.updateUserSettings(XiaoV2bUpdateUserRequest(remindExpire = if (enabled) 1 else 0))
        }
    }

    override suspend fun updateRemindTraffic(enabled: Boolean) {
        execute {
            userApi.updateUserSettings(XiaoV2bUpdateUserRequest(remindTraffic = if (enabled) 1 else 0))
        }
    }

    override suspend fun changePassword(oldPassword: String, newPassword: String) {
        execute {
            userApi.changePassword(XiaoV2bChangePasswordRequest(oldPassword, newPassword))
        }
    }

    override suspend fun fetchSubscribeInfo(): SubscribeInfoDto {
        val response = execute { userApi.fetchSubscribe() }
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
        val response = execute { userApi.fetchOrders() }
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
        val response = execute { userPlanApi.fetchPlans() }
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
        val response = execute {
            userApi.createOrder(XiaoV2bCreateOrderRequest(planId, period, couponCode))
        }
        val tradeNo = response.data ?: throw ApiException("创建订单失败", ApiErrors.CREATE_ORDER)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "createOrder success: tradeNo=$tradeNo")
        }
        return CreateOrderResultDto(tradeNo)
    }

    override suspend fun getOrderDetail(tradeNo: String): OrderInfoDto {
        val response = execute { userApi.getOrderDetail(tradeNo) }
        val data = response.data ?: throw ApiException("获取订单详情失败", ApiErrors.ORDER_DETAIL)
        return data.toDomainOrder()
    }

    override suspend fun checkCoupon(code: String, planId: Int?): CouponCheckResultDto {
        val response = execute { userApi.checkCoupon(XiaoV2bCouponCheckRequest(code, planId)) }
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
        val response = execute {
            userApi.checkoutOrder(XiaoV2bCheckoutRequest(tradeNo, paymentMethod))
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
        val response = execute { userApi.getPaymentMethods() }
        val data = response.data ?: return emptyList()
        return data.map { it.toDomainPaymentMethod() }
    }

    override suspend fun cancelOrder(tradeNo: String) {
        execute { userApi.cancelOrder(XiaoV2bCancelOrderRequest(tradeNo)) }
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "cancelOrder: tradeNo=$tradeNo")
        }
    }


    override suspend fun fetchInviteInfo(): InviteInfo {
        val response = execute { userApi.fetchInviteInfo() }
        val data = response.data ?: throw ApiException("获取邀请信息失败", ApiErrors.INVITE_INFO)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchInviteInfo: codes=${data.codes.size}, stat=${data.stat}")
        }
        return data.toDomain()
    }

    override suspend fun generateInviteCode(): Boolean {
        val response = execute { userApi.generateInviteCode() }
        return response.data ?: false
    }

    override suspend fun fetchCommissionRecords(page: Int, pageSize: Int): List<CommissionRecord> {
        val response = execute { userApi.fetchCommissionRecords(page, pageSize) }
        val data = response.data ?: return emptyList()
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchCommissionRecords: ${data.size} 条记录")
        }
        return data.map { it.toDomain() }
    }

    override suspend fun transferCommission(transferAmount: Int): Boolean {
        val response = execute {
            userApi.transferCommission(XiaoV2bTransferRequest(transferAmount))
        }
        return response.data ?: false
    }

    override suspend fun withdrawCommission(withdrawMethod: String, withdrawAccount: String): Boolean {
        val response = execute {
            userApi.withdrawCommission(XiaoV2bWithdrawRequest(withdrawMethod, withdrawAccount))
        }
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "withdrawCommission: method=$withdrawMethod")
        }
        return response.data ?: false
    }


    override suspend fun fetchNotices(page: Int, pageSize: Int): List<Notice> {
        val response = execute { userApi.fetchNotices(page, pageSize) }
        val data = response.data ?: return emptyList()
        return data.map { it.toDomain() }
    }


    override suspend fun fetchServers(): List<ServerNode> {
        val response = execute { userApi.fetchServers() }
        val data = response.data ?: return emptyList()
        return data.map { it.toServerNode() }
    }

    override suspend fun fetchSubscribeYaml(token: String): okhttp3.ResponseBody? =
        userApi.fetchSubscribeYaml(token)

    private suspend fun <T> execute(block: suspend () -> XiaoV2bResponse<T>): XiaoV2bResponse<T> {
        return try {
            val response = block()
            if (response.data == null && response.message != null) {
                if (BuildConfig.DEBUG) {
                    AppLog.w("SLTE-Api", "execute: data=null, message=${sanitizeLog(response.message)}")
                }
                throw ApiException(response.message)
            }
            response
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            // 网络/服务端错误无条件留痕（脱敏），release 也需可排查；DEBUG 时附带截断脱敏后的响应体
            if (BuildConfig.DEBUG) {
                AppLog.e("SLTE-Api", "execute HttpException: code=${e.code()}, body=${AppLog.sanitize(errorBody?.take(500) ?: "")}")
            } else {
                AppLog.w("SLTE-Api", "execute HttpException: code=${e.code()}")
            }
            val errorMessage = try {
                errorBody?.let {
                    Json.parseToJsonElement(it)
                        .jsonObject["message"]?.let { m -> (m as? JsonPrimitive)?.content }
                }
            } catch (_: Exception) { null }
            throw ApiException(errorMessage ?: "请求失败，请检查网络连接", ApiErrors.NETWORK)
        } catch (e: IOException) {
            // 网络异常无条件留痕（脱敏）：IOException 消息含完整请求 URL，需 sanitize
            AppLog.w("SLTE-Api", "execute IOException: ${sanitizeLog(e.message ?: "Unknown")}")
            throw ApiException("请求失败，请检查网络连接", ApiErrors.NETWORK)
        } catch (e: Exception) {
            AppLog.w("SLTE-Api", "execute unexpected ${e.javaClass.simpleName}: ${sanitizeLog(e.message ?: "Unknown")}")
            throw ApiException("服务器响应异常", ApiErrors.NETWORK)
        }
    }
}
