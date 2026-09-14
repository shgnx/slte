package com.slte.app.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteTheme
import com.slte.app.utils.Dimens

/**
 * 共享组件设计系统预览：覆盖 component/ 下可独立渲染（无需 ViewModel / Hilt）的基础组件，
 * 提供浅色/深色两档，方便改主题令牌时直接目视回归。
 * 预览体使用示例文案字面量，不进运行时资源，不参与 strings.xml 对齐。
 */
@Preview(name = "按钮 · 浅色", showBackground = true, widthDp = 360)
@Preview(name = "按钮 · 深色", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSlteButton() {
    SlteTheme {
        Column(
            modifier = Modifier.padding(Dimens.gap.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap.md),
        ) {
            SlteButton(text = "主要操作", onClick = {}, style = SlteButtonStyle.Primary)
            SlteButton(text = "推进操作", onClick = {}, style = SlteButtonStyle.Secondary)
            SlteButton(text = "中性操作", onClick = {}, style = SlteButtonStyle.Neutral)
            SlteButton(text = "行内操作", onClick = {}, style = SlteButtonStyle.Tonal)
            SlteButton(text = "紧凑 CTA", onClick = {}, style = SlteButtonStyle.Medium)
            SlteButton(text = "危险操作", onClick = {}, style = SlteButtonStyle.Danger)
            SlteButton(text = "禁用态", onClick = {}, enabled = false)
            SlteButton(text = "加载中", onClick = {}, loading = true)
        }
    }
}

@Preview(name = "输入框 · 浅色", showBackground = true, widthDp = 360)
@Preview(name = "输入框 · 深色", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSlteInput() {
    SlteTheme {
        Column(
            modifier = Modifier.padding(Dimens.gap.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap.md),
        ) {
            SlteInput(
                value = "",
                onValueChange = {},
                placeholder = "请输入邮箱",
                icon = SlteIcons.Email,
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Email,
            )
            SlteInput(
                value = "••••••••",
                onValueChange = {},
                placeholder = "密码",
                visualTransformation = PasswordVisualTransformation(),
                trailing = {
                    SlteButton(text = "显示", onClick = {}, style = SlteButtonStyle.Tonal)
                },
            )
            SlteInput(
                value = "utun",
                onValueChange = {},
                placeholder = "紧凑档",
                size = SlteInputSize.Compact,
            )
            SlteInput(
                value = "只读内容",
                onValueChange = {},
                placeholder = "只读",
                readOnly = true,
            )
        }
    }
}

@Preview(name = "行卡片 · 浅色", showBackground = true, widthDp = 360)
@Preview(name = "行卡片 · 深色", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSlteRowCard() {
    SlteTheme {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(Dimens.gap.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap.sm),
        ) {
            SlteRowCard(
                icon = SlteIcons.Server,
                title = "节点列表",
                value = "12 个",
                chevron = true,
                onClick = {},
            )
            SlteRowCard(
                icon = SlteIcons.Expiry,
                title = "到期时间",
                subtitle = "2026-12-31",
            )
            SlteRowCard(
                icon = SlteIcons.SyncSubscription,
                title = "更新订阅",
                onClick = {},
            )
        }
    }
}

@Preview(name = "开关 · 浅色", showBackground = true)
@Preview(name = "开关 · 深色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSlteSwitch() {
    SlteTheme {
        Column(
            modifier = Modifier.padding(Dimens.gap.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap.md),
        ) {
            SlteSwitch(checked = true, onCheckedChange = {})
            SlteSwitch(checked = false, onCheckedChange = {})
            SlteSwitch(checked = true, onCheckedChange = {}, enabled = false)
        }
    }
}

@Preview(name = "空态 · 浅色", showBackground = true, widthDp = 360, heightDp = 480)
@Preview(name = "空态 · 深色", showBackground = true, widthDp = 360, heightDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewEmptyState() {
    SlteTheme {
        EmptyState(
            title = "暂无订单",
            description = "购买套餐后可在这里查看订单记录",
            actionText = "去购买",
            onAction = {},
        )
    }
}

@Preview(name = "错误态 · 浅色", showBackground = true, widthDp = 360, heightDp = 480)
@Preview(name = "错误态 · 深色", showBackground = true, widthDp = 360, heightDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewErrorState() {
    SlteTheme {
        ErrorState(message = "加载失败，请检查网络后重试", onRetry = {})
    }
}

@Preview(name = "圆形图标按钮 · 浅色", showBackground = true)
@Preview(name = "圆形图标按钮 · 深色", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCircleIconButton() {
    SlteTheme {
        Column(
            modifier = Modifier.padding(Dimens.gap.lg),
            verticalArrangement = Arrangement.spacedBy(Dimens.gap.md),
        ) {
            CircleIconButton(icon = SlteIcons.Back, description = "返回", onClick = {})
            CircleIconButton(icon = SlteIcons.Support, description = "客服", onClick = {}, showBackground = false)
        }
    }
}
