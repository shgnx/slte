package com.slte.app.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.domain.model.isPlanValid
import com.slte.app.ui.component.CircleIconButton
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.component.SubmitTipHost
import com.slte.app.ui.component.UsageCard
import com.slte.app.ui.screen.giftcard.GiftCardRedeemSheet
import com.slte.app.ui.screen.giftcard.GiftCardRedeemViewModel
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOrders: () -> Unit = {},
    onInvite: () -> Unit = {},
    onRenew: () -> Unit = {},
    onContact: () -> Unit = {},
    onSettings: () -> Unit = {},
    onAbout: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    giftCardViewModel: GiftCardRedeemViewModel = hiltViewModel(),
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val errorMessageRes by viewModel.errorMessageRes.collectAsStateWithLifecycle()
    val giftCardState by giftCardViewModel.state.collectAsStateWithLifecycle()
    val giftCardTip by giftCardViewModel.tip.collectAsStateWithLifecycle()
    var showLogoutSheet by rememberSaveable { mutableStateOf(false) }

    SlteScaffold(
        title = stringResource(R.string.profile_title),
        onBack = onBack,
        actions = {
            CircleIconButton(
                icon = SlteIcons.GiftCard,
                description = stringResource(R.string.gift_card_title),
                onClick = giftCardViewModel::open,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.dashboardScreenPaddingH),
            verticalArrangement = Arrangement.spacedBy(Dimens.dashboardCardSpacing),
            contentPadding = PaddingValues(vertical = Dimens.dashboardScreenPaddingV),
        ) {
            item {
                UserInfoCard(email = data.email, balance = data.balance)
            }

            item {
                val errorRes = errorMessageRes
                if (data.isLoading) {
                    LoadingCard()
                } else if (errorRes != null) {
                    ErrorCard(
                        messageRes = errorRes,
                        onRetry = viewModel::retry,
                    )
                } else {
                    val info = data.subscribeInfo
                    val hasPlan = info?.hasPlan == true
                    UsageCard(
                        planName = info?.planName ?: "",
                        usedBytes = info?.usedTraffic ?: 0L,
                        totalBytes = info?.transferEnable ?: 0L,
                        isValid = isPlanValid(info),
                        hasPlan = hasPlan,
                        daysUntilExpired = data.daysUntilExpired,
                        expiredAtDate = info?.expiredAt?.takeIf { it > 0L }?.let { FormatUtils.formatExpiryDate(it) },
                        actionText =
                        stringResource(
                            if (hasPlan) R.string.plan_renew_button else R.string.plan_buy_button,
                        ),
                        actionEnabled = true,
                        onAction = onRenew,
                    )
                }
            }

            item {
                NavigateCard(
                    icon = SlteIcons.Orders,
                    title = stringResource(R.string.profile_orders),
                    onClick = onOrders,
                )
            }

            item {
                NavigateCard(
                    icon = SlteIcons.InviteRow,
                    title = stringResource(R.string.invite_title),
                    onClick = onInvite,
                )
            }

            item {
                NavigateCard(
                    icon = SlteIcons.CustomerService,
                    title = stringResource(R.string.profile_contact),
                    onClick = onContact,
                )
            }
            item {
                NavigateCard(
                    icon = SlteIcons.Settings,
                    title = stringResource(R.string.settings_title),
                    onClick = onSettings,
                )
            }
            item {
                NavigateCard(
                    icon = SlteIcons.About,
                    title = stringResource(R.string.profile_about),
                    onClick = onAbout,
                )
            }

            item {
                LogoutCard(onClick = { showLogoutSheet = true })
            }
        }
    }

    if (showLogoutSheet) {
        LogoutConfirmSheet(
            onConfirm = {
                showLogoutSheet = false
                onLogout()
            },
            onDismiss = { showLogoutSheet = false },
        )
    }

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
