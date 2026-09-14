package com.slte.app

import com.slte.app.ui.screen.about.compareVersions
import com.slte.app.ui.screen.about.shouldShowUpdateDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 更新弹窗决策（force/manual/稍后提醒）与版本号比较规则测试。
 */
class UpdateViewModelLogicTest {
    @Test
    fun manualCheckAlwaysResponds() {
        // "稍后提醒"只抑制本次运行的自动检查：手动检查同一版本仍要弹窗
        assertTrue(shouldShowUpdateDialog("1.1.0", "1.0.0", force = false, dismissedInSession = true, manual = true))
        assertFalse(shouldShowUpdateDialog("1.1.0", "1.0.0", force = false, dismissedInSession = true, manual = false))
        assertTrue(shouldShowUpdateDialog("1.1.0", "1.0.0", force = false, dismissedInSession = false, manual = false))
    }

    @Test
    fun forceAlwaysShows() {
        assertTrue(shouldShowUpdateDialog("1.1.0", "1.0.0", force = true, dismissedInSession = true, manual = false))
    }

    @Test
    fun noUpdateMeansNoDialog() {
        assertFalse(shouldShowUpdateDialog("1.0.0", "1.0.0", force = false, dismissedInSession = false, manual = true))
        assertFalse(shouldShowUpdateDialog("", "1.0.0", force = false, dismissedInSession = false, manual = true))
        assertTrue(compareVersions("1.0.0-debug", "1.0.0") == 0)
    }

    @Test
    fun compareVersions_大小与各段差异() {
        assertTrue(compareVersions("1.1.0", "1.0.0") > 0)
        assertTrue(compareVersions("1.0.0", "1.1.0") < 0)
        assertTrue(compareVersions("2.0.0", "1.9.9") > 0)
        assertTrue(compareVersions("1.10.0", "1.9.9") > 0)
        assertTrue(compareVersions("1.0.1", "1.0.0") > 0)
        assertEquals(0, compareVersions("1.0.0", "1.0.0"))
        // 忽略 v 前缀与 - 后缀
        assertEquals(0, compareVersions("v1.2.3", "1.2.3"))
        assertEquals(0, compareVersions("1.2.3-rc1", "1.2.3"))
        // 段数不等按 0 补齐
        assertEquals(0, compareVersions("1.0", "1.0.0"))
        assertTrue(compareVersions("1.0.1", "1.0") > 0)
        // 非法版本按 0 处理
        assertTrue(compareVersions("abc", "1.0.0") < 0)
        assertTrue(compareVersions("", "0.0.1") < 0)
    }
}
