package com.slte.app.kernel

import com.slte.app.utils.AppLog

/**
 * 订阅 YAML 清洗器：落盘前清零入站端口、清空 ui-subtitle-pattern、注入直连规则与 fake-ip 豁免。
 * 同时补全测速配置（组 url/timeout、provider health-check），保证缺省订阅也走统一测速 URL 与超时。
 * 行级编辑不改变 YAML 结构；结构异常时安全跳过，绝不抛异常。
 */
object SubscriptionSanitizer {

    /** 测速 URL：官方内核默认（HTTPS），与主流客户端一致；明文 HTTP 探活易被节点/落地限制导致超时 */
    private const val HEALTH_CHECK_URL = "https://www.gstatic.com/generate_204"

    /** 测速超时（毫秒）：官方内核默认值 */
    private const val HEALTH_CHECK_TIMEOUT_MS = 5_000

    /** 需要测速配置的组类型：仅这些类型消费 url/timeout */
    private val HEALTH_CHECK_GROUP_TYPES = setOf("url-test", "fallback", "load-balance")

    /** 需清零的顶层端口键（0 = 不监听） */
    private val ZEROED_PORT_KEYS = setOf("port", "socks-port", "mixed-port", "redir-port", "tproxy-port")

    /** 匹配顶层 "key: value" 行 */
    private val TOP_LEVEL_KEY_VALUE = Regex("^(port|socks-port|mixed-port|redir-port|tproxy-port|allow-lan|bind-address)\\s*:\\s*.*$")

    /** 匹配任意缩进的 ui-subtitle-pattern 行 */
    private val SUBTITLE_PATTERN_LINE = Regex("^(\\s*ui-subtitle-pattern\\s*:\\s*).*$")

    private val PROXY_GROUPS_KEY = Regex("^proxy-groups\\s*:\\s*$")
    private val PROXY_PROVIDERS_KEY = Regex("^proxy-providers\\s*:\\s*$")
    private val GROUP_ITEM_START = Regex("^\\s*-\\s*name\\s*:")
    private val PROVIDER_KEY = Regex("^[A-Za-z0-9_-]+\\s*:\\s*$")
    private val BLOCK_KEY = Regex("^(\\s*)([A-Za-z0-9_-]+)\\s*:")
    private val RULES_KEY = Regex("^rules\\s*:\\s*$")
    private val DNS_KEY = Regex("^dns\\s*:\\s*$")
    private val FAKE_IP_FILTER_KEY = Regex("^fake-ip-filter\\s*:\\s*$")
    private val FLOW_START = Regex("^[^#].*\\{\\s*$")

    /**
     * 订阅内容校验：必须是含 proxies 块的 Clash YAML；
     * HTML 错误页/JSON 错误体等异常响应直接拒绝。
     */
    fun isValidSubscribeYaml(text: String): Boolean {
        if (text.isBlank()) return false
        val trimmed = text.trimStart()
        if (trimmed.startsWith("<") || trimmed.startsWith("{")) return false
        return text.contains("proxies:")
    }

    /**
     * 清洗订阅 YAML。
     *
     * @param domains 需要直连的自家域名列表（如 example.com），全部注入直连规则与 fake-ip 豁免
     * @return 清洗后的 YAML；异常时返回原文，不阻断订阅导入（行编辑出错时宁可保留原配置也不破坏订阅）。
     */
    fun sanitize(text: String, domains: List<String>): String {
        if (text.isBlank()) return text
        return try {
            val lines = text.lines().toMutableList()
            zeroTopLevelPorts(lines)
            clearSubtitlePattern(lines)
            injectHealthCheckConfig(lines)
            val validDomains = domains.filter { it.isNotBlank() }
            validDomains.forEach { injectDirectRule(lines, it) }
            validDomains.forEach { injectFakeIpFilter(lines, it) }
            lines.joinToString("\n")
        } catch (_: Exception) {
            AppLog.w("SLTE-Sanitizer", "sanitize 异常回退原文，内核补丁链兜底")
            text
        }
    }

