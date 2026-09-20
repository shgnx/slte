package com.slte.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.slte.app.ui.screen.about.UpdateViewModel
import com.slte.app.ui.screen.giftcard.GiftCardRedeemViewModel
import com.slte.app.ui.screen.invite.InviteViewModel
import com.slte.app.ui.screen.main.MainViewModel
import com.slte.app.ui.screen.notice.NoticeViewModel
import com.slte.app.ui.screen.order.OrdersViewModel
import com.slte.app.ui.screen.plans.PlansViewModel
import com.slte.app.ui.screen.plans.PurchaseViewModel
import com.slte.app.ui.screen.profile.ProfileViewModel
import com.slte.app.ui.screen.server.ServerViewModel

internal class LoggedInViewModels(
    val giftCard: GiftCardRedeemViewModel,
    val invite: InviteViewModel,
    val notice: NoticeViewModel,
    val orders: OrdersViewModel,
    val plans: PlansViewModel,
    val purchase: PurchaseViewModel,
    val profile: ProfileViewModel,
    val server: ServerViewModel,
    val main: MainViewModel,
    val update: UpdateViewModel,
)

@Composable
internal fun rememberLoggedInViewModels(accountKey: String): LoggedInViewModels = LoggedInViewModels(
    giftCard = hiltViewModel(key = "giftcard-$accountKey"),
    invite = hiltViewModel(key = "invite-$accountKey"),
    notice = hiltViewModel(key = "notice-$accountKey"),
    orders = hiltViewModel(key = "orders-$accountKey"),
    plans = hiltViewModel(key = "plans-$accountKey"),
    purchase = hiltViewModel(key = "purchase-$accountKey"),
    profile = hiltViewModel(key = "profile-$accountKey"),
    server = hiltViewModel(key = "server-$accountKey"),
    main = hiltViewModel(key = "main-$accountKey"),
    update = hiltViewModel(key = "update"),
)
