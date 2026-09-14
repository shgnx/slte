package com.slte.app.ui.screen.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.domain.model.PlanInfo
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.component.SlteInput
import com.slte.app.ui.component.SlteInputSize
import com.slte.app.ui.component.SlteSheet
import com.slte.app.ui.component.formatCurrency
import com.slte.app.ui.component.formatNegCurrency
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectPeriodSheet(
    step: PurchaseStep.SelectPeriod,
    onSelectPeriod: (String) -> Unit,
    onUpdateCoupon: (String) -> Unit,
    onVerifyCoupon: () -> Unit,
    onConfirmOrder: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    SlteSheet(
        onDismiss = onDismiss,
        title = "${stringResource(R.string.plans_subscribe)} - ${step.plan.name}",
    ) {
        PeriodGrid(
            periods = step.plan.periodPrices,
            selectedPeriod = step.selectedPeriod,
            onSelect = onSelectPeriod,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.lg))

        CouponInput(
            code = step.couponCode,
            onCodeChange = onUpdateCoupon,
            onVerify = onVerifyCoupon,
            isVerifying = step.isVerifying,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.lg))

        PriceRow(
            label = stringResource(R.string.order_price),
            value = formatCurrency(step.priceCents),
        )
        PriceRow(
            label = stringResource(R.string.purchase_coupon_discount),
            value =
            if (step.couponDiscount > 0) {
                formatNegCurrency(step.couponDiscount)
            } else {
                FormatUtils.balance(step.couponDiscount)
            },
        )

        Spacer(modifier = Modifier.height(Dimens.gap.lg))

        val canConfirm = step.couponCode.isBlank() || step.couponVerified
        androidx.compose.material3.Surface(
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                if (canConfirm) onConfirmOrder()
            },
            modifier =
            Modifier
                .fillMaxWidth()
                .height(Dimens.size.button),
            shape = SlteShapes.medium,
            color =
            MaterialTheme.colorScheme.primary.copy(
                alpha = if (canConfirm) 1f else Dimens.disabledAlpha,
            ),
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${stringResource(R.string.purchase_confirm_order)} ${formatCurrency(step.finalPrice)}",
                    style = SlteType.title,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * 周期选择：2x2 网格卡片布局，全部周期展示（超出 4 个自动换行）。
 * 选中为主色填充（蓝）+ 白字，未选为浅灰填充 + 深字。
 */
@Composable
internal fun PeriodGrid(
    periods: List<PlanInfo.PeriodPrice>,
    selectedPeriod: String,
    onSelect: (String) -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val rows = periods.chunked(2)

    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.gap.md),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.gap.md),
            ) {
                row.forEach { pp ->
                    val selected = selectedPeriod == pp.period
                    val price = pp.price.toLongOrNull()?.toInt() ?: 0
                    androidx.compose.material3.Surface(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onSelect(pp.period)
                        },
                        modifier = Modifier.weight(1f),
                        shape = SlteShapes.medium,
                        color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor =
                        if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    ) {
                        Column(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.periodGridItemPaddingV),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = FormatUtils.periodLabel(pp.period, LocalContext.current),
                                style = SlteType.title,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            )
                            Spacer(modifier = Modifier.height(Dimens.gap.sm))
                            Text(
                                text = formatCurrency(price),
                                style = SlteType.title,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun CouponInput(
    code: String,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    isVerifying: Boolean,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    SlteInput(
        value = code,
        onValueChange = onCodeChange,
        placeholder = stringResource(R.string.purchase_coupon_hint),
        icon = SlteIcons.Coupon,
        iconDesc = stringResource(R.string.purchase_coupon_hint),
        trailing = {
            Spacer(modifier = Modifier.width(Dimens.gap.sm))
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onVerify()
                },
                enabled = code.isNotBlank(),
            ) {
                if (isVerifying) {
                    LottieLoadingIcon(modifier = Modifier.size(Dimens.icon.lg))
                } else {
                    Text(
                        text = stringResource(R.string.purchase_verify),
                        style = SlteType.body.copy(fontWeight = FontWeight.Medium),
                    )
                }
            }
        },
        size = SlteInputSize.Compact,
    )
}
