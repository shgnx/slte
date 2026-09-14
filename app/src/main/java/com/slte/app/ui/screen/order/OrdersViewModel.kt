package com.slte.app.ui.screen.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.repository.OrderRepository
import com.slte.app.domain.model.OrderInfo
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrdersData(
    val orders: List<OrderInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isEntering: Boolean = false,
    val isRefreshing: Boolean = false,
    val toastRes: Int? = null,
)

@HiltViewModel
class OrdersViewModel
@Inject
constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val _data = MutableStateFlow(OrdersData())
    val data: StateFlow<OrdersData> = _data.asStateFlow()

    private val _errorMessageRes = MutableStateFlow<Int?>(null)
    val errorMessageRes: StateFlow<Int?> = _errorMessageRes.asStateFlow()

    /** 预加载入口：点击时调用，数据就绪后 SlteApp 切换到订单页 */
    fun enterAndRefresh() {
        _data.update { it.copy(isEntering = true) }
        loadOrders()
    }

    fun retry() {
        loadOrders()
    }

    fun refresh() {
        if (_data.value.isRefreshing) return
        _data.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            orderRepository.fetchOrders().fold(
                onSuccess = { orders ->
                    _data.update { it.copy(orders = orders, isLoading = false, isEntering = false, isRefreshing = false) }
                    _errorMessageRes.value = null
                },
                onFailure = { throwable ->
                    _data.update { it.copy(isLoading = false, isEntering = false, isRefreshing = false) }
                    _errorMessageRes.value = ErrorMessages.forOrder(throwable)
                },
            )
        }
    }

    fun cancelOrder(tradeNo: String) {
        viewModelScope.launch {
            orderRepository.cancelOrder(tradeNo).fold(
                onSuccess = {
                    _data.update { it.copy(toastRes = R.string.order_cancel_success) }
                    loadOrders()
                },
                onFailure = {
                    _data.update { it.copy(toastRes = R.string.order_cancel_failed) }
                },
            )
        }
    }

    fun clearToast() {
        _data.update { it.copy(toastRes = null) }
    }

    private fun loadOrders() {
        _errorMessageRes.value = null
        _data.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            orderRepository.fetchOrders().fold(
                onSuccess = { orders ->
                    _data.update { it.copy(orders = orders, isLoading = false, isEntering = false) }
                },
                onFailure = { throwable ->
                    _data.update { it.copy(isLoading = false, isEntering = false) }
                    _errorMessageRes.value = ErrorMessages.forOrder(throwable)
                },
            )
        }
    }
}
