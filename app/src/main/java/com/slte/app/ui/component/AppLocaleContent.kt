package com.slte.app.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import com.slte.app.data.local.LocaleStore
import com.slte.app.utils.LocaleContextWrapper
import java.util.Locale

/**
 * 当前界面语言（null = 跟随系统）。
 *
 * 弹窗/Sheet 运行在独立窗口组合中，LocalContext 被 Dialog 覆盖为窗口 Context，
 * 根组件注入的 LocalContext 不会传入；此 local 供弹窗内容读取语言值，
 * 语言变化时产生重组依赖，配合 [AppLocaleContent] 实现弹窗内原地刷新。
 */
val LocalAppLocale = staticCompositionLocalOf<Locale?> { null }

/** 语言偏好存储：由根组件注入，弹窗内容经组合继承读取，供实时解析语言 */
val LocalLocaleStore = staticCompositionLocalOf<LocaleStore?> { null }

/**
 * 应用语言内容容器：以 locale 为 key 重建语言上下文并注入 LocalContext。
 *
 * 语言切换时生成新的 LocaleContextWrapper，LocalContext 值变化触发当前组合全树原地重组，
 * 导航栈、tab、滚动位置均不重置，无需重建 Activity。弹窗/Sheet 内容需在此容器内渲染，
 * 否则独立窗口组合仍使用创建时刻的窗口 Context，文案不随语言切换刷新。
 *
 * @param locale 当前语言（null = 跟随系统），同时作为重建 wrapper 的 key
 * @param localeStore 语言偏好存储；为 null 时从 [LocalLocaleStore] 读取（弹窗内使用），
 *        包装器按此实时解析语言，使已创建的窗口 Context 也能随切换刷新
 */
@Composable
fun AppLocaleContent(
    locale: Locale?,
    localeStore: LocaleStore? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val store = localeStore ?: LocalLocaleStore.current
    val localeContext =
        remember(locale) {
            LocaleContextWrapper(context) { store?.locale?.value ?: locale }
        }
    CompositionLocalProvider(
        LocalContext provides localeContext,
        LocalAppLocale provides locale,
        LocalLocaleStore provides store,
    ) {
        content()
    }
}

/**
 * 语言感知的 AlertDialog：在 title/text/按钮 各内容作用域内注入 [AppLocaleContent]。
 *
 * AlertDialog 的每个内容参数是独立组合作用域，且 LocalContext 被 Dialog 覆盖为窗口 Context；
 * 逐作用域按语言重建后，语言切换时各作用域原地重组、文案即时刷新。
 * 参数与 Material3 AlertDialog 一致，仅确认按钮必填。
 */
@Composable
fun LocaleAwareAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            AppLocaleContent(locale = LocalAppLocale.current) { confirmButton() }
        },
        modifier = modifier,
        dismissButton =
        dismissButton?.let { d ->
            { AppLocaleContent(locale = LocalAppLocale.current) { d() } }
        },
        icon =
        icon?.let { i ->
            { AppLocaleContent(locale = LocalAppLocale.current) { i() } }
        },
        title =
        title?.let { t ->
            { AppLocaleContent(locale = LocalAppLocale.current) { t() } }
        },
        text =
        text?.let { t ->
            { AppLocaleContent(locale = LocalAppLocale.current) { t() } }
        },
        shape = shape,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = properties,
    )
}
