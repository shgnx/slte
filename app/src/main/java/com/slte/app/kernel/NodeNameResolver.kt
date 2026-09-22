package com.slte.app.kernel

internal object NodeNameResolver {

    private const val FULL_WIDTH_FIRST = 0xFF01
    private const val FULL_WIDTH_LAST = 0xFF5E
    private const val FULL_WIDTH_OFFSET = 0xFEE0
    private const val MAX_STRIP_PASSES = 4

    private val ZERO_WIDTH = Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2060-\\u2064\\uFEFF]")

    private val LEADING_DECORATION =
        Regex("^\\s*(?:\\[[^\\[\\]]*\\]|【[^【】]*】|\\([^()]*\\)|（[^（）]*）)\\s*")

    private val PROTOCOL_TAG_PREFIX =
        Regex(
            "^\\s*\\[(?:vless|vmess|trojan|ss|hy|hy2|tuic|anytls|socks)\\]\\s*",
            RegexOption.IGNORE_CASE,
        )

    private val SYMBOL_DECORATION = Regex(
        "[\\u2190-\\u21FF\\u2300-\\u23FF\\u25A0-\\u2775\\u27A0-\\u27BF" +
            "\\u2B00-\\u2BFF\\u3000-\\u303F\\uFE00-\\uFE0F" +
            "\\x{1F000}-\\x{1F0FF}\\x{1F1E6}-\\x{1FAFF}]",
    )

    private val WHITESPACE = Regex("[\\s\\u00A0\\u1680\\u2000-\\u200A\\u202F\\u205F\\u3000]+")

    fun of(name: String): String {
        var body = foldWidth(ZERO_WIDTH.replace(name, ""))
        var passes = 0
        while (passes < MAX_STRIP_PASSES) {
            val next = foldWidth(SYMBOL_DECORATION.replace(stripLeadingDecoration(body), ""))
            if (next == body) break
            body = next
            passes++
        }
        return WHITESPACE.replace(body, "").lowercase()
    }

    fun resolve(
        members: List<String>,
        target: String,
    ): String? {
        members.firstOrNull { it == target }?.let { return it }

        val key = of(target)
        if (key.isEmpty()) return null

        return members.filter { of(it) == key }.singleOrNull()
    }

    fun protocolTag(name: String): String? = PROTOCOL_TAG_PREFIX
        .find(name)
        ?.value
        ?.trim()
        ?.removeSurrounding("[", "]")
        ?.lowercase()

    fun displayName(name: String): String = PROTOCOL_TAG_PREFIX.replace(name, "").trim()

    private fun stripLeadingDecoration(name: String): String {
        var body = name
        while (true) {
            val next = LEADING_DECORATION.replaceFirst(body, "")
            if (next == body) return body
            body = next
        }
    }

    private fun foldWidth(text: String): String {
        val builder = StringBuilder(text.length)
        text.forEach { char ->
            builder.append(
                when {
                    char == '\u3000' -> ' '
                    char.code in FULL_WIDTH_FIRST..FULL_WIDTH_LAST -> (char.code - FULL_WIDTH_OFFSET).toChar()
                    else -> char
                },
            )
        }
        return builder.toString()
    }
}
