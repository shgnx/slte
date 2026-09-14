package com.slte.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.slte.app.ui.screen.main.DashboardData
import com.slte.app.ui.screen.main.MainScreen
import com.slte.app.ui.screen.main.MainViewModel
import com.slte.app.ui.screen.order.OrdersScreen
import com.slte.app.ui.screen.order.OrdersViewModel
import com.slte.app.ui.screen.plans.PurchaseFlow
import com.slte.app.ui.screen.plans.PurchaseStep
import com.slte.app.ui.screen.plans.PurchaseViewModel

/** 订单页组合：订单列表 + 支付流程 + 支付等待轮询（LoggedInApp 页面分发的一部分） */
@Composable
internal fun OrdersPageContent(
    ordersViewModel: OrdersViewModel,
    purchaseViewModel: PurchaseViewModel,
    purchaseStep: PurchaseStep,
    onBack: () -> Unit,
    pendingPaymentTradeNo: String?,
    onPendingPaymentConsumed: () -> Unit,
) {
    OrdersScreen(
        onBack = onBack,
        onPay = { tradeNo -> purchaseViewModel.loadPaymentForOrder(tradeNo) },
        viewModel = ordersViewModel,
    )
    PurchaseFlow(
        step = purchaseStep,
        onSelectPeriod = {},
        onUpdateCoupon = {},
        onVerifyCoupon = {},
        onConfirmOrder = {},
        onCancelWarning = {},
        onConfirmWarning = {},
        onSelectPayment = purchaseViewModel::selectPaymentMethod,
        onConfirmPayment = purchaseViewModel::confirmPayment,
        onPaymentReturn = {
            purchaseViewModel.onPaymentReturn()
            ordersViewModel.refresh()
        },
        onDismiss = purchaseViewModel::goBack,
    )
    // 支付等待轮询（轮询逻辑在 PurchaseViewModel 内，UI 只负责启动与副作用编排）
    val payingTradeNo = (purchaseStep as? PurchaseStep.OrderPayment)?.tradeNo
    LaunchedEffect(payingTradeNo) {
        if (payingTradeNo != null) purchaseViewModel.startOrderPolling(payingTradeNo)
    }
    pendingPaymentTradeNo?.let { tradeNo ->
        LaunchedEffect(tradeNo) {
            purchaseViewModel.loadPaymentForOrder(tradeNo)
            onPendingPaymentConsumed()
        }
    }
}

/** 首页组合（LoggedInApp 页面分发的一部分） */
@Composable
internal fun DashboardPageContent(
    mainViewModel: MainViewModel,
    mainData: DashboardData,
    onInvite: () -> Unit,
    onServer: () -> Unit,
    onNotice: () -> Unit,
    onSupport: () -> Unit,
    onProfile: () -> Unit,
    onRenew: () -> Unit,
) {
    MainScreen(
        mainViewModel = mainViewModel,
        data = mainData,
        onInvite = onInvite,
        onServer = onServer,
        onNotice = onNotice,
        onSupport = onSupport,
        onProfile = onProfile,
        onRenew = onRenew,
    )
}
