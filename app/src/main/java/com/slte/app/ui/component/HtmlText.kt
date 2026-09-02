package com.slte.app.ui.component

import android.graphics.Typeface
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import com.slte.app.ui.theme.TextSizes

/** HTML 富文本组件：用 HtmlCompat 解析为 Spanned 再转 Compose AnnotatedString 渲染，避免 WebView 开销。
 *
 * @param html 原始 HTML 字符串
 * @param modifier 额外布局修饰
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier
) {
    val onSurface = MaterialTheme.colorScheme.onSurface

    val annotatedString = remember(html, onSurface) {
        spannedToAnnotatedString(html, onSurface)
    }

    Text(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium,
        color = onSurface,
        modifier = modifier
    )
}

/** 将 HTML 解析为 Spanned 后转 AnnotatedString，遍历 span 时保留源文本换行结构。 */
private fun spannedToAnnotatedString(
    html: String,
    defaultColor: Color
): AnnotatedString {
    val spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
    val source = spanned.toString()

    data class SpanInfo(
        val start: Int,
        val end: Int,
        val style: SpanStyle
    )

    val spans = mutableListOf<SpanInfo>()
    val allSpans = spanned.getSpans(0, source.length, Any::class.java)
    for (span in allSpans) {
        val start = spanned.getSpanStart(span)
        val end = spanned.getSpanEnd(span)
        if (start < 0 || end < 0 || start >= end || start > source.length || end > source.length) continue

        when (span) {
            is StyleSpan -> {
                val fontWeight = if (span.style == Typeface.BOLD) FontWeight.Bold else FontWeight.Normal
                val fontStyle = if (span.style == Typeface.ITALIC) FontStyle.Italic else FontStyle.Normal
                spans += SpanInfo(start, end, SpanStyle(fontWeight = fontWeight, fontStyle = fontStyle))
            }
            is ForegroundColorSpan -> {
                spans += SpanInfo(start, end, SpanStyle(color = Color(span.foregroundColor)))
            }
            is UnderlineSpan -> {
                spans += SpanInfo(start, end, SpanStyle(textDecoration = TextDecoration.Underline))
            }
            is RelativeSizeSpan -> {
                spans += SpanInfo(start, end, SpanStyle(fontSize = (TextSizes.htmlBaseFontSize.value * span.sizeChange).sp))
            }
        }
    }

    return buildAnnotatedString {
        append(source)
        for (info in spans) {
            addStyle(info.style, info.start, info.end)
        }
    }
}
