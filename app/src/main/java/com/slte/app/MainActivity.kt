package com.slte.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.svg.SvgDecoder
import com.slte.app.data.local.LocaleStore
import com.slte.app.data.local.ThemePreference
import com.slte.app.ui.navigation.SlteApp
import com.slte.app.ui.theme.SlteTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themePreference: ThemePreference

    override fun attachBaseContext(newBase: Context?) {
        // Activity 的 base 由 ActivityThread 独立创建，不继承 Application 的 base：
        // 必须在此按应用语言包装，独立组件（如 Crisp 客服页）经 Activity/Application
        // 取资源时才随应用语言渲染
        super.attachBaseContext(newBase?.let { LocaleStore.wrapBase(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            setSingletonImageLoaderFactory { context ->
                ImageLoader
                    .Builder(context)
                    .components { add(SvgDecoder.Factory()) }
                    .build()
            }
            val dark by themePreference.dark.collectAsStateWithLifecycle()
            SlteTheme(darkTheme = dark) {
                SlteApp()
            }
        }
    }
}
