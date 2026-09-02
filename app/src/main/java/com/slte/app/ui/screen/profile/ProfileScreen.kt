package com.slte.app.ui.screen.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.component.UsageCard
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

/**
 * 个人中心页面（二级页面）。
 */
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
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val errorMessageRes by viewModel.errorMessageRes.collectAsStateWithLifecycle()
    var showLogoutSheet by rememberSaveable { mutableStateOf(false) }

    SlteScaffold(
        title = stringResource(R.string.profile_title),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.dashboardScreenPaddingH),
            verticalArrangement = Arrangement.spacedBy(Dimens.dashboardCardSpacing),
            contentPadding = PaddingValues(vertical = Dimens.dashboardScreenPaddingV)
        ) {
            item {
                UserInfoCard(email = data.email, balance = data.balance)
            }

            item {
                if (data.isLoading) {
                    LoadingCard()
                } else if (errorMessageRes != null) {
                    ErrorCard(
                        messageRes = errorMessageRes!!,
                        onRetry = viewModel::retry
                    )
                } else {
                    val info = data.subscribeInfo
                    val hasPlan = info?.hasPlan == true
                    UsageCard(
                        planName = info?.planName ?: "",
                        usedBytes = info?.usedTraffic ?: 0L,
                        totalBytes = info?.transferEnable ?: 0L,
                        isValid = !(info?.expired == true),
                        hasPlan = hasPlan,
                        daysUntilExpired = data.daysUntilExpired,
                        expiredAtDate = info?.expiredAt?.takeIf { it > 0L }?.let { FormatUtils.formatExpiryDate(it) },
                        actionText = stringResource(
                            if (hasPlan) R.string.plan_renew_button else R.string.plan_buy_button
                        ),
                        actionEnabled = true,
                        onAction = onRenew
                    )
                }
            }

            item {
                NavigateCard(
                    icon = Icons.Outlined.Receipt,
                    title = stringResource(R.string.profile_orders),
                    onClick = onOrders
                )
            }

            item {
                NavigateCard(
                    icon = Icons.Outlined.GroupAdd,
                    title = stringResource(R.string.invite_title),
                    onClick = onInvite
                )
            }

            item {
                NavigateCard(
                    icon = Icons.Outlined.SupportAgent,
                    title = stringResource(R.string.profile_contact),
                    onClick = onContact
                )
            }
            item {
                NavigateCard(
                    icon = Icons.Outlined.Settings,
                    title = stringResource(R.string.settings_title),
                    onClick = onSettings
                )
            }
            item {
                NavigateCard(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.profile_about),
                    onClick = onAbout
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
            onDismiss = { showLogoutSheet = false }
        )
    }
}
