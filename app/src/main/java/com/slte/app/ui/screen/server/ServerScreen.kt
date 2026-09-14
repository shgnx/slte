package com.slte.app.ui.screen.server

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.ui.component.CircleIconButton
import com.slte.app.ui.component.EmptyState
import com.slte.app.ui.component.ErrorState
import com.slte.app.ui.component.FlagPlaceholder
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.component.SpecialNodeIcon
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Constants
import com.slte.app.utils.Dimens

/**
 * 节点选择页面（二级页面）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(
    onBack: () -> Unit,
    onUpdateSubscription: (() -> Unit)? = null,
    viewModel: ServerViewModel = hiltViewModel(),
) {
    val data by viewModel.data.collectAsStateWithLifecycle()
    val errorMessageRes by viewModel.errorMessageRes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    LaunchedEffect(errorMessageRes) {
        errorMessageRes?.let {
            android.widget.Toast
                .makeText(context, context.getString(it), android.widget.Toast.LENGTH_SHORT)
                .show()
            viewModel.dismissError()
        }
    }

    SlteScaffold(
        title = stringResource(R.string.server_title),
        onBack = onBack,
        actions = {
            CircleIconButton(
                icon = SlteIcons.SpeedTest,
                description = stringResource(R.string.server_speed_test),
                onClick = viewModel::startSpeedTest,
            )
            CircleIconButton(
                icon = SlteIcons.SyncSubscription,
                description = stringResource(R.string.dashboard_update_subscription),
                onClick = onUpdateSubscription ?: viewModel::updateSubscription,
            )
        },
    ) { innerPadding ->
        if (errorMessageRes != null && data.nodes.isEmpty()) {
            ErrorState(
                message = stringResource(errorMessageRes!!),
                onRetry = viewModel::retry,
                modifier = Modifier.padding(innerPadding),
            )
        } else if (data.nodes.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.server_nodes_empty),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = Dimens.dashboardScreenPaddingH),
                verticalArrangement = Arrangement.spacedBy(Dimens.gap.sm),
            ) {
                item { Spacer(modifier = Modifier.height(Dimens.gap.md)) }

                item {
                    NodeCard(
                        name = stringResource(R.string.server_auto),
                        desc = stringResource(R.string.server_auto_now, data.autoNode ?: "--"),
                        icon = {
                            val code = data.autoNodeCountryCode
                            if (code != null) {
                                FlagPlaceholder(countryCode = code, circular = true)
                            } else {
                                SpecialNodeIcon(
                                    icon = "A",
                                    contentDescription = stringResource(R.string.server_auto),
                                )
                            }
                        },
                        delay = data.autoDelay,
                        selected = data.selectedNodeId == 0,
                        isTesting = data.isTesting,
                        onClick = { viewModel.selectNode(0) },
                    )
                }

                item {
                    NodeCard(
                        name = stringResource(R.string.server_fallback),
                        desc = stringResource(R.string.server_fallback_now, data.fallbackNode ?: "--"),
                        icon = {
                            val code = data.fallbackNodeCountryCode
                            if (code != null) {
                                FlagPlaceholder(countryCode = code, circular = true)
                            } else {
                                SpecialNodeIcon(
                                    icon = "F",
                                    contentDescription = stringResource(R.string.server_fallback),
                                )
                            }
                        },
                        delay = data.fallbackDelay,
                        selected = data.selectedNodeId == -1,
                        isTesting = data.isTesting,
                        onClick = { viewModel.selectNode(-1) },
                    )
                }

                items(data.nodes.distinctBy { it.id }, key = { it.id }) { node ->
                    NodeCard(
                        name = node.name,
                        icon = {
                            if (node.countryCode != "XX") {
                                FlagPlaceholder(countryCode = node.countryCode, circular = true)
                            } else {
                                SpecialNodeIcon(
                                    icon = "🌐",
                                    contentDescription = stringResource(R.string.server_special_node),
                                )
                            }
                        },
                        delay = node.delay,
                        selected = data.selectedNodeId == node.id,
                        isTesting = data.isTesting && node.name !in data.testedNodes,
                        onClick = { viewModel.selectNode(node.id) },
                    )
                }
            }
        }
    }
}

/**
 * 节点卡片：展示名称、延迟与选中态，点击由调用方处理。
 *
 * @param delay 延迟毫秒；null 表示特殊节点，DELAY_TIMEOUT 表示超时
 * @param icon 左侧图标 composable（国旗或特殊图标）
 */
@Composable
private fun NodeCard(
    name: String,
    desc: String? = null,
    icon: @Composable () -> Unit,
    delay: Int?,
    selected: Boolean,
    isTesting: Boolean,
    onClick: () -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        color = MaterialTheme.colorScheme.surface,
        border =
        if (selected) {
            BorderStroke(Dimens.strokeMedium, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        shadowElevation = Dimens.cardElevation,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(Dimens.gap.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()

            Spacer(modifier = Modifier.width(Dimens.gap.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    style = SlteType.body,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (desc != null) {
                    Text(
                        text = desc,
                        style = SlteType.label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isTesting) {
                LottieLoadingIcon(modifier = Modifier.size(Dimens.icon.lg))
            } else if (delay != null) {
                DelayText(delay = delay)
            }
        }
    }
}

@Composable
private fun DelayText(delay: Int) {
    val (text, color) =
        when {
            delay == Constants.DELAY_TIMEOUT -> stringResource(R.string.server_timeout) to SlteColors.current.statusDanger
            delay < 100 -> stringResource(R.string.format_delay_ms, delay) to SlteColors.current.statusSuccess
            delay < 200 -> stringResource(R.string.format_delay_ms, delay) to SlteColors.current.statusInfo
            else -> stringResource(R.string.format_delay_ms, delay) to SlteColors.current.statusSlow
        }

    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        style = SlteType.label,
        color = color,
    )
}
