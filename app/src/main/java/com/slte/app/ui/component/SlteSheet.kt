package com.slte.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

/**
 * 项目统一底部弹窗骨架：标题（居中）+ 可选副标题 + 内容。
 *
 * 内边距与纵向节奏由本组件固定，各弹窗只提供内容。内容区字段间距用 gap.md、
 * 区块间距用 gap.xl、底部留白用 gap.lg。
 *
 * @param compact 紧凑模式：标题与内容间距减为 gap.sm（首行内容需紧贴标题的弹窗，如更新弹窗）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlteSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    compact: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = SlteShapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        AppLocaleContent(locale = LocalAppLocale.current) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = Dimens.sheetPaddingH,
                        vertical = Dimens.sheetPaddingV,
                    ),
            ) {
                header?.let { headerContent ->
                    // 头部内容（动画贴纸等）与标题一样居中，避免贴左
                    Column(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { headerContent() }
                }
                if (title != null) {
                    if (header != null) Spacer(modifier = Modifier.height(Dimens.gap.md))
                    Text(
                        text = title,
                        style = SlteType.heading,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(Dimens.gap.sm))
                    Text(
                        text = subtitle,
                        style = SlteType.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(if (compact) Dimens.gap.sm else Dimens.gap.xl))
                content()
                Spacer(modifier = Modifier.height(Dimens.gap.lg))
            }
        }
    }
}
