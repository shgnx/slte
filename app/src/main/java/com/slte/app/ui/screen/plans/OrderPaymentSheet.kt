package com.slte.app.ui.screen.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.domain.model.PaymentMethod
import com.slte.app.ui.component.AppLocaleContent
import com.slte.app.ui.component.formatNegCurrency
import com.slte.app.ui.component.formatCurrency
import com.slte.app.ui.component.LocalAppLocale
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils
import com.slte.app.ui.component.LottieLoadingIcon




@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OrderPaymentSheet(
    step: PurchaseStep.OrderPayment,
    onSelectPayment: (Int) -> Unit,
    onConfirmPayment: () -> Unit,
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
                    .padding(
                        horizontal = Dimens.inviteSheetPaddingH,
                        vertical = Dimens.inviteSheetPaddingV
                    )
            ) {
            Text(
                text = stringResource(R.string.purchase_order_info),
                fontSize = TextSizes.sheetTitle,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(Dimens.spacingXl))

            if (step.isLoading) {
                LottieLoadingIcon(
                    modifier = Modifier.align(Alignment.CenterHorizontally).size(Dimens.loadingIndicatorSize)
                )
                Spacer(modifier = Modifier.height(Dimens.spacingXl))
            } else {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SlteShapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Dimens.cardContentPadding,
                                vertical = Dimens.spacingMd
                            )
                    ) {
                        OrderInfoRow(
                            label = stringResource(R.string.purchase_product),
                            value = step.planName,
                            isValueEmphasize = true
                        )
                        OrderInfoDivider()
                        OrderInfoRow(
                            label = stringResource(R.string.order_price),
                            value = formatCurrency(step.totalAmount)
                        )
                        OrderInfoDivider()
                        OrderInfoRow(
                            label = stringResource(R.string.purchase_balance),
                            value = formatNegCurrency(step.balanceAmount)
                        )
                        OrderInfoDivider()
                        OrderInfoRow(
                            label = stringResource(R.string.purchase_coupon_discount),
                            value = formatNegCurrency(step.couponDiscount)
                        )
                        OrderInfoDivider()
                        OrderInfoRow(
                            label = stringResource(R.string.purchase_handling),
                            value = formatCurrency(step.handlingAmount)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingLg))

                Text(
                    text = stringResource(R.string.purchase_payment_method),
                    fontSize = TextSizes.actionSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                if (step.paymentMethods.isEmpty()) {
                    Text(
                        text = stringResource(R.string.purchase_payment_method_empty),
                        fontSize = TextSizes.actionSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    PaymentMethodList(
                        methods = step.paymentMethods,
                        selectedId = step.selectedMethod,
                        onSelect = onSelectPayment
                    )
                }

                Spacer(modifier = Modifier.height(Dimens.spacingXl))

                val payAmount = step.payAmount

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.buttonHeight),
                        shape = SlteShapes.extraLarge
                    ) {
                        Text(
                            text = stringResource(R.string.back),
                            fontSize = TextSizes.actionSubtitle,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    androidx.compose.material3.Surface(
                        onClick = {
                            if (step.selectedMethod != null && !step.isPaying) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onConfirmPayment()
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .height(Dimens.buttonHeight),
                        shape = SlteShapes.extraLarge,
                        color = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (step.selectedMethod != null) 1f else Dimens.disabledAlpha
                        ),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            if (step.isPaying) {
                                LottieLoadingIcon(modifier = Modifier.size(Dimens.topBarActionIconSize))
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.purchase_pay_amount,
                                        FormatUtils.balance(payAmount)
                                    ),
                                    fontSize = TextSizes.actionTitle,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingXl))
                }
            }
        }
    }
}

/**
 * 支付方式列表：卡片式选择，选中为深色填充，未选为浅灰填充。
 */
@Composable
internal fun PaymentMethodList(
    methods: List<PaymentMethod>,
    selectedId: Int?,
    onSelect: (Int) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
    ) {
        methods.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
            ) {
                row.forEach { method ->
                    PaymentMethodCell(
                        method = method,
                        selected = selectedId == method.id,
                        onClick = { onSelect(method.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** 支付方式单元格：单选圆点 + 名称，两列网格布局 */
@Composable
private fun PaymentMethodCell(
    method: PaymentMethod,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    androidx.compose.material3.Surface(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.height(Dimens.paymentMethodCellHeight),
        shape = SlteShapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.cardContentPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dotColor = if (selected) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.paymentMethodDotAlpha)
            androidx.compose.foundation.Canvas(
                modifier = Modifier.size(Dimens.radioDotSize)
            ) {
                val radiusOuter = Dimens.radioDotInnerSize.toPx()
                val radiusInner = Dimens.radioDotGap.toPx()
                drawCircle(
                    color = dotColor,
                    radius = radiusOuter,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = if (selected) Dimens.strokeThick.toPx() else Dimens.strokeMedium.toPx()
                    )
                )
                if (selected) {
                    drawCircle(
                        color = dotColor,
                        radius = radiusInner
                    )
                }
            }
            Spacer(modifier = Modifier.width(Dimens.spacingMd))
            Text(
                text = method.name,
                fontSize = TextSizes.actionSubtitle,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
