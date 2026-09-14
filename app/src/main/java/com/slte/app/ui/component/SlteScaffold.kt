package com.slte.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.slte.app.R
import com.slte.app.ui.theme.SlteIcons
import com.slte.app.ui.theme.SlteType

/**
 * 统一页面脚手架：保证所有二级页面 TopBar 样式一致。
 *
 * 顶栏背景需用 Modifier.background 自绘：Material3 TopAppBar 内部对 containerColor
 * 做了 animateColorAsState，直接用会导致主题切换时仅顶栏渐变。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlteScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        style = SlteType.title,
                    )
                },
                navigationIcon = {
                    CircleIconButton(
                        icon = SlteIcons.Back,
                        description = stringResource(R.string.back),
                        onClick = onBack,
                        showBackground = false,
                    )
                },
                actions = { actions() },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        content = content,
    )
}
