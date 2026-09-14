package com.slte.app.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.slte.app.R
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

/**
 * 信息列表卡片：到期/节点/模式/IP 四行。
 * IP 行显示压缩后的真实出口 IP（IPv6 过长时省略中段），长按复制完整地址。
 */
@Composable
fun InfoListCard(
    daysUntilExpired: Int?,
    serverName: String,
    proxyMode: String,
    currentIp: String,
    onServerClick: () -> Unit,
    onProxyClick: () -> Unit,
    modifier: Modifier = Modifier,
    ipCountryCode: String? = null,
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            InfoRow(
                icon = SlteIcons.Expiry,
                label = stringResource(R.string.dashboard_expiry),
                value =
                if (daysUntilExpired != null) {
                    pluralStringResource(R.plurals.dashboard_days, daysUntilExpired, daysUntilExpired)
                } else {
                    stringResource(R.string.plan_no_expiry)
                },
                onClick = null,
            )
            InfoRow(
                icon = SlteIcons.Server,
                label = stringResource(R.string.action_server),
                value = serverName,
                onClick = onServerClick,
            )
            InfoRow(
                icon = SlteIcons.ProxyMode,
                label = stringResource(R.string.action_proxy_mode),
                // 模式标识是内核稳定值，展示时映射为当前语言文案
                value = proxyModeLabelRes(proxyMode)?.let { stringResource(it) } ?: proxyMode,
                onClick = onProxyClick,
            )
            InfoRow(
                icon = SlteIcons.CurrentIp,
                label = stringResource(R.string.dashboard_current_ip),
                value = FormatUtils.compactIp(currentIp),
                leadingValue =
                ipCountryCode?.let { code ->
                    {
                        FlagPlaceholder(
                            countryCode = code,
                            size = Dimens.icon.sm,
                        )
                        Spacer(modifier = Modifier.width(Dimens.dashboardChevronGap))
                    }
                },
                onClick = null,
                isMonospace = true,
                onLongClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("exit_ip", currentIp))
                    Toast
                        .makeText(
                            context,
                            context.getString(R.string.dashboard_ip_copied),
                            Toast.LENGTH_SHORT,
                        ).show()
                },
            )
        }
    }
}

/**
 * 单行信息：[彩色圆角图标] [间距] [标题 weight=1] [前置图标?] [值] [间距] [箭头?]
 * onClick 点击回调；onLongClick 长按回调（独立于点击，长按同样带震感）
 */
@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    leadingValue: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isMonospace: Boolean = false,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(Dimens.size.row)
            .let {
                if (onClick != null || onLongClick != null) {
                    it.combinedClickable(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onClick?.invoke()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongClick?.invoke()
                        },
                    )
                } else {
                    it
                }
            }.padding(horizontal = Dimens.gap.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 首页信息行＝「展示·可操作」场景：蓝底衬 + 蓝图标（SlteIconBadge，规范见 SlteIcons）
        SlteIconBadge(icon = icon)

        Spacer(modifier = Modifier.width(Dimens.gap.md))

        Text(
            text = label,
            // 与右侧值同字号（14sp）、仅以字重区分，保持原版「标签:值」的轻主次关系
            style = SlteType.body.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        leadingValue?.invoke()

        Text(
            text = value,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (isMonospace) FontWeight.Normal else FontWeight.Medium,
            style = if (isMonospace) SlteType.bodySmall else SlteType.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = Dimens.dashboardListValueMaxWidth),
        )

        if (onClick != null) {
            Spacer(modifier = Modifier.width(Dimens.dashboardChevronGap))
            Icon(
                imageVector = SlteIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(Dimens.icon.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
