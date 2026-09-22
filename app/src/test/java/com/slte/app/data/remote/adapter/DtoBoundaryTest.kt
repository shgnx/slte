package com.slte.app.data.remote.adapter

import com.slte.app.data.remote.adapter.xboard.XboardCommissionRecordData
import com.slte.app.data.remote.adapter.xboard.XboardCouponData
import com.slte.app.data.remote.adapter.xboard.XboardInviteCodeData
import com.slte.app.data.remote.adapter.xboard.XboardInviteData
import com.slte.app.data.remote.adapter.xboard.XboardLoginData
import com.slte.app.data.remote.adapter.xboard.XboardOrderData
import com.slte.app.data.remote.adapter.xboard.XboardPaymentMethodData
import com.slte.app.data.remote.adapter.xboard.XboardPlanData
import com.slte.app.data.remote.adapter.xboard.XboardResponse
import com.slte.app.data.remote.adapter.xboard.XboardSubscribeData
import com.slte.app.data.remote.adapter.xboard.XboardUserInfoData
import com.slte.app.data.remote.adapter.xboard.toDomain
import com.slte.app.data.remote.adapter.xboard.toDomainCouponCheck
import com.slte.app.data.remote.adapter.xboard.toDomainLoginResponse
import com.slte.app.data.remote.adapter.xboard.toDomainOrder
import com.slte.app.data.remote.adapter.xboard.toDomainPaymentMethod
import com.slte.app.data.remote.adapter.xboard.toDomainPlan
import com.slte.app.data.remote.adapter.xboard.toDomainSubscribeInfo
import com.slte.app.data.remote.adapter.xboard.toDomainUserInfo
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bCommissionRecordData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bCouponData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bInviteCodeData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bInviteData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bLoginData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bOrderData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bPaymentMethodData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bPlanData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bResponse
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bSubscribeData
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bUserInfoData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DtoBoundaryTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

    @Test
    fun `Xboard 用户信息零值构造时全部字段回落默认值`() {
        val dto = XboardUserInfoData().toDomainUserInfo()

        assertEquals("", dto.email)
        assertEquals(0, dto.balance)
        assertEquals(0, dto.planId)
        assertEquals(0L, dto.expiredAt)
        assertEquals(0L, dto.transferEnable)
        assertEquals(0, dto.remindExpire)
        assertEquals(0, dto.remindTraffic)
    }

    @Test
    fun `XiaoV2b 用户信息零值构造时全部字段回落默认值`() {
        val dto = XiaoV2bUserInfoData().toDomainUserInfo()

        assertEquals("", dto.email)
        assertEquals(0, dto.balance)
        assertEquals(0, dto.planId)
        assertEquals(0L, dto.expiredAt)
        assertEquals(0L, dto.transferEnable)
        assertEquals(0, dto.remindExpire)
        assertEquals(0, dto.remindTraffic)
    }

    @Test
    fun `Xboard 套餐零值构造时开关回落为开启`() {
        val dto = XboardPlanData().toDomainPlan()

        assertEquals(true, dto.show)
        assertEquals(true, dto.renew)
        assertNull(dto.monthPrice)
        assertNull(dto.yearPrice)
        assertNull(dto.speedLimit)
        assertNull(dto.content)
        assertEquals(0, dto.transferEnable)
    }

    @Test
    fun `XiaoV2b 套餐零值构造时开关归一化为开启`() {
        val dto = XiaoV2bPlanData().toDomainPlan()

        assertEquals(true, dto.show)
        assertEquals(true, dto.renew)
        assertNull(dto.monthPrice)
        assertNull(dto.yearPrice)
        assertNull(dto.speedLimit)
        assertNull(dto.content)
        assertEquals(0, dto.transferEnable)
    }

    @Test
    fun `Xboard 订单零值构造时金额回落为零且处理费保持为空`() {
        val dto = XboardOrderData().toDomainOrder()

        assertEquals(0, dto.id)
        assertEquals("", dto.tradeNo)
        assertEquals("", dto.planName)
        assertEquals(0, dto.totalAmount)
        assertEquals(0, dto.balanceAmount)
        assertEquals(0, dto.discountAmount)
        assertEquals(0, dto.surplusAmount)
        assertEquals(0, dto.refundAmount)
        assertNull(dto.handlingAmount)
        assertEquals(0, dto.status)
        assertEquals("", dto.period)
        assertEquals(0L, dto.createdAt)
        assertEquals(0L, dto.expiredAt)
    }

    @Test
    fun `XiaoV2b 订单零值构造时金额回落为零且处理费保持为空`() {
        val dto = XiaoV2bOrderData().toDomainOrder()

        assertEquals(0, dto.id)
        assertEquals("", dto.tradeNo)
        assertEquals("", dto.planName)
        assertEquals(0, dto.totalAmount)
        assertEquals(0, dto.balanceAmount)
        assertEquals(0, dto.discountAmount)
        assertEquals(0, dto.surplusAmount)
        assertEquals(0, dto.refundAmount)
        assertNull(dto.handlingAmount)
        assertEquals(0, dto.status)
        assertEquals("", dto.period)
        assertEquals(0L, dto.createdAt)
        assertEquals(0L, dto.expiredAt)
    }

    @Test
    fun `Xboard 订阅零值构造时套餐名与订阅地址为空`() {
        val dto = XboardSubscribeData().toDomainSubscribeInfo()

        assertEquals(0, dto.planId)
        assertEquals("", dto.planName)
        assertEquals(0L, dto.expiredAt)
        assertEquals(0L, dto.transferEnable)
        assertEquals(0L, dto.upload)
        assertEquals(0L, dto.download)
        assertNull(dto.resetDay)
        assertNull(dto.subscribeUrl)
    }

    @Test
    fun `XiaoV2b 订阅零值构造时套餐名与订阅地址为空`() {
        val dto = XiaoV2bSubscribeData().toDomainSubscribeInfo()

        assertEquals(0, dto.planId)
        assertEquals("", dto.planName)
        assertEquals(0L, dto.expiredAt)
        assertEquals(0L, dto.transferEnable)
        assertEquals(0L, dto.upload)
        assertEquals(0L, dto.download)
        assertNull(dto.resetDay)
        assertNull(dto.subscribeUrl)
    }

    @Test
    fun `两套后端订阅缺省时产出相同 domain 结果`() {
        assertEquals(XboardSubscribeData().toDomainSubscribeInfo(), XiaoV2bSubscribeData().toDomainSubscribeInfo())
    }

    @Test
    fun `优惠券缺少名称时用券码兜底且类型默认二`() {
        assertEquals("C1", XboardCouponData(code = "C1").toDomainCouponCheck().name)
        assertEquals("C1", XiaoV2bCouponData(code = "C1").toDomainCouponCheck().name)
        assertEquals(2, XboardCouponData().toDomainCouponCheck().type)
        assertEquals(2, XiaoV2bCouponData().toDomainCouponCheck().type)
        assertEquals(0, XboardCouponData().toDomainCouponCheck().value)
        assertEquals(0, XiaoV2bCouponData().toDomainCouponCheck().value)
        assertEquals("", XboardCouponData().toDomainCouponCheck().name)
    }

    @Test
    fun `邀请信息缺省时回落空列表与全零统计`() {
        val xboard = XboardInviteData().toDomain()
        val xiao = XiaoV2bInviteData().toDomain()

        assertEquals(emptyList<Any>(), xboard.codes)
        assertEquals(0, xboard.stat.registeredUsers)
        assertEquals(0, xboard.stat.totalCommission)
        assertEquals(0, xboard.stat.pendingCommission)
        assertEquals(0, xboard.stat.commissionRate)
        assertEquals(0, xboard.stat.availableBalance)
        assertEquals(xboard, xiao)
    }

    @Test
    fun `邀请统计数组缺失位置补零超出部分被忽略`() {
        assertEquals(1, XboardInviteData(stat = listOf(1, 2)).toDomain().stat.registeredUsers)
        assertEquals(2, XboardInviteData(stat = listOf(1, 2)).toDomain().stat.totalCommission)
        assertEquals(0, XboardInviteData(stat = listOf(1, 2)).toDomain().stat.pendingCommission)
        assertEquals(5, XboardInviteData(stat = listOf(1, 2, 3, 4, 5, 6)).toDomain().stat.availableBalance)
        assertEquals(0, XiaoV2bInviteData(stat = listOf(9)).toDomain().stat.commissionRate)
    }

    @Test
    fun `佣金记录缺省时全部回落为零`() {
        val xboard = XboardCommissionRecordData().toDomain()
        val xiao = XiaoV2bCommissionRecordData().toDomain()

        assertEquals(0, xboard.id)
        assertEquals("", xboard.tradeNo)
        assertEquals(0, xboard.orderAmount)
        assertEquals(0, xboard.getAmount)
        assertEquals(0L, xboard.createdAt)
        assertEquals(xboard, xiao)
    }

    @Test
    fun `支付方式缺少图标与可选字段时回落为空串或 null`() {
        val xboard = XboardPaymentMethodData().toDomainPaymentMethod()
        val xiao = XiaoV2bPaymentMethodData().toDomainPaymentMethod()

        assertEquals(0, xboard.id)
        assertEquals("", xboard.name)
        assertEquals("", xboard.payment)
        assertNull(xboard.icon)
        assertEquals(xboard, xiao)
    }

    @Test
    fun `响应信封缺省时 data 与 message 均为 null`() {
        assertNull(XboardResponse<XboardUserInfoData>().data)
        assertNull(XboardResponse<XboardUserInfoData>().message)
        assertNull(XiaoV2bResponse<XiaoV2bUserInfoData>().data)
        assertNull(XiaoV2bResponse<XiaoV2bUserInfoData>().message)
    }

    @Test
    fun `登录数据缺少必填字段时解析失败`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<XboardLoginData>("""{"auth_data":"ad-1"}""")
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<XiaoV2bLoginData>("""{"token":"t-1"}""")
        }

        val login = json.decodeFromString<XboardLoginData>("""{"token":"t-1","auth_data":"ad-1"}""").toDomainLoginResponse()
        assertEquals("t-1", login.token)
        assertEquals("ad-1", login.authData)
    }

    @Test
    fun `订单金额为 JSON null 时回落为零`() {
        val dto =
            json.decodeFromString<XboardOrderData>(
                """{"balance_amount":null,"discount_amount":null,"surplus_amount":null,"handling_amount":null}""",
            ).toDomainOrder()

        assertEquals(0, dto.balanceAmount)
        assertEquals(0, dto.discountAmount)
        assertEquals(0, dto.surplusAmount)
        assertNull(dto.handlingAmount)
    }

    @Test
    fun `订单金额为布尔对象数组时回落为零且浮点字面量四舍五入`() {
        val dto =
            json.decodeFromString<XiaoV2bOrderData>(
                """{"balance_amount":true,"discount_amount":{"amount":1},"surplus_amount":[1,2],"handling_amount":12.5}""",
            ).toDomainOrder()

        assertEquals(0, dto.balanceAmount)
        assertEquals(0, dto.discountAmount)
        assertEquals(0, dto.surplusAmount)
        assertEquals(13, dto.handlingAmount)
    }

    @Test
    fun `订单金额为小数文本时四舍五入为整数`() {
        val dto =
            json.decodeFromString<XboardOrderData>(
                """{"balance_amount":"12.50","discount_amount":"0.01","surplus_amount":"1e3","handling_amount":" 2.5 "}""",
            ).toDomainOrder()

        assertEquals(13, dto.balanceAmount)
        assertEquals(0, dto.discountAmount)
        assertEquals(1000, dto.surplusAmount)
        assertEquals(3, dto.handlingAmount)
    }

    @Test
    fun `订单金额九种输入均按四舍五入或回落解析`() {
        val decimals =
            json.decodeFromString<XboardOrderData>(
                """{"balance_amount":"50.00","discount_amount":"0.01","surplus_amount":"1e3","handling_amount":12.5}""",
            ).toDomainOrder()

        assertEquals(50, decimals.balanceAmount)
        assertEquals(0, decimals.discountAmount)
        assertEquals(1000, decimals.surplusAmount)
        assertEquals(13, decimals.handlingAmount)

        val negative =
            XboardOrderData(
                balanceAmount = JsonPrimitive(-5),
                discountAmount = JsonPrimitive("-5"),
                surplusAmount = JsonPrimitive(" -.5 "),
            ).toDomainOrder()

        assertEquals(-5, negative.balanceAmount)
        assertEquals(-5, negative.discountAmount)
        assertEquals(-1, negative.surplusAmount)

        val fallbacks =
            XboardOrderData(
                balanceAmount = JsonNull,
                discountAmount = JsonPrimitive(true),
                surplusAmount = JsonObject(emptyMap()),
                surplusCredit = JsonArray(emptyList()),
                handlingAmount = JsonPrimitive("abc"),
            ).toDomainOrder()

        assertEquals(0, fallbacks.balanceAmount)
        assertEquals(0, fallbacks.discountAmount)
        assertEquals(0, fallbacks.surplusAmount)
        assertEquals(0, fallbacks.refundAmount)
        assertNull(fallbacks.handlingAmount)
    }

    @Test
    fun `两套后端对同一小数文本金额语义产出一致结果`() {
        val raw =
            """{"balance_amount":"50.00","discount_amount":"0.01","surplus_amount":"1e3","handling_amount":12.5}"""

        val xboard = json.decodeFromString<XboardOrderData>(raw).toDomainOrder()
        val xiao = json.decodeFromString<XiaoV2bOrderData>(raw).toDomainOrder()

        assertEquals(xboard, xiao)
        assertEquals(50, xboard.balanceAmount)
        assertEquals(0, xboard.discountAmount)
        assertEquals(1000, xboard.surplusAmount)
        assertEquals(13, xboard.handlingAmount)
    }

    @Test
    fun `订单金额为数字文本或负数时原样解析`() {
        val dto =
            XboardOrderData(
                balanceAmount = JsonPrimitive("100"),
                discountAmount = JsonPrimitive(20),
                surplusAmount = JsonPrimitive(-5),
                handlingAmount = JsonPrimitive(0),
            ).toDomainOrder()

        assertEquals(100, dto.balanceAmount)
        assertEquals(20, dto.discountAmount)
        assertEquals(-5, dto.surplusAmount)
        assertEquals(0, dto.handlingAmount)
    }

    @Test
    fun `订单金额为布尔或结构化 JsonElement 时回落为零`() {
        val dto =
            XboardOrderData(
                balanceAmount = JsonPrimitive(true),
                discountAmount = JsonObject(mapOf("a" to JsonPrimitive(1))),
                surplusAmount = JsonArray(listOf(JsonPrimitive(9))),
            ).toDomainOrder()

        assertEquals(0, dto.balanceAmount)
        assertEquals(0, dto.discountAmount)
        assertEquals(0, dto.surplusAmount)
    }

    @Test
    fun `退款金额在两套后端取自不同字段`() {
        val xboardWithSurplusCredit =
            json.decodeFromString<XboardOrderData>("""{"surplus_credit":7,"refund_amount":9}""").toDomainOrder()
        val xiaoWithRefundAmount =
            json.decodeFromString<XiaoV2bOrderData>("""{"surplus_credit":7,"refund_amount":9}""").toDomainOrder()

        assertEquals(7, xboardWithSurplusCredit.refundAmount)
        assertEquals(9, xiaoWithRefundAmount.refundAmount)
        assertNull(xboardWithSurplusCredit.handlingAmount)
    }

    @Test
    fun `宽松解析 Xboard 订单 JSON 并忽略未知字段`() {
        val raw =
            """
            {"id":9,"trade_no":"TN-1","total_amount":5000,"balance_amount":"100","discount_amount":20,
            "surplus_amount":"5","surplus_credit":"3","handling_amount":1,"status":3,"period":"monthly",
            "created_at":11,"expired_at":22,"commission_balance":0,"unknown_field":{"a":1},
            "plan":{"id":3,"name":"P1","show":true,"renew":false}}
            """.trimIndent()

        val dto = json.decodeFromString<XboardOrderData>(raw).toDomainOrder()

        assertEquals(9, dto.id)
        assertEquals("TN-1", dto.tradeNo)
        assertEquals("P1", dto.planName)
        assertEquals(5000, dto.totalAmount)
        assertEquals(100, dto.balanceAmount)
        assertEquals(20, dto.discountAmount)
        assertEquals(5, dto.surplusAmount)
        assertEquals(3, dto.refundAmount)
        assertEquals(1, dto.handlingAmount)
        assertEquals(3, dto.status)
        assertEquals("monthly", dto.period)
        assertEquals(11L, dto.createdAt)
        assertEquals(22L, dto.expiredAt)
    }

    @Test
    fun `宽松解析 XiaoV2b 订单 JSON 并忽略未知字段`() {
        val raw =
            """
            {"id":9,"trade_no":"TN-1","total_amount":5000,"balance_amount":"100","discount_amount":20,
            "surplus_amount":"5","refund_amount":"3","handling_amount":1,"status":3,"period":"monthly",
            "created_at":11,"expired_at":22,"is_expired":false,"unknown_field":"x",
            "plan":{"id":3,"name":"P1","show":1,"renew":0}}
            """.trimIndent()

        val dto = json.decodeFromString<XiaoV2bOrderData>(raw).toDomainOrder()

        assertEquals(9, dto.id)
        assertEquals("TN-1", dto.tradeNo)
        assertEquals("P1", dto.planName)
        assertEquals(5000, dto.totalAmount)
        assertEquals(100, dto.balanceAmount)
        assertEquals(20, dto.discountAmount)
        assertEquals(5, dto.surplusAmount)
        assertEquals(3, dto.refundAmount)
        assertEquals(1, dto.handlingAmount)
        assertEquals(3, dto.status)
        assertEquals("monthly", dto.period)
        assertEquals(11L, dto.createdAt)
        assertEquals(22L, dto.expiredAt)
    }

    @Test
    fun `Xboard 信封真实 JSON 缺失 message 时按 null 处理`() {
        val raw =
            """
            {"code":200,"data":{"email":"a@b.c","balance":1200,"plan_id":7,"expired_at":1800000000,
            "transfer_enable":1099511627776,"remind_expire":true,"remind_traffic":false,
            "is_admin":true,"uuid":"u-1"},"extra":{"x":1}}
            """.trimIndent()

        val response = json.decodeFromString<XboardResponse<XboardUserInfoData>>(raw)
        val dto = response.data!!.toDomainUserInfo()

        assertNull(response.message)
        assertEquals("a@b.c", dto.email)
        assertEquals(1200, dto.balance)
        assertEquals(7, dto.planId)
        assertEquals(1800000000L, dto.expiredAt)
        assertEquals(1099511627776L, dto.transferEnable)
        assertEquals(1, dto.remindExpire)
        assertEquals(0, dto.remindTraffic)
    }

    @Test
    fun `XiaoV2b 信封真实 JSON 缺失 data 时按 null 处理`() {
        val raw = """{"code":200,"message":"账号或密码错误"}"""

        val response = json.decodeFromString<XiaoV2bResponse<XiaoV2bUserInfoData>>(raw)

        assertNull(response.data)
        assertEquals("账号或密码错误", response.message)
    }

    @Test
    fun `引号包裹的数字与布尔在宽松模式下正常解析`() {
        val xiaoOrder =
            json.decodeFromString<XiaoV2bOrderData>("""{"id":"9","trade_no":"TN-1","total_amount":"5000","expired_at":"22"}""").toDomainOrder()
        val xiaoUser = json.decodeFromString<XiaoV2bUserInfoData>("""{"email":"a@b.c","balance":"1200"}""").toDomainUserInfo()
        val xboardPlan = json.decodeFromString<XboardPlanData>("""{"show":"true","renew":"false"}""").toDomainPlan()

        assertEquals(9, xiaoOrder.id)
        assertEquals("TN-1", xiaoOrder.tradeNo)
        assertEquals(5000, xiaoOrder.totalAmount)
        assertEquals(22L, xiaoOrder.expiredAt)
        assertEquals(1200, xiaoUser.balance)
        assertEquals(true, xboardPlan.show)
        assertEquals(false, xboardPlan.renew)
    }

    @Test
    fun `Xboard 布尔开关字段拒绝数字字面量`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<XboardUserInfoData>("""{"remind_expire":1,"remind_traffic":0}""")
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<XboardPlanData>("""{"show":1,"renew":0}""")
        }
    }

    @Test
    fun `XiaoV2b 整型开关字段拒绝布尔字面量`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<XiaoV2bInviteCodeData>("""{"status":true}""")
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<XiaoV2bPlanData>("""{"show":true,"renew":false}""")
        }
    }

    @Test
    fun `浮点字面量无法解析为整数字段`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<XboardOrderData>("""{"total_amount":50.0}""")
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<XiaoV2bUserInfoData>("""{"balance":100.5}""")
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString<XiaoV2bOrderData>("""{"status":1.0}""")
        }
    }

    @Test
    fun `非空字段收到 JSON null 时回落到声明默认值`() {
        val xiaoUser = json.decodeFromString<XiaoV2bUserInfoData>("""{"balance":null,"plan_id":null}""").toDomainUserInfo()
        val xboardUser = json.decodeFromString<XboardUserInfoData>("""{"remind_expire":null,"balance":null}""").toDomainUserInfo()
        val xiaoOrder = json.decodeFromString<XiaoV2bOrderData>("""{"total_amount":null,"status":null}""").toDomainOrder()

        assertEquals(0, xiaoUser.balance)
        assertEquals(0, xiaoUser.planId)
        assertEquals(0, xboardUser.balance)
        assertEquals(0, xboardUser.remindExpire)
        assertEquals(0, xiaoOrder.totalAmount)
        assertEquals(0, xiaoOrder.status)
    }

    @Test
    fun `订单金额与状态为零或负数时原样透传`() {
        val dto =
            json.decodeFromString<XiaoV2bOrderData>(
                """{"total_amount":-1,"balance_amount":-100,"discount_amount":0,"handling_amount":-9,"status":0}""",
            ).toDomainOrder()

        assertEquals(-1, dto.totalAmount)
        assertEquals(-100, dto.balanceAmount)
        assertEquals(0, dto.discountAmount)
        assertEquals(-9, dto.handlingAmount)
        assertEquals(0, dto.status)
    }

    @Test
    fun `两套后端对同一订单语义产出相同 domain 结果`() {
        val raw =
            """
            {"id":9,"trade_no":"TN-1","total_amount":5000,"balance_amount":"100","discount_amount":20,
            "surplus_amount":"5","handling_amount":1,"status":3,"period":"monthly","created_at":11,"expired_at":22,
            "plan":{"id":3,"name":"P1","month_price":1000}}
            """.trimIndent()

        val xboard = json.decodeFromString<XboardOrderData>(raw).toDomainOrder()
        val xiao = json.decodeFromString<XiaoV2bOrderData>(raw).toDomainOrder()

        assertEquals(xboard, xiao)
        assertEquals(100, xboard.balanceAmount)
        assertEquals(5, xboard.surplusAmount)
        assertEquals(1, xboard.handlingAmount)
        assertEquals(0, xboard.refundAmount)
        assertEquals("P1", xboard.planName)
    }

    @Test
    fun `两套后端对同一套餐语义产出相同 domain 结果`() {
        val raw =
            """
            {"id":3,"name":"P1","description":"d","month_price":1000,"quarter_price":2700,"half_year_price":5000,
            "year_price":9000,"two_year_price":17000,"three_year_price":24000,"onetime_price":50000,
            "reset_price":100,"speed_limit":5,"device_limit":2,"content":"c","capacity_limit":3,"group_id":1,
            "sort":2,"transfer_enable":1024}
            """.trimIndent()

        val xboard = json.decodeFromString<XboardPlanData>(raw).toDomainPlan()
        val xiao = json.decodeFromString<XiaoV2bPlanData>(raw).toDomainPlan()

        assertEquals(xboard, xiao)
        assertEquals(1000L, xboard.monthPrice)
        assertEquals(1024, xboard.transferEnable)
        assertEquals(true, xboard.show)
        assertEquals(true, xboard.renew)
    }

    @Test
    fun `两套后端对同一用户信息语义产出相同 domain 结果`() {
        val raw =
            """
            {"email":"a@b.c","balance":1200,"plan_id":7,"expired_at":1800000000,"transfer_enable":1024}
            """.trimIndent()

        val xboard = json.decodeFromString<XboardUserInfoData>(raw).toDomainUserInfo()
        val xiao = json.decodeFromString<XiaoV2bUserInfoData>(raw).toDomainUserInfo()

        assertEquals(xboard, xiao)
        assertEquals(1200, xboard.balance)
        assertEquals(1024L, xboard.transferEnable)
    }

    @Test
    fun `两套后端对同一订阅语义产出相同 domain 结果`() {
        val raw =
            """
            {"plan_id":7,"expired_at":1800000000,"transfer_enable":1024,"u":10,"d":20,"reset_day":5,
            "subscribe_url":"https://app.example.com/api/v1/client/subscribe?token=x","plan":{"name":"P1"}}
            """.trimIndent()

        val xboard = json.decodeFromString<XboardSubscribeData>(raw).toDomainSubscribeInfo()
        val xiao = json.decodeFromString<XiaoV2bSubscribeData>(raw).toDomainSubscribeInfo()

        assertEquals(xboard, xiao)
        assertEquals("P1", xboard.planName)
        assertEquals(10L, xboard.upload)
        assertEquals(5, xboard.resetDay)
    }

    @Test
    fun `两套后端对同一优惠券语义产出相同 domain 结果`() {
        val raw = """{"id":1,"code":"C1","name":"N1","type":1,"value":500,"plan_id":3}"""

        val xboard = json.decodeFromString<XboardCouponData>(raw).toDomainCouponCheck()
        val xiao = json.decodeFromString<XiaoV2bCouponData>(raw).toDomainCouponCheck()

        assertEquals(xboard, xiao)
        assertEquals("N1", xboard.name)
        assertEquals(1, xboard.type)
        assertEquals(500, xboard.value)
    }

    @Test
    fun `两套后端邀请码在各自线格式下产出相同 domain 结果`() {
        val xboard =
            json.decodeFromString<XboardInviteCodeData>("""{"id":5,"code":"C1","status":true,"pv":3}""").toDomain()
        val xiao =
            json.decodeFromString<XiaoV2bInviteCodeData>("""{"id":5,"code":"C1","status":1,"pv":3}""").toDomain()

        assertEquals(xboard, xiao)
        assertEquals(1, xboard.status)
        assertEquals(3, xboard.pv)
    }

    @Test
    fun `XiaoV2b 邀请码状态不归一化而 Xboard 归一化为零一`() {
        val xiao = XiaoV2bInviteCodeData(status = 2).toDomain()
        val xboard = XboardInviteCodeData(status = true).toDomain()

        assertEquals(2, xiao.status)
        assertEquals(1, xboard.status)
    }
}
