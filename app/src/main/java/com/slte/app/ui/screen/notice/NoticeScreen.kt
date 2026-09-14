package com.slte.app.ui.screen.notice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.R
import com.slte.app.domain.model.Notice
import com.slte.app.ui.component.EmptyState
import com.slte.app.ui.component.ErrorState
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.component.RichText
import com.slte.app.ui.component.SltePullRefresh
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.component.SlteSheet
import com.slte.app.ui.component.ToastTip
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

/**
 * 公告列表页面（二级页面）。
 *
 * 点击公告卡片后通过底部弹窗展示完整内容。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeScreen(
    onBack: () -> Unit,
    viewModel: NoticeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedNotice by remember { mutableStateOf<Notice?>(null) }

    ToastTip(
        messageRes = uiState.toastRes,
        onDismiss = viewModel::clearToast,
    )

    SlteScaffold(
        title = stringResource(R.string.notice_title),
        onBack = onBack,
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingContent(modifier = Modifier.padding(innerPadding))
        } else {
            SltePullRefresh(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.padding(innerPadding),
            ) {
                when {
                    uiState.errorMessageRes != null ->
                        PullRefreshScrollable {
                            ErrorState(
                                message = stringResource(uiState.errorMessageRes!!),
                                onRetry = viewModel::loadNotices,
                            )
                        }
                    uiState.notices.isEmpty() ->
                        PullRefreshScrollable {
                            EmptyContent()
                        }
                    else ->
                        NoticeList(
                            notices = uiState.notices,
                            onClick = { selectedNotice = it },
                        )
                }
            }
        }
    }

    selectedNotice?.let { notice ->
        NoticeDetailSheet(
            notice = notice,
            onDismiss = { selectedNotice = null },
        )
    }
}

@Composable
private fun NoticeList(
    notices: List<Notice>,
    onClick: (Notice) -> Unit,
) {
    LazyColumn(
        modifier =
        Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.dashboardScreenPaddingH),
        verticalArrangement = Arrangement.spacedBy(Dimens.gap.md),
        contentPadding =
        androidx.compose.foundation.layout.PaddingValues(
            vertical = Dimens.gap.lg,
        ),
    ) {
        items(notices.distinctBy { it.id }, key = { it.id }) { notice ->
            NoticeCard(
                notice = notice,
                onClick = { onClick(notice) },
            )
        }
    }
}

/**
 * 公告卡片：展示标题、摘要和标签。
 *
 * 正文仅显示前两行，超出部分截断，点击后弹窗展示完整内容。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoticeCard(
    notice: Notice,
    onClick: () -> Unit,
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimens.gap.lg,
                    vertical = Dimens.gap.lg,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = notice.title,
                    fontWeight = FontWeight.SemiBold,
                    style = SlteType.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(Dimens.gap.sm))
                Icon(
                    imageVector = SlteIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.icon.md),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (notice.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.gap.sm))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.noticeTagSpacing),
                ) {
                    notice.tags.forEach { tag ->
                        NoticeTag(text = tag)
                    }
                }
            }

            val plainBody =
                remember(notice.body) {
                    notice.body.replace(Regex("<[^>]*>"), "").trim()
                }
            if (plainBody.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Dimens.gap.sm))
                Text(
                    text = plainBody,
                    style = SlteType.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = Dimens.noticeBodyMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(Dimens.gap.sm))
            Text(
                text = FormatUtils.formatDate(notice.createdAt),
                style = SlteType.label,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.noticeTimeAlpha),
            )
        }
    }
}

@Composable
private fun NoticeTag(text: String) {
    Surface(
        shape = SlteShapes.small,
        color = SlteColors.current.accentInteractiveBg,
    ) {
        Text(
            text = text,
            style = SlteType.caption,
            fontWeight = FontWeight.Medium,
            color = SlteColors.current.accentInteractive,
            modifier =
            Modifier.padding(
                horizontal = Dimens.noticeTagPaddingH,
                vertical = Dimens.gap.xs,
            ),
        )
    }
}

/**
 * 公告详情底部弹窗。
 *
 * 使用 Compose Text 原生渲染 HTML 内容，无需 WebView，
 * 滚动由 ModalBottomSheet 自身管理。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoticeDetailSheet(
    notice: Notice,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SlteSheet(
        onDismiss = onDismiss,
        title = notice.title,
    ) {
        if (notice.tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.gap.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.noticeTagSpacing)) {
                notice.tags.forEach { tag -> NoticeTag(text = tag) }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.gap.xs))
        Text(
            text = FormatUtils.formatDate(notice.createdAt),
            style = SlteType.label,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = Dimens.noticeTimeAlpha),
        )

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        RichText(
            text = notice.body,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 空态/错误态的滚动容器：无列表内容时下拉手势依然可用（LazyColumn 提供嵌套滚动） */
@Composable
private fun PullRefreshScrollable(content: @Composable () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier.fillParentMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        LottieLoadingIcon(modifier = Modifier.size(Dimens.icon.lg))
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    EmptyState(
        title = stringResource(R.string.notice_empty),
        modifier = modifier,
    )
}
