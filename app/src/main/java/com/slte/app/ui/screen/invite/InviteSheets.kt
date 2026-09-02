package com.slte.app.ui.screen.invite

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import com.slte.app.R
import com.slte.app.ui.component.AppLocaleContent
import com.slte.app.ui.component.formatCurrency
import com.slte.app.ui.component.InputFieldColors
import com.slte.app.ui.component.LocalAppLocale
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferSheet(
    availableBalance: Int,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AppLocaleContent(locale = LocalAppLocale.current) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(
                        horizontal = Dimens.inviteSheetPaddingH,
                        vertical = Dimens.inviteSheetPaddingV
                    )
            ) {
                SheetFormTitle(text = stringResource(R.string.invite_transfer_title))

                Spacer(modifier = Modifier.height(Dimens.spacingXl))

                BalanceCard(
                    label = stringResource(R.string.invite_transfer_available),
                    cents = availableBalance
                ) {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            amountText = FormatUtils.balance(availableBalance)
                        },
                        shape = SlteShapes.medium
                    ) {
                        Text(stringResource(R.string.invite_transfer_all))
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingLg))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    placeholder = { Text(stringResource(R.string.invite_transfer_amount_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AttachMoney,
                            contentDescription = null,
                            tint = SlteColors.current.iconBlue
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = SlteShapes.medium,
                    colors = InputFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(Dimens.spacingXl))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        amountText.toDoubleOrNull()?.let { onConfirm(it) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeight),
                    shape = SlteShapes.medium,
                    enabled = amountText.toDoubleOrNull()?.let { it > 0 } ?: false
                ) {
                    Text(stringResource(R.string.invite_transfer_confirm))
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSheet(
    availableBalance: Int,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMethod by remember { mutableStateOf("") }
    var account by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AppLocaleContent(locale = LocalAppLocale.current) {
            val methods = stringArrayResource(R.array.invite_withdraw_methods).toList()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(
                        horizontal = Dimens.inviteSheetPaddingH,
                        vertical = Dimens.inviteSheetPaddingV
                    )
            ) {
                SheetFormTitle(text = stringResource(R.string.invite_withdraw_title))

                Spacer(modifier = Modifier.height(Dimens.spacingXl))

                BalanceCard(
                    label = stringResource(R.string.invite_withdraw_available),
                    cents = availableBalance
                )

                Spacer(modifier = Modifier.height(Dimens.spacingLg))

                Text(
                    text = stringResource(R.string.invite_withdraw_method),
                    fontSize = TextSizes.actionSubtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSm)
                ) {
                    methods.forEach { m ->
                        val selected = selectedMethod == m
                        FilterChip(
                            selected = selected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedMethod = m
                            },
                            label = {
                                Text(
                                    text = m,
                                    fontSize = TextSizes.inviteSheetMethod,
                                    modifier = Modifier.padding(horizontal = Dimens.spacingSm)
                                )
                            },
                            shape = SlteShapes.medium,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingLg))

                OutlinedTextField(
                    value = account,
                    onValueChange = { input ->
                        account = input.filter { it.isLetterOrDigit() || it in "@.-_+" }.take(100)
                    },
                    placeholder = { Text(stringResource(R.string.invite_withdraw_account_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.AlternateEmail,
                            contentDescription = stringResource(R.string.invite_withdraw_account_hint),
                            tint = SlteColors.current.iconBlue
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = SlteShapes.medium,
                    colors = InputFieldColors()
                )

                Spacer(modifier = Modifier.height(Dimens.spacingXl))

                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onConfirm(selectedMethod, account)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.buttonHeight),
                    shape = SlteShapes.medium,
                    enabled = selectedMethod.isNotBlank() && account.isNotBlank()
                ) {
                    Text(stringResource(R.string.invite_withdraw_confirm))
                }
            }
        }
    }
}

/** 弹窗标题：居中，与全局底部弹窗标题风格一致 */
@Composable
private fun SheetFormTitle(text: String) {
    Text(
        text = text,
        fontSize = TextSizes.sheetTitle,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
}

/** 可用余额卡片：描边圆角卡片衬托金额，右侧可挂操作按钮（转额"全部"）。 */
@Composable
private fun BalanceCard(
    label: String,
    cents: Int,
    action: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.dividerThickness, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Dimens.inviteSheetBalancePaddingH,
                vertical = Dimens.inviteSheetBalancePaddingV
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BalanceAmount(label = label, cents = cents)
            action?.let {
                Spacer(modifier = Modifier.weight(1f))
                it()
            }
        }
    }
}

@Composable
private fun BalanceAmount(
    label: String,
    cents: Int
) {
    Column {
        Text(
            text = label,
            fontSize = TextSizes.inviteSheetBalanceLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Dimens.spacingXs))
        Text(
            text = formatCurrency(cents),
            fontSize = TextSizes.inviteSheetBalance,
            fontWeight = FontWeight.Bold,
            color = SlteColors.current.iconBlue
        )
    }
}
