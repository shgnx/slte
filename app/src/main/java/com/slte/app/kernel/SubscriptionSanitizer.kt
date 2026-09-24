package com.slte.app.kernel

import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog

object SubscriptionSanitizer {

    fun isValidSubscribeYaml(text: String): Boolean {
        if (text.isBlank()) return false
        if (text.any { it < ' ' && it != '\n' && it != '\r' && it != '\t' }) return false
        val body = SanitizerYamlLines.stripBom(text)
        val head = body.trimStart()
        if (head.startsWith("<") || head.startsWith("{")) return false
        return body.lineSequence().any { SanitizerRules.SUBSCRIBE_ENTRY_KEY.containsMatchIn(it.trimStart()) }
    }

    fun isKernelLoadable(text: String): Boolean {
        if (text.isBlank()) return false
        val names = SanitizerProxyNames.names(text)
        if (names.isEmpty()) return SanitizerProxyNames.hasProviders(text)
        if (SanitizerProxyNames.itemsWithoutName(text) > 0) return false
        return SanitizerProxyNames.duplicateNames(text).isEmpty()
    }

    fun sanitize(
        text: String,
        domains: List<String>,
    ): String {
        if (text.isBlank()) return text
        val lines = SanitizerYamlLines.stripBom(text).lines().toMutableList()
        SanitizerYamlLines.dedentRootKeys(lines)
        SanitizerYamlLines.normalizeQuotedKeys(lines)

        val portsRewritten = runStep("zeroTopLevelPorts") { SanitizerNeutralizer.zeroTopLevelPorts(lines) }
        val controlNeutralized = runStep("neutralizeControlSurface") { SanitizerNeutralizer.neutralizeControlSurface(lines) }
        runStep("clearSubtitlePattern") { SanitizerNeutralizer.clearSubtitlePattern(lines) }
        runStep("dedupeProxyNames") { SanitizerProxyNames.dedupe(lines) }
        runStep("injectHealthCheckConfig") { SanitizerInjector.injectHealthCheckConfig(lines) }

        var ruleInjected = true
        SanitizerInjector.directDomains(domains).forEach { domain ->
            if (!runStep("injectDirectRule") { SanitizerInjector.injectDirectRule(lines, domain) }) ruleInjected = false
            runStep("injectFakeIpFilter") { SanitizerInjector.injectFakeIpFilter(lines, domain) }
        }

        if (!portsRewritten || !controlNeutralized || !ruleInjected) {
            AppLog.w(SanitizerRules.LOG_TAG, "清洗关键步骤失败，放弃输出")
            return ""
        }
        if (SanitizerNeutralizer.hasUnsafeResidue(lines)) {
            AppLog.w(SanitizerRules.LOG_TAG, "清洗后仍存在危险顶层键，放弃输出")
            return ""
        }
        return lines.joinToString("\n")
    }

    private fun runStep(
        step: String,
        block: () -> Unit,
    ): Boolean = try {
        block()
        true
    } catch (e: Throwable) {
        AppLog.w(SanitizerRules.LOG_TAG, "清洗步骤 $step 出错，跳过该步: ${e.javaClass.simpleName}: ${sanitizeLog(e.message ?: "Unknown")}")
        false
    }
}
