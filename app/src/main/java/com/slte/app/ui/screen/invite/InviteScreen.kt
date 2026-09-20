package com.slte.app.ui.screen.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.ui.component.LoadingOverlay
import com.slte.app.ui.component.SltePullRefresh
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.component.SubmitTipHost
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteScreen(
    onBack: () -> Unit = {},
    viewModel: InviteViewModel,
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SubmitTipHost(tip = data.tip, onTipShown = viewModel::clearTip)

    SlteScaffold(
        title = stringResource(R.string.invite_title),
        onBack = onBack,
    ) { innerPadding ->
        SltePullRefresh(
            isRefreshing = data.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(innerPadding),
        ) {
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.dashboardScreenPaddingH),
                verticalArrangement = Arrangement.spacedBy(Dimens.dashboardCardSpacing),
                contentPadding =
                PaddingValues(
                    vertical = Dimens.dashboardScreenPaddingV,
                ),
            ) {
                item { InviteStatCard(stat = data.stat) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.dashboardCardSpacing),
                    ) {
                        InviteActionButton(
                            icon = SlteIcons.Transfer,
                            text = stringResource(R.string.invite_transfer),
                            tint = SlteColors.current.accentInteractive,
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::showTransferSheet,
                        )
                        InviteActionButton(
                            icon = SlteIcons.Wallet,
                            text = stringResource(R.string.invite_withdraw),
                            tint = SlteColors.current.accentInteractive,
                            modifier = Modifier.weight(1f),
                            onClick = viewModel::showWithdrawSheet,
                        )
                    }
                }

                item {
                    InviteCodeCard(
                        codes = data.codes,
                        isGenerating = data.isGenerating,
                        onGenerate = viewModel::generateCode,
                        context = context,
                    )
                }

                item {
                    CommissionRecordsCard(records = data.records)
                }
            }
        }
    }

    if (data.sheet == InviteSheet.Transfer) {
        TransferSheet(
            availableBalance = data.stat.availableBalance,
            isSubmitting = data.isSubmitting,
            onDismiss = viewModel::hideTransferSheet,
            onConfirm = { yuan -> viewModel.transferCommission(yuan) },
        )
    }

    if (data.sheet == InviteSheet.Withdraw) {
        WithdrawSheet(
            methodsState = data.withdrawMethods,
            isSubmitting = data.isSubmitting,
            onDismiss = viewModel::hideWithdrawSheet,
            onRetryMethods = viewModel::retryWithdrawMethods,
            onConfirm = { method, account -> viewModel.withdraw(method, account) },
        )
    }

    LoadingOverlay(visible = data.isGenerating, onDismiss = viewModel::cancelLoading)
}
