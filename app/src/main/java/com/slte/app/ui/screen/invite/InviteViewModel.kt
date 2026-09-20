package com.slte.app.ui.screen.invite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.remote.ApiException
import com.slte.app.data.repository.InviteRepository
import com.slte.app.domain.model.CommissionRecord
import com.slte.app.domain.model.InviteCodeInfo
import com.slte.app.domain.model.InviteInfo
import com.slte.app.domain.model.InviteStat
import com.slte.app.ui.component.SubmitTip
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WithdrawMethodsState {
    object Loading : WithdrawMethodsState

    object Failed : WithdrawMethodsState

    data class Ready(val methods: List<String>) : WithdrawMethodsState
}

enum class InviteSheet { None, Transfer, Withdraw }

data class InviteData(
    val stat: InviteStat = InviteStat(),
    val codes: List<InviteCodeInfo> = emptyList(),
    val records: List<CommissionRecord> = emptyList(),
    val isRefreshing: Boolean = false,
    val isEntering: Boolean = false,
    val isGenerating: Boolean = false,

    val isSubmitting: Boolean = false,
    val tip: SubmitTip? = null,
    val sheet: InviteSheet = InviteSheet.None,
    val withdrawMethods: WithdrawMethodsState = WithdrawMethodsState.Loading,
)

@HiltViewModel
class InviteViewModel
@Inject
constructor(
    private val inviteRepository: InviteRepository,
) : ViewModel() {
    private val _data = MutableStateFlow(InviteData())
    val data: StateFlow<InviteData> = _data.asStateFlow()

    private var generateJob: Job? = null

    fun enterAndRefresh() {
        _data.update { it.copy(isEntering = true) }
        refresh()
    }

    fun refresh() {
        _data.update { it.copy(isRefreshing = true, withdrawMethods = WithdrawMethodsState.Loading) }
        viewModelScope.launch {
            val inviteResult: Result<InviteInfo>
            val recordsResult: Result<List<CommissionRecord>>
            val methodsResult: Result<List<String>>
            coroutineScope {
                val inviteDeferred = async { inviteRepository.fetchInviteInfo() }
                val recordsDeferred = async { inviteRepository.fetchCommissionRecords() }
                val methodsDeferred = async { inviteRepository.fetchWithdrawMethods() }
                inviteResult = inviteDeferred.await()
                recordsResult = recordsDeferred.await()
                methodsResult = methodsDeferred.await()
            }

            inviteResult
                .onSuccess { info ->
                    _data.update {
                        it.copy(
                            stat = info.stat,
                            codes = info.codes,
                            isRefreshing = false,
                            isEntering = false,
                        )
                    }
                }.onFailure {
                    _data.update { it.copy(isRefreshing = false, isEntering = false) }
                }

            recordsResult.onSuccess { records ->
                _data.update { it.copy(records = records) }
            }

            methodsResult
                .onSuccess { methods ->
                    _data.update { it.copy(withdrawMethods = WithdrawMethodsState.Ready(methods)) }
                }.onFailure {
                    _data.update { it.copy(withdrawMethods = WithdrawMethodsState.Failed) }
                }
        }
    }

    fun retryWithdrawMethods() {
        if (_data.value.withdrawMethods is WithdrawMethodsState.Loading) return
        _data.update { it.copy(withdrawMethods = WithdrawMethodsState.Loading) }
        viewModelScope.launch {
            inviteRepository
                .fetchWithdrawMethods()
                .onSuccess { methods ->
                    _data.update { it.copy(withdrawMethods = WithdrawMethodsState.Ready(methods)) }
                }.onFailure {
                    _data.update { it.copy(withdrawMethods = WithdrawMethodsState.Failed) }
                }
        }
    }

    fun generateCode() {
        _data.update { it.copy(isGenerating = true) }
        generateJob?.cancel()
        generateJob =
            viewModelScope.launch {
                inviteRepository
                    .generateInviteCode()
                    .onSuccess { success ->
                        if (success) {
                            refresh()
                            _data.update { it.copy(isGenerating = false, tip = SubmitTip(messageRes = R.string.invite_success_generate)) }
                        } else {
                            _data.update { it.copy(isGenerating = false, tip = SubmitTip(messageRes = R.string.invite_error_generate)) }
                        }
                    }.onFailure { e ->

                        val tip =
                            if (e is ApiException && e.message?.contains("上限") == true) {
                                SubmitTip(messageRes = R.string.invite_error_generate_limit)
                            } else {
                                SubmitTip(messageRes = R.string.invite_error_generate)
                            }
                        _data.update { it.copy(isGenerating = false, tip = tip) }
                    }
            }
    }

    fun cancelLoading() {
        generateJob?.cancel()
        _data.update { it.copy(isGenerating = false) }
    }

    fun transferCommission(yuanAmount: Double) {
        if (_data.value.isSubmitting) return

        val cents = Math.round(yuanAmount * 100)
        if (cents <= 0) return
        val capped = cents.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        _data.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            inviteRepository
                .transferCommission(capped)
                .onSuccess { success ->
                    if (success) {
                        _data.update { it.copy(sheet = InviteSheet.None, tip = SubmitTip(messageRes = R.string.invite_success_transfer)) }
                        refresh()
                    } else {
                        _data.update { it.copy(tip = SubmitTip(messageRes = R.string.invite_error_transfer)) }
                    }
                }.onFailure {
                    _data.update { it.copy(tip = SubmitTip(messageRes = R.string.invite_error_transfer)) }
                }
            _data.update { it.copy(isSubmitting = false) }
        }
    }

    fun withdraw(
        method: String,
        account: String,
    ) {
        if (_data.value.isSubmitting) return
        _data.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            inviteRepository
                .withdrawCommission(method, account)
                .onSuccess { success ->
                    if (success) {
                        _data.update { it.copy(sheet = InviteSheet.None, tip = SubmitTip(messageRes = R.string.invite_success_withdraw)) }
                        refresh()
                    } else {
                        _data.update { it.copy(tip = SubmitTip(messageRes = R.string.invite_error_withdraw)) }
                    }
                }.onFailure {
                    _data.update { it.copy(tip = SubmitTip(messageRes = R.string.invite_error_withdraw)) }
                }
            _data.update { it.copy(isSubmitting = false) }
        }
    }

    fun showTransferSheet() = _data.update { it.copy(sheet = InviteSheet.Transfer) }

    fun showWithdrawSheet() = _data.update { it.copy(sheet = InviteSheet.Withdraw) }

    fun hideTransferSheet() = _data.update { it.copy(sheet = InviteSheet.None) }

    fun hideWithdrawSheet() = _data.update { it.copy(sheet = InviteSheet.None) }

    fun clearTip() = _data.update { it.copy(tip = null) }
}
