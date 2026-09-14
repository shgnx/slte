package com.slte.app.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import com.slte.app.ui.theme.SlteType

/** HTML 标签检测：含标签即按 HTML 渲染（V2Board/Xboard 公告等富文本） */
private val HTML_TAG_REGEX = Regex("</?[a-zA-Z][^>]*>")

/**
 * 富文本组件：含 HTML 标签走 HtmlCompat，否则 Markdown。
 * 排版一律取 [SlteType] 角色（不用 M3 全局 Typography，保证与全项目同一套字号/行高/字距）。
 */
@Composable
fun RichText(
    text: String,
    modifier: Modifier = Modifier,
) {
    if (HTML_TAG_REGEX.containsMatchIn(text)) {
        HtmlText(html = text, modifier = modifier)
    } else {
        val typography =
            markdownTypography(
                h1 = SlteType.heading,
                h2 = SlteType.title,
                h3 = SlteType.title,
                h4 = SlteType.body,
                h5 = SlteType.body,
                h6 = SlteType.body,
                text = SlteType.body,
                paragraph = SlteType.body,
                ordered = SlteType.body,
                bullet = SlteType.body,
                list = SlteType.body,
                quote = SlteType.body.copy(fontStyle = FontStyle.Italic),
                link =
                SlteType.body.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                ),
            )
        Markdown(
            content = text,
            modifier = modifier,
            typography = typography,
        )
    }
}
