package com.slte.app.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NodeNameResolverTest {

    @Test
    fun `去除协议装饰前缀后同名`() {
        assertEquals(NodeNameResolver.of("🇸🇬新加坡01"), NodeNameResolver.of("[vless]🇸🇬新加坡01"))
        assertEquals(NodeNameResolver.of("香港01"), NodeNameResolver.of("[vless]【香港】香港01"))
        assertEquals(NodeNameResolver.of("香港01"), NodeNameResolver.of("🇭🇰[vless]香港01"))
        assertEquals(NodeNameResolver.of("香港01"), NodeNameResolver.of("［vless］香港01"))
    }

    @Test
    fun `忽略全角字符与空白差异`() {
        assertEquals(NodeNameResolver.of("hk01"), NodeNameResolver.of("ＨＫ 01"))
        assertEquals(NodeNameResolver.of("香港01"), NodeNameResolver.of("\u200B香港01\u00A0"))
    }

    @Test
    fun `精确同名优先于规范化匹配`() {
        assertEquals("香港01", NodeNameResolver.resolve(listOf("[vless]香港01", "香港01"), "香港01"))
    }

    @Test
    fun `唯一规范化候选时返回内核名字`() {
        val members = listOf("[vless]🇭🇰香港01", "[ss]🇯🇵日本01")
        assertEquals("[ss]🇯🇵日本01", NodeNameResolver.resolve(members, "🇯🇵日本01"))
    }

    @Test
    fun `无候选或候选不唯一时返回空`() {
        assertNull(NodeNameResolver.resolve(listOf("[vless]香港01"), "日本01"))
        assertNull(NodeNameResolver.resolve(listOf("[vless]香港01", "[ss]香港01"), "🇭🇰香港01"))
        assertNull(NodeNameResolver.resolve(listOf("香港01"), ""))
    }

    @Test
    fun `序号类符号不参与归一化`() {
        assertNotEquals(NodeNameResolver.of("香港①"), NodeNameResolver.of("香港②"))
        assertNotEquals(NodeNameResolver.of("香港❶"), NodeNameResolver.of("香港❷"))
        assertNull(NodeNameResolver.resolve(listOf("香港①"), "香港②"))
        assertNull(NodeNameResolver.resolve(listOf("香港①", "香港❷"), "香港③"))
    }

    @Test
    fun `展示名只去掉协议标签`() {
        assertEquals("🇸🇬node.example.com", NodeNameResolver.displayName("[vless]🇸🇬node.example.com"))
        assertEquals("香港丨IEPL 1", NodeNameResolver.displayName("[Hy2] 香港丨IEPL 1"))
        assertEquals("[GIA]香港01", NodeNameResolver.displayName("[GIA]香港01"))
        assertEquals("香港01", NodeNameResolver.displayName("香港01"))
    }

    @Test
    fun `协议标签可解析且仅识别协议标签`() {
        assertEquals("vless", NodeNameResolver.protocolTag("[vless]香港01"))
        assertEquals("ss", NodeNameResolver.protocolTag("[ss]香港01"))
        assertEquals("hy2", NodeNameResolver.protocolTag("[HY2]香港01"))
        assertNull(NodeNameResolver.protocolTag("[GIA]香港01"))
        assertNull(NodeNameResolver.protocolTag("香港01"))
    }
}
