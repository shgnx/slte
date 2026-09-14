package com.slte.app.data.remote.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 端点健康状态机测试：降级、熔断、半开、恢复与指数退避。
 */
class EndpointHealthTest {
    @Test
    fun `连续失败未达阈值保持降级`() {
        var cur = EndpointHealth("https://a.example.com")
        repeat(2) { cur = EndpointHealthRules.onFailure(cur, 1000L) }
        assertEquals(HealthState.DEGRADED, EndpointHealthRules.state(cur, 2000L))
    }

    @Test
    fun `连续失败达到阈值进入熔断并在退避期内保持`() {
        var cur = EndpointHealth("https://a.example.com")
        repeat(3) { cur = EndpointHealthRules.onFailure(cur, 1000L) }
        assertEquals(3, cur.consecutiveFailures)
        assertEquals(HealthState.OPEN, EndpointHealthRules.state(cur, 1000L))
        assertTrue(EndpointHealthRules.isOpen(cur, 1000L))
        // 退避期内仍为熔断
        assertTrue(EndpointHealthRules.isOpen(cur, 1000L + 1000L))
    }

    @Test
    fun `退避期结束后进入半开可探测`() {
        var cur = EndpointHealth("https://a.example.com")
        repeat(3) { cur = EndpointHealthRules.onFailure(cur, 1000L) }
        val openedAt = cur.openedAt
        val backoff = cur.backoffMs
        val after = openedAt + backoff + 10_000
        assertEquals(HealthState.HALF_OPEN, EndpointHealthRules.state(cur, after))
    }

    @Test
    fun `半开后成功恢复健康并清零失败计数`() {
        var cur = EndpointHealth("https://a.example.com")
        repeat(3) { cur = EndpointHealthRules.onFailure(cur, 1000L) }
        cur = EndpointHealthRules.onSuccess(cur, 120L, 100_000L)
        assertEquals(HealthState.HEALTHY, EndpointHealthRules.state(cur, 100_000L))
        assertEquals(0, cur.consecutiveFailures)
        assertEquals(120L, cur.lastLatencyMs)
    }

    @Test
    fun `退避指数增长且含抖动`() {
        var cur = EndpointHealth("https://a.example.com")
        repeat(3) { cur = EndpointHealthRules.onFailure(cur, 0L) }
        // 基础 5s ±20%：4000..6000
        assertTrue(cur.backoffMs in 4_000..6_000)
        // 再失败一次退避翻倍：8s..12s
        cur = EndpointHealthRules.onFailure(cur, 1000L)
        assertTrue(cur.backoffMs in 8_000..12_000)
        // 上限 5 分钟
        repeat(8) { cur = EndpointHealthRules.onFailure(cur, 2000L) }
        assertTrue(cur.backoffMs <= 5 * 60_000L)
    }

    @Test
    fun `主地址粘滞需要新地址快30%以上`() {
        val current = 100L
        // 持平或改善不足 30% 不切换
        assertFalse(EndpointHealthRules.shouldSwitchPrimary(current, 100L))
        assertFalse(EndpointHealthRules.shouldSwitchPrimary(current, 80L))
        assertFalse(EndpointHealthRules.shouldSwitchPrimary(current, 71L))
        // 快 30% 以上才切换
        assertTrue(EndpointHealthRules.shouldSwitchPrimary(current, 69L))
        assertTrue(EndpointHealthRules.shouldSwitchPrimary(current, 50L))
    }
}
