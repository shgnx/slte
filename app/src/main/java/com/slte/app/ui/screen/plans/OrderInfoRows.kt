package com.slte.app.ui.screen.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

@Composable
internal fun OrderInfoRow(
    label: String,
    value: String,
    isValueEmphasize: Boolean = false,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.gap.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = SlteType.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = SlteType.bodySmall,
            fontWeight = if (isValueEmphasize) FontWeight.SemiBold else FontWeight.Medium,
            color =
            if (isValueEmphasize) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
internal fun OrderInfoDivider() {
    androidx.compose.foundation.layout.Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(Dimens.dividerThickness)
            .padding(horizontal = Dimens.dividerThickness)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = Dimens.dividerAlpha)),
    )
}

@Composable
internal fun PriceRow(
    label: String,
    value: String,
    isBold: Boolean = false,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.gap.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = SlteType.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = SlteType.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
