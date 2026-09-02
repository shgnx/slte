package com.slte.app.ui.screen.invite

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.domain.model.CommissionRecord
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.utils.Dimens
import com.slte.app.utils.FormatUtils
import com.slte.app.ui.theme.TextSizes

@Composable
fun CommissionRecordsCard(records: List<CommissionRecord>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.inviteCodeCardPaddingH, vertical = Dimens.inviteCodeCardPaddingV)
        ) {
            Text(
                text = stringResource(R.string.invite_records_title),
                fontWeight = FontWeight.SemiBold,
                fontSize = TextSizes.dashboardUsageTitle,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (records.isEmpty()) {
                Text(
                    text = stringResource(R.string.invite_records_empty),
                    fontSize = TextSizes.inviteEmpty,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Dimens.spacingMd)
                )
            } else {
                Spacer(modifier = Modifier.height(Dimens.spacingSm))
                records.forEach { record ->
                    CommissionRecordItem(record = record)
                }
            }
        }
    }
}

@Composable
private fun CommissionRecordItem(record: CommissionRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.inviteRecordItemHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.invite_record_order, record.tradeNo),
                fontWeight = FontWeight.Medium,
                fontSize = TextSizes.inviteRecordOrder,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.spacingXs))
            Text(
                text = stringResource(R.string.invite_record_amount_label, FormatUtils.balance(record.orderAmount)),
                fontSize = TextSizes.inviteRecordOrder,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.invite_record_amount, FormatUtils.balance(record.getAmount)),
            fontWeight = FontWeight.SemiBold,
            fontSize = TextSizes.inviteRecordAmount,
            color = SlteColors.current.iconBlue
        )
    }
}
