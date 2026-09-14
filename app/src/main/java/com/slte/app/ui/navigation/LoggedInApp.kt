package com.slte.app.ui.navigation

import android.widget.Toast
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.ui.component.LoadingOverlay
import com.slte.app.ui.screen.about.AboutScreen
import com.slte.app.ui.screen.about.ForceUpdateDialog
import com.slte.app.ui.screen.about.UpdateSheet
import com.slte.app.ui.screen.about.UpdateUiState
import com.slte.app.ui.screen.about.UpdateViewModel
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
import com.slte.app.utils.findActivity
import kotlinx.coroutines.launch

/** 主界面页面枚举，驱动 AnimatedContent 过渡动画 */
enum class Page { Dashboard, Invite, Server, Notice, Orders, Plans, Profile, Settings, About }

/**
 * 需要「先预加载数据、加载完成后再进入」的目标页。
 *
 * 首页与个人中心共 4 个此类入口，此前用 5 个状态 + 4 个 LaunchedEffect 驱动，
 * 新增入口易漏改；现收敛为一个可空状态 + 单个效果。
 */
private enum class PendingNav {
    Invite,
    Notice,
    Orders,
    Plans,
    ;

    /** 预加载完成后进入的页面 */
    val page: Page
        get() = when (this) {
            Invite -> Page.Invite
            Notice -> Page.Notice
            Orders -> Page.Orders
            Plans -> Page.Plans
        }
}

/**
 * 页面栈保存器：Page 是枚举，存名字列表即可。
 * 用 saveable 保存，避免旋转屏幕或进程重建后导航位置丢失。
 */
private val pageStackSaver =
    listSaver<SnapshotStateList<Page>, String>(
        save = { stack -> stack.map { it.name } },
        restore = { names -> names.map { Page.valueOf(it) }.toMutableStateList() },
    )

