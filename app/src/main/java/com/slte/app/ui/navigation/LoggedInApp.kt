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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.BuildConfig
import com.slte.app.ui.component.LoadingOverlay
import com.slte.app.ui.screen.invite.InviteScreen
import com.slte.app.ui.screen.invite.InviteViewModel
import com.slte.app.ui.screen.main.MainViewModel
import com.slte.app.ui.screen.notice.NoticeScreen
import com.slte.app.ui.screen.notice.NoticeViewModel
import com.slte.app.ui.screen.order.OrdersViewModel
import com.slte.app.ui.screen.plans.PlansScreen
import com.slte.app.ui.screen.plans.PlansViewModel
import com.slte.app.ui.screen.plans.PurchaseViewModel
import com.slte.app.ui.screen.profile.ProfileScreen
import com.slte.app.ui.screen.profile.ProfileViewModel
import com.slte.app.ui.screen.server.ServerScreen
import com.slte.app.ui.screen.server.ServerViewModel
import com.slte.app.ui.screen.settings.SettingsScreen
import com.slte.app.ui.screen.about.AboutScreen
import com.slte.app.ui.screen.about.UpdateSheet
import com.slte.app.ui.screen.about.UpdateUiState
import com.slte.app.ui.screen.about.UpdateViewModel
import com.slte.app.utils.findActivity

/** 主界面页面枚举，驱动 AnimatedContent 过渡动画 */
enum class Page { Dashboard, Invite, Server, Notice, Orders, Plans, Profile, Settings, About }

