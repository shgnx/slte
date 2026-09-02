package com.slte.app.ui.screen.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
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
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens

/**
 * 检查更新底部弹窗。
 *
 * 强制更新：无"稍后提醒"按钮，禁止下滑/点遮罩关闭，只能立即更新；
 * 非强制更新：可关闭，立即更新（上）/ 稍后提醒（下）二选一。
 * 版本号以"当前 → 新版"展示；更新日志标题与内容优先远程下发，为空时回退本地文案。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSheet(
    currentVersion: String,
    state: UpdateUiState.Available,
    onDismiss: () -> Unit,
    onUpdateNow: () -> Unit,
    onLater: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { value ->
            if (state.force && value == SheetValue.Hidden) false else true
        }
    )
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = { if (!state.force) onDismiss() },
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
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(Dimens.actionIconBgSize),
                tint = SlteColors.current.iconBlue
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            Text(
                text = stringResource(
                    if (state.force) R.string.update_force_title else R.string.update_title
                ),
                fontSize = TextSizes.sheetTitle,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    text = "v$currentVersion",
                    fontSize = TextSizes.actionTitle,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Dimens.spacingMd))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.dashboardChevronSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(Dimens.spacingMd))
                Text(
                    text = "v${state.versionName}",
                    fontSize = TextSizes.sheetTitle,
                    fontWeight = FontWeight.Bold,
                    color = SlteColors.current.iconBlue
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            Text(
                text = state.changelogTitle ?: stringResource(R.string.update_changelog_title_default),
                fontSize = TextSizes.actionTitle,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            Text(
                text = state.changelog ?: stringResource(R.string.update_changelog_default),
                fontSize = TextSizes.inviteSheetDesc,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onUpdateNow()
                },
                enabled = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                shape = SlteShapes.medium,
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(stringResource(R.string.update_now))
            }

            if (!state.force) {
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onLater()
                    },
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeight),
                    shape = SlteShapes.medium
                ) {
                    Text(stringResource(R.string.update_later))
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingLg))
            }
        }
    }
}
