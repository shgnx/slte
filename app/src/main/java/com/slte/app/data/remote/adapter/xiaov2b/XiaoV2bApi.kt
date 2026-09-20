package com.slte.app.data.remote.adapter.xiaov2b

import com.slte.app.data.remote.api.ApiHeaders
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface XiaoV2bAuthRetrofit {
    @POST("passport/auth/login")
    suspend fun login(
        @Body request: XiaoV2bLoginRequest,
    ): XiaoV2bResponse<XiaoV2bLoginData>

    @POST("passport/auth/register")
    suspend fun register(
        @Body request: XiaoV2bRegisterRequest,
    ): XiaoV2bResponse<XiaoV2bLoginData>

    @GET("guest/comm/config")
    suspend fun fetchConfig(): XiaoV2bResponse<XiaoV2bSiteConfig>

    @POST("passport/auth/forget")
    suspend fun forgotPassword(
        @Body request: XiaoV2bForgotRequest,
    ): XiaoV2bResponse<Boolean>

    @POST("passport/comm/sendEmailVerify")
    suspend fun sendEmailCode(
        @Body request: XiaoV2bSendCodeRequest,
    ): XiaoV2bResponse<Boolean>
}

interface XiaoV2bUserPlanRetrofit {
    @GET("user/plan/fetch")
    suspend fun fetchPlans(): XiaoV2bResponse<List<XiaoV2bPlanData>>
}

interface XiaoV2bUserRetrofit {
    @GET("user/info")
    suspend fun fetchUserInfo(): XiaoV2bResponse<XiaoV2bUserInfoData>

    @GET("user/getSubscribe")
    suspend fun fetchSubscribe(): XiaoV2bResponse<XiaoV2bSubscribeData>

    @GET("user/order/fetch")
    suspend fun fetchOrders(): XiaoV2bResponse<List<XiaoV2bOrderData>>

    @POST("user/order/save")
    suspend fun createOrder(
        @Body request: XiaoV2bCreateOrderRequest,
    ): XiaoV2bResponse<String>

    @GET("user/order/detail")
    suspend fun getOrderDetail(
        @Query("trade_no") tradeNo: String,
    ): XiaoV2bResponse<XiaoV2bOrderData>

    @POST("user/coupon/check")
    suspend fun checkCoupon(
        @Body request: XiaoV2bCouponCheckRequest,
    ): XiaoV2bResponse<XiaoV2bCouponData>

    @POST("user/order/checkout")
    suspend fun checkoutOrder(
        @Body request: XiaoV2bCheckoutRequest,
    ): okhttp3.ResponseBody

    @GET("user/order/getPaymentMethod")
    suspend fun getPaymentMethods(): XiaoV2bResponse<List<XiaoV2bPaymentMethodData>>

    @POST("user/order/cancel")
    suspend fun cancelOrder(
        @Body request: XiaoV2bCancelOrderRequest,
    ): XiaoV2bResponse<Boolean>

    @GET("user/invite/fetch")
    suspend fun fetchInviteInfo(): XiaoV2bResponse<XiaoV2bInviteData>

    @GET("user/invite/save")
    suspend fun generateInviteCode(): XiaoV2bResponse<Boolean>

    @GET("user/invite/details")
    suspend fun fetchCommissionRecords(
        @Query("current") page: Int,
        @Query("page_size") pageSize: Int,
    ): XiaoV2bResponse<List<XiaoV2bCommissionRecordData>>

    @POST("user/transfer")
    suspend fun transferCommission(
        @Body request: XiaoV2bTransferRequest,
    ): XiaoV2bResponse<Boolean>

    @POST("user/ticket/withdraw")
    suspend fun withdrawCommission(
        @Body request: XiaoV2bWithdrawRequest,
    ): XiaoV2bResponse<Boolean>

    @GET("user/comm/config")
    suspend fun fetchUserCommConfig(): XiaoV2bResponse<XiaoV2bUserCommConfigData>

    @GET("user/notice/fetch")
    suspend fun fetchNotices(
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): XiaoV2bResponse<List<XiaoV2bNoticeData>>

    @GET("user/server/fetch")
    suspend fun fetchServers(): XiaoV2bResponse<List<XiaoV2bServerData>>

    @GET("user/getActiveSession")
    suspend fun getActiveSessions(
        @Header("Authorization") authData: String?,
    ): XiaoV2bResponse<Map<String, XiaoV2bActiveSessionData>>

    @POST("user/removeActiveSession")
    suspend fun removeActiveSession(
        @Header("Authorization") authData: String?,
        @Body request: XiaoV2bRemoveSessionRequest,
    ): XiaoV2bResponse<Boolean>

    @GET
    @Headers(
        ApiHeaders.USER_AGENT_HEADER,
        ApiHeaders.NO_FAILOVER_HEADER,
    )
    suspend fun fetchSubscribeYaml(
        @Url url: String,
    ): ResponseBody

    @POST("user/update")
    suspend fun updateUserSettings(
        @Body request: XiaoV2bUpdateUserRequest,
    ): XiaoV2bResponse<Boolean>

    @POST("user/changePassword")
    suspend fun changePassword(
        @Body request: XiaoV2bChangePasswordRequest,
    ): XiaoV2bResponse<Boolean>

    @POST("user/redeemgiftcard")
    suspend fun redeemGiftCard(
        @Body request: XiaoV2bGiftCardRedeemRequest,
    ): XiaoV2bResponse<Boolean>
}
