package com.slte.app.data.local

import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.ServerType
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.model.User
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `SessionStore` 纯 JVM 单测：注入 [InMemoryPreferences] 覆盖会话与各类缓存的读写、损坏容错与清除语义。
 */
class SessionStoreTest {
    private val prefs = InMemoryPreferences()
    private val store = SessionStore(prefs)

    @Test
    fun `会话读写往返`() {
        assertFalse("初始无会话", store.hasSession())

        store.save(authData = "jwt-token", email = "user@example.com", subscribeToken = "sub-token")

        assertTrue(store.hasSession())
        assertEquals("jwt-token", store.getAuthData())
        assertEquals("user@example.com", store.getEmail())
        assertEquals("sub-token", store.getSubscribeToken())
    }

    @Test
    fun `只有 authData 没有 subscribeToken 时不算已登录`() {
        prefs.edit().putString("auth_data", "jwt-token").commit()

        assertFalse("缺少订阅 token 时不应判定为已登录", store.hasSession())
    }

    @Test
    fun `订阅信息缓存读写往返`() {
        val info =
            SubscribeInfo(
                planName = "高级套餐",
                transferEnable = 100L * 1024 * 1024 * 1024,
                usedTraffic = 12L * 1024 * 1024 * 1024,
                expiredAt = 1_800_000_000L,
                planId = 7,
                subscribeUrl = "https://example.com/sub",
            )

        store.saveSubscribeInfo(info)

        assertEquals(info, store.getSubscribeInfo())
    }

    @Test
    fun `缓存损坏时返回 null 并删除脏键`() {
        // 键名是持久化契约的一部分（改名等于静默丢弃用户已有缓存），故在测试里显式锁定
        prefs.edit().putString("subscribe_info", "{ 这不是合法 JSON").commit()

        assertNull("解析失败应返回 null 而不是抛出", store.getSubscribeInfo())
        assertFalse("脏键应被清除，避免每次启动都重复解析失败", prefs.contains("subscribe_info"))
    }

    @Test
    fun `用户信息缓存读写往返`() {
        val user =
            User(
                id = "jwt-token",
                email = "user@example.com",
                authData = "jwt-token",
                subscribeToken = "sub-token",
                balance = "12.34",
            )

        store.saveUserInfo(user)

        assertEquals(user, store.getUserInfo())
    }

    @Test
    fun `节点缓存读写往返与清除`() {
        val nodes =
            listOf(
                ServerNode(id = 1, name = "香港 01", type = ServerType.SHADOWSOCKS, host = "1.2.3.4"),
                ServerNode(id = 2, name = "日本 01", type = ServerType.VMESS, host = "5.6.7.8"),
            )

        store.saveServerNodes(nodes)

        assertEquals(nodes, store.getServerNodes())

        store.clearServerNodes()

        assertNull(store.getServerNodes())
    }

    @Test
    fun `测速结果缓存读写往返`() {
        assertEquals(null, store.getSpeedResults())

        store.saveSpeedResults(mapOf("香港 01" to 42, "日本 01" to 88))

        assertEquals(mapOf("香港 01" to 42, "日本 01" to 88), store.getSpeedResults())
    }

    @Test
    fun `clear 清空会话与全部缓存`() {
        store.save(authData = "jwt", email = "e@x.com", subscribeToken = "sub")
        store.saveSubscribeInfo(SubscribeInfo("p", 1L, 1L, 0L))
        store.saveUserInfo(User(id = "jwt"))
        store.saveSpeedResults(mapOf("a" to 1))
        store.saveSubscribeUrl("https://example.com/sub")

        store.clear()

        assertFalse(store.hasSession())
        assertNull(store.getEmail())
        assertNull(store.getSubscribeInfo())
        assertNull(store.getUserInfo())
        assertNull(store.getSpeedResults())
        assertNull(store.getSubscribeUrl())
    }

    @Test
    fun `clearDataCache 保留会话只清数据`() {
        store.save(authData = "jwt", email = "e@x.com", subscribeToken = "sub")
        store.saveSubscribeInfo(SubscribeInfo("p", 1L, 1L, 0L))

        store.clearDataCache()

        assertTrue("会话凭证必须保留", store.hasSession())
        assertNull(store.getSubscribeInfo())
    }
}
