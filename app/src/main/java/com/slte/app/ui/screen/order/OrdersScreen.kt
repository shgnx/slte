package com.slte.app.ui.screen.order

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.domain.model.OrderInfo
import com.slte.app.domain.model.OrderStatus
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.component.formatCurrency
import com.slte.app.ui.component.EmptyState
import com.slte.app.ui.component.SltePullRefresh
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

/**
 * 订单列表页面（二级页面）。
 */
@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    onPay: (String) -> Unit = {},
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val errorMessageRes by viewModel.errorMessageRes.collectAsStateWithLifecycle()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    com.slte.app.ui.component.ToastTip(
        messageRes = data.toastRes,
        onDismiss = viewModel::clearToast
    )

    SlteScaffold(
        title = stringResource(R.string.profile_orders),
        onBack = onBack
    ) { innerPadding ->
        when {
            data.isLoading && data.orders.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    com.slte.app.ui.component.LoadingBox()
                }
            }
            errorMessageRes != null && data.orders.isEmpty() -> {
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
                            text = stringResource(errorMessageRes!!),
                            fontSize = TextSizes.actionTitle,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Dimens.spacingLg))
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                viewModel.retry()
                            }
                        ) {
                            Text(stringResource(R.string.notice_retry))
                        }
                    }
                }
            }
            data.orders.isEmpty() -> {
                EmptyState(
                    title = stringResource(R.string.order_empty),
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
                        items(data.orders.distinctBy { it.tradeNo }, key = { it.tradeNo }) { order ->
                            OrderCard(
                                order = order,
                                onCancel = { viewModel.cancelOrder(order.tradeNo) },
                                onPay = { onPay(order.tradeNo) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OrderInfo,
    onCancel: () -> Unit,
    onPay: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val isPending = order.statusClass == OrderStatus.PENDING

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
                    text = order.planName.ifBlank { stringResource(R.string.order_unknown_plan) },
                    fontSize = TextSizes.actionTitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                OrderStatusChip(status = order.statusClass)
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            HorizontalDivider(
                thickness = Dimens.dividerThickness,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMd))

            OrderDetailRow(
                label = stringResource(R.string.order_price),
                value = formatCurrency(order.totalAmount)
            )
            Spacer(modifier = Modifier.height(Dimens.spacingSm))
            OrderDetailRow(
                label = stringResource(R.string.order_created),
                value = FormatUtils.formatDate(order.createdAt)
            )
            Spacer(modifier = Modifier.height(Dimens.spacingSm))
            OrderDetailRow(
                label = stringResource(R.string.order_id),
                value = order.tradeNo
            )

            if (isPending) {
                Spacer(modifier = Modifier.height(Dimens.spacingLg))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onCancel()
                        },
                        modifier = Modifier.weight(1f),
                        shape = SlteShapes.medium
                    ) {
                        Text(stringResource(R.string.order_cancel))
                    }
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            onPay()
                        },
                        modifier = Modifier.weight(1f),
                        shape = SlteShapes.medium
                    ) {
                        Text(stringResource(R.string.order_pay))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = TextSizes.dashboardListDesc,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = TextSizes.dashboardListDesc,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun OrderStatusChip(status: OrderStatus) {
    val (text, icon, fg, bg) = when (status) {
        OrderStatus.PENDING -> StatusStyle(
            stringResource(R.string.order_status_pending),
            Icons.Outlined.Schedule,
            SlteColors.current.statusWarning,
            SlteColors.current.statusWarning.copy(alpha = Dimens.iconBgAlphaMedium)
        )
        OrderStatus.COMPLETED -> StatusStyle(
            stringResource(R.string.order_status_completed),
            Icons.Outlined.CheckCircle,
            SlteColors.current.statusSuccess,
            SlteColors.current.statusSuccess.copy(alpha = Dimens.iconBgAlphaMedium)
        )
        OrderStatus.CANCELLED -> StatusStyle(
            stringResource(R.string.order_status_cancelled),
            Icons.Outlined.Cancel,
            SlteColors.current.statusNeutral,
            SlteColors.current.statusNeutral.copy(alpha = Dimens.iconBgAlphaMedium)
        )
        OrderStatus.ABNORMAL -> StatusStyle(
            stringResource(R.string.order_status_abnormal),
            Icons.Outlined.ErrorOutline,
            SlteColors.current.statusDanger,
            SlteColors.current.statusDanger.copy(alpha = Dimens.iconBgAlphaMedium)
        )
    }

    Surface(
        shape = SlteShapes.medium,
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Dimens.spacingSm,
                vertical = Dimens.spacingXs
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.paymentMethodDotSize),
                tint = fg
            )
            Spacer(modifier = Modifier.width(Dimens.spacingXs))
            Text(
                text = text,
                fontSize = TextSizes.inviteStatLabel,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

private data class StatusStyle(
    val text: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val fg: Color,
    val bg: Color
)
