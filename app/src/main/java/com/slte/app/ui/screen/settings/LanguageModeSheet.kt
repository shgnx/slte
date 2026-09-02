package com.slte.app.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.slte.app.R
import com.slte.app.ui.component.AppLocaleContent
import com.slte.app.ui.component.LocalAppLocale
import com.slte.app.ui.theme.TextSizes
import com.slte.app.ui.theme.SlteColors
import com.slte.app.utils.Dimens
import com.slte.app.utils.isTraditionalChinese
import java.util.Locale

/** 语言选项（顺序：跟随系统 / 简体中文 / 繁体中文 / English），locale=null 表示跟随系统 */
enum class LanguageMode(val locale: Locale?, val labelRes: Int, val descRes: Int?) {
    FOLLOW_SYSTEM(null, R.string.language_follow_system, R.string.language_follow_system_desc),
    SIMPLIFIED(Locale.SIMPLIFIED_CHINESE, R.string.language_simplified, null),
    TRADITIONAL(Locale.TRADITIONAL_CHINESE, R.string.language_traditional, null),
    ENGLISH(Locale.ENGLISH, R.string.language_english, null);

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageModeSheet(
    currentMode: LanguageMode,
    onDismiss: () -> Unit,
    onSelect: (LanguageMode) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AppLocaleContent(locale = LocalAppLocale.current) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimens.inviteSheetPaddingH,
                        vertical = Dimens.inviteSheetPaddingV
                    )
            ) {
                Text(
                    text = stringResource(R.string.settings_language),
                    fontSize = TextSizes.sheetTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Dimens.spacingLg))

                LanguageMode.entries.forEach { mode ->
                    val selected = currentMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                onClick = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    onSelect(mode)
                                }
                            )
                            .padding(vertical = Dimens.spacingSm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = SlteColors.current.iconBlue
                            )
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = Dimens.spacingSm)
                        ) {
                            Text(
                                text = stringResource(mode.labelRes),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = TextSizes.actionTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            mode.descRes?.let { descRes ->
                                Text(
                                    text = stringResource(descRes),
                                    fontSize = TextSizes.inviteSheetDesc,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingLg))
            }
        }
    }
}