    /** 补全测速配置：组缺 url/timeout 时注入，provider 缺 health-check 时注入（均幂等） */
    private fun injectHealthCheckConfig(lines: MutableList<String>) {
        injectGroupHealthCheck(lines)
        injectProviderHealthCheck(lines)
    }

    /** 组测速配置：仅 url-test/fallback/load-balance 组，缺 url/timeout 或值为空时补齐 */
    private fun injectGroupHealthCheck(lines: MutableList<String>) {
        val groupsIndex = lines.indexOfFirst { it.trim() == it && PROXY_GROUPS_KEY.matches(it.trim()) }
        if (groupsIndex < 0) return
        if (lines[groupsIndex].contains('[') || lines[groupsIndex].contains('{')) return
        val itemIndent = blockItemIndent(lines, groupsIndex) ?: return
        val keyIndent = itemIndent + "  "
        // 从后往前插入，避免先插入使后续行号失效
        val pending = mutableListOf<Pair<Int, List<String>>>()
        var i = groupsIndex + 1
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                i++
                continue
            }
            val indent = line.substringBefore(line.trimStart())
            if (indent.length < itemIndent.length) break
            if (indent != itemIndent || !GROUP_ITEM_START.containsMatchIn(line)) {
                i++
                continue
            }
            // flow 风格项（- {name: ...} / 行内列表）无法安全行级编辑，跳过
            if (line.contains('{') || line.contains('[')) {
                i++
                continue
            }
            val blockEnd = blockEndIndex(lines, i, itemIndent)
            if (groupType(lines, i, blockEnd, keyIndent) !in HEALTH_CHECK_GROUP_TYPES) {
                i = blockEnd
                continue
            }
            val additions = mutableListOf<String>()
            val replacements = mutableListOf<Pair<Int, String>>()
            for ((key, value) in listOf("url" to HEALTH_CHECK_URL, "timeout" to HEALTH_CHECK_TIMEOUT_MS.toString())) {
                when (blockKeyValue(lines, i, blockEnd, keyIndent, key)) {
                    KeyState.MISSING -> additions.add(keyIndent + "$key: $value")
                    KeyState.EMPTY -> blockKeyLineIndex(lines, i, blockEnd, keyIndent, key)
                        ?.let { replacements.add(it to keyIndent + "$key: $value") }
                    KeyState.PRESENT -> Unit
                }
            }
            if (additions.isNotEmpty()) pending.add(i + 1 to additions)
            // 空值行替换与插入位置（i+1）不重叠：先替换后插入，行号均保持有效
            for ((at, text) in replacements.asReversed()) {
                lines[at] = text
            }
            i = blockEnd
        }
        for ((at, additions) in pending.asReversed()) {
            lines.addAll(at, additions)
        }
    }

    /** provider 测速配置：缺 health-check 块时注入（含 URL/超时），已配置或 flow 风格跳过 */
    private fun injectProviderHealthCheck(lines: MutableList<String>) {
        val providersIndex = lines.indexOfFirst { it.trim() == it && PROXY_PROVIDERS_KEY.matches(it.trim()) }
        if (providersIndex < 0) return
        if (lines[providersIndex].contains('[') || lines[providersIndex].contains('{')) return
        val providerIndent = blockKeyIndent(lines, providersIndex) ?: return
        val pending = mutableListOf<Pair<Int, List<String>>>()
        var i = providersIndex + 1
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                i++
                continue
            }
            val indent = line.substringBefore(line.trimStart())
            if (indent.length < providerIndent.length) break
            if (indent != providerIndent || !PROVIDER_KEY.matches(line.trim())) {
                i++
                continue
            }
            // flow 风格 provider（含 { 或 [）无法安全行级编辑，跳过
            if (line.contains('{') || line.contains('[')) {
                i++
                continue
            }
            val blockEnd = blockEndIndex(lines, i, providerIndent)
            val childKeyIndent = childKeyIndent(lines, i, blockEnd, providerIndent)
            if (childKeyIndent == null) {
                i = blockEnd
                continue
            }
            if (hasBlockKey(lines, i, blockEnd, childKeyIndent, "health-check")) {
                i = blockEnd
                continue
            }
            // 内联 health-check（行内非纯键，如 flow 风格）视为已配置，避免重复键
            if (lines.subList(i, blockEnd).any {
                    it.contains("health-check:") && it.trim() != "health-check:"
                }) {
                i = blockEnd
                continue
            }
            pending.add(
                i + 1 to listOf(
                    childKeyIndent + "health-check:",
                    childKeyIndent + "  enable: true",
                    childKeyIndent + "  url: $HEALTH_CHECK_URL",
                    childKeyIndent + "  interval: 300",
                    childKeyIndent + "  timeout: $HEALTH_CHECK_TIMEOUT_MS",
                    childKeyIndent + "  lazy: true"
                )
            )
            i = blockEnd
        }
        for ((at, additions) in pending.asReversed()) {
            lines.addAll(at, additions)
        }
    }

    /** 块结束索引：下一个同层或更浅缩进的非空行；找不到返回行尾 */
    private fun blockEndIndex(lines: List<String>, start: Int, itemIndent: String): Int {
        for (i in start + 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = line.substringBefore(line.trimStart())
            if (indent.length <= itemIndent.length) return i
        }
        return lines.size
    }

    /** 块内是否已存在指定键（仅匹配 keyIndent 层级的块键） */
    private fun hasBlockKey(
        lines: List<String>,
        start: Int,
        end: Int,
        keyIndent: String,
        key: String
    ): Boolean = blockKeyValue(lines, start, end, keyIndent, key) != KeyState.MISSING

    /** 块键存在状态：缺失 / 存在但值为空 / 存在且有值 */
    private fun blockKeyValue(
        lines: List<String>,
        start: Int,
        end: Int,
        keyIndent: String,
        key: String
    ): KeyState {
        for (i in start + 1 until end) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = line.substringBefore(line.trimStart())
            if (indent.length < keyIndent.length) return KeyState.MISSING
            if (indent != keyIndent) continue
            val m = BLOCK_KEY.find(line) ?: continue
            if (m.groupValues[2] != key) continue
            val value = line.substringAfter(':').trim()
            return if (value.isEmpty() || value == "\"\"" || value == "''") KeyState.EMPTY else KeyState.PRESENT
        }
        return KeyState.MISSING
    }

    /** 块键所在行号：未找到返回 null */
    private fun blockKeyLineIndex(
        lines: List<String>,
        start: Int,
        end: Int,
        keyIndent: String,
        key: String
    ): Int? {
        for (i in start + 1 until end) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = line.substringBefore(line.trimStart())
            if (indent.length < keyIndent.length) return null
            if (indent != keyIndent) continue
            val m = BLOCK_KEY.find(line) ?: continue
            if (m.groupValues[2] == key) return i
        }
        return null
    }

    /** 组类型：块内 type 键的值（去引号小写）；未找到返回空串 */
    private fun groupType(lines: List<String>, start: Int, end: Int, keyIndent: String): String {
        for (i in start + 1 until end) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = line.substringBefore(line.trimStart())
            if (indent.length < keyIndent.length) return ""
            if (indent != keyIndent) continue
            val m = BLOCK_KEY.find(line) ?: continue
            if (m.groupValues[2] != "type") continue
            return line.substringAfter(':').trim().trim('\'').trim('"').lowercase()
        }
        return ""
    }

    /** 块内子键缩进：首个非空子键行的前导空白；空块返回 null */
    private fun childKeyIndent(lines: List<String>, start: Int, end: Int, parentIndent: String): String? {
        for (i in start + 1 until end) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = line.substringBefore(line.trimStart())
            if (indent.length <= parentIndent.length) return null
            return indent
        }
        return null
    }

    /** 清零顶层端口/allow-lan/bind-address（仅顶层行） */
    private fun zeroTopLevelPorts(lines: MutableList<String>) {
        for (i in lines.indices) {
            val line = lines[i]
            if (line.isEmpty() || line[0] == ' ' || line[0] == '\t') continue
            val m = TOP_LEVEL_KEY_VALUE.matchEntire(line) ?: continue
            lines[i] = when (m.groupValues[1]) {
                "allow-lan" -> "allow-lan: false"
                "bind-address" -> "bind-address: \"\""
                in ZEROED_PORT_KEYS -> "${m.groupValues[1]}: 0"
                else -> line
            }
        }
    }

    /** 清空 ui-subtitle-pattern（消除 ReDoS 输入面） */
    private fun clearSubtitlePattern(lines: MutableList<String>) {
        for (i in lines.indices) {
            val m = SUBTITLE_PATTERN_LINE.matchEntire(lines[i]) ?: continue
            lines[i] = m.groupValues[1] + "\"\""
        }
    }

    /** 注入直连规则：插入 rules 块头部，缩进跟随已有条目；flow 风格或缺失时跳过 */
    private fun injectDirectRule(lines: MutableList<String>, domain: String) {
        val rule = "DOMAIN-SUFFIX,$domain,DIRECT"
        if (lines.any { it.trim().removePrefix("- ").trim().trim('\'').trim('"') == rule }) return

        val rulesIndex = lines.indexOfFirst { it.trim() == it && RULES_KEY.matches(it.trim()) }
        if (rulesIndex < 0) return
        // flow 风格：rules: [ 同行有 [ 或 { ，无法安全行插入
        if (lines[rulesIndex].contains('[') || lines[rulesIndex].contains('{')) return

        val indent = blockItemIndent(lines, rulesIndex) ?: return
        lines.add(rulesIndex + 1, indent + "- '$rule'")
    }

    /** 注入 fake-ip-filter 条目：跟随已有块缩进；缺失时在 dns 块内新建；异常跳过 */
    private fun injectFakeIpFilter(lines: MutableList<String>, domain: String) {
        val entry = "+.$domain"
        if (lines.any { it.trim().removePrefix("- ").trim().trim('\'').trim('"') == entry }) return

        // 内联/flow 形式（行尾非冒号）无法安全合并：跳过注入避免重复键，域名豁免由内核 patchDns 兜底
        if (lines.any { it.trim().startsWith("fake-ip-filter:") && !it.trim().endsWith(":") }) return

        val keyIndex = lines.indexOfFirst { FAKE_IP_FILTER_KEY.matches(it.trim()) }
        if (keyIndex >= 0) {
            val indent = blockItemIndent(lines, keyIndex) ?: return
            lines.add(keyIndex + 1, indent + "- '$entry'")
            return
        }

        val dnsIndex = lines.indexOfFirst { it.trim() == it && DNS_KEY.matches(it.trim()) }
        if (dnsIndex < 0) return
        if (lines[dnsIndex].contains('{')) return // flow 风格

        val keyIndent = blockKeyIndent(lines, dnsIndex) ?: return
        lines.add(dnsIndex + 1, keyIndent + "fake-ip-filter:")
        lines.add(dnsIndex + 2, keyIndent + "    - '$entry'")
    }

    /** 块内条目缩进：取键后首个非空、非注释行的前导空白 */
    private fun blockItemIndent(lines: List<String>, keyIndex: Int): String? {
        for (i in keyIndex + 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            if (line.trimStart().startsWith("-")) {
                return line.substringBefore(line.trimStart())
            }
            return null // 下一行是键而非条目：块为空或格式异常
        }
        return null
    }

    /** 块内键缩进：取键后首个非空、非注释行的前导空白 */
    private fun blockKeyIndent(lines: List<String>, keyIndex: Int): String? {
        for (i in keyIndex + 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = line.substringBefore(line.trimStart())
            return indent
        }
        return null
    }

    /** 块键状态 */
    private enum class KeyState {
        MISSING,
        EMPTY,
        PRESENT
    }
}
