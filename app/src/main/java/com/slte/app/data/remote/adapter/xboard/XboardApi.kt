package com.slte.app.data.remote.adapter.xboard

import com.slte.app.data.remote.api.ApiHeaders
import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface XboardAuthRetrofit {
    @POST("passport/auth/login")
    suspend fun login(
        @Body request: XboardLoginRequest,
    ): XboardResponse<XboardLoginData>

    @POST("passport/auth/register")
    suspend fun register(
        @Body request: XboardRegisterRequest,
    ): XboardResponse<XboardLoginData>

    @GET("guest/comm/config")
    suspend fun fetchConfig(): XboardResponse<XboardSiteConfig>

    @POST("passport/auth/forget")
    suspend fun forgotPassword(
        @Body request: XboardForgotRequest,
    ): XboardResponse<Boolean>

    @POST("passport/comm/sendEmailVerify")
    suspend fun sendEmailCode(
        @Body request: XboardSendCodeRequest,
    ): XboardResponse<Boolean>
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
        @Body request: XboardCreateOrderRequest,
    ): XboardResponse<String>

    @GET("user/order/detail")
    suspend fun getOrderDetail(
        @Query("trade_no") tradeNo: String,
    ): XboardResponse<XboardOrderData>

    @POST("user/coupon/check")
    suspend fun checkCoupon(
        @Body request: XboardCouponCheckRequest,
    ): XboardResponse<XboardCouponData>

    @POST("user/order/checkout")
    suspend fun checkoutOrder(
        @Body request: XboardCheckoutRequest,
    ): okhttp3.ResponseBody

    @GET("user/order/getPaymentMethod")
    suspend fun getPaymentMethods(): XboardResponse<List<XboardPaymentMethodData>>

    @POST("user/order/cancel")
    suspend fun cancelOrder(
        @Body request: XboardCancelOrderRequest,
    ): XboardResponse<Boolean>

    @GET("user/invite/fetch")
    suspend fun fetchInviteInfo(): XboardResponse<XboardInviteData>

    @GET("user/invite/save")
    suspend fun generateInviteCode(): XboardResponse<Boolean>

    @GET("user/invite/details")
    suspend fun fetchCommissionRecords(
        @Query("current") page: Int,
        @Query("page_size") pageSize: Int,
    ): XboardResponse<List<XboardCommissionRecordData>>

    @POST("user/transfer")
    suspend fun transferCommission(
        @Body request: XboardTransferRequest,
    ): XboardResponse<Boolean>

    @POST("user/ticket/withdraw")
    suspend fun withdrawCommission(
        @Body request: XboardWithdrawRequest,
    ): XboardResponse<Boolean>

    @GET("user/comm/config")
    suspend fun fetchUserCommConfig(): XboardResponse<XboardUserCommConfigData>

    @GET("user/notice/fetch")
    suspend fun fetchNotices(
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): XboardResponse<List<XboardNoticeData>>

    @GET("user/server/fetch")
    suspend fun fetchServers(): XboardResponse<List<XboardServerData>>

    @GET("user/getActiveSession")
    suspend fun getActiveSessions(
        @Header("Authorization") authData: String?,
    ): XboardResponse<kotlinx.serialization.json.JsonElement?>

    @POST("user/removeActiveSession")
    suspend fun removeActiveSession(
        @Header("Authorization") authData: String?,
        @Body request: XboardRemoveSessionRequest,
    ): XboardResponse<Boolean>

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
        @Body request: XboardUpdateUserRequest,
    ): XboardResponse<Boolean>

    @POST("user/changePassword")
    suspend fun changePassword(
        @Body request: XboardChangePasswordRequest,
    ): XboardResponse<Boolean>

    @POST("user/gift-card/redeem")
    suspend fun redeemGiftCard(
        @Body request: XboardGiftCardRedeemRequest,
    ): XboardResponse<JsonElement>
}
