package com.slte.app.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.component.SubmitTipHost
import com.slte.app.ui.component.rememberToast
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    var showTunStackSheet by rememberSaveable { mutableStateOf(false) }
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    val data by viewModel.data.collectAsStateWithLifecycle()

    SlteScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
    ) { innerPadding ->
        LazyColumn(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.dashboardScreenPaddingH),
            verticalArrangement = Arrangement.spacedBy(Dimens.dashboardCardSpacing),
            contentPadding =
            androidx.compose.foundation.layout
                .PaddingValues(vertical = Dimens.dashboardScreenPaddingV),
        ) {
            item {
                SettingsRowCard(
                    icon = SlteIcons.TunStack,
                    title = stringResource(R.string.settings_tun_stack),
                    value = stringResource(data.tunStackMode.labelRes),
                    onClick = { showTunStackSheet = true },
                )
            }

            item {
                SettingsRowCard(
                    icon = SlteIcons.Language,
                    title = stringResource(R.string.settings_language),
                    value = stringResource(LanguageMode.fromLocale(data.locale).labelRes),
                    onClick = { showLanguageSheet = true },
                )
            }

            item {
                SettingsRowCard(
                    icon = SlteIcons.ChangePassword,
                    title = stringResource(R.string.settings_change_password),
                    onClick = viewModel::showChangePassword,
                )
            }

            item {
                SettingsSwitchCard(
                    icon = if (data.darkModeEnabled) SlteIcons.LightMode else SlteIcons.DarkMode,
                    title = stringResource(R.string.settings_dark_mode),
                    checked = data.darkModeEnabled,
                    onCheckedChange = viewModel::setDarkMode,
                )
            }

            item {
                SettingsSwitchCard(
                    icon = SlteIcons.Email,
                    title = stringResource(R.string.settings_expire_remind),
                    checked = data.expireRemindEnabled,
                    enabled = data.remindSync == RemindSync.Idle,
                    onCheckedChange = viewModel::setExpireRemind,
                )
            }

            item {
                SettingsSwitchCard(
                    icon = SlteIcons.Remind,
                    title = stringResource(R.string.settings_traffic_remind),
                    checked = data.trafficRemindEnabled,
                    enabled = data.remindSync == RemindSync.Idle,
                    onCheckedChange = viewModel::setTrafficRemind,
                )
            }

            data.errorMessageRes?.let { res ->
                item {
                    Text(
                        text = stringResource(res),
                        style = SlteType.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = Dimens.gap.lg),
                    )
                }
            }
        }
    }

    if (showTunStackSheet) {
        TunStackModeSheet(
            currentMode = data.tunStackMode,
            onDismiss = { showTunStackSheet = false },
            onSelect = {
                viewModel.setTunStackMode(it)
                showTunStackSheet = false
            },
        )
    }

    if (showLanguageSheet) {
        LanguageModeSheet(
            currentMode = LanguageMode.fromLocale(data.locale),
            onDismiss = { showLanguageSheet = false },
            onSelect = { mode ->
                viewModel.setLocale(mode.locale)
                showLanguageSheet = false
            },
        )
    }

    val changePasswordState = viewModel.changePasswordState.collectAsStateWithLifecycle().value
    val editing = changePasswordState as? ChangePasswordState.Editing
    if (editing != null) {
        ChangePasswordSheet(
            state = editing,
            onOldPasswordChange = viewModel::onOldPasswordChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onSubmit = viewModel::submitChangePassword,
            onDismiss = viewModel::dismissChangePassword,
        )
    }

    val toast = rememberToast()
    val tip = viewModel.tip.collectAsStateWithLifecycle().value
    SubmitTipHost(tip = tip, onTipShown = viewModel::clearTip)

    LaunchedEffect(data.tunStackSwitchCount) {
        if (data.tunStackSwitchCount > 0) {
            toast.show(R.string.settings_tun_stack_switched)
            viewModel.consumeTunStackSwitch()
        }
    }
}
