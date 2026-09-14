package com.slte.app.ui.screen.invite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.slte.app.R
import com.slte.app.ui.component.SlteButton
import com.slte.app.ui.component.SlteButtonStyle
import com.slte.app.ui.component.SlteInput
import com.slte.app.ui.component.SlteInputSize
import com.slte.app.ui.component.SlteSheet
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils

/**
 * 佣金划转弹窗：可划转佣金（只读）+ 划转金额输入。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSheet(
    availableBalance: Int,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    SlteSheet(
        title = stringResource(R.string.invite_transfer_title),
        subtitle = stringResource(R.string.invite_transfer_subtitle, stringResource(R.string.app_name)),
        onDismiss = onDismiss,
    ) {
        ReadOnlyAmountField(cents = availableBalance)

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        SlteInput(
            value = amountText,
            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
            placeholder = stringResource(R.string.invite_transfer_amount_hint),
            icon = SlteIcons.Amount,
            keyboardType = KeyboardType.Decimal,
            size = SlteInputSize.Compact,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.xl))

        SlteButton(
            text = stringResource(R.string.invite_transfer_confirm),
            onClick = { amountText.toDoubleOrNull()?.let { onConfirm(it) } },
            modifier = Modifier.fillMaxWidth(),
            style = SlteButtonStyle.Primary,
            enabled = amountText.toDoubleOrNull()?.let { it > 0 } == true,
            loading = isSubmitting,
        )
    }
}

/**
 * 申请提现弹窗：提现方式（后端下发）+ 提现账号。
 * 未拿到后端列表前不渲染任何默认项。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSheet(
    methods: List<String>,
    isLoadingMethods: Boolean,
    methodsFailed: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onRetryMethods: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMethod by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    // 默认选中后端下发的第一个方式；列表为空（加载中/失败/暂无）时留空，由字段显示提示文案
    LaunchedEffect(methods) {
        if (selectedMethod !in methods) {
            selectedMethod = methods.firstOrNull().orEmpty()
        }
    }

    SlteSheet(
        title = stringResource(R.string.invite_withdraw_title),
        subtitle = stringResource(R.string.invite_withdraw_subtitle),
        onDismiss = onDismiss,
    ) {
        WithdrawMethodField(
            methods = methods,
            selected = selectedMethod,
            isLoading = isLoadingMethods,
            failed = methodsFailed,
            onRetry = onRetryMethods,
            onSelect = { method ->
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                selectedMethod = method
            },
        )

        Spacer(modifier = Modifier.height(Dimens.gap.md))

        SlteInput(
            value = account,
            onValueChange = { input ->
                account = input.filter { it.isLetterOrDigit() || it in "@.-_+" }.take(100)
            },
            placeholder = stringResource(R.string.invite_withdraw_account_hint),
            icon = SlteIcons.AtSign,
            iconDesc = stringResource(R.string.invite_withdraw_account_label),
            size = SlteInputSize.Compact,
        )

        Spacer(modifier = Modifier.height(Dimens.gap.xl))

        SlteButton(
            text = stringResource(R.string.invite_withdraw_confirm),
            onClick = { onConfirm(selectedMethod, account) },
            modifier = Modifier.fillMaxWidth(),
            style = SlteButtonStyle.Primary,
            enabled = selectedMethod.isNotBlank() && account.isNotBlank(),
            loading = isSubmitting,
        )
    }
}

@Composable
private fun WithdrawMethodField(
    methods: List<String>,
    selected: String,
    isLoading: Boolean,
    failed: Boolean,
    onRetry: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    // 加载失败时仍可点按重试；空列表（未失败）不可展开
    val enabled = !isLoading && (failed || methods.isNotEmpty())
    val hint =
        when {
            isLoading -> stringResource(R.string.invite_withdraw_methods_loading)
            failed -> stringResource(R.string.invite_withdraw_methods_failed)
            methods.isEmpty() -> stringResource(R.string.invite_withdraw_methods_empty)
            else -> stringResource(R.string.invite_withdraw_method_hint)
        }
    val haptic = LocalHapticFeedback.current

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val anchorWidth = maxWidth
        Surface(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (failed) onRetry() else expanded = true
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            shape = SlteShapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(Dimens.dividerThickness, MaterialTheme.colorScheme.outline),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(Dimens.size.button)
                    .padding(horizontal = Dimens.gap.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = SlteIcons.WithdrawMethod,
                    contentDescription = stringResource(R.string.invite_withdraw_method),
                    modifier = Modifier.size(Dimens.icon.md),
                    tint = SlteColors.current.accentInteractive,
                )
                Spacer(modifier = Modifier.width(Dimens.gap.sm))
                Text(
                    text = selected.ifBlank { hint },
                    style = SlteType.body,
                    color =
                    if (selected.isBlank()) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (expanded) SlteIcons.ExpandLess else SlteIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.icon.md),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier =
            Modifier
                .width(anchorWidth)
                .heightIn(max = Dimens.inviteMethodListMaxHeight),
            shape = SlteShapes.medium,
            containerColor = MaterialTheme.colorScheme.surface,
            border = BorderStroke(Dimens.dividerThickness, MaterialTheme.colorScheme.outline),
            shadowElevation = Dimens.popupShadowElevation,
        ) {
            methods.forEach { method ->
                val isSelected = method == selected
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(method)
                            expanded = false
                        }.padding(horizontal = Dimens.gap.lg, vertical = Dimens.gap.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = method,
                        style = SlteType.title,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = SlteIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(Dimens.icon.lg),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadOnlyAmountField(cents: Int) {
    SlteInput(
        value = FormatUtils.balance(cents),
        onValueChange = {},
        placeholder = "",
        icon = SlteIcons.Balance,
        iconDesc = stringResource(R.string.invite_transfer_available),
        readOnly = true,
        enabled = false,
        size = SlteInputSize.Compact,
    )
}
