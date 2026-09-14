package com.slte.app.data.remote.config

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 多源配置竞速（race）测试：选版本最高者、忽略失败源与整体超时取消。
 */
class ConfigRaceTest {
    @Test
    fun `全部成功时选版本最高者`() = runBlocking {
        val result =
            ConfigRace.race(listOf("https://a.example.com", "https://b.example.com")) { url ->
                when (url) {
                    "https://a.example.com" -> FetchedConfig(url, "{}", "1.0", 30)
                    else -> FetchedConfig(url, "{}", "2.0", 10)
                }
            }
        assertEquals("https://b.example.com", result.chosen?.url)
        assertEquals("https://b.example.com", result.lastUrl)
    }

    @Test
    fun `部分源失败时忽略失败源`() = runBlocking {
        val result =
            ConfigRace.race(listOf("https://a.example.com", "https://b.example.com")) { url ->
                if (url.endsWith("b.example.com")) null else FetchedConfig(url, "{}", "1.0", 5)
            }
        assertEquals("https://a.example.com", result.chosen?.url)
        assertEquals("https://a.example.com", result.lastUrl)
    }

    @Test
    fun `全部源失败返回空结果`() = runBlocking {
        val result = ConfigRace.race(listOf("https://a.example.com", "https://b.example.com")) { null }
        assertNull(result.chosen)
        assertNull(result.lastUrl)
    }

    @Test
    fun `空列表直接返回空结果`() = runBlocking {
        val result = ConfigRace.race(emptyList()) { FetchedConfig("x", "{}", "1.0", 0) }
        assertNull(result.chosen)
    }

    @Test
    fun `整体超时取消所有并发子请求`() = runBlocking {
        val started = AtomicInteger(0)
        val outcome =
            runCatching {
                withTimeout(50) {
                    ConfigRace.race(listOf("https://a.example.com", "https://b.example.com")) { url ->
                        started.incrementAndGet()
                        delay(10_000)
                        FetchedConfig(url, "{}", "1.0", 0)
                    }
                }
            }
        assertTrue("应因超时失败", outcome.isFailure)
        // 两个请求均并发发起，且随 withTimeout 一并取消（结构化并发）
        assertEquals(2, started.get())
    }
}
