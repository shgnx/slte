package com.slte.app.data.remote.adapter

import com.slte.app.data.remote.ApiException
import com.slte.app.data.remote.adapter.xboard.XboardAuthApi
import com.slte.app.data.remote.adapter.xboard.XboardGiftCardRedeemRequest
import com.slte.app.data.remote.adapter.xboard.XboardResponse
import com.slte.app.data.remote.adapter.xboard.XboardUserRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bAuthApi
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bGiftCardRedeemRequest
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bResponse
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bUserRetrofit
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftCardRedeemApiTest {
    @Test
    fun `v2board 兑换使用 giftcard 字段且 data=true 视为成功`() = runTest {
        val userApi = mockk<XiaoV2bUserRetrofit>()
        val request = slot<XiaoV2bGiftCardRedeemRequest>()
        coEvery { userApi.redeemGiftCard(capture(request)) } returns
            XiaoV2bResponse(data = true, message = "操作成功")
        val api = XiaoV2bAuthApi(mockk(relaxed = true), userApi, mockk(relaxed = true))

        api.redeemGiftCard("SLTE2026")

        assertEquals("SLTE2026", request.captured.giftcard)
    }

    @Test
    fun `v2board 兑换 data 非 true 时抛出后端文案`() = runTest {
        val userApi = mockk<XiaoV2bUserRetrofit>()
        coEvery { userApi.redeemGiftCard(any()) } returns
            XiaoV2bResponse(data = false, message = "The gift card has already been used by this user")
        val api = XiaoV2bAuthApi(mockk(relaxed = true), userApi, mockk(relaxed = true))

        val error = runCatching { api.redeemGiftCard("SLTE2026") }.exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals("The gift card has already been used by this user", error?.message)
    }

    @Test
    fun `Xboard 兑换使用 code 字段且 data 非空视为成功`() = runTest {
        val userApi = mockk<XboardUserRetrofit>()
        val request = slot<XboardGiftCardRedeemRequest>()
        coEvery { userApi.redeemGiftCard(capture(request)) } returns
            XboardResponse(data = JsonObject(emptyMap()), message = "操作成功")
        val api = XboardAuthApi(mockk(relaxed = true), userApi, mockk(relaxed = true))

        api.redeemGiftCard("SLTE2026")

        assertEquals("SLTE2026", request.captured.code)
    }

    @Test
    fun `Xboard 兑换 data 为空时抛出后端文案`() = runTest {
        val userApi = mockk<XboardUserRetrofit>()
        coEvery { userApi.redeemGiftCard(any()) } returns
            XboardResponse(data = null, message = "兑换码不存在")
        val api = XboardAuthApi(mockk(relaxed = true), userApi, mockk(relaxed = true))

        val error = runCatching { api.redeemGiftCard("SLTE2026") }.exceptionOrNull()

        assertTrue(error is ApiException)
        assertEquals("兑换码不存在", error?.message)
    }
}
