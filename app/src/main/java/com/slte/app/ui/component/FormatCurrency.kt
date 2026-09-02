package com.slte.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.slte.app.R
import com.slte.app.utils.FormatUtils

/** 金额文本：货币符号（本地化资源） + 数值，如 "¥12.50" */
@Composable
fun formatCurrency(cents: Int): String =
    stringResource(R.string.currency_symbol) + FormatUtils.balance(cents)

/** 扣减金额文本："-¥5.00"（折扣/扣减展示） */
@Composable
fun formatNegCurrency(cents: Int): String =
    "-" + stringResource(R.string.currency_symbol) + FormatUtils.balance(cents)
