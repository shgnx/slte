package com.slte.app.ui.screen.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.data.repository.OrderRepository
import com.slte.app.domain.model.PlanInfo
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlansData(
    val plans: List<PlanInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isEntering: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessageRes: Int? = null,
)

/**
 * 订阅 ViewModel：加载套餐列表。
 *
 * 使用预加载模式：enterAndRefresh() 触发加载，
 * 数据就绪后 isEntering 回到 false，由 SlteApp 控制页面切换。
 */
@HiltViewModel
class PlansViewModel
@Inject
constructor(
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val _data = MutableStateFlow(PlansData())
    val data: StateFlow<PlansData> = _data.asStateFlow()

    fun enterAndRefresh() {
        _data.update { it.copy(isEntering = true) }
        loadPlans()
    }

    fun retry() {
        loadPlans()
    }

    fun refresh() {
        if (_data.value.isRefreshing) return
        _data.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            orderRepository.fetchPlans().fold(
                onSuccess = { plans ->
                    _data.update {
                        it.copy(
                            plans = plans.filter { p -> p.show },
                            isLoading = false,
                            isEntering = false,
                            isRefreshing = false,
                            errorMessageRes = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    _data.update {
                        it.copy(
                            isLoading = false,
                            isEntering = false,
                            isRefreshing = false,
                            errorMessageRes = ErrorMessages.forOrder(throwable),
                        )
                    }
                },
            )
        }
    }

    private fun loadPlans() {
        _data.update { it.copy(isLoading = true, errorMessageRes = null) }
        viewModelScope.launch {
            orderRepository.fetchPlans().fold(
                onSuccess = { plans ->
                    _data.update {
                        it.copy(
                            plans = plans.filter { p -> p.show },
                            isLoading = false,
                            isEntering = false,
                            errorMessageRes = null,
                        )
                    }
                },
                onFailure = { throwable ->
                    _data.update {
                        it.copy(
                            isLoading = false,
                            isEntering = false,
                            errorMessageRes = ErrorMessages.forOrder(throwable),
                        )
                    }
                },
            )
        }
    }
}
