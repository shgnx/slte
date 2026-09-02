package com.slte.app.ui.screen.main

import android.Manifest
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HeadsetMic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.component.CircleIconButton
import com.slte.app.ui.component.ConnectToggleCard
import com.slte.app.ui.component.DashboardActionButtons
import com.slte.app.ui.component.InfoListCard
import com.slte.app.ui.component.ProxyModeSheet
import com.slte.app.ui.component.UsageCard
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

/** 登录后主页面 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(
    mainViewModel: MainViewModel,
    data: DashboardData,
    onInvite: () -> Unit = {},
    onServer: () -> Unit = {},
    onNotice: () -> Unit = {},
    onSupport: () -> Unit = {},
    onProfile: () -> Unit = {},
    onRenew: () -> Unit = {}
) {
    val context = LocalContext.current
    var showProxySheet by remember { mutableStateOf(false) }
    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            mainViewModel.toggleConnection()
        } else {
            mainViewModel.onVpnPermissionDenied()
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        mainViewModel.refreshKernelInfo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = TextSizes.topBarTitle,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                actions = {
                    CircleIconButton(
                        icon = Icons.Rounded.HeadsetMic,
                        description = stringResource(R.string.topbar_support),
                        onClick = onSupport
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.Notifications,
                        description = stringResource(R.string.topbar_notice),
                        onClick = onNotice
                    )
                    CircleIconButton(
                        icon = Icons.Rounded.Person,
                        description = stringResource(R.string.topbar_profile),
                        onClick = onProfile
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        DashboardContent(
            data = data,
            onToggleConnection = {
                if (!data.hasPlan) {
                    android.widget.Toast.makeText(
                        context,
                        context.getString(R.string.dashboard_no_plan_tip),
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    onRenew()
                } else {
                    requestNotificationPermission(context, notificationPermissionLauncher)
                    val request = VpnService.prepare(context)
                    if (request != null) {
                        vpnPermissionLauncher.launch(request)
                    } else {
                        mainViewModel.toggleConnection()
                    }
                }
            },
            onServerClick = onServer,
            onProxyClick = { showProxySheet = true },
            onUpdateSubscription = {
                if (data.hasPlan) {
                    mainViewModel.updateSubscription()
                } else {
                    onRenew()
                }
            },
            onInvite = onInvite,
            onRenew = onRenew,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    if (showProxySheet) {
        ProxyModeSheet(
            currentMode = data.proxyMode,
            onDismiss = { showProxySheet = false },
            onSelect = mainViewModel::setProxyMode
        )
    }

}

private fun requestNotificationPermission(
    context: android.content.Context,
    launcher: androidx.activity.result.ActivityResultLauncher<String>
) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** 仪表盘内容 */
@Composable
internal fun DashboardContent(
    data: DashboardData,
    onToggleConnection: () -> Unit,
    onServerClick: () -> Unit,
    onProxyClick: () -> Unit,
    onUpdateSubscription: () -> Unit,
    onInvite: () -> Unit,
    onRenew: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 小屏设备压缩间距与开关卡片高度，保证首屏完整可见不突破底部
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier.background(MaterialTheme.colorScheme.background)
    ) {
        val compact = maxHeight < Dimens.dashboardCompactBreakpoint
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.dashboardScreenPaddingH),
            verticalArrangement = Arrangement.spacedBy(
                if (compact) Dimens.dashboardCardSpacingCompact else Dimens.dashboardCardSpacing
            ),
            contentPadding = PaddingValues(
                vertical = if (compact) Dimens.dashboardScreenPaddingVCompact else Dimens.dashboardScreenPaddingV
            )
        ) {
        item {
            UsageCard(
                planName = data.planName,
                usedBytes = data.usedBytes,
                totalBytes = data.totalBytes,
                isValid = data.isValid,
                hasPlan = data.hasPlan,
                daysUntilExpired = if (data.expiredAt > 0L) data.daysUntilExpired else null,
                expiredAtDate = if (data.expiredAt > 0L) FormatUtils.formatExpiryDate(data.expiredAt) else null,
                actionText = stringResource(
                    if (data.hasPlan) R.string.plan_renew_button
                    else R.string.plan_buy_button
                ),
                actionEnabled = true,
                onAction = onRenew
            )
        }
        item {
                    InfoListCard(
                        daysUntilExpired = if (data.expiredAt > 0L) data.daysUntilExpired else null,
                        serverName = data.serverName,
                        proxyMode = data.proxyMode,
                        currentIp = data.currentIp,
                        ipCountryCode = data.ipCountryCode,
                        onServerClick = onServerClick,
                        onProxyClick = onProxyClick
            )
        }
        item {
                    DashboardActionButtons(
                        onUpdateSubscription = onUpdateSubscription,
                        hasPlan = data.hasPlan,
                        onInvite = onInvite
                    )
        }
        item {
            ConnectToggleCard(
                isConnected = data.isConnected,
                isConnecting = data.isConnecting,
                onToggle = onToggleConnection,
                minHeight = if (compact) Dimens.dashboardToggleCardMinHeightCompact
                else Dimens.dashboardToggleCardMinHeight
            )
        }
        }
    }
}
