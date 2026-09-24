package com.slte.app.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.data.local.SessionManager
import com.slte.app.data.repository.AuthRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.model.isPlanValid
import com.slte.app.domain.usecase.DaysUntilExpiryUseCase
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileData(
    val subscribeInfo: SubscribeInfo? = null,
    val isLoading: Boolean = true,
    val email: String = "",
    val balance: String = "0.00",

    val planName: String = "",
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val hasPlan: Boolean = false,
    val isValid: Boolean = false,
    val expiredAt: Long = 0L,
    val daysUntilExpired: Int? = null,
)

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    private val subscribeRepository: SubscribeRepository,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val expiryUseCase: DaysUntilExpiryUseCase,
) : ViewModel() {
    private val _data = MutableStateFlow(ProfileData())
    val data: StateFlow<ProfileData> = _data.asStateFlow()

    private val _errorMessageRes = MutableStateFlow<Int?>(null)
    val errorMessageRes: StateFlow<Int?> = _errorMessageRes.asStateFlow()

    private var loading = false

    init {

        val cachedUser = subscribeRepository.getCachedUserInfo()
        val cachedSubscribe = subscribeRepository.subscribeInfo.value
        val email =
            cachedUser?.email
                ?: (sessionManager.sessionState.value as? com.slte.app.domain.model.SessionState.LoggedIn)?.user?.email
                ?: ""
        if (cachedUser != null || cachedSubscribe != null) {
            _data.update {
                it.withPlanFields(cachedSubscribe).copy(
                    email = email,
                    balance = cachedUser?.balance ?: "0.00",
                    isLoading = false,
                )
            }
        }
        viewModelScope.launch {
            subscribeRepository.subscribeInfo.collect(::applySubscribeInfo)
        }
    }

    fun refresh(force: Boolean = false) {
        loadProfile(force)
    }

    fun retry() {
        loadProfile()
    }

    fun logout() {
        authRepository.logout()
    }

    private fun loadProfile(force: Boolean = false) {
        if (loading) return
        loading = true
        _errorMessageRes.value = null
        viewModelScope.launch {
            if (_data.value.subscribeInfo == null) {
                _data.update { it.copy(isLoading = true) }
            }
            val userResult = async { subscribeRepository.fetchUserInfo(force = force) }
            val subscribeResult = async { subscribeRepository.fetchSubscribeInfo(force = force) }

            userResult.await().fold(
                onSuccess = { user ->
                    _data.update { it.copy(email = user.email, balance = user.balance) }
                },
                onFailure = { },
            )
            subscribeResult.await().fold(
                onSuccess = { applySubscribeInfo(it) },
                onFailure = { throwable ->
                    _data.update { it.copy(isLoading = false) }
                    if (_data.value.subscribeInfo == null) {
                        _errorMessageRes.value = ErrorMessages.forSubscribe(throwable)
                    }
                },
            )
            loading = false
        }
    }

    private fun applySubscribeInfo(info: SubscribeInfo?) {
        _data.update {
            it.withPlanFields(info).copy(isLoading = if (info == null) it.isLoading else false)
        }
    }

    private fun ProfileData.withPlanFields(info: SubscribeInfo?): ProfileData = copy(
        subscribeInfo = info,
        planName = info?.planName ?: "",
        usedBytes = info?.usedTraffic ?: 0L,
        totalBytes = info?.transferEnable ?: 0L,
        hasPlan = info?.hasPlan == true,
        isValid = isPlanValid(info),
        expiredAt = info?.expiredAt ?: 0L,
        daysUntilExpired = expiryUseCase(info?.expiredAt ?: 0L),
    )
}