/** 登录后主界面：预加载 + 栈式导航 + 页面渲染 */
@Composable
fun LoggedInApp(
    accountKey: String,
    onSupport: () -> Unit
) {
    val context = LocalContext.current

    val inviteViewModel: InviteViewModel = hiltViewModel(key = "invite-$accountKey")
    val inviteData by inviteViewModel.data.collectAsStateWithLifecycle()

    val noticeViewModel: NoticeViewModel = hiltViewModel(key = "notice-$accountKey")
    val noticeData by noticeViewModel.uiState.collectAsStateWithLifecycle()

    val ordersViewModel: OrdersViewModel = hiltViewModel(key = "orders-$accountKey")
    val ordersData by ordersViewModel.data.collectAsStateWithLifecycle()

    val plansViewModel: PlansViewModel = hiltViewModel(key = "plans-$accountKey")
    val plansData by plansViewModel.data.collectAsStateWithLifecycle()

    val purchaseViewModel: PurchaseViewModel = hiltViewModel(key = "purchase-$accountKey")
    val purchaseStep by purchaseViewModel.step.collectAsStateWithLifecycle()
    val purchaseToast by purchaseViewModel.toastRes.collectAsStateWithLifecycle()

    val profileViewModel: ProfileViewModel = hiltViewModel(key = "profile-$accountKey")
    val serverViewModel: ServerViewModel = hiltViewModel(key = "server-$accountKey")
    val mainViewModel: MainViewModel = hiltViewModel(key = "main-$accountKey")
    val mainData by mainViewModel.data.collectAsStateWithLifecycle()

    purchaseToast?.let { res ->
        LaunchedEffect(res) {
            android.widget.Toast.makeText(context, context.getString(res), android.widget.Toast.LENGTH_SHORT).show()
            purchaseViewModel.clearToast()
        }
    }

    // 首页状态提示（订阅更新结果/连接错误等）全局消费：任何页面都能弹出，不限于首页
    LaunchedEffect(mainData.errorMessageRes) {
        val res = mainData.errorMessageRes
        if (res != null) {
            android.widget.Toast.makeText(context, context.getString(res), android.widget.Toast.LENGTH_SHORT).show()
            mainViewModel.clearError()
        }
    }

    val pageStack = remember { mutableStateListOf(Page.Dashboard) }
    var pendingInvite by remember { mutableStateOf(false) }
    var pendingNotice by remember { mutableStateOf(false) }
    var pendingOrders by remember { mutableStateOf(false) }
    var pendingPlans by remember { mutableStateOf(false) }
    var ordersPreloaded by remember { mutableStateOf(false) }

    fun pushPage(page: Page) {
        if (pageStack.last() != page) pageStack.add(page)
    }

    fun popPage() {
        if (pageStack.size > 1) pageStack.removeAt(pageStack.lastIndex)
    }

    fun navigateToOrders() {
        if (!ordersPreloaded) ordersViewModel.refresh()
        ordersPreloaded = false
        pushPage(Page.Orders)
    }

    // 支付完成（余额支付成功或轮询确认）：回首页并立即刷新订阅/用户/订单/个人中心信息
    LaunchedEffect(Unit) {
        purchaseViewModel.paymentCompleted.collect { tradeNo ->
            ordersViewModel.refresh()
            profileViewModel.refresh()
            pageStack.clear()
            pageStack.add(Page.Dashboard)
            // 全屏 Loading：首页信息 + 服务器列表节点全部更新完成后再关闭
            val job = mainViewModel.refreshAfterPurchase(tradeNo)
            runCatching { job.join() }
            serverViewModel.refreshNodesForPurchase()
            mainViewModel.finishPurchaseRefresh()
        }
    }

    // 双击退出：首次返回静默拦截，2 秒内连续第二次才退出
    var lastBackPress by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (pageStack.size > 1) {
            popPage()
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPress < 2000L) {
                // LocalContext 已被语言包装，需解包出宿主 Activity
                context.findActivity()?.finish()
            } else {
                lastBackPress = now
            }
        }
    }

    var pendingPaymentTradeNo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(inviteData.isEntering) {
        if (pendingInvite && !inviteData.isEntering) {
            pushPage(Page.Invite)
            pendingInvite = false
        }
    }
    LaunchedEffect(noticeData.isEntering) {
        if (pendingNotice && !noticeData.isEntering) {
            pushPage(Page.Notice)
            pendingNotice = false
        }
    }
    LaunchedEffect(ordersData.isEntering) {
        if (pendingOrders && !ordersData.isEntering) {
            ordersPreloaded = true
            pendingOrders = false
            navigateToOrders()
        }
    }
    LaunchedEffect(plansData.isEntering) {
        if (pendingPlans && !plansData.isEntering) {
            pushPage(Page.Plans)
            pendingPlans = false
        }
    }

    val createdTradeNo by purchaseViewModel.createdTradeNo.collectAsStateWithLifecycle()
    LaunchedEffect(createdTradeNo) {
        val tradeNo = createdTradeNo
        if (tradeNo != null) {
            navigateToOrders()
            purchaseViewModel.clearCreatedTradeNo()
            pendingPaymentTradeNo = tradeNo
        }
    }

    val transitionSpec = remember {
        {
            (slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280)))
                .togetherWith(slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(280)))
        }
    }

    AnimatedContent(
        targetState = pageStack.last(),
        transitionSpec = { transitionSpec() },
        contentKey = { it }
    ) { page ->
        when (page) {
            Page.Notice -> {
                NoticeScreen(
                    onBack = { popPage() },
                    viewModel = noticeViewModel
                )
            }
            Page.Server -> {
                LaunchedEffect(Unit) {
                    serverViewModel.loadNodes()
                }
                ServerScreen(
                    onBack = { popPage() },
                    onUpdateSubscription = mainViewModel::updateSubscription,
                    viewModel = serverViewModel
                )
            }
            Page.Invite -> {
                InviteScreen(
                    onBack = { popPage() },
                    viewModel = inviteViewModel
                )
            }
            Page.Orders -> {
                OrdersPageContent(
                    ordersViewModel = ordersViewModel,
                    purchaseViewModel = purchaseViewModel,
                    purchaseStep = purchaseStep,
                    onBack = { popPage() },
                    pendingPaymentTradeNo = pendingPaymentTradeNo,
                    onPendingPaymentConsumed = { pendingPaymentTradeNo = null }
                )
            }
            Page.Plans -> {
                PlansScreen(
                    onBack = { popPage() },
                    viewModel = plansViewModel,
                    purchaseViewModel = purchaseViewModel,
                    onGoToOrders = {
                        purchaseViewModel.goBack()
                        navigateToOrders()
                    }
                )
            }
            Page.Profile -> {
                LaunchedEffect(Unit) {
                    profileViewModel.refresh()
                }
                ProfileScreen(
                    onBack = { popPage() },
                    onOrders = {
                        pendingOrders = true
                        ordersViewModel.enterAndRefresh()
                    },
                    onInvite = {
                        pendingInvite = true
                        inviteViewModel.enterAndRefresh()
                    },
                    onRenew = {
                        pendingPlans = true
                        plansViewModel.enterAndRefresh()
                    },
                    onContact = onSupport,
                    onSettings = { pushPage(Page.Settings) },
                    onAbout = { pushPage(Page.About) },
                    onLogout = { profileViewModel.logout() },
                    viewModel = profileViewModel
                )
            }
            Page.Settings -> {
                SettingsScreen(
                    onBack = { popPage() }
                )
            }
            Page.About -> {
                AboutScreen(
                    onBack = { popPage() }
                )
            }
            Page.Dashboard -> {
                DashboardPageContent(
                    mainViewModel = mainViewModel,
                    mainData = mainData,
                    onInvite = {
                        pendingInvite = true
                        inviteViewModel.enterAndRefresh()
                    },
                    onServer = { pushPage(Page.Server) },
                    onNotice = {
                        pendingNotice = true
                        noticeViewModel.enterAndRefresh()
                    },
                    onSupport = onSupport,
                    onProfile = { pushPage(Page.Profile) },
                    onRenew = {
                        pendingPlans = true
                        plansViewModel.enterAndRefresh()
                    }
                )
            }
        }
    }

    LoadingOverlay(
        visible = pendingInvite || pendingNotice || pendingOrders || pendingPlans || mainData.isUpdating,
        onDismiss = {
            pendingInvite = false
            pendingNotice = false
            pendingOrders = false
            pendingPlans = false
            mainViewModel.cancelUpdating()
        }
    )

    // 更新弹窗（全局：任何页面都可弹出；强制更新时覆盖所有页面不可关闭）
    val updateViewModel: UpdateViewModel = hiltViewModel(key = "update")
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()
    val available = updateState as? UpdateUiState.Available
    if (available != null) {
        UpdateSheet(
            currentVersion = BuildConfig.VERSION_NAME,
            state = available,
            onDismiss = updateViewModel::dismiss,
            onUpdateNow = updateViewModel::updateNow,
            onLater = updateViewModel::later
        )
    }

    // 下载/安装失败提示（全局消费）
    LaunchedEffect(updateState) {
        val failedRes = (updateState as? UpdateUiState.Failed)?.messageRes
        if (failedRes != null) {
            android.widget.Toast.makeText(
                context,
                context.getString(failedRes),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            updateViewModel.consumeTip()
        }
    }
}
