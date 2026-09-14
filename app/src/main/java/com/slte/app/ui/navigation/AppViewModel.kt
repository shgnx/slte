package com.slte.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.slte.app.data.local.LocaleStore
import com.slte.app.data.remote.config.CrispManager
import com.slte.app.domain.model.SessionManager
import com.slte.app.domain.model.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AppViewModel
@Inject
constructor(
    sessionManager: SessionManager,
    val crispManager: CrispManager,
    val localeStore: LocaleStore,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = sessionManager.sessionState

    val sessionExpiredEvents: SharedFlow<Unit> = sessionManager.sessionExpiredEvents

    /** 当前界面语言（null = 跟随系统），驱动根组件全树热切换 */
    val locale: StateFlow<Locale?> = localeStore.locale
}
