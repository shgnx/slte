package com.slte.app.ui.screen.about

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.slte.app.BuildConfig
import com.slte.app.R
import com.slte.app.ui.component.AnimatedSticker
import com.slte.app.ui.component.SlteScaffold
import com.slte.app.ui.component.LottieLoadingIcon
import com.slte.app.ui.theme.SlteShapes
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens
import com.slte.app.utils.AppLog
import com.slte.app.utils.Constants
import com.slte.app.utils.Stickers

/**
 * 关于软件页面（二级页面）。
 *
 * 软件介绍 + 版本信息（应用版本/内核版本）+ 检查更新（底部弹窗）+ 日志导出。
 * 检查更新：拉取远程配置并弹出更新弹窗。
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: UpdateViewModel = hiltViewModel(key = "update")
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val kernelVersion by viewModel.kernelVersion.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

    SlteScaffold(
        title = stringResource(R.string.about_title),
        onBack = onBack
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.dashboardScreenPaddingH),
            verticalArrangement = Arrangement.spacedBy(Dimens.dashboardCardSpacing),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Dimens.dashboardScreenPaddingV)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SlteShapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.spacingXl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedSticker(
                            assetPath = Stickers.LOGIN,
                            modifier = Modifier.size(Dimens.logoSize)
                        )
                        Spacer(modifier = Modifier.height(Dimens.spacingMd))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = TextSizes.planName,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(Dimens.spacingSm))
                        Text(
                            text = stringResource(R.string.about_app_desc),
                            fontSize = TextSizes.inviteSheetDesc,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SlteShapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
                ) {
                    Column {
                        AboutRowContent(
                            icon = Icons.Outlined.Info,
                            title = stringResource(R.string.about_app_version),
                            value = BuildConfig.VERSION_NAME
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = Dimens.actionIconSize + Dimens.spacingMd * 2 + Dimens.cardContentPadding),
                            thickness = Dimens.dividerThickness,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        AboutRowContent(
                            icon = Icons.Outlined.Settings,
                            title = stringResource(R.string.about_kernel_version),
                            value = kernelVersion ?: Constants.PLACEHOLDER_DASH
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = Dimens.actionIconSize + Dimens.spacingMd * 2 + Dimens.cardContentPadding),
                            thickness = Dimens.dividerThickness,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(Dimens.actionRowHeight)
                                .clickable {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    viewModel.checkUpdate(manual = true)
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (state is UpdateUiState.Checking) {
                                LottieLoadingIcon(modifier = Modifier.size(Dimens.topBarActionIconSize))
                            } else {
                                Text(
                                    text = stringResource(R.string.about_check_update),
                                    fontSize = TextSizes.actionTitle,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(Dimens.spacingXs))
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(Dimens.dashboardChevronSize),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                AboutRowCard(
                    icon = Icons.Outlined.Description,
                    title = stringResource(R.string.about_log_export),
                    onClick = {
                        val file = AppLog.export(context)
                        if (file == null) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.about_log_export_failed),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            return@AboutRowCard
                        }
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.about_log_exported),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        try {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                clipData = ClipData.newRawUri(null, uri)
                                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.about_log_export))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(send, context.getString(R.string.about_log_export_share))
                            )
                        } catch (e: Exception) {
                        }
                    }
                )
            }
        }
    }

    LaunchedEffect(state) {
        val res = when (state) {
            is UpdateUiState.Latest -> R.string.about_latest
            is UpdateUiState.Error -> R.string.about_update_failed
            else -> null
        }
        if (res != null) {
            android.widget.Toast.makeText(context, context.getString(res), android.widget.Toast.LENGTH_SHORT).show()
            viewModel.consumeTip()
        }
    }
}

/** 关于页行卡片（与个人中心 NavigateCard 同款：图标 + 标题 + 可选副标题/值 + 箭头） */
@Composable
private fun AboutRowCard(
    icon: ImageVector,
    title: String,
    value: String? = null,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Card(
        onClick = {
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            onClick?.invoke()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = SlteShapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.cardElevation)
    ) {
        AboutRowContent(icon = icon, title = title, value = value, subtitle = subtitle, onClick = onClick)
    }
}

/** 关于页单行内容（无卡片外壳，供版本信息卡内嵌两行使用） */
@Composable
private fun AboutRowContent(
    icon: ImageVector,
    title: String,
    value: String? = null,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (subtitle != null) Dimens.inviteRecordItemHeight else Dimens.actionRowHeight)
            .padding(horizontal = Dimens.cardContentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(Dimens.actionIconSize),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(Dimens.spacingMd))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = TextSizes.actionTitle,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Dimens.spacingXs))
                Text(
                    text = subtitle,
                    fontSize = TextSizes.inviteSheetDesc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                fontSize = TextSizes.actionTitle,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (onClick != null) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(Dimens.dashboardChevronSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

