package com.slte.app.ui.screen.notice

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.domain.model.Notice
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoticeUiState(
    val isLoading: Boolean = true,
    /** 下拉刷新中：不显示全屏加载，仅驱动下拉指示器 */
    val isRefreshing: Boolean = false,
    val notices: List<Notice> = emptyList(),
    @StringRes val errorMessageRes: Int? = null,
    /** 刷新失败时的轻提示（已有列表时不打断，不置 errorMessageRes） */
    @StringRes val toastRes: Int? = null,
    /** 预加载中标志：加载完成后才切换到公告页面 */
    val isEntering: Boolean = false,
)

@HiltViewModel
class NoticeViewModel
@Inject
constructor(
    private val subscribeRepository: SubscribeRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoticeUiState())
    val uiState: StateFlow<NoticeUiState> = _uiState.asStateFlow()

    /** 预加载入口 */
    fun enterAndRefresh() {
        _uiState.update { it.copy(isEntering = true) }
        loadNotices()
    }

    fun loadNotices() {
        _uiState.update { it.copy(isLoading = true, errorMessageRes = null) }
        viewModelScope.launch {
            subscribeRepository.fetchNotices().fold(
                onSuccess = { notices ->
                    AppLog.d("SLTE-Notice", "fetchNotices: ${notices.size} 条")
                    _uiState.update {
                        it.copy(isLoading = false, notices = notices, isEntering = false)
                    }
                },
                onFailure = { e ->
                    AppLog.w("SLTE-Notice", "fetchNotices 失败: ${sanitizeLog(e.message ?: "Unknown")}")
                    _uiState.update {
                        it.copy(isLoading = false, errorMessageRes = R.string.notice_error, isEntering = false)
                    }
                },
            )
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            subscribeRepository.fetchNotices().fold(
                onSuccess = { notices ->
                    AppLog.d("SLTE-Notice", "refresh: ${notices.size} 条")
                    _uiState.update { it.copy(isRefreshing = false, notices = notices, errorMessageRes = null) }
                },
                onFailure = { e ->
                    AppLog.w("SLTE-Notice", "refresh 失败: ${sanitizeLog(e.message ?: "Unknown")}")
                    // 已有列表时刷新失败只弹轻提示，保留列表内容不打断阅读；无数据时走全屏错误态
                    val hasData = _uiState.value.notices.isNotEmpty()
                    _uiState.update {
                        if (hasData) {
                            it.copy(isRefreshing = false, toastRes = R.string.notice_refresh_failed)
                        } else {
                            it.copy(isRefreshing = false, errorMessageRes = R.string.notice_error)
                        }
                    }
                },
            )
        }
    }

    fun clearToast() = _uiState.update { it.copy(toastRes = null) }
}
