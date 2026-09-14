package com.slte.app.ui.screen.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.slte.app.R
import com.slte.app.domain.model.InviteStat
import com.slte.app.ui.component.AnimatedSticker
import com.slte.app.ui.component.formatCurrency
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.Stickers

/**
 * 佣金概览卡片：
 *
 * 上半部分：TG 动画 + 可提现佣金大字
 * 下半部分：分割线 + 横向 4 列统计（已注册 / 佣金比例 / 累计 / 确认中）
 */
@Composable
fun InviteStatCard(stat: InviteStat) {
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
                .padding(horizontal = Dimens.gap.lg, vertical = Dimens.inviteStatCardPaddingV),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedSticker(
                assetPath = Stickers.INVITE,
                modifier = Modifier.size(Dimens.inviteStickerSize),
            )

            Spacer(modifier = Modifier.height(Dimens.gap.sm))

            Text(
                text = formatCurrency(stat.availableBalance),
                style = SlteType.display,
                fontWeight = FontWeight.Bold,
                color = SlteColors.current.accentInteractive,
            )
            Spacer(modifier = Modifier.height(Dimens.gap.xs))
            Text(
                text = stringResource(R.string.invite_stat_available),
                style = SlteType.caption,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Dimens.gap.lg))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = com.slte.app.utils.Dimens.dividerAlpha),
                thickness = Dimens.dividerThickness,
            )

            Spacer(modifier = Modifier.height(Dimens.gap.lg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatColumn(
                    label = stringResource(R.string.invite_stat_registered),
                    value =
                    pluralStringResource(
                        R.plurals.invite_users,
                        stat.registeredUsers,
                        stat.registeredUsers,
                    ),
                    modifier = Modifier.weight(1f),
                )
                StatColumn(
                    label = stringResource(R.string.invite_stat_commission_rate),
                    value = stringResource(R.string.invite_rate, stat.commissionRate),
                    modifier = Modifier.weight(1f),
                )
                StatColumn(
                    label = stringResource(R.string.invite_stat_total),
                    value = formatCurrency(stat.totalCommission),
                    modifier = Modifier.weight(1f),
                )
                StatColumn(
                    label = stringResource(R.string.invite_stat_pending),
                    value = formatCurrency(stat.pendingCommission),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * 统计列：值在上（统一色），标签在下（灰色小字）。
 * 与传统布局相反，突出数值本身，弱化标签。
 */
@Composable
private fun StatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = SlteType.body,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(Dimens.gap.xs))
        Text(
            text = label,
            style = SlteType.caption,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun InviteActionButton(
    icon: ImageVector,
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.height(Dimens.size.button),
        shape = SlteShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(Dimens.icon.md), tint = tint)
            Spacer(modifier = Modifier.width(Dimens.gap.sm))
            Text(text, fontWeight = FontWeight.SemiBold, style = SlteType.body, color = tint)
        }
    }
}
