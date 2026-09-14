package com.slte.app.ui.screen.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.component.SlteSheet
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.isTraditionalChinese
import java.util.Locale

/** 语言选项（顺序：跟随系统 / 简体中文 / 繁体中文 / English），locale=null 表示跟随系统 */
enum class LanguageMode(
    val locale: Locale?,
    val labelRes: Int,
) {
    FOLLOW_SYSTEM(null, R.string.language_follow_system),
    SIMPLIFIED(Locale.SIMPLIFIED_CHINESE, R.string.language_simplified),
    TRADITIONAL(Locale.TRADITIONAL_CHINESE, R.string.language_traditional),
    ENGLISH(Locale.ENGLISH, R.string.language_english),
    ;

    companion object {
        /** 按当前存储的语言解析展示选项；未知语言按跟随系统展示 */
        fun fromLocale(locale: Locale?): LanguageMode = when {
            locale == null -> FOLLOW_SYSTEM
            locale.language == "zh" && isTraditionalChinese(locale) -> TRADITIONAL
            locale.language == "zh" -> SIMPLIFIED
            locale.language == "en" -> ENGLISH
            else -> FOLLOW_SYSTEM
        }
    }
}

/** 语言选择弹窗（仅 UI 状态，选择即写入 LocaleStore，全树热切换立即生效） */
@Composable
internal fun LanguageModeSheet(
    currentMode: LanguageMode,
    onDismiss: () -> Unit,
    onSelect: (LanguageMode) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    SlteSheet(
        title = stringResource(R.string.settings_language),
        onDismiss = onDismiss,
    ) {
        LanguageMode.entries.forEach { mode ->
            val selected = currentMode == mode
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(mode)
                        },
                    ).padding(vertical = Dimens.gap.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors =
                    RadioButtonDefaults.colors(
                        selectedColor = SlteColors.current.accentInteractive,
                    ),
                )
                Text(
                    text = stringResource(mode.labelRes),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    style = SlteType.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = Dimens.gap.sm),
                )
            }
        }
    }
}
