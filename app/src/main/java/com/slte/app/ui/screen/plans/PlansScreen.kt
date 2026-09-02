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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.domain.model.PlanInfo
import com.slte.app.ui.component.RichText
import com.slte.app.ui.component.formatCurrency
import com.slte.app.ui.component.EmptyState
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.component.SltePullRefresh
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

/** 订阅页面：套餐卡片列表（名称/流量/周期/描述/订阅按钮）。 */
@Composable
fun PlansScreen(
    onBack: () -> Unit,
    onGoToOrders: () -> Unit = {},
    viewModel: PlansViewModel = hiltViewModel(),
    purchaseViewModel: PurchaseViewModel = hiltViewModel()
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val purchaseStep by purchaseViewModel.step.collectAsStateWithLifecycle()

    SlteScaffold(
        title = stringResource(R.string.plans_title),
        onBack = onBack
    ) { innerPadding ->
        when {
            data.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LottieLoadingIcon(modifier = Modifier.size(Dimens.loadingIndicatorSize))
                }
            }
            data.errorMessageRes != null && data.plans.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.errorIconSize),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(Dimens.spacingMd))
                        Text(
                            text = stringResource(data.errorMessageRes!!),
                            fontSize = TextSizes.dashboardListLabel,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Dimens.spacingLg))
                        OutlinedButton(
                            onClick = viewModel::retry
                        ) {
                            Text(stringResource(R.string.notice_retry))
                        }
                    }
                }
            }
            data.plans.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.plan_empty),
                    modifier = Modifier.padding(innerPadding)
                )
            }
            else -> {
                SltePullRefresh(
                    isRefreshing = data.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Dimens.dashboardScreenPaddingH),
                        verticalArrangement = Arrangement.spacedBy(Dimens.dashboardCardSpacing),
                        contentPadding = PaddingValues(vertical = Dimens.dashboardScreenPaddingV)
                    ) {
                        items(data.plans.distinctBy { it.id }, key = { it.id }) { plan ->
                            PlanCard(
                                plan = plan,
                                onSubscribe = { purchaseViewModel.startPurchase(plan) }
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
        onGoToOrders = onGoToOrders
    )
}

/** 套餐卡片。 */
@Composable
private fun PlanCard(
    plan: PlanInfo,
    onSubscribe: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val firstPrice = plan.periodPrices.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.cardContentPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.name,
                    fontSize = TextSizes.planName,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (firstPrice != null) {
                    Text(
                        text = formatCurrency(firstPrice.price.toLongOrNull()?.toInt() ?: 0),
                        fontSize = TextSizes.planName,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${plan.transferEnable}${stringResource(R.string.plans_traffic_unit)}${stringResource(R.string.plans_traffic_label)}",
                    fontSize = TextSizes.actionSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (firstPrice != null) {
                    Text(
                        text = " ${stringResource(R.string.plan_separator)} ${FormatUtils.periodLabel(firstPrice.period, context)}",
                        fontSize = TextSizes.actionSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!plan.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(Dimens.spacingMd))
                RichText(
                    text = plan.content,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingLg))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onSubscribe()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.buttonHeight),
                shape = SlteShapes.medium
            ) {
                Text(
                    text = stringResource(R.string.plans_subscribe),
                    fontSize = TextSizes.planButton
                )
            }
        }
    }
}
