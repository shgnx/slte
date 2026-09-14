package com.slte.app.ui.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.component.ErrorState
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.component.SlteRowCard
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.SlteType
import com.slte.app.utils.Dimens

@Composable
internal fun UserInfoCard(
    email: String,
    balance: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            InfoRow(
                icon = SlteIcons.Email,
                text = "${stringResource(R.string.profile_email_label)} ${email.ifBlank { stringResource(R.string.profile_not_logged_in) }}",
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = Dimens.gap.lg),
                thickness = Dimens.dividerThickness,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            InfoRow(
                icon = SlteIcons.Balance,
                text =
                "${stringResource(R.string.purchase_balance)} " +
                    stringResource(R.string.currency_symbol) + balance,
            )
        }
    }
}

@Composable
internal fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(Dimens.size.row)
            .padding(horizontal = Dimens.gap.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.icon.lg),
            // 个人中心行＝导航场景：灰色图标（规范见 SlteIcons）
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(Dimens.gap.md))
        Text(
            text = text,
            style = SlteType.title,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun ErrorCard(
    messageRes: Int,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        ErrorState(
            message = stringResource(messageRes),
            onRetry = onRetry,
            modifier =
            Modifier.padding(
                horizontal = Dimens.gap.lg,
                vertical = Dimens.gap.lg,
            ),
        )
    }
}

/** 订阅卡加载占位：与 UsageCard 同尺寸卡片，居中加载动画 */
@Composable
internal fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        Box(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = Dimens.gap.xl),
            contentAlignment = Alignment.Center,
        ) {
            LottieLoadingIcon(modifier = Modifier.size(Dimens.loadingAnimSize))
        }
    }
}

@Composable
internal fun NavigateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
) = SlteRowCard(
    icon = icon,
    title = title,
    chevron = true,
    onClick = onClick,
)

@Composable
internal fun LogoutCard(onClick: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.medium,
        colors =
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation),
    ) {
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(Dimens.size.row)
                .padding(horizontal = Dimens.gap.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = SlteIcons.Logout,
                contentDescription = null,
                modifier = Modifier.size(Dimens.icon.lg),
                tint = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.width(Dimens.gap.md))
            Text(
                text = stringResource(R.string.profile_logout),
                style = SlteType.title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
