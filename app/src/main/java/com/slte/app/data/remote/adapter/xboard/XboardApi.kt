package com.slte.app.data.remote.adapter.xboard

import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Xboard Retrofit 接口（用户前端 V1 API）。
 * 独立 DTO（Xboard 响应与 V2Board 存在布尔/数组格式差异，见 XboardDto）。
 */
interface XboardAuthRetrofit {

    @POST("passport/auth/login")
    suspend fun login(@Body request: XboardLoginRequest): XboardResponse<XboardLoginData>

    @POST("passport/auth/register")
    suspend fun register(@Body request: XboardRegisterRequest): XboardResponse<XboardLoginData>

    @GET("guest/comm/config")
    suspend fun fetchConfig(): XboardResponse<XboardSiteConfig>

    @POST("passport/auth/forget")
    suspend fun forgotPassword(@Body request: XboardForgotRequest): XboardResponse<Boolean>

    @POST("passport/comm/sendEmailVerify")
    suspend fun sendEmailCode(@Body request: XboardSendCodeRequest): XboardResponse<Boolean>
}

interface XboardUserPlanRetrofit {

    @GET("user/plan/fetch")
    suspend fun fetchPlans(): XboardResponse<List<XboardPlanData>>
}

interface XboardUserRetrofit {

    @GET("user/info")
    suspend fun fetchUserInfo(): XboardResponse<XboardUserInfoData>

    @GET("user/getSubscribe")
    suspend fun fetchSubscribe(): XboardResponse<XboardSubscribeData>

    @GET("user/order/fetch")
    suspend fun fetchOrders(): XboardResponse<List<XboardOrderData>>


    @POST("user/order/save")
    suspend fun createOrder(
        @Body request: XboardCreateOrderRequest
    ): XboardResponse<String>

    @GET("user/order/detail")
    suspend fun getOrderDetail(
        @Query("trade_no") tradeNo: String
    ): XboardResponse<XboardOrderDetailData>

    @POST("user/coupon/check")
    suspend fun checkCoupon(
        @Body request: XboardCouponCheckRequest
    ): XboardResponse<XboardCouponData>

    @POST("user/order/checkout")
    suspend fun checkoutOrder(
        @Body request: XboardCheckoutRequest
    ): XboardResponse<XboardCheckoutData>

    @GET("user/order/getPaymentMethod")
    suspend fun getPaymentMethods(): XboardResponse<List<XboardPaymentMethodData>>

    @POST("user/order/cancel")
    suspend fun cancelOrder(
        @Body request: XboardCancelOrderRequest
    ): XboardResponse<Boolean>


    @GET("user/invite/fetch")
    suspend fun fetchInviteInfo(): XboardResponse<XboardInviteData>

    // 后端路由仅注册 GET（实测 POST 返回 405）
    @GET("user/invite/save")
    suspend fun generateInviteCode(): XboardResponse<Boolean>

    @GET("user/invite/details")
    suspend fun fetchCommissionRecords(
        @Query("current") page: Int,
        @Query("page_size") pageSize: Int
    ): XboardResponse<List<XboardCommissionRecordData>>

    @POST("user/transfer")
    suspend fun transferCommission(
        @Body request: XboardTransferRequest
    ): XboardResponse<Boolean>

    @POST("user/ticket/withdraw")
    suspend fun withdrawCommission(
        @Body request: XboardWithdrawRequest
    ): XboardResponse<Boolean>


    @GET("user/notice/fetch")
    suspend fun fetchNotices(
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): XboardResponse<List<XboardNoticeData>>

    @GET("user/server/fetch")
    suspend fun fetchServers(): XboardResponse<List<XboardServerData>>

    // 会话管理（登出吊销）：Xboard 返回数组（元素含 id 字段），用 JsonElement 容错解析

    @GET("user/getActiveSession")
    suspend fun getActiveSessions(
        @Header("Authorization") authData: String?
    ): XboardResponse<kotlinx.serialization.json.JsonElement?>

    @POST("user/removeActiveSession")
    suspend fun removeActiveSession(
        @Header("Authorization") authData: String?,
        @Body request: XboardRemoveSessionRequest
    ): XboardResponse<Boolean>

    /** 订阅 Clash YAML（文本响应），与用户 API 共用同一 OkHttp 客户端 */
    @GET("client/subscribe")
    @Headers("User-Agent: ClashMetaForAndroid/2.11.32")
    suspend fun fetchSubscribeYaml(@Query("token") token: String): ResponseBody

    /** 更新用户设置（到期/流量邮件提醒开关） */
    @POST("user/update")
    suspend fun updateUserSettings(
        @Body request: XboardUpdateUserRequest
    ): XboardResponse<Boolean>

    /** 修改密码（成功后服务端清除所有活跃会话，需重新登录） */
    @POST("user/changePassword")
    suspend fun changePassword(
        @Body request: XboardChangePasswordRequest
    ): XboardResponse<Boolean>
}
