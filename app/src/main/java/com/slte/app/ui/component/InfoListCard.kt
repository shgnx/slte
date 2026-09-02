package com.slte.app.ui.component

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.slte.app.R
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils
import com.slte.app.ui.theme.TextSizes

/** 信息列表卡片：到期/节点/模式/IP 四行；IP 行显示压缩后的真实出口 IP（IPv6 过长时省略中段），长按复制完整地址。 */
@Composable
fun InfoListCard(
    daysUntilExpired: Int?,
    serverName: String,
    proxyMode: String,
    currentIp: String,
    ipCountryCode: String? = null,
    onServerClick: () -> Unit,
    onProxyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            InfoRow(
                icon = Icons.Rounded.Schedule,
                iconTint = SlteColors.current.iconBlue,
                iconBg = SlteColors.current.iconBlueBg,
                label = stringResource(R.string.dashboard_expiry),
                value = if (daysUntilExpired != null) {
                    stringResource(R.string.dashboard_days, daysUntilExpired)
                } else {
                    stringResource(R.string.plan_no_expiry)
                },
                onClick = null
            )
            InfoRow(
                icon = Icons.Rounded.Storage,
                iconTint = SlteColors.current.iconBlue,
                iconBg = SlteColors.current.iconBlueBg,
                label = stringResource(R.string.action_server),
                value = serverName,
                onClick = onServerClick
            )
            InfoRow(
                icon = Icons.Rounded.Layers,
                iconTint = SlteColors.current.iconBlue,
                iconBg = SlteColors.current.iconBlueBg,
                label = stringResource(R.string.action_proxy_mode),
                // 模式标识是内核稳定值，展示时映射为当前语言文案
                value = proxyModeLabelRes(proxyMode)?.let { stringResource(it) } ?: proxyMode,
                onClick = onProxyClick
            )
            InfoRow(
                icon = Icons.Rounded.CheckCircle,
                iconTint = SlteColors.current.iconBlue,
                iconBg = SlteColors.current.iconBlueBg,
                label = stringResource(R.string.dashboard_current_ip),
                value = FormatUtils.compactIp(currentIp),
                leadingValue = ipCountryCode?.let { code ->
                    {
                        FlagPlaceholder(
                            countryCode = code,
                            size = Dimens.flagSizeSmall
                        )
                        Spacer(modifier = Modifier.width(Dimens.dashboardChevronGap))
                    }
                },
                onClick = null,
                isMonospace = true,
                onLongClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    clipboard?.setPrimaryClip(ClipData.newPlainText("exit_ip", currentIp))
                    Toast.makeText(
                        context,
                        context.getString(R.string.dashboard_ip_copied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
    iconTint: Color,
    iconBg: Color,
    label: String,
    value: String,
    leadingValue: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isMonospace: Boolean = false
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.dashboardListRowHeight)
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
                        }
                    )
                } else it
            }
            .padding(horizontal = Dimens.dashboardListRowPaddingH),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.dashboardIconBgSize)
                .clip(RoundedCornerShape(Dimens.dashboardIconBgRadius))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.dashboardIconSize),
                tint = iconTint
            )
        }

        Spacer(modifier = Modifier.width(Dimens.dashboardListRowGap))

        Text(
            text = label,
            fontWeight = FontWeight.Medium,
            fontSize = TextSizes.dashboardListLabel,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        leadingValue?.invoke()

        Text(
            text = value,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            fontWeight = if (isMonospace) FontWeight.Normal else FontWeight.Medium,
            fontSize = if (isMonospace) TextSizes.dashboardIpValue else TextSizes.dashboardListValue,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = Dimens.dashboardListValueMaxWidth)
        )

        if (onClick != null) {
            Spacer(modifier = Modifier.width(Dimens.dashboardChevronGap))
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(Dimens.dashboardChevronSize),
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
