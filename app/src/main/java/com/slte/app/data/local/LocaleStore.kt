package com.slte.app.data.local

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import com.slte.app.utils.LocaleContextWrapper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 界面语言偏好：设置页选择写入，根组件读取驱动全树热切换；null = 跟随系统 */
@Singleton
class LocaleStore
@Inject
constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

    private val _locale = MutableStateFlow(readTag(context)?.let { Locale.forLanguageTag(it) })

    /** 当前界面语言；null 表示跟随系统 */
    val locale: StateFlow<Locale?> = _locale.asStateFlow()

    /** 设置界面语言（null = 跟随系统）并持久化 */
    fun setLocale(locale: Locale?) {
        prefs.edit { putString(KEY_LOCALE, locale?.toLanguageTag()) }
        _locale.value = locale
    }

    companion object {
        private const val PREFS_NAME = "slte_locale"
        private const val KEY_LOCALE = "locale"

        /** attachBaseContext 阶段读取持久化语言标签（Hilt 未就绪）；null 表示跟随系统 */
        internal fun readTag(context: Context): String? = context
            .getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getString(KEY_LOCALE, null)
            ?.takeIf { it.isNotEmpty() }

        /**
         * 按语言偏好包装 base context（attachBaseContext 阶段使用，Hilt 未就绪）。
         *
         * Application 与 Activity 的 base 由系统分别创建，须各自包装；包装后
         * 经 applicationContext 取资源的组件（如 Crisp 客服 SDK）也随之渲染对应语言。
         * wrapper 每次取资源时实时读取偏好，运行时切换语言后新建的页面立即生效。
         */
        internal fun wrapBase(base: Context): Context = LocaleContextWrapper(base) {
            readTag(base)?.let { tag -> Locale.forLanguageTag(tag) }
        }
    }
}
