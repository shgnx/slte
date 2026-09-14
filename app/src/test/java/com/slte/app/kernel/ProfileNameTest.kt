package com.slte.app.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 邮箱 → 配置文件名标记的派生规则测试：稳定、可区分、无邮箱回退与固定长度。
 */
class ProfileNameTest {
    @Test
    fun `同一邮箱标记稳定`() {
        assertEquals(profileNameFor("user@example.com"), profileNameFor("user@example.com"))
    }

    @Test
    fun `不同邮箱标记可区分`() {
        assertNotEquals(profileNameFor("a@example.com"), profileNameFor("b@example.com"))
    }

    @Test
    fun `无邮箱回退通用名`() {
        assertEquals("SLTE", profileNameFor(null))
        assertEquals("SLTE", profileNameFor(""))
        assertEquals("SLTE", profileNameFor("   "))
    }

    @Test
    fun `标记前缀与长度固定`() {
        val name = profileNameFor("user@example.com")
        assertTrue(name.startsWith("SLTE-"))
        // SHA-256 十六进制长度固定：SLTE- + 64 位
        assertEquals("SLTE-".length + 64, name.length)
    }
}
