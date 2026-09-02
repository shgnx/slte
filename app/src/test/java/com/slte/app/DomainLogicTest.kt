package com.slte.app

import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.usecase.DaysUntilExpiryUseCase
import com.slte.app.utils.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class DomainLogicTest {

    @Test
    fun `订阅信息有效性与到期判断`() {
        val active = SubscribeInfo(
            planName = "初级套餐",
            transferEnable = 120L,
            usedTraffic = 10L,
            expiredAt = Instant.now().epochSecond + 86400L
        )
        assertTrue(active.hasPlan)
        assertFalse(active.expired)

        val empty = SubscribeInfo(planName = "", transferEnable = 0L, usedTraffic = 0L, expiredAt = 0L)
        assertFalse(empty.hasPlan)

        val expired = active.copy(expiredAt = Instant.now().epochSecond - 1L)
        assertTrue(expired.expired)
    }

    @Test
    fun `hasPlan 以套餐 ID 为准，兼容 transferEnable 为 0 的套餐`() {
        val byPlanId = SubscribeInfo(
            planName = "Pro",
            transferEnable = 0L,
            usedTraffic = 0L,
            expiredAt = 0L,
            planId = 1
        )
        assertTrue(byPlanId.hasPlan)

        // 缓存未携带 planId 时按流量判断
        val legacy = SubscribeInfo(planName = "Pro", transferEnable = 0L, usedTraffic = 0L, expiredAt = 0L)
        assertFalse(legacy.hasPlan)

        // planId 存在但套餐名为空：仍视为无套餐
        val noName = byPlanId.copy(planName = "")
        assertFalse(noName.hasPlan)
    }

    @Test
    fun `到期剩余天数`() {
        val useCase = DaysUntilExpiryUseCase()
        assertEquals(0, useCase(0L))
        val fiveDays = Instant.now().plus(5, ChronoUnit.DAYS).plusSeconds(2).epochSecond
        assertEquals(5, useCase(fiveDays))
        val expired = Instant.now().minus(1, ChronoUnit.DAYS).epochSecond
        assertEquals(0, useCase(expired))
        // 当天到期 → 0（不足一天向下取整）
        val today = Instant.now().plusSeconds(60).epochSecond
        assertEquals(0, useCase(today))
    }

    @Test
    fun `余额与流量格式化`() {
        assertEquals("12.50", FormatUtils.balance(1250))
        assertEquals("0B", FormatUtils.traffic(0))
        assertEquals("1.5GB", FormatUtils.traffic((1024L * 1024 * 1024 * 3) / 2))
        assertEquals("-5.00", FormatUtils.balance(-500))
        // TB 量级
        assertEquals("2TB", FormatUtils.traffic(2L * 1024 * 1024 * 1024 * 1024))
    }

    @Test
    fun `出口 IP 压缩仅作用于长 IPv6`() {
        assertEquals("1.2.3.4", FormatUtils.compactIp("1.2.3.4"))
        assertEquals("::1", FormatUtils.compactIp("::1"))
        val compact = FormatUtils.compactIp("2001:db8:85a3:8d3:1319:8a2e:370:7348")
        assertTrue(compact.length <= 20)
        assertTrue(compact.startsWith("2001:db8:85a3"))
        assertTrue(compact.endsWith(":7348"))
    }
}
