package com.slte.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.domain.model.SessionState
import com.slte.app.ui.component.AnimatedSticker
import com.slte.app.ui.component.AppLocaleContent
import com.slte.app.utils.Dimens
import com.slte.app.utils.Stickers

/** 应用根组件：按会话状态切换登录流与主界面；语言切换时全树原地重组 */
@Composable
fun SlteApp(
    viewModel: AppViewModel = hiltViewModel(),
) {
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val locale by viewModel.locale.collectAsStateWithLifecycle()

    // 会话过期被动登出：提示用户重新登录（手动登出不提示）
    LaunchedEffect(Unit) {
        viewModel.sessionExpiredEvents.collect {
            android.widget.Toast
                .makeText(
                    context,
                    context.getString(R.string.session_expired_relogin),
                    android.widget.Toast.LENGTH_LONG,
                ).show()
        }
    }

    AppLocaleContent(
        locale = locale,
        localeStore = viewModel.localeStore,
    ) {
        when (sessionState) {
            is SessionState.LoggedIn -> {
                val loggedIn = sessionState as SessionState.LoggedIn
                LoggedInApp(
                    accountKey = loggedIn.user.subscribeToken,
                    onSupport = { viewModel.crispManager.openChat(context, loggedIn.user.email) },
                )
            }
            is SessionState.LoggedOut -> AuthNavGraph()
            is SessionState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        }

        // 启动遮罩：会话恢复后自动消失
        AnimatedVisibility(
            visible = sessionState is SessionState.Loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedSticker(
                    assetPath = Stickers.LOGIN,
                    modifier = Modifier.size(Dimens.logoSize),
                )
            }
        }
    }
}
