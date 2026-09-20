package com.slte.app.ui.screen.giftcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.remote.ApiException
import com.slte.app.data.repository.GiftCardRepository
import com.slte.app.ui.component.SubmitTip
import com.slte.app.utils.ApiErrors
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GiftCardRedeemState(
    val visible: Boolean = false,
    val code: String = "",
    val submitting: Boolean = false,
)

@HiltViewModel
class GiftCardRedeemViewModel
@Inject
constructor(
    private val giftCardRepository: GiftCardRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(GiftCardRedeemState())
    val state: StateFlow<GiftCardRedeemState> = _state.asStateFlow()

    private val _tip = MutableStateFlow<SubmitTip?>(null)
    val tip: StateFlow<SubmitTip?> = _tip.asStateFlow()

    private val _redeemed = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 1)
    val redeemed: SharedFlow<Unit> = _redeemed.asSharedFlow()

    private var requestId = 0

    fun open() {
        requestId++
        _state.value = GiftCardRedeemState(visible = true)
    }

    fun dismiss() {
        _state.value = GiftCardRedeemState()
    }

    fun updateCode(code: String) {
        val current = _state.value
        if (current.submitting) return
        _state.value = current.copy(code = code.take(CODE_MAX_LENGTH))
    }

    fun submit() {
        val current = _state.value
        val code = current.code.trim()
        if (current.submitting || code.isEmpty()) return

        _state.value = current.copy(submitting = true)
        val id = ++requestId
        viewModelScope.launch {
            giftCardRepository.redeem(code).fold(
                onSuccess = {
                    _tip.value = SubmitTip(messageRes = R.string.gift_card_success_toast)
                    if (id == requestId) _state.value = GiftCardRedeemState()
                    _redeemed.tryEmit(Unit)
                },
                onFailure = { e ->
                    _tip.value = e.toTip()
                    if (id == requestId) _state.value = _state.value.copy(submitting = false)
                },
            )
        }
    }

    fun clearTip() {
        _tip.value = null
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun ackRedeemed() {
        _redeemed.resetReplayCache()
    }

    private fun Throwable.toTip(): SubmitTip {
        val api = this as? ApiException
        if (api?.stringResId != null) return SubmitTip(messageRes = api.stringResId)
        val mapped = ErrorMessages.giftCardMessageRes(message)
        return when {
            mapped != null -> SubmitTip(messageRes = mapped)
            !message.isNullOrBlank() -> SubmitTip(message = message)
            else -> SubmitTip(messageRes = ApiErrors.GIFT_CARD)
        }
    }

    private companion object {
        const val CODE_MAX_LENGTH = 32
    }
}
