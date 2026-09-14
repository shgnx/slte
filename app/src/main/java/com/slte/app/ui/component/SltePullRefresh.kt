package com.slte.app.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 项目统一下拉刷新容器（基于 Material3 PullToRefreshBox）。
 *
 * 指示器配色在本组件统一构建并随主题切换，各页面无需单独配置样式。
 *
 * @param isRefreshing 是否正在刷新（驱动指示器显示）
 * @param onRefresh 下拉触发回调（需防重入：回调内先置 isRefreshing=true）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SltePullRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                color = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            )
        },
    ) {
        content()
    }
}
