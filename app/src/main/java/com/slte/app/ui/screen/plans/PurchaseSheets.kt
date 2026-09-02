package com.slte.app.ui.screen.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.domain.model.PlanInfo
import com.slte.app.ui.component.AppLocaleContent
import com.slte.app.ui.component.formatNegCurrency
import com.slte.app.ui.component.formatCurrency
import com.slte.app.ui.component.LocalAppLocale
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils
import com.slte.app.ui.component.LottieLoadingIcon




@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectPeriodSheet(
    step: PurchaseStep.SelectPeriod,
    onSelectPeriod: (String) -> Unit,
    onUpdateCoupon: (String) -> Unit,
    onVerifyCoupon: () -> Unit,
    onConfirmOrder: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = SlteShapes.large
    ) {
        AppLocaleContent(locale = LocalAppLocale.current) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = Dimens.inviteSheetPaddingH,
                        vertical = Dimens.inviteSheetPaddingV
                    )
            ) {
            Text(
                text = "${stringResource(R.string.plans_subscribe)} - ${step.plan.name}",
                fontSize = TextSizes.sheetTitle,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            PeriodGrid(
                periods = step.plan.periodPrices,
                selectedPeriod = step.selectedPeriod,
                onSelect = onSelectPeriod
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            CouponInput(
                code = step.couponCode,
                onCodeChange = onUpdateCoupon,
                onVerify = onVerifyCoupon,
                isVerifying = step.isVerifying
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            PriceRow(
                label = stringResource(R.string.order_price),
                value = formatCurrency(step.priceCents)
            )
            PriceRow(
                label = stringResource(R.string.purchase_coupon_discount),
                value = if (step.couponDiscount > 0) formatNegCurrency(step.couponDiscount)
                        else FormatUtils.balance(step.couponDiscount)
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            val canConfirm = step.couponCode.isBlank() || step.couponVerified
            androidx.compose.material3.Surface(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    if (canConfirm) onConfirmOrder()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                shape = SlteShapes.extraLarge,
                color = MaterialTheme.colorScheme.primary.copy(
                    alpha = if (canConfirm) 1f else Dimens.disabledAlpha
                ),
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${stringResource(R.string.purchase_confirm_order)} ${formatCurrency(step.finalPrice)}",
                        fontSize = TextSizes.actionTitle,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXl))
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
    onSelect: (String) -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val rows = periods.chunked(2)

    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMd)
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
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.periodGridItemPaddingV),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = FormatUtils.periodLabel(pp.period, LocalContext.current),
                                fontSize = TextSizes.actionTitle,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(Dimens.spacingTiny))
                            Text(
                                text = formatCurrency(price),
                                fontSize = TextSizes.actionTitle,
                                fontWeight = FontWeight.SemiBold
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

/**
 * 优惠券输入区：描边容器 + 标签图标 + 输入框 + 验证按钮。
 * 验证结果通过 Toast 提示，不在输入框下方显示错误。
 */
@Composable
internal fun CouponInput(
    code: String,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    isVerifying: Boolean
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            Dimens.dividerThickness,
            MaterialTheme.colorScheme.outline
        ),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.spacingMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ConfirmationNumber,
                contentDescription = stringResource(R.string.purchase_coupon_hint),
                modifier = Modifier.size(Dimens.topBarActionIconSize),
                tint = SlteColors.current.iconBlue
            )
            Spacer(modifier = Modifier.width(Dimens.spacingSm))
            androidx.compose.foundation.text.BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = TextSizes.actionSubtitle,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                decorationBox = { innerTextField ->
                    if (code.isEmpty()) {
                        Text(
                            text = stringResource(R.string.purchase_coupon_hint),
                            fontSize = TextSizes.actionSubtitle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )
            Spacer(modifier = Modifier.width(Dimens.spacingSm))
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onVerify()
                },
                enabled = code.isNotBlank()
            ) {
                if (isVerifying) {
                    LottieLoadingIcon(modifier = Modifier.size(Dimens.spacingLg))
                } else {
                    Text(
                        text = stringResource(R.string.purchase_verify),
                        fontSize = TextSizes.actionSubtitle,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