/** 登录后主界面：预加载 + 栈式导航 + 页面渲染 */
@Composable
fun LoggedInApp(
    accountKey: String,
    onSupport: () -> Unit,
) {
    // LocalContext 已被语言包装（AppLocaleContent），此处取到的是包装后的 context
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
    val createdTradeNo by purchaseViewModel.createdTradeNo.collectAsStateWithLifecycle()

    val profileViewModel: ProfileViewModel = hiltViewModel(key = "profile-$accountKey")
    val serverViewModel: ServerViewModel = hiltViewModel(key = "server-$accountKey")
    val mainViewModel: MainViewModel = hiltViewModel(key = "main-$accountKey")
    val mainData by mainViewModel.data.collectAsStateWithLifecycle()

    val updateViewModel: UpdateViewModel = hiltViewModel(key = "update")
    val updateState by updateViewModel.state.collectAsStateWithLifecycle()

    GlobalToastHosts(
        purchaseToast = purchaseToast,
        onPurchaseToastShown = purchaseViewModel::clearToast,
        mainErrorRes = mainData.errorMessageRes,
        onMainErrorShown = mainViewModel::clearError,
        updateState = updateState,
        onUpdateTipShown = updateViewModel::consumeTip,
    )

    val pageStack = rememberSaveablePageStack()

    fun pushPage(page: Page) {
        if (pageStack.last() != page) pageStack.add(page)
    }

    /** 进入订单页；[preload] 表示需要自行触发一次加载（预加载路径已加载过，无需重复请求） */
    fun goToOrders(preload: Boolean) {
        if (preload) ordersViewModel.refresh()
        pushPage(Page.Orders)
    }

    var pending by remember { mutableStateOf<PendingNav?>(null) }

    /** 记录待进入页并立即触发该页的预加载（各 ViewModel 会同步置 isEntering，不会提前跳转） */
    fun enterPage(target: PendingNav) {
        pending = target
        when (target) {
            PendingNav.Invite -> inviteViewModel.enterAndRefresh()
            PendingNav.Notice -> noticeViewModel.enterAndRefresh()
            PendingNav.Orders -> ordersViewModel.enterAndRefresh()
            PendingNav.Plans -> plansViewModel.enterAndRefresh()
        }
    }

    // 预加载完成信号：只关心当前 pending 目标对应的数据源
    val pendingLoaded =
        when (pending) {
            PendingNav.Invite -> !inviteData.isEntering
            PendingNav.Notice -> !noticeData.isEntering
            PendingNav.Orders -> !ordersData.isEntering
            PendingNav.Plans -> !plansData.isEntering
            null -> false
        }
    LaunchedEffect(pending, pendingLoaded) {
        val target = pending
        if (target != null && pendingLoaded) {
            pending = null
            pushPage(target.page)
        }
    }

    // 双击退出：首次返回静默拦截，2 秒内连续第二次才退出
    var lastBackPress by remember { mutableLongStateOf(0L) }
    BackHandler {
        if (pageStack.size > 1) {
            pageStack.removeAt(pageStack.lastIndex)
        } else {
            val now = System.currentTimeMillis()
            if (now - lastBackPress < 2000L) {
                // 解包出宿主 Activity（context 已被语言包装）
                context.findActivity()?.finish()
            } else {
                lastBackPress = now
            }
        }
    }

    var pendingPaymentTradeNo by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(createdTradeNo) {
        val tradeNo = createdTradeNo
        if (tradeNo != null) {
            goToOrders(preload = true)
            purchaseViewModel.clearCreatedTradeNo()
            pendingPaymentTradeNo = tradeNo
        }
    }

    /**
     * 支付完成后的整屏刷新：回到首页并刷新订阅/用户/订单/个人中心信息，
     * 等首页数据与服务器节点都就绪后再关闭全屏 Loading。
     */
    suspend fun refreshAfterPurchase(tradeNo: String?) {
        purchaseViewModel.goBack()
        ordersViewModel.refresh()
        profileViewModel.refresh()
        pageStack.clear()
        pageStack.add(Page.Dashboard)
        mainViewModel.refreshAfterPurchase(tradeNo).join()
        serverViewModel.refreshNodesForPurchase()
        mainViewModel.finishPurchaseRefresh()
    }

    // 支付完成（余额支付成功或轮询确认）
    val purchaseRefreshScope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        // 收集器必须保持空闲：此前在 collect 体内直接 join() 刷新任务（最长 60 秒），
        // 期间新到的完成事件会被丢弃。改为派发到独立协程后本收集器立即返回。
        purchaseViewModel.paymentCompleted.collect { tradeNo ->
            purchaseViewModel.ackPaymentCompleted()
            purchaseRefreshScope.launch { refreshAfterPurchase(tradeNo) }
        }
    }

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
                    mainViewModel = mainViewModel,
                    mainData = mainData,
                    onInvite = { enterPage(PendingNav.Invite) },
                    onServer = { pushPage(Page.Server) },
                    onNotice = { enterPage(PendingNav.Notice) },
                    onSupport = onSupport,
                    onProfile = { pushPage(Page.Profile) },
                    onRenew = { enterPage(PendingNav.Plans) },
                )

            Page.Profile -> {
                LaunchedEffect(Unit) { profileViewModel.refresh() }
                ProfileScreen(
                    onBack = { pageStack.removeAt(pageStack.lastIndex) },
                    onOrders = { enterPage(PendingNav.Orders) },
                    onInvite = { enterPage(PendingNav.Invite) },
                    onRenew = { enterPage(PendingNav.Plans) },
                    onContact = onSupport,
                    onSettings = { pushPage(Page.Settings) },
                    onAbout = { pushPage(Page.About) },
                    onLogout = { profileViewModel.logout() },
                    viewModel = profileViewModel,
                )
            }

            Page.Server -> {
                LaunchedEffect(Unit) { serverViewModel.loadNodes() }
                ServerScreen(
                    onBack = { pageStack.removeAt(pageStack.lastIndex) },
                    onUpdateSubscription = mainViewModel::updateSubscription,
                    viewModel = serverViewModel,
                )
            }

            Page.Invite ->
                InviteScreen(
                    onBack = { pageStack.removeAt(pageStack.lastIndex) },
                    viewModel = inviteViewModel,
                )

            Page.Notice ->
                NoticeScreen(
                    onBack = { pageStack.removeAt(pageStack.lastIndex) },
                    viewModel = noticeViewModel,
                )

            Page.Orders ->
                OrdersPageContent(
                    ordersViewModel = ordersViewModel,
                    purchaseViewModel = purchaseViewModel,
                    purchaseStep = purchaseStep,
                    onBack = { pageStack.removeAt(pageStack.lastIndex) },
                    pendingPaymentTradeNo = pendingPaymentTradeNo,
                    onPendingPaymentConsumed = { pendingPaymentTradeNo = null },
                )

            Page.Plans ->
                PlansScreen(
                    onBack = { pageStack.removeAt(pageStack.lastIndex) },
                    viewModel = plansViewModel,
                    purchaseViewModel = purchaseViewModel,
                    onGoToOrders = {
                        // 进订单页前中止购买流程：goBack() 会保留 SelectPeriod（套餐页错误弹窗返回用），
                        // 残留的 step 会让订单页渲染出无回调的 SelectPeriodSheet，流程卡死
                        purchaseViewModel.abortFlow()
                        goToOrders(preload = true)
                    },
                )

            Page.Settings ->
                SettingsScreen(
                    onBack = { pageStack.removeAt(pageStack.lastIndex) },
                )

            Page.About ->
                AboutScreen(
                    onBack = { pageStack.removeAt(pageStack.lastIndex) },
                )
        }
    }

    LoadingOverlay(
        visible = pending != null || mainData.isUpdating,
        onDismiss = {
            pending = null
            mainViewModel.cancelUpdating()
        },
    )

    UpdateHost(updateState = updateState, updateViewModel = updateViewModel)
}

