package com.slte.app.ui.screen.plans

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.domain.model.PlanInfo
import com.slte.app.ui.ContentPhase
import com.slte.app.ui.component.CircleIconButton
import com.slte.app.ui.component.EmptyState
import com.slte.app.ui.component.ErrorState
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.component.RichText
import com.slte.app.ui.component.SlteButton
import com.slte.app.ui.component.SlteButtonStyle
import com.slte.app.ui.component.SlteCard
import com.slte.app.ui.component.SltePullRefresh
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.component.SubmitTipHost
import com.slte.app.ui.component.formatCurrency
import com.slte.app.ui.screen.giftcard.GiftCardRedeemSheet
import com.slte.app.ui.screen.giftcard.GiftCardRedeemViewModel
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

@Composable
fun PlansScreen(
    onBack: () -> Unit,
    onGoToOrders: () -> Unit = {},
    viewModel: PlansViewModel = hiltViewModel(),
    purchaseViewModel: PurchaseViewModel = hiltViewModel(),
    giftCardViewModel: GiftCardRedeemViewModel = hiltViewModel(),
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val purchaseStep by purchaseViewModel.step.collectAsStateWithLifecycle()
    val giftCardState by giftCardViewModel.state.collectAsStateWithLifecycle()
    val giftCardTip by giftCardViewModel.tip.collectAsStateWithLifecycle()

    SlteScaffold(
        title = stringResource(R.string.plans_title),
        onBack = onBack,
        actions = {
            CircleIconButton(
                icon = SlteIcons.GiftCard,
                description = stringResource(R.string.gift_card_title),
                onClick = giftCardViewModel::open,
            )
        },
    ) { innerPadding ->
        val errorRes = data.errorMessageRes
        when {
            data.phase == ContentPhase.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LottieLoadingIcon(modifier = Modifier.size(Dimens.icon.lg))
                }
            }
            errorRes != null && data.plans.isEmpty() -> {
                ErrorState(
                    message = stringResource(errorRes),
                    onRetry = viewModel::retry,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            data.plans.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.plan_empty),
                    modifier = Modifier.padding(innerPadding),
                )
            }
            else -> {
                SltePullRefresh(
                    isRefreshing = data.phase == ContentPhase.Refreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.padding(innerPadding),
                ) {
                    LazyColumn(
                        modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = Dimens.dashboardScreenPaddingH),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dashboardCardSpacing),
                        contentPadding = PaddingValues(vertical = Dimens.dashboardScreenPaddingV),
                    ) {
                        items(data.plans.distinctBy { it.id }, key = { it.id }) { plan ->
                            PlanCard(
                                plan = plan,
                                onSubscribe = { purchaseViewModel.startPurchase(plan) },
                            )
                        }
                    }
                }
            }
        }
    }

    PurchaseFlow(
        step = purchaseStep,
        onSelectPeriod = purchaseViewModel::selectPeriod,
        onUpdateCoupon = purchaseViewModel::updateCouponCode,
        onVerifyCoupon = purchaseViewModel::verifyCoupon,
        onConfirmOrder = purchaseViewModel::showConfirmWarning,
        onCancelWarning = purchaseViewModel::cancelWarning,
        onConfirmWarning = purchaseViewModel::confirmWarning,
        onSelectPayment = purchaseViewModel::selectPaymentMethod,
        onConfirmPayment = purchaseViewModel::confirmPayment,
        onPaymentReturn = purchaseViewModel::onPaymentReturn,
        onDismiss = purchaseViewModel::goBack,
        onGoToOrders = onGoToOrders,
    )

    if (giftCardState.visible) {
        GiftCardRedeemSheet(
            state = giftCardState,
            onCodeChange = giftCardViewModel::updateCode,
            onSubmit = giftCardViewModel::submit,
            onDismiss = giftCardViewModel::dismiss,
        )
    }

    SubmitTipHost(tip = giftCardTip, onTipShown = giftCardViewModel::clearTip)
}

@Composable
private fun PlanCard(
    plan: PlanInfo,
    onSubscribe: () -> Unit,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val firstPrice = plan.periodPrices.firstOrNull()

    SlteCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(Dimens.gap.lg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = plan.name,
                    style = SlteType.title,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (firstPrice != null) {
                    Text(
                        text = formatCurrency(firstPrice.price.toLongOrNull()?.toInt() ?: 0),
                        style = SlteType.title,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.gap.xs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${plan.transferEnable}${stringResource(R.string.plans_traffic_unit)}${stringResource(R.string.plans_traffic_label)}",
                    style = SlteType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (firstPrice != null) {
                    Text(
                        text = " ${stringResource(R.string.plan_separator)} ${FormatUtils.periodLabel(firstPrice.period, context)}",
                        style = SlteType.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!plan.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.gap.md))
                RichText(
                    text = plan.content,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(Dimens.gap.lg))

            SlteButton(
                text = stringResource(R.string.plans_subscribe),
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth(),
                style = SlteButtonStyle.Primary,
            )
        }
    }
}
