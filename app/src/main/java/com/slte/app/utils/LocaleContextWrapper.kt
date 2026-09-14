package com.slte.app.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

/**
 * 按语言覆盖资源的 Context 包装。
 *
 * 通过 createConfigurationContext 以目标语言生成 Resources，使 stringResource 等
 * 资源读取命中对应语言目录；stored 为 null（跟随系统）时按系统语言解析，
 * 未支持的系统语言回退英文。语言变化时由调用方以 locale 为 key 重建实例，
 * LocalContext 值变化驱动全树原地重组，不重建 Activity。
 */
class LocaleContextWrapper(
    base: Context,
    private val storedLocaleProvider: () -> Locale?,
) : ContextWrapper(base) {
    private var effectiveLocale: Locale? = null
    private var cachedResources: Resources? = null

    override fun getResources(): Resources {
        val stored = storedLocaleProvider()
        val systemLocale = baseContext.resources.configuration.locales[0] ?: Locale.getDefault()
        val effective = resolveEffectiveLocale(systemLocale, stored)
        if (effective != effectiveLocale || cachedResources == null) {
            val config = Configuration(baseContext.resources.configuration)
            config.setLocales(LocaleList.forLanguageTags(effective.toLanguageTag()))
            cachedResources = createConfigurationContext(config).resources
            effectiveLocale = effective
        }
        return cachedResources ?: super.getResources()
    }
}

/** 解析实际生效语言：显式选择优先，跟随系统时未支持语言回退英文 */
internal fun resolveEffectiveLocale(
    systemLocale: Locale,
    storedLocale: Locale?,
): Locale = when {
    storedLocale != null -> storedLocale
    systemLocale.language == "zh" && isTraditionalChinese(systemLocale) -> Locale.TRADITIONAL_CHINESE
    systemLocale.language == "zh" -> Locale.SIMPLIFIED_CHINESE
    systemLocale.language == "en" -> Locale.ENGLISH
    else -> Locale.ENGLISH
}

/** 判断是否繁体中文：优先按 script，低版本系统按地区兜底 */
internal fun isTraditionalChinese(locale: Locale): Boolean = locale.language == "zh" &&
    (
        locale.script == "Hant" ||
            locale.country == "TW" ||
            locale.country == "HK" ||
            locale.country == "MO"
        )

/**
 * 从（可能被语言包装的）Context 链中解包出 Activity。
 *
 * LocalContext 被 LocaleContextWrapper 包装后不再是 Activity 本身，
 * 需要沿 baseContext 解包才能拿到宿主 Activity。
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
