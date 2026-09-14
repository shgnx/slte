package com.slte.app

import com.slte.app.ui.screen.settings.LanguageMode
import com.slte.app.utils.resolveEffectiveLocale
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 有效语言解析测试：显式选择优先于系统语言，未支持语言回退英文。
 */
class LocaleResolutionTest {
    @Test
    fun `显式选择优先于系统语言`() {
        assertEquals(Locale.SIMPLIFIED_CHINESE, resolveEffectiveLocale(Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE))
        assertEquals(Locale.TRADITIONAL_CHINESE, resolveEffectiveLocale(Locale.ENGLISH, Locale.TRADITIONAL_CHINESE))
        assertEquals(Locale.ENGLISH, resolveEffectiveLocale(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH))
    }

    @Test
    fun `跟随系统命中支持语言`() {
        assertEquals(Locale.ENGLISH, resolveEffectiveLocale(Locale.ENGLISH, null))
        assertEquals(Locale.ENGLISH, resolveEffectiveLocale(Locale.forLanguageTag("en-US"), null))
        assertEquals(Locale.SIMPLIFIED_CHINESE, resolveEffectiveLocale(Locale.SIMPLIFIED_CHINESE, null))
        assertEquals(Locale.TRADITIONAL_CHINESE, resolveEffectiveLocale(Locale.TRADITIONAL_CHINESE, null))
        assertEquals(Locale.TRADITIONAL_CHINESE, resolveEffectiveLocale(Locale.forLanguageTag("zh-HK"), null))
    }

    @Test
    fun `未支持系统语言回退英文`() {
        assertEquals(Locale.ENGLISH, resolveEffectiveLocale(Locale.JAPANESE, null))
        assertEquals(Locale.ENGLISH, resolveEffectiveLocale(Locale.FRENCH, null))
    }

    @Test
    fun `语言选项按存储值解析`() {
        assertEquals(LanguageMode.FOLLOW_SYSTEM, LanguageMode.fromLocale(null))
        assertEquals(LanguageMode.SIMPLIFIED, LanguageMode.fromLocale(Locale.SIMPLIFIED_CHINESE))
        assertEquals(LanguageMode.TRADITIONAL, LanguageMode.fromLocale(Locale.TRADITIONAL_CHINESE))
        assertEquals(LanguageMode.ENGLISH, LanguageMode.fromLocale(Locale.ENGLISH))
        assertEquals(LanguageMode.ENGLISH, LanguageMode.fromLocale(Locale.forLanguageTag("en-GB")))
        assertEquals(LanguageMode.FOLLOW_SYSTEM, LanguageMode.fromLocale(Locale.JAPANESE))
    }
}
