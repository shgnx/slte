package com.slte.app.ui.screen.invite

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.remote.ApiException
import com.slte.app.data.repository.InviteRepository
import com.slte.app.domain.model.CommissionRecord
import com.slte.app.domain.model.InviteCodeInfo
import com.slte.app.domain.model.InviteInfo
import com.slte.app.domain.model.InviteStat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 邀请返利页面数据；isEntering 为进入页面前的预加载状态。 */
data class InviteData(
    val stat: InviteStat = InviteStat(),
    val codes: List<InviteCodeInfo> = emptyList(),
    val records: List<CommissionRecord> = emptyList(),
    val isRefreshing: Boolean = false,
    val isEntering: Boolean = false,
    val isGenerating: Boolean = false,
    /** 转账/提现提交中，防重复提交 */
    val isSubmitting: Boolean = false,
    @StringRes val toastRes: Int? = null,
    val showTransferSheet: Boolean = false,
    val showWithdrawSheet: Boolean = false,
)

@HiltViewModel
class InviteViewModel @Inject constructor(
    private val inviteRepository: InviteRepository,
) : ViewModel() {

    private val _data = MutableStateFlow(InviteData())
    val data: StateFlow<InviteData> = _data.asStateFlow()

    private var generateJob: Job? = null

    /** 进入页面前预加载 */
    fun enterAndRefresh() {
        _data.update { it.copy(isEntering = true) }
        refresh()
    }

    /** 刷新所有数据（邀请信息 + 佣金记录并行请求） */
    fun refresh() {
        _data.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val inviteResult: Result<InviteInfo>
            val recordsResult: Result<List<CommissionRecord>>
            coroutineScope {
                val inviteDeferred = async { inviteRepository.fetchInviteInfo() }
                val recordsDeferred = async { inviteRepository.fetchCommissionRecords() }
                inviteResult = inviteDeferred.await()
                recordsResult = recordsDeferred.await()
            }

            inviteResult.onSuccess { info ->
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
        }
    }

    /** 生成新邀请码 */
    fun generateCode() {
        _data.update { it.copy(isGenerating = true) }
        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            inviteRepository.generateInviteCode().onSuccess { success ->
                if (success) {
                    refresh()
                    _data.update { it.copy(isGenerating = false, toastRes = R.string.invite_success_generate) }
                } else {
                    _data.update { it.copy(isGenerating = false, toastRes = R.string.invite_error_generate) }
                }
            }.onFailure { e ->
                // 服务端约定：已达创建数量上限时错误 message 含"上限"字样，
                // 此时直接提示已达上限，避免误导为通用生成失败
                val toastRes = if (e is ApiException && e.message?.contains("上限") == true) {
                    R.string.invite_error_generate_limit
                } else {
                    R.string.invite_error_generate
                }
                _data.update { it.copy(isGenerating = false, toastRes = toastRes) }
            }
        }
    }

    /** 取消全屏加载（系统返回/点击遮罩） */
    fun cancelLoading() {
        generateJob?.cancel()
        _data.update { it.copy(isGenerating = false) }
    }

    /** 佣金转余额 */
    fun transferCommission(yuanAmount: Double) {
        if (_data.value.isSubmitting) return
        // 分值用 Long 承载并夹取到 Int 上限：超大输入经 toInt() 回绕会变成小正数绕过检查
        val cents = Math.round(yuanAmount * 100)
        if (cents <= 0) return
        val capped = cents.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        _data.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            inviteRepository.transferCommission(capped)
                .onSuccess { success ->
                    if (success) {
                        _data.update { it.copy(showTransferSheet = false, toastRes = R.string.invite_success_transfer) }
                        refresh()
                    } else {
                        _data.update { it.copy(toastRes = R.string.invite_error_transfer) }
                    }
                }
                .onFailure {
                    _data.update { it.copy(toastRes = R.string.invite_error_transfer) }
                }
            _data.update { it.copy(isSubmitting = false) }
        }
    }

    /** 佣金提现 */
    fun withdraw(method: String, account: String) {
        if (_data.value.isSubmitting) return
        _data.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            inviteRepository.withdrawCommission(method, account)
                .onSuccess { success ->
                    if (success) {
                        _data.update { it.copy(showWithdrawSheet = false, toastRes = R.string.invite_success_withdraw) }
                        refresh()
                    } else {
                        _data.update { it.copy(toastRes = R.string.invite_error_withdraw) }
                    }
                }
                .onFailure {
                    _data.update { it.copy(toastRes = R.string.invite_error_withdraw) }
                }
            _data.update { it.copy(isSubmitting = false) }
        }
    }

    fun showTransferSheet() = _data.update { it.copy(showTransferSheet = true) }
    fun showWithdrawSheet() = _data.update { it.copy(showWithdrawSheet = true) }
    fun hideTransferSheet() = _data.update { it.copy(showTransferSheet = false) }
    fun hideWithdrawSheet() = _data.update { it.copy(showWithdrawSheet = false) }
    fun clearToast() = _data.update { it.copy(toastRes = null) }
}
