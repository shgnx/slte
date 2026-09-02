package com.slte.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil3.compose.SubcomposeAsyncImage
import com.slte.app.ui.theme.SlteColors
import com.slte.app.ui.theme.TextSizes
import com.slte.app.utils.Dimens

/**
 * 国旗组件：加载本地 SVG 国旗资源（assets/flags/{code}.svg）；
 * 资源缺失或加载失败时回退国家代码文字。
 *
 * @param countryCode ISO 3166-1 alpha-2 国家代码（如 "HK"）
 * @param size 国旗宽度；圆形时作为直径，长方形时高度按 4:3 比例
 * @param circular 圆形裁剪并铺满（节点列表），否则按原始比例显示（首页）
 */
@Composable
fun FlagPlaceholder(
    countryCode: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.flagSize,
    circular: Boolean = false
) {
    val normalized = countryCode.lowercase()
    val flagModifier = if (circular) {
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
    } else {
        modifier.size(width = size, height = size * FlagHeightRatio)
    }
    SubcomposeAsyncImage(
        model = "file:///android_asset/flags/$normalized.svg",
        contentDescription = countryCode,
        contentScale = if (circular) ContentScale.Crop else ContentScale.Fit,
        loading = { FlagFallback(countryCode, size, circular) },
        error = { FlagFallback(countryCode, size, circular) },
        modifier = flagModifier
    )
}

@Composable
private fun FlagFallback(countryCode: String, size: Dp, circular: Boolean) {
    val shape = if (circular) CircleShape else RoundedCornerShape(Dimens.flagCornerRadius)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = countryCode.uppercase(),
            fontSize = FlagFontSize,
            fontWeight = FontWeight.Bold,
            color = SlteColors.current.iconBlue
        )
    }
}

/** 特殊节点图标（自动选择、故障转移） */
@Composable
fun SpecialNodeIcon(
    icon: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(Dimens.flagSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = icon,
            fontSize = FlagFontSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private val FlagFontSize = TextSizes.flagFontSize

/** flag-icons 4x3 比例：高 = 宽 * 3/4 */
private val FlagHeightRatio = 0.75f
