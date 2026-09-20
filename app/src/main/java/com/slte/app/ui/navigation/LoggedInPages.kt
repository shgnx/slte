package com.slte.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.slte.app.ui.screen.about.AboutScreen
import com.slte.app.ui.screen.giftcard.GiftCardRedeemViewModel
import com.slte.app.ui.screen.invite.InviteScreen
import com.slte.app.ui.screen.invite.InviteViewModel
import com.slte.app.ui.screen.main.DashboardData
import com.slte.app.ui.screen.main.MainScreen
import com.slte.app.ui.screen.main.MainViewModel
import com.slte.app.ui.screen.notice.NoticeScreen
import com.slte.app.ui.screen.notice.NoticeViewModel
import com.slte.app.ui.screen.order.OrdersScreen
import com.slte.app.ui.screen.order.OrdersViewModel
import com.slte.app.ui.screen.plans.PlansScreen
import com.slte.app.ui.screen.plans.PlansViewModel
import com.slte.app.ui.screen.plans.PurchaseFlow
import com.slte.app.ui.screen.plans.PurchaseStep
import com.slte.app.ui.screen.plans.PurchaseViewModel
import com.slte.app.ui.screen.profile.ProfileScreen
import com.slte.app.ui.screen.profile.ProfileViewModel
import com.slte.app.ui.screen.server.ServerScreen
import com.slte.app.ui.screen.server.ServerViewModel
import com.slte.app.ui.screen.settings.SettingsScreen

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

@Composable
internal fun ProfilePageContent(
    profileViewModel: ProfileViewModel,
    giftCardViewModel: GiftCardRedeemViewModel,
    onBack: () -> Unit,
    onOrders: () -> Unit,
    onInvite: () -> Unit,
    onRenew: () -> Unit,
    onContact: () -> Unit,
    onSettings: () -> Unit,
    onAbout: () -> Unit,
) {
    LaunchedEffect(Unit) { profileViewModel.refresh() }
    ProfileScreen(
        onBack = onBack,
        onOrders = onOrders,
        onInvite = onInvite,
        onRenew = onRenew,
        onContact = onContact,
        onSettings = onSettings,
        onAbout = onAbout,
        onLogout = profileViewModel::logout,
        viewModel = profileViewModel,
        giftCardViewModel = giftCardViewModel,
    )
}

@Composable
internal fun ServerPageContent(
    serverViewModel: ServerViewModel,
    onUpdateSubscription: () -> Unit,
    onBack: () -> Unit,
) {
    LaunchedEffect(Unit) { serverViewModel.loadNodes() }
    ServerScreen(
        onBack = onBack,
        onUpdateSubscription = onUpdateSubscription,
        viewModel = serverViewModel,
    )
}

@Composable
internal fun InvitePageContent(
    inviteViewModel: InviteViewModel,
    onBack: () -> Unit,
) {
    InviteScreen(
        onBack = onBack,
        viewModel = inviteViewModel,
    )
}

@Composable
internal fun NoticePageContent(
    noticeViewModel: NoticeViewModel,
    onBack: () -> Unit,
) {
    NoticeScreen(
        onBack = onBack,
        viewModel = noticeViewModel,
    )
}

@Composable
internal fun PlansPageContent(
    plansViewModel: PlansViewModel,
    purchaseViewModel: PurchaseViewModel,
    giftCardViewModel: GiftCardRedeemViewModel,
    onBack: () -> Unit,
    onGoToOrders: () -> Unit,
) {
    PlansScreen(
        onBack = onBack,
        viewModel = plansViewModel,
        purchaseViewModel = purchaseViewModel,
        giftCardViewModel = giftCardViewModel,
        onGoToOrders = onGoToOrders,
    )
}

@Composable
internal fun SettingsPageContent(onBack: () -> Unit) {
    SettingsScreen(onBack = onBack)
}

@Composable
internal fun AboutPageContent(onBack: () -> Unit) {
    AboutScreen(onBack = onBack)
}
