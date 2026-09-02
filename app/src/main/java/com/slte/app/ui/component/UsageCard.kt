package com.slte.app.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

/**
 * 套餐用量卡片（首页流量卡 / 个人中心订阅卡统一组件）。
 *
 * 结构固定、高度不变；数据刷新只替换文字。hasPlan 只影响胶囊态与按钮可用性。
 *
 * @param planName 套餐名；空串时显示"流量用量"占位标题
 * @param daysUntilExpired 剩余天数；**null 表示不限时套餐**（无到期时间），0 表示今天到期
 * @param actionText CTA 按钮文案（续费/续订/去购买）
 * @param actionEnabled CTA 是否可点击（无套餐时首页禁用）
 */
@Composable
fun UsageCard(
    planName: String,
    usedBytes: Long,
    totalBytes: Long,
    isValid: Boolean,
    hasPlan: Boolean,
    daysUntilExpired: Int?,
    /** 到期日期文本（如 YYYY-MM-DD）；null 时回退"X天后到期" */
    expiredAtDate: String? = null,
    modifier: Modifier = Modifier,
    actionText: String = stringResource(R.string.plan_renew_button),
    actionEnabled: Boolean = hasPlan,
    onAction: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val percent = if (totalBytes > 0L) {
        ((usedBytes.toFloat() / totalBytes.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0

    // 充能动画：从 0 动画到实际百分比（数据刷新时只动进度，不抖布局）
    var animTarget by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(durationMillis = 1000, easing = LinearEasing),
        label = "progress_anim"
    )
    LaunchedEffect(percent) {
        animTarget = percent / 100f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.cardContentPadding, vertical = Dimens.spacingMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = planName.ifBlank { stringResource(R.string.dashboard_usage_title) },
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TextSizes.planName,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                UsageBadge(isValid = isValid, hasPlan = hasPlan)
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${FormatUtils.traffic(usedBytes)} / ${FormatUtils.traffic(totalBytes)}",
                    fontWeight = FontWeight.Medium,
                    fontSize = TextSizes.dashboardUsageText,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.plan_percent, percent),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = TextSizes.planMeta,
                    color = if (hasPlan && !isValid) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.dashboardUsageBarHeight)
                    .clip(RoundedCornerShape(Dimens.dashboardUsageBarRadius)),
                color = if (hasPlan && !isValid) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (hasPlan) {
                        Text(
                            text = when {
                                // 不限时套餐：无到期时间，不显示"今天到期"
                                daysUntilExpired == null -> stringResource(R.string.plan_no_expiry)
                                daysUntilExpired > 0 && expiredAtDate != null ->
                                    stringResource(R.string.plan_expire_date_label, expiredAtDate)
                                daysUntilExpired > 0 -> stringResource(R.string.plan_expire_days_label, daysUntilExpired)
                                else -> stringResource(R.string.plan_expired_today)
                            },
                            fontWeight = FontWeight.Light,
                            fontSize = TextSizes.planMeta,
                            color = if (isValid) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.plan_empty),
                            fontWeight = FontWeight.Light,
                            fontSize = TextSizes.planMeta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAction()
                    },
                    enabled = actionEnabled,
                    shape = SlteShapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(
                        horizontal = Dimens.planRenewButtonPaddingH,
                        vertical = Dimens.planRenewButtonPaddingV
                    )
                ) {
                    Text(
                        text = actionText,
                        fontWeight = FontWeight.Medium,
                        fontSize = TextSizes.planButton
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageBadge(isValid: Boolean, hasPlan: Boolean = true) {
    val bg = when {
        !hasPlan -> MaterialTheme.colorScheme.surfaceVariant
        isValid -> SlteColors.current.iconGreenBg
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val fg = when {
        !hasPlan -> MaterialTheme.colorScheme.onSurfaceVariant
        isValid -> SlteColors.current.iconGreen
        else -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.planStatusChipCornerRadius))
            .background(bg)
            .padding(horizontal = Dimens.planStatusPaddingH, vertical = Dimens.planStatusPaddingV)
    ) {
        Text(
            text = when {
                !hasPlan -> stringResource(R.string.dashboard_no_plan_badge)
                isValid -> stringResource(R.string.dashboard_usage_valid)
                else -> stringResource(R.string.plan_status_expired)
            },
            fontWeight = FontWeight.SemiBold,
            fontSize = TextSizes.dashboardUsageBadge,
            color = fg
        )
    }
}
