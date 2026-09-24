package com.slte.app.kernel

internal object SanitizerProxyNames {

    private const val PROXIES_KEY = "proxies"
    private const val PROVIDERS_KEY = "proxy-providers"
    private const val GROUPS_KEY = "proxy-groups"
    private const val NAME_KEY = "name"
    private const val RENAME_SUFFIX_LIMIT = 99
    private const val RENAME_MARKER = " #"

    private val ITEM_NAME = Regex("^\\s*-\\s*name\\s*:")

    private val NAME_KEY_ANYWHERE = Regex("(?:^|[\\[{,])\\s*-?\\s*['\"]?name['\"]?\\s*:")

    private val FLOW_PROXIES = Regex("proxies\\s*:\\s*\\[")

    fun dedupe(lines: MutableList<String>): Int {
        val proxiesIndex = SanitizerYamlLines.topLevelBlockIndices(lines, PROXIES_KEY).firstOrNull() ?: return 0
        val itemIndent = SanitizerYamlLines.blockItemIndent(lines, proxiesIndex) ?: return 0
        val limit = SanitizerYamlLines.topLevelBlockEnd(lines, proxiesIndex)

        val entries = mutableListOf<Entry>()
        var index = proxiesIndex + 1
        while (index < limit && index < lines.size) {
            val line = lines[index]
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                index++
                continue
            }
            if (SanitizerYamlLines.leadingIndent(line) != itemIndent ||
                !SanitizerRules.LIST_ITEM.containsMatchIn(line.trimStart())
            ) {
                index++
                continue
            }

            val itemEnd = SanitizerYamlLines.blockEndIndex(lines, index, itemIndent).coerceAtMost(limit)
            val span = nameSpan(lines, index, itemEnd)
            if (span != null) {
                val raw = lines[span.line].substring(span.start, span.end)
                val name = SanitizerYamlLines.unwrapQuotes(raw.trim()).trim()
                if (name.isNotEmpty()) entries.add(Entry(name, span))
            }
            index = if (itemEnd > index) itemEnd else index + 1
        }

        val used = entries.mapTo(mutableSetOf()) { it.name }
        val seen = mutableSetOf<String>()
        val renamed = linkedMapOf<String, MutableList<String>>()
        for (entry in entries) {
            if (seen.add(entry.name)) continue
            val fresh = freshName(entry.name, used)
            used.add(fresh)
            val line = lines[entry.span.line]
            lines[entry.span.line] = line.replaceRange(entry.span.start, entry.span.end, quote(fresh))
            renamed.getOrPut(entry.name) { mutableListOf() }.add(fresh)
        }

