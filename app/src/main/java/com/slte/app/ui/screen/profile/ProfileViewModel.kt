package com.slte.app.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.data.repository.AuthRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.domain.model.SessionManager
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.usecase.DaysUntilExpiryUseCase
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 个人中心页面数据。
 *
 * @param subscribeInfo 订阅信息，null 表示未加载或无订阅
 * @param isLoading 是否正在加载
 * @param email 用户邮箱
 */
data class ProfileData(
    val subscribeInfo: SubscribeInfo? = null,
    val isLoading: Boolean = true,
    val email: String = "",
    val balance: String = "0.00",
    /** 到期剩余天数；null 表示不限时套餐 */
    val daysUntilExpired: Int? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val subscribeRepository: SubscribeRepository,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val expiryUseCase: DaysUntilExpiryUseCase,
) : ViewModel() {

    private val _data = MutableStateFlow(ProfileData())
    val data: StateFlow<ProfileData> = _data.asStateFlow()

    private val _errorMessageRes = MutableStateFlow<Int?>(null)
    val errorMessageRes: StateFlow<Int?> = _errorMessageRes.asStateFlow()

    /** 加载防重入 */
    private var loading = false

    init {
        // 先显示本地缓存：离线也能看到上次的用户信息/订阅信息
        val cachedUser = subscribeRepository.getCachedUserInfo()
        val cachedSubscribe = subscribeRepository.getCachedSubscribeInfo()
        val email = cachedUser?.email
            ?: (sessionManager.sessionState.value as? com.slte.app.domain.model.SessionState.LoggedIn)?.user?.email
            ?: ""
        if (cachedUser != null || cachedSubscribe != null) {
            _data.update {
                it.copy(
                    subscribeInfo = cachedSubscribe,
                    email = email,
                    balance = cachedUser?.balance ?: "0.00",
                    daysUntilExpired = expiryDays(cachedSubscribe),
                    isLoading = false
                )
            }
        }
        loadProfile()
    }

    fun refresh() {
        loadProfile()
    }

    fun retry() {
        loadProfile()
    }

    /** 退出登录：吊销服务端会话并统一清理（内核/缓存由 SessionManager 处理） */
    fun logout() {
        authRepository.logout()
    }

    private fun loadProfile() {
        if (loading) return
        loading = true
        _errorMessageRes.value = null
        viewModelScope.launch {
            if (_data.value.subscribeInfo == null) {
                _data.update { it.copy(isLoading = true) }
            }
            val userResult = async { subscribeRepository.fetchUserInfo() }
            val subscribeResult = async { subscribeRepository.fetchSubscribeInfo() }

            userResult.await().fold(
                onSuccess = { user ->
                    _data.update { it.copy(email = user.email, balance = user.balance) }
                },
                onFailure = { /* 保留缓存展示 */ }
            )
            subscribeResult.await().fold(
                onSuccess = { info ->
                    _data.update {
                        it.copy(
                            subscribeInfo = info,
                            daysUntilExpired = expiryDays(info),
                            isLoading = false
                        )
                    }
                },
                onFailure = { throwable ->
                    _data.update { it.copy(isLoading = false) }
                    if (_data.value.subscribeInfo == null) {
                        _errorMessageRes.value = ErrorMessages.mapSubscribeError(throwable.message)
                    }
                }
            )
            loading = false
        }
    }

    /** 到期剩余天数；0/负到期时间视为不限时（null） */
    private fun expiryDays(info: SubscribeInfo?): Int? =
        info?.expiredAt?.takeIf { it > 0L }?.let { expiryUseCase(it) }
}
