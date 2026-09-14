package com.slte.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * `CredentialStore` 纯 JVM 单测（依赖收窄同 [SessionStoreTest]）。
 * 守护「记住账号」语义边界：登出后保留、清空只清凭证。
 */
class CredentialStoreTest {
    private val prefs = InMemoryPreferences()
    private val store = CredentialStore(prefs)

    @Test
    fun `保存后读取往返`() {
        store.save(email = "user@example.com", password = "s3cret")

        assertEquals("user@example.com", store.getSavedEmail())
        assertEquals("s3cret", store.getSavedPassword())
    }

    @Test
    fun `未保存时读取为 null`() {
        assertNull(store.getSavedEmail())
        assertNull(store.getSavedPassword())
    }

    @Test
    fun `单槽位：后保存的账号覆盖前一个`() {
        store.save(email = "first@example.com", password = "p1")
        store.save(email = "second@example.com", password = "p2")

        assertEquals("second@example.com", store.getSavedEmail())
        assertEquals("p2", store.getSavedPassword())
    }

    @Test
    fun `clear 清空邮箱与密码`() {
        store.save(email = "user@example.com", password = "p")

        store.clear()

        assertNull(store.getSavedEmail())
        assertNull(store.getSavedPassword())
    }
}