        val derived = derivedNames(entries.map { it.name }, renamed)
        if (derived.isEmpty()) return 0
        extendGroupMembers(lines, derived)
        return renamed.values.sumOf { it.size }
    }

    private fun derivedNames(
        proxyNames: List<String>,
        renamed: Map<String, List<String>>,
    ): Map<String, List<String>> {
        if (proxyNames.isEmpty()) return renamed
        val result = linkedMapOf<String, MutableList<String>>()
        renamed.forEach { (base, fresh) -> result.getOrPut(base) { mutableListOf() }.addAll(fresh) }
        for (name in proxyNames) {
            val marker = name.lastIndexOf(RENAME_MARKER)
            if (marker <= 0) continue
            if (name.substring(marker + RENAME_MARKER.length).toIntOrNull() == null) continue
            val base = name.substring(0, marker)
            val bucket = result.getOrPut(base) { mutableListOf() }
            if (name !in bucket) bucket.add(name)
        }
        return result
    }

    fun names(text: String): List<String> {
        val lines = SanitizerYamlLines.stripBom(text).lines()
        val proxiesIndex = SanitizerYamlLines.topLevelBlockIndices(lines, PROXIES_KEY).firstOrNull() ?: return emptyList()
        val end = SanitizerYamlLines.topLevelBlockEnd(lines, proxiesIndex)
        return lines.subList(proxiesIndex, end).flatMap(::nameValues)
    }

    fun duplicateNames(text: String): List<String> = names(text)
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys
        .toList()

    fun hasProviders(text: String): Boolean = SanitizerYamlLines
        .topLevelBlockIndices(SanitizerYamlLines.stripBom(text).lines(), PROVIDERS_KEY)
        .isNotEmpty()

    fun itemsWithoutName(text: String): Int {
        val lines = SanitizerYamlLines.stripBom(text).lines()
        val proxiesIndex = SanitizerYamlLines.topLevelBlockIndices(lines, PROXIES_KEY).firstOrNull() ?: return 0
        val itemIndent = SanitizerYamlLines.blockItemIndent(lines, proxiesIndex) ?: return 0
        val limit = SanitizerYamlLines.topLevelBlockEnd(lines, proxiesIndex)

        var total = 0
        var missing = 0
        var index = proxiesIndex + 1
        while (index < limit && index < lines.size) {
            val line = lines[index]
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                index++
                continue
            }
            if (SanitizerYamlLines.leadingIndent(line) != itemIndent ||
                !SanitizerRules.LIST_ITEM.containsMatchIn(line.trimStart())
            ) {
                index++
                continue
            }
            val itemEnd = SanitizerYamlLines.blockEndIndex(lines, index, itemIndent).coerceAtMost(limit)
            total++
            if (nameSpan(lines, index, itemEnd) == null) missing++
            index = if (itemEnd > index) itemEnd else index + 1
        }
        return if (total == 0) 0 else missing
    }

    private data class ValueSpan(val line: Int, val start: Int, val end: Int)

    private data class Entry(val name: String, val span: ValueSpan)

    private fun nameSpan(
        lines: List<String>,
        start: Int,
        end: Int,
    ): ValueSpan? {
        val head = lines[start]
        if (head.contains('{')) return flowSpan(start, head)

        ITEM_NAME.find(head)?.let { return valueSpan(start, head, it.range.last + 1) }

        for (index in start + 1 until end) {
            val line = lines[index]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val match = SanitizerRules.BLOCK_KEY.find(line) ?: continue
            if (match.groupValues[2] != NAME_KEY) continue
            return valueSpan(index, line, match.range.last + 1)
        }
        return null
    }

    private fun valueSpan(
        line: Int,
        text: String,
        from: Int,
    ): ValueSpan? {
        var index = from
        while (index < text.length && text[index] == ' ') index++
        if (index >= text.length) return null
        if (text[index] == '#') return null
        return ValueSpan(line, index, text.length)
    }

    private fun flowSpan(
        line: Int,
        text: String,
    ): ValueSpan? {
        val match = NAME_KEY_ANYWHERE.find(text) ?: return null
        var index = match.range.last + 1
        while (index < text.length && text[index] == ' ') index++
        val start = index
        var quote: Char? = null
        while (index < text.length) {
            val char = text[index]
            if (quote != null) {
                if (char == quote) quote = null
            } else {
                if (char == '"' || char == '\'') quote = char
                if (char == ',' || char == '}') break
            }
            index++
        }
        if (index <= start) return null
        return ValueSpan(line, start, index)
    }

    private fun nameValues(line: String): List<String> {
        if (line.isBlank() || line.trimStart().startsWith("#")) return emptyList()
        val values = mutableListOf<String>()
        var from = 0
        while (from < line.length) {
            val match = NAME_KEY_ANYWHERE.find(line, from) ?: break
            var index = match.range.last + 1
            while (index < line.length && line[index] == ' ') index++
            val start = index
            if (line.contains('{') || line.contains('[')) {
                var quote: Char? = null
                while (index < line.length) {
                    val char = line[index]
                    if (quote != null) {
                        if (char == quote) quote = null
                    } else {
                        if (char == '"' || char == '\'') quote = char
                        if (char == ',' || char == '}') break
                    }
                    index++
                }
            } else {
                index = line.length
            }
            val name = SanitizerYamlLines.unwrapQuotes(line.substring(start, index).trim()).trim()
            if (name.isNotEmpty()) values.add(name)
            from = if (index > match.range.last) index else match.range.last + 1
        }
        return values
    }

    private fun freshName(
        name: String,
        used: Set<String>,
    ): String {
        var suffix = 2
        while (suffix <= RENAME_SUFFIX_LIMIT) {
            val candidate = "$name$RENAME_MARKER$suffix"
            if (candidate !in used) return candidate
            suffix++
        }
        var fallback = RENAME_SUFFIX_LIMIT + 1
        while (true) {
            val candidate = "$name$RENAME_MARKER$fallback"
            if (candidate !in used) return candidate
            fallback++
        }
    }

    private fun quote(name: String): String = "\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun extendGroupMembers(
        lines: MutableList<String>,
        renamed: Map<String, List<String>>,
    ) {
        val groupsIndex = SanitizerYamlLines.topLevelBlockIndices(lines, GROUPS_KEY).firstOrNull() ?: return
        val itemIndent = SanitizerYamlLines.blockItemIndent(lines, groupsIndex) ?: return
        val keyIndent = itemIndent + "  "
        val pending = mutableListOf<Pair<Int, List<String>>>()

        var index = groupsIndex + 1
        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                index++
                continue
            }
            val indent = SanitizerYamlLines.leadingIndent(line)
            if (indent.length < itemIndent.length) break
            if (indent != itemIndent || !SanitizerRules.LIST_ITEM.containsMatchIn(line.trimStart())) {
                index++
                continue
            }
            if (line.contains('{')) {
                extendFlowGroup(lines, index, renamed)?.let { lines[index] = it }
                index++
                continue
            }
            if (!SanitizerRules.GROUP_ITEM_START.containsMatchIn(line)) {
                index++
                continue
            }

            val itemEnd = SanitizerYamlLines.blockEndIndex(lines, index, itemIndent)
            val listKeyIndex = SanitizerYamlLines.blockKeyLineIndex(lines, index, itemEnd, keyIndent, PROXIES_KEY)
            if (listKeyIndex != null && lines[listKeyIndex].substringAfter(':').trim().isEmpty()) {
                pending.addAll(collectMembers(lines, listKeyIndex + 1, itemEnd, keyIndent, renamed))
            }
            index = if (itemEnd > index) itemEnd else index + 1
        }

        for ((at, added) in pending.sortedByDescending { it.first }) {
            lines.addAll(at + 1, added)
        }
    }

    private fun extendFlowGroup(
        lines: List<String>,
        index: Int,
        renamed: Map<String, List<String>>,
    ): String? {
        val line = lines[index]
        val head = FLOW_PROXIES.find(line) ?: return null
        val open = head.range.last
        val close = flowListEnd(line, open)
        if (close < 0) return null

        val tokens = splitFlowList(line.substring(open + 1, close))
        if (tokens.isEmpty()) return null

        val existing = tokens.mapTo(mutableSetOf()) { SanitizerYamlLines.unwrapQuotes(it.trim()).trim() }
        val additions = mutableListOf<String>()
        for (token in tokens) {
            val name = SanitizerYamlLines.unwrapQuotes(token.trim()).trim()
            renamed[name]?.forEach { fresh -> if (existing.add(fresh)) additions.add(quote(fresh)) }
        }
        if (additions.isEmpty()) return null

        val merged = (tokens.map { it.trim() } + additions).joinToString(", ")
        return line.substring(0, open + 1) + merged + line.substring(close)
    }

    private fun flowListEnd(
        line: String,
        open: Int,
    ): Int {
        var index = open + 1
        var quote: Char? = null
        var depth = 0
        while (index < line.length) {
            val char = line[index]
            if (quote != null) {
                if (char == quote) quote = null
            } else {
                when (char) {
                    '"', '\'' -> quote = char
                    '[' -> depth++
                    ']' -> if (depth == 0) return index else depth--
                }
            }
            index++
        }
        return -1
    }

    private fun splitFlowList(inner: String): List<String> {
        val tokens = mutableListOf<String>()
        val buffer = StringBuilder()
        var quote: Char? = null
        for (char in inner) {
            if (quote != null) {
                buffer.append(char)
                if (char == quote) quote = null
                continue
            }
            when (char) {
                '"', '\'' -> {
                    quote = char
                    buffer.append(char)
                }
                ',' -> {
                    if (buffer.isNotBlank()) tokens.add(buffer.toString())
                    buffer.clear()
                }
                else -> buffer.append(char)
            }
        }
        if (buffer.isNotBlank()) tokens.add(buffer.toString())
        return tokens
    }

    private fun collectMembers(
        lines: List<String>,
        from: Int,
        to: Int,
        keyIndent: String,
        renamed: Map<String, List<String>>,
    ): List<Pair<Int, List<String>>> {
        val (existing, entries) = groupMembers(lines, from, to, keyIndent)
        val pending = mutableListOf<Pair<Int, List<String>>>()
        for ((index, name) in entries) {
            val fresh = renamed[name] ?: continue
            val additions = fresh.filter { existing.add(it) }
            if (additions.isEmpty()) continue
            val indent = SanitizerYamlLines.leadingIndent(lines[index])
            pending.add(index to additions.map { indent + "- " + quote(it) })
        }
        return pending
    }

    private fun groupMembers(
        lines: List<String>,
        from: Int,
        to: Int,
        keyIndent: String,
    ): Pair<MutableSet<String>, List<Pair<Int, String>>> {
        val existing = mutableSetOf<String>()
        val entries = mutableListOf<Pair<Int, String>>()
        for (index in from until minOf(to, lines.size)) {
            val line = lines[index]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = SanitizerYamlLines.leadingIndent(line)
            if (indent.length <= keyIndent.length) break
            val trimmed = line.trimStart()
            if (!SanitizerRules.LIST_ITEM.containsMatchIn(trimmed)) continue
            val name = SanitizerYamlLines.unwrapQuotes(trimmed.removePrefix("-").trim()).trim()
            if (name.isEmpty()) continue
            existing.add(name)
            entries.add(index to name)
        }
        return existing to entries
    }
}
