package com.slte.app.data.remote.adapter

import com.slte.app.data.remote.ApiException
import com.slte.app.data.remote.adapter.xboard.XboardAuthApi
import com.slte.app.data.remote.adapter.xboard.XboardAuthRetrofit
import com.slte.app.data.remote.adapter.xboard.XboardLoginData
import com.slte.app.data.remote.adapter.xboard.XboardOrderData
import com.slte.app.data.remote.adapter.xboard.XboardResponse
import com.slte.app.data.remote.adapter.xboard.XboardSubscribeData
import com.slte.app.data.remote.adapter.xboard.XboardUserInfoData
import com.slte.app.data.remote.adapter.xboard.XboardUserPlanRetrofit
import com.slte.app.data.remote.adapter.xboard.XboardUserRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bAuthApi
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bAuthRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bLoginData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bOrderData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bResponse
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bSubscribeData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bUserInfoData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bUserPlanRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bUserRetrofit
import com.slte.app.data.remote.api.AuthApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

interface BackendContract {
    val name: String

    fun authApi(): AuthApi

    fun stubLoginSuccess()

    fun stubLoginEmptyData()

    fun stubUserInfo()

    fun stubSubscribeInfo()

    fun stubOrderDetail()
}

private const val TOKEN = "t-1"
private const val AUTH_DATA = "ad-1"
private const val EMAIL = "a@b.c"
private const val EXPIRED_AT = 1_800_000_000L
private const val TRANSFER = 400L
private const val TRADE_NO = "TN-1"
private const val TOTAL_AMOUNT = 5_000
private const val SUBSCRIBE_URL = "https://app.example.com/api/v1/client/subscribe?token=x"

private class XboardContract : BackendContract {
    override val name = "xboard"
    private val auth = mockk<XboardAuthRetrofit>()
    private val user = mockk<XboardUserRetrofit>()
    private val plan = mockk<XboardUserPlanRetrofit>()

    override fun authApi(): AuthApi = XboardAuthApi(auth, user, plan)

    override fun stubLoginSuccess() {
        coEvery { auth.login(any()) } returns XboardResponse(data = XboardLoginData(token = TOKEN, auth_data = AUTH_DATA))
    }

    override fun stubLoginEmptyData() {
        coEvery { auth.login(any()) } returns XboardResponse(data = null)
    }

    override fun stubUserInfo() {
        coEvery { user.fetchUserInfo() } returns
            XboardResponse(
                data =
                XboardUserInfoData(
                    email = EMAIL,
                    balance = 1200,
                    planId = 7,
                    expiredAt = EXPIRED_AT,
                    transferEnable = TRANSFER,
                    remindExpire = true,
                    remindTraffic = false,
                ),
            )
    }

    override fun stubSubscribeInfo() {
        coEvery { user.fetchSubscribe() } returns
            XboardResponse(
                data =
                XboardSubscribeData(
                    planId = 7,
                    expiredAt = EXPIRED_AT,
                    transferEnable = TRANSFER,
                    subscribeUrl = SUBSCRIBE_URL,
                ),
            )
    }

    override fun stubOrderDetail() {
        coEvery { user.getOrderDetail(any()) } returns
            XboardResponse(data = XboardOrderData(id = 1, tradeNo = TRADE_NO, totalAmount = TOTAL_AMOUNT, status = 3))
    }

    override fun toString() = name
}

private class XiaoV2bContract : BackendContract {
    override val name = "xiaov2b"
    private val auth = mockk<XiaoV2bAuthRetrofit>()
    private val user = mockk<XiaoV2bUserRetrofit>()
    private val plan = mockk<XiaoV2bUserPlanRetrofit>()

    override fun authApi(): AuthApi = XiaoV2bAuthApi(auth, user, plan)

    override fun stubLoginSuccess() {
        coEvery { auth.login(any()) } returns XiaoV2bResponse(data = XiaoV2bLoginData(token = TOKEN, auth_data = AUTH_DATA))
    }

    override fun stubLoginEmptyData() {
        coEvery { auth.login(any()) } returns XiaoV2bResponse(data = null)
    }

    override fun stubUserInfo() {
        coEvery { user.fetchUserInfo() } returns
            XiaoV2bResponse(
                data =
                XiaoV2bUserInfoData(
                    email = EMAIL,
                    balance = 1200,
                    planId = 7,
                    expiredAt = EXPIRED_AT,
                    transferEnable = TRANSFER,
                    remindExpire = 1,
                    remindTraffic = 0,
                ),
            )
    }

    override fun stubSubscribeInfo() {
        coEvery { user.fetchSubscribe() } returns
            XiaoV2bResponse(
                data =
                XiaoV2bSubscribeData(
                    planId = 7,
                    expiredAt = EXPIRED_AT,
                    transferEnable = TRANSFER,
                    subscribeUrl = SUBSCRIBE_URL,
                ),
            )
    }

    override fun stubOrderDetail() {
        coEvery { user.getOrderDetail(any()) } returns
            XiaoV2bResponse(data = XiaoV2bOrderData(id = 1, tradeNo = TRADE_NO, totalAmount = TOTAL_AMOUNT, status = 3))
    }

    override fun toString() = name
}

@RunWith(Parameterized::class)
class AuthApiContractTest(private val backend: BackendContract) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun backends() = listOf(arrayOf<Any>(XboardContract()), arrayOf<Any>(XiaoV2bContract()))
    }

    @Test
    fun `登录成功透传 token 与 authData`() {
        backend.stubLoginSuccess()

        val result = runBlocking { backend.authApi().login(EMAIL, "pw123456") }

        assertEquals(TOKEN, result.token)
        assertEquals(AUTH_DATA, result.authData)
    }

    @Test
    fun `登录返回空数据时抛出保护性异常`() {
        backend.stubLoginEmptyData()

        assertThrows(ApiException::class.java) {
            runBlocking { backend.authApi().login(EMAIL, "pw123456") }
        }
    }

    @Test
    fun `用户信息映射提醒开关与套餐字段`() {
        backend.stubUserInfo()

        val info = runBlocking { backend.authApi().fetchUserInfo() }

        assertEquals(EMAIL, info.email)
        assertEquals(1200, info.balance)
        assertEquals(7, info.planId)
        assertEquals(EXPIRED_AT, info.expiredAt)
        assertEquals(TRANSFER, info.transferEnable)
        assertEquals("提醒开关统一归一化为 0/1", 1, info.remindExpire)
        assertEquals(0, info.remindTraffic)
    }

    @Test
    fun `订阅信息透传流量与订阅地址`() {
        backend.stubSubscribeInfo()

        val info = runBlocking { backend.authApi().fetchSubscribeInfo() }

        assertEquals(7, info.planId)
        assertEquals(EXPIRED_AT, info.expiredAt)
        assertEquals(TRANSFER, info.transferEnable)
        assertEquals(SUBSCRIBE_URL, info.subscribeUrl)
    }

    @Test
    fun `订单详情透传订单号状态与金额`() {
        backend.stubOrderDetail()

        val order = runBlocking { backend.authApi().getOrderDetail(TRADE_NO) }

        assertEquals(TRADE_NO, order.tradeNo)
        assertEquals(TOTAL_AMOUNT, order.totalAmount)
        assertEquals(3, order.status)
    }
}
