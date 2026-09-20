package com.slte.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.ui.component.LoadingOverlay
import com.slte.app.utils.findActivity

@Composable
fun LoggedInApp(
    accountKey: String,
    onSupport: () -> Unit,
) {
    val context = LocalContext.current
    val viewModels = rememberLoggedInViewModels(accountKey)
    val mainData by viewModels.main.data.collectAsStateWithLifecycle()
    val purchaseStep by viewModels.purchase.step.collectAsStateWithLifecycle()
    val purchaseToast by viewModels.purchase.toastRes.collectAsStateWithLifecycle()
    val updateState by viewModels.update.state.collectAsStateWithLifecycle()

    GlobalToastHosts(
        purchaseToast = purchaseToast,
        onPurchaseToastShown = viewModels.purchase::clearToast,
        mainErrorRes = mainData.errorMessageRes,
        onMainErrorShown = viewModels.main::clearError,
        updateState = updateState,
        onUpdateTipShown = viewModels.update::consumeTip,
    )

    val pageStack = rememberSaveablePageStack()
    val preload = rememberPreloadNavigation(viewModels, pageStack)

    fun pushPage(page: Page) {
        if (pageStack.last() != page) pageStack.add(page)
    }

    fun popPage() {
        if (pageStack.size > 1) pageStack.removeAt(pageStack.lastIndex)
    }

    var lastBackPress by remember { mutableLongStateOf(0L) }
    BackHandler {
        if (pageStack.size > 1) {
            pageStack.removeAt(pageStack.lastIndex)
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPress < 2000L) {
                context.findActivity()?.finish()
            } else {
                lastBackPress = now
            }
        }
    }

    var pendingPaymentTradeNo by remember { mutableStateOf<String?>(null) }
    PurchaseCompletionHost(
        viewModels = viewModels,
        pageStack = pageStack,
        onPendingPaymentTradeNo = { pendingPaymentTradeNo = it },
    )
    GiftCardRedeemHost(viewModels = viewModels)

    val transitionSpec =
        remember {
            {
                (slideInHorizontally(tween(PAGE_TRANSITION_DURATION)) { it / 3 } + fadeIn(tween(PAGE_TRANSITION_DURATION)))
                    .togetherWith(
                        slideOutHorizontally(tween(PAGE_TRANSITION_DURATION)) { -it / 3 } + fadeOut(tween(PAGE_TRANSITION_DURATION)),
                    )
            }
        }

    AnimatedContent(
        targetState = pageStack.last(),
        transitionSpec = { transitionSpec() },
        contentKey = { it },
    ) { page ->
        when (page) {
            Page.Dashboard ->
                DashboardPageContent(
                    mainViewModel = viewModels.main,
                    mainData = mainData,
                    onInvite = { preload.enterPage(PendingNav.Invite) },
                    onServer = { pushPage(Page.Server) },
                    onNotice = { preload.enterPage(PendingNav.Notice) },
                    onSupport = onSupport,
                    onProfile = { pushPage(Page.Profile) },
                    onRenew = { preload.enterPage(PendingNav.Plans) },
                )

            Page.Profile ->
                ProfilePageContent(
                    profileViewModel = viewModels.profile,
                    giftCardViewModel = viewModels.giftCard,
                    onBack = ::popPage,
                    onOrders = { preload.enterPage(PendingNav.Orders) },
                    onInvite = { preload.enterPage(PendingNav.Invite) },
                    onRenew = { preload.enterPage(PendingNav.Plans) },
                    onContact = onSupport,
                    onSettings = { pushPage(Page.Settings) },
                    onAbout = { pushPage(Page.About) },
                )

            Page.Server ->
                ServerPageContent(
                    serverViewModel = viewModels.server,
                    onUpdateSubscription = viewModels.main::updateSubscription,
                    onBack = ::popPage,
                )

            Page.Invite ->
                InvitePageContent(
                    inviteViewModel = viewModels.invite,
                    onBack = ::popPage,
                )

            Page.Notice ->
                NoticePageContent(
                    noticeViewModel = viewModels.notice,
                    onBack = ::popPage,
                )

            Page.Orders ->
                OrdersPageContent(
                    ordersViewModel = viewModels.orders,
                    purchaseViewModel = viewModels.purchase,
                    purchaseStep = purchaseStep,
                    onBack = ::popPage,
                    pendingPaymentTradeNo = pendingPaymentTradeNo,
                    onPendingPaymentConsumed = { pendingPaymentTradeNo = null },
                )

            Page.Plans ->
                PlansPageContent(
                    plansViewModel = viewModels.plans,
                    purchaseViewModel = viewModels.purchase,
                    giftCardViewModel = viewModels.giftCard,
                    onBack = ::popPage,
                    onGoToOrders = {
                        viewModels.purchase.goBack()
                        viewModels.orders.refresh()
                        pushPage(Page.Orders)
                    },
                )

            Page.Settings -> SettingsPageContent(onBack = ::popPage)

            Page.About -> AboutPageContent(onBack = ::popPage)
        }
    }

    LoadingOverlay(
        visible = preload.pending != null || mainData.isUpdating,
        onDismiss = {
            preload.cancel()
            viewModels.main.cancelUpdating()
        },
    )

    UpdateHost(updateState = updateState, updateViewModel = viewModels.update)
}
