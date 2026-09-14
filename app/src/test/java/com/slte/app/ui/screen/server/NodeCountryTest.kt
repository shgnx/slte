package com.slte.app.ui.screen.server

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 节点名 → 国家/地区码提取规则测试：中文/英文关键词、ISO 分段识别与无法识别回退 XX。
 */
class NodeCountryTest {
    @Test
    fun `中文地名关键词`() {
        assertEquals("HK", extractCountryCode("香港 01"))
        assertEquals("SG", extractCountryCode("新加坡 - SG02"))
        assertEquals("JP", extractCountryCode("日本东京 03"))
        assertEquals("KR", extractCountryCode("韩国首尔"))
        assertEquals("US", extractCountryCode("美国洛杉矶"))
        assertEquals("DE", extractCountryCode("德国法兰克福"))
    }

    @Test
    fun `英文地名关键词`() {
        assertEquals("HK", extractCountryCode("Hong Kong 01"))
        assertEquals("GB", extractCountryCode("London-UK"))
        assertEquals("JP", extractCountryCode("Tokyo 04"))
        assertEquals("DE", extractCountryCode("Frankfurt 05"))
    }

    @Test
    fun `ISO代码分段识别`() {
        assertEquals("US", extractCountryCode("US-Los Angeles"))
        assertEquals("SG", extractCountryCode("SG-01"))
        assertEquals("JP", extractCountryCode("JP|东京"))
        assertEquals("AU", extractCountryCode("AU 悉尼"))
    }

    @Test
    fun `无法识别返回XX`() {
        assertEquals("XX", extractCountryCode("普通节点"))
        assertEquals("XX", extractCountryCode(""))
        assertEquals("XX", extractCountryCode("自定义节点-01"))
    }
}
