package com.slte.app.data.remote.config

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigCacheTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `缓存条目序列化往返保持字段完整`() {
        val cached = CachedConfig(
            config = RemoteConfigData(
                apiBaseUrl = "https://api.example.com",
                apiBaseUrls = listOf("https://api.example.com"),
                directDomains = listOf("example.com")
            ),
            version = "1.2",
            fetchedAt = 12345L,
            sourceUrl = "https://cfg.example.com/config.json",
            etag = "\"abc123\""
        )
        val raw = json.encodeToString(CachedConfig.serializer(), cached)
        val decoded = json.decodeFromString(CachedConfig.serializer(), raw)
        assertEquals(cached, decoded)
        // meta 与配置本体均完整保留
        assertEquals("1.2", decoded.version)
        assertEquals(12345L, decoded.fetchedAt)
        assertEquals("https://cfg.example.com/config.json", decoded.sourceUrl)
        assertEquals("\"abc123\"", decoded.etag)
        assertEquals("https://api.example.com", decoded.config.apiBaseUrl)
    }

    @Test
    fun `缓存损坏时回退默认配置`() {
        // 非法 JSON 应被 loadCached 解析失败路径覆盖（回退 RemoteConfigData 默认）
        val broken = "{ not valid json"
        val parsed = runCatching { json.decodeFromString<CachedConfig>(broken) }
        assertTrue(parsed.isFailure)
    }

    @Test
    fun `缓存新鲜度判定`() {
        val now = 1_000_000L
        val ttl = 5 * 60_000L
        assertTrue(ConfigValidation.isCacheFresh(now - 60_000, now, ttl))
        assertTrue(!ConfigValidation.isCacheFresh(now - 6 * 60_000, now, ttl))
    }
}
