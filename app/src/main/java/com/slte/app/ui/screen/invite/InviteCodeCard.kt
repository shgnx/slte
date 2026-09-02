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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
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
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteColors
import com.slte.app.utils.Dimens
import com.slte.app.ui.theme.TextSizes

@Composable
fun InviteCodeCard(
    codes: List<InviteCodeInfo>,
    isGenerating: Boolean,
    onGenerate: () -> Unit,
    context: android.content.Context
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.inviteCodeCardPaddingH, vertical = Dimens.inviteCodeCardPaddingV)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.invite_code_title),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TextSizes.dashboardUsageTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onGenerate()
                }, enabled = !isGenerating) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(Dimens.inviteCodeCopyIconSize))
                    Spacer(modifier = Modifier.width(Dimens.spacingXs))
                    Text(stringResource(R.string.invite_code_generate), fontSize = TextSizes.inviteSheetMethod)
                }
            }

            if (codes.isEmpty()) {
                Text(
                    text = stringResource(R.string.invite_code_empty),
                    fontSize = TextSizes.inviteEmpty,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimens.spacingMd)
                )
            } else {
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                codes.forEach { code ->
                    InviteCodeItem(code = code, context = context)
                }
            }
        }
    }
}

@Composable
private fun InviteCodeItem(code: InviteCodeInfo, context: android.content.Context) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.inviteCodeItemHeight)
            .clip(SlteShapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = Dimens.inviteCodeItemBgAlpha))
            .padding(horizontal = Dimens.inviteCodeItemPaddingH),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code.code,
            fontWeight = FontWeight.Medium,
            fontSize = TextSizes.inviteCodeText,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.invite_code_pv, code.pv),
            fontSize = TextSizes.inviteRecordOrder,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(Dimens.spacingSm))
        IconButton(
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("invite_code", code.code))
                Toast.makeText(context, context.getString(R.string.invite_code_copied), Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(Dimens.inviteCodeCopyBtnSize)
        ) {
            Icon(
                Icons.Rounded.ContentCopy,
                contentDescription = stringResource(R.string.invite_code_copy),
                modifier = Modifier.size(Dimens.inviteCodeCopyIconSize),
                tint = SlteColors.current.iconBlue
            )
        }
    }
    Spacer(modifier = Modifier.height(Dimens.spacingSm))
}