/** 页面栈：可保存版本，避免旋转/进程重建后导航位置丢失 */
@Composable
private fun rememberSaveablePageStack(): SnapshotStateList<Page> = rememberSaveable(saver = pageStackSaver) { mutableStateListOf(Page.Dashboard) }

/**
 * 全局 Toast 宿主：统一把各 ViewModel 的一次性资源 ID 状态消费成 Toast。
 *
 * 提示与当前页面无关（订阅结果、连接错误、更新失败都需在任何页面弹出），
 * 集中在此避免在主体里散落多个 LaunchedEffect。
 */
@Composable
private fun GlobalToastHosts(
    purchaseToast: Int?,
    onPurchaseToastShown: () -> Unit,
    mainErrorRes: Int?,
    onMainErrorShown: () -> Unit,
    updateState: UpdateUiState,
    onUpdateTipShown: () -> Unit,
) {
    val context = LocalContext.current
    fun toast(resId: Int) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
    }

    purchaseToast?.let { resId ->
        LaunchedEffect(resId) {
            toast(resId)
            onPurchaseToastShown()
        }
    }
    LaunchedEffect(mainErrorRes) {
        if (mainErrorRes != null) {
            toast(mainErrorRes)
            onMainErrorShown()
        }
    }
    LaunchedEffect(updateState) {
        val failedRes = (updateState as? UpdateUiState.Failed)?.messageRes
        if (failedRes != null) {
            toast(failedRes)
            onUpdateTipShown()
        }
    }
}

/** 更新弹窗宿主（全局：任何页面都可弹出；强制更新为居中弹窗且不可关闭） */
@Composable
private fun UpdateHost(
    updateState: UpdateUiState,
    updateViewModel: UpdateViewModel,
) {
    val available = updateState as? UpdateUiState.Available ?: return
    if (available.force) {
        ForceUpdateDialog(onUpdateNow = updateViewModel::updateNow)
    } else {
        UpdateSheet(
            state = available,
            onDismiss = updateViewModel::dismiss,
            onUpdateNow = updateViewModel::updateNow,
            onLater = updateViewModel::later,
        )
    }
}
