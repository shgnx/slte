package com.slte.app.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material.icons.outlined.Translate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens

/**
 * 其他设置页面（二级页面）。
 *
 * 到期/流量邮件提醒已接服务端 API（user/update）；TUN 堆栈切换已接内核
 * （后台进程 ServiceStore 持久化，运行中原地重建 TUN）；修改密码已接服务端 API。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var showTunStackSheet by rememberSaveable { mutableStateOf(false) }
    var showLanguageSheet by rememberSaveable { mutableStateOf(false) }
    val data by viewModel.data.collectAsStateWithLifecycle()

    SlteScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.dashboardScreenPaddingH),
            verticalArrangement = Arrangement.spacedBy(Dimens.dashboardCardSpacing),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Dimens.dashboardScreenPaddingV)
        ) {
            item {
                SettingsRowCard(
                    icon = Icons.Outlined.Settings,
                    title = stringResource(R.string.settings_tun_stack),
                    value = stringResource(data.tunStackMode.labelRes),
                    onClick = { showTunStackSheet = true }
                )
            }

            item {
                SettingsRowCard(
                    icon = Icons.Outlined.Translate,
                    title = stringResource(R.string.settings_language),
                    value = stringResource(LanguageMode.fromLocale(data.locale).labelRes),
                    onClick = { showLanguageSheet = true }
                )
            }

            item {
                SettingsRowCard(
                    icon = Icons.Outlined.Key,
                    title = stringResource(R.string.settings_change_password),
                    onClick = viewModel::showChangePassword
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Outlined.DarkMode,
                    title = stringResource(R.string.settings_dark_mode),
                    checked = data.darkModeEnabled,
                    onCheckedChange = viewModel::setDarkMode
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Outlined.Email,
                    title = stringResource(R.string.settings_expire_remind),
                    checked = data.expireRemindEnabled,
                    enabled = !data.remindSaving,
                    onCheckedChange = viewModel::setExpireRemind
                )
            }

            item {
                SettingsSwitchCard(
                    icon = Icons.Outlined.SwitchAccount,
                    title = stringResource(R.string.settings_traffic_remind),
                    checked = data.trafficRemindEnabled,
                    enabled = !data.remindSaving,
                    onCheckedChange = viewModel::setTrafficRemind
                )
            }

            data.errorMessageRes?.let { res ->
                item {
                    Text(
                        text = stringResource(res),
                        fontSize = TextSizes.inviteSheetDesc,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = Dimens.cardContentPadding)
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
            }
        )
    }

    if (showLanguageSheet) {
        LanguageModeSheet(
            currentMode = LanguageMode.fromLocale(data.locale),
            onDismiss = { showLanguageSheet = false },
            onSelect = { mode ->
                viewModel.setLocale(mode.locale)
                showLanguageSheet = false
            }
        )
    }

    val changePasswordState = viewModel.changePasswordState.collectAsStateWithLifecycle().value
    if (changePasswordState.showChangePasswordSheet) {
        ChangePasswordSheet(
            state = changePasswordState,
            onOldPasswordChange = viewModel::onOldPasswordChange,
            onNewPasswordChange = viewModel::onNewPasswordChange,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
            onToggleOldVisible = viewModel::toggleOldPasswordVisible,
            onToggleNewVisible = viewModel::toggleNewPasswordVisible,
            onSubmit = viewModel::submitChangePassword,
            onDismiss = viewModel::dismissChangePassword
        )
    }

    val context = LocalContext.current
    LaunchedEffect(changePasswordState.success) {
        if (changePasswordState.success) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.settings_change_pwd_success),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            viewModel.consumeChangePasswordSuccess()
        }
    }

    LaunchedEffect(changePasswordState.errorMessageRes) {
        changePasswordState.errorMessageRes?.let { res ->
            val toast = android.widget.Toast.makeText(
                context,
                context.getString(res),
                android.widget.Toast.LENGTH_SHORT
            )
            toast.setGravity(android.view.Gravity.CENTER, 0, 0)
            toast.show()
            viewModel.consumeChangePasswordError()
        }
    }

    LaunchedEffect(data.tunStackSwitchCount) {
        if (data.tunStackSwitchCount > 0) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.settings_tun_stack_switched),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            viewModel.consumeTunStackSwitch()
        }
    }
}
