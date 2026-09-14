package com.slte.app.ui.screen.invite

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.domain.model.InviteCodeInfo
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

@Composable
fun InviteCodeCard(
    codes: List<InviteCodeInfo>,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    context: android.content.Context,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.gap.lg, vertical = Dimens.gap.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.invite_code_title),
                    fontWeight = FontWeight.SemiBold,
                    style = SlteType.body,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onGenerate()
                }, enabled = !isGenerating) {
                    Icon(SlteIcons.Add, contentDescription = null, modifier = Modifier.size(Dimens.inviteCodeCopyIconSize))
                    Spacer(modifier = Modifier.width(Dimens.gap.xs))
                    Text(stringResource(R.string.invite_code_generate), style = SlteType.bodySmall)
                }
            }

            if (codes.isEmpty()) {
                Text(
                    text = stringResource(R.string.invite_code_empty),
                    style = SlteType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimens.gap.md),
                )
            } else {
                Spacer(modifier = Modifier.height(Dimens.gap.sm))
                codes.forEach { code ->
                    InviteCodeItem(code = code, context = context)
                }
            }
        }
    }
}

@Composable
private fun InviteCodeItem(
    code: InviteCodeInfo,
    context: android.content.Context,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(Dimens.inviteCodeItemHeight)
            .clip(SlteShapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Dimens.inviteCodeItemBgAlpha))
            .padding(horizontal = Dimens.inviteCodeItemPaddingH),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = code.code,
            fontWeight = FontWeight.Medium,
            style = SlteType.title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.invite_code_pv, code.pv),
            style = SlteType.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(Dimens.gap.sm))
        IconButton(
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("invite_code", code.code))
                Toast.makeText(context, context.getString(R.string.invite_code_copied), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(Dimens.inviteCodeCopyBtnSize),
        ) {
            Icon(
                SlteIcons.Copy,
                contentDescription = stringResource(R.string.invite_code_copy),
                modifier = Modifier.size(Dimens.inviteCodeCopyIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(Dimens.gap.sm))
}
