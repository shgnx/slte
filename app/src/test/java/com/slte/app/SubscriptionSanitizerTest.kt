package com.slte.app

import com.slte.app.kernel.SubscriptionSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.yaml.snakeyaml.Yaml

/**
 * 订阅清洗器测试：验证端口清零、pattern 清空与缩进感知注入
 * （直连规则/测速配置）在多种模板风格下产出结构合法且语义正确的 YAML。
 */
class SubscriptionSanitizerTest {
    private val healthCheckUrl = "https://www.gstatic.com/generate_204"

    /** 2 空格缩进模板（含已有 fake-ip-filter） */
    private val template2Space =
        """
        |mixed-port: 7890
        |allow-lan: true
        |bind-address: "*"
        |mode: rule
        |dns:
        |  enable: true
        |  enhanced-mode: fake-ip
        |  nameserver:
        |    - 223.5.5.5
        |  fake-ip-filter:
        |    - "*.lan"
        |    - "*.local"
        |clash-for-android:
        |  ui-subtitle-pattern: "^(a+)*\$"
        |proxies:
        |  - name: "jp-01"
        |    type: ss
        |    server: 1.2.3.4
        |    port: 8388
        |    cipher: aes-128-gcm
        |    password: "x"
        |rules:
        |  - MATCH,节点选择
        """.trimMargin()

    /** 4 空格缩进模板（模拟 Symfony Yaml::dump($config, 2, 4) 输出，无 fake-ip-filter） */
    private val template4Space =
        """
        |mixed-port: 7890
        |allow-lan: true
        |mode: rule
        |dns:
        |    enable: true
        |    enhanced-mode: fake-ip
        |proxies:
        |    - name: "jp-01"
        |      type: ss
        |      server: 1.2.3.4
        |      port: 8388
        |      cipher: aes-128-gcm
        |      password: "x"
        |rules:
        |    - MATCH,节点选择
        """.trimMargin()

    /** 含 proxy-groups（url-test 缺测速配置）的模板 */
    private val templateWithGroups =
        """
        |mixed-port: 7890
        |proxies:
        |  - name: "jp-01"
        |    type: ss
        |    server: 1.2.3.4
        |    port: 8388
        |proxy-groups:
        |  - name: "自动选择"
        |    type: url-test
        |    interval: 300
        |    proxies:
        |      - jp-01
        |rules:
        |  - MATCH,自动选择
        """.trimMargin()

    /** 含 proxy-providers（缺 health-check）的模板 */
    private val templateWithProviders =
        """
        |proxies:
        |  - name: "jp-01"
        |    type: ss
        |    server: 1.2.3.4
        |    port: 8388
        |proxy-providers:
        |  airport:
        |    type: http
        |    url: "https://sub.example.com"
        |    interval: 86400
        |proxy-groups:
        |  - name: "自动选择"
        |    type: url-test
        |    proxies:
        |      - jp-01
        |rules:
        |  - MATCH,自动选择
        """.trimMargin()

    private fun parseOk(text: String): Map<String, Any?> {
        val yaml = Yaml()
        val parsed = yaml.load<Any?>(text)
        @Suppress("UNCHECKED_CAST")
        return parsed as Map<String, Any?>
    }

    @Test
    fun `2空格模板 - 端口清零且结构合法`() {
        val out = SubscriptionSanitizer.sanitize(template2Space, listOf("example.com"))
        val doc = parseOk(out)
        assertEquals(0, doc["mixed-port"])
        assertEquals(false, doc["allow-lan"])
        assertEquals("", doc["bind-address"])
        // 节点 port 未被误清零
        val proxies = doc["proxies"] as List<Map<String, Any?>>
        assertEquals(8388, proxies[0]["port"])
    }

    @Test
    fun `2空格模板 - 直连规则与fake-ip注入且缩进正确`() {
        val out = SubscriptionSanitizer.sanitize(template2Space, listOf("example.com"))
        val doc = parseOk(out)
        val rules = doc["rules"] as List<String>
        assertEquals("DOMAIN-SUFFIX,example.com,DIRECT", rules.first())
        val dns = doc["dns"] as Map<String, Any?>
        val filter = dns["fake-ip-filter"] as List<String>
        assertTrue(filter.contains("+.example.com"))
        assertEquals(listOf("+.example.com", "*.lan", "*.local"), filter)
    }

    @Test
    fun `2空格模板 - ui-subtitle-pattern 被清空`() {
        val out = SubscriptionSanitizer.sanitize(template2Space, listOf("example.com"))
        val doc = parseOk(out)
        val cfa = doc["clash-for-android"] as Map<String, Any?>
        assertEquals("", cfa["ui-subtitle-pattern"])
    }

    @Test
    fun `4空格模板 - 注入后结构合法`() {
        val out = SubscriptionSanitizer.sanitize(template4Space, listOf("example.com"))
        val doc = parseOk(out)
        val rules = doc["rules"] as List<String>
        assertEquals("DOMAIN-SUFFIX,example.com,DIRECT", rules.first())
        val dns = doc["dns"] as Map<String, Any?>
        assertEquals(listOf("+.example.com"), dns["fake-ip-filter"])
        assertEquals(0, doc["mixed-port"])
        assertEquals(false, doc["allow-lan"])
    }

    @Test
    fun `组 - url-test缺测速配置时注入url与timeout`() {
        val out = SubscriptionSanitizer.sanitize(templateWithGroups, listOf("example.com"))
        val doc = parseOk(out)
        val group = (doc["proxy-groups"] as List<Map<String, Any?>>)[0]
        assertEquals(healthCheckUrl, group["url"])
        assertEquals(5_000, group["timeout"])
        assertEquals("url-test", group["type"])
    }

    @Test
    fun `组 - select组不注入测速配置`() {
        val selectGroup =
            """
            |proxy-groups:
            |  - name: "节点选择"
            |    type: select
            |    proxies:
            |      - jp-01
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(selectGroup, listOf("example.com"))
        val doc = parseOk(out)
        val group = (doc["proxy-groups"] as List<Map<String, Any?>>)[0]
        assertFalse(group.containsKey("url"))
        assertFalse(group.containsKey("timeout"))
    }

    @Test
    fun `组 - 已有测速配置保持原值不重复注入`() {
        val configured =
            """
            |proxy-groups:
            |  - name: "自动选择"
            |    type: url-test
            |    url: "https://www.gstatic.com/generate_204"
            |    timeout: 3000
            |    proxies:
            |      - jp-01
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(configured, listOf("example.com"))
        val doc = parseOk(out)
        val group = (doc["proxy-groups"] as List<Map<String, Any?>>)[0]
        assertEquals("https://www.gstatic.com/generate_204", group["url"])
        assertEquals(3000, group["timeout"])
    }

    @Test
    fun `组 - 空url补齐且不产生重复键`() {
        val emptyUrl =
            """
            |proxy-groups:
            |  - name: "自动选择"
            |    type: url-test
            |    url: ""
            |    proxies:
            |      - jp-01
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(emptyUrl, listOf("example.com"))
        val doc = parseOk(out)
        val group = (doc["proxy-groups"] as List<Map<String, Any?>>)[0]
        assertEquals(healthCheckUrl, group["url"])
        // 空值行被替换而非追加：url 键全文档唯一
        val urlLines = out.lineSequence().filter { it.trimStart().startsWith("url:") }.count()
        assertEquals(1, urlLines)
    }

    @Test
    fun `组 - flow风格项跳过注入`() {
        val flowGroup =
            """
            |proxy-groups:
            |  - {name: "自动选择", type: url-test, proxies: [jp-01]}
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(flowGroup, listOf("example.com"))
        val doc = parseOk(out)
        val group = (doc["proxy-groups"] as List<Map<String, Any?>>)[0]
        assertEquals("url-test", group["type"])
        assertFalse(group.containsKey("url"))
    }

    @Test
    fun `provider - 缺health-check时注入完整配置块`() {
        val out = SubscriptionSanitizer.sanitize(templateWithProviders, listOf("example.com"))
        val doc = parseOk(out)
        val provider = (doc["proxy-providers"] as Map<String, Any?>)["airport"] as Map<String, Any?>
        val healthCheck = provider["health-check"] as Map<String, Any?>
        assertEquals(true, healthCheck["enable"])
        assertEquals(healthCheckUrl, healthCheck["url"])
        assertEquals(5_000, healthCheck["timeout"])
        assertEquals(300, healthCheck["interval"])
        assertEquals(true, healthCheck["lazy"])
    }

    @Test
    fun `provider - 已有health-check不重复注入`() {
        val configured =
            """
            |proxy-providers:
            |  airport:
            |    type: http
            |    url: "https://sub.example.com"
            |    health-check:
            |      enable: true
            |      url: "https://www.gstatic.com/generate_204"
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(configured, listOf("example.com"))
        val doc = parseOk(out)
        val provider = (doc["proxy-providers"] as Map<String, Any?>)["airport"] as Map<String, Any?>
        val healthCheck = provider["health-check"] as Map<String, Any?>
        assertEquals("https://www.gstatic.com/generate_204", healthCheck["url"])
        val hcLines = out.lineSequence().filter { it.trimStart().startsWith("health-check:") }.count()
        assertEquals(1, hcLines)
    }

    @Test
    fun `provider - flow风格health-check不重复注入`() {
        val flow =
            """
            |proxy-providers:
            |  airport:
            |    type: http
            |    url: "https://sub.example.com"
            |    health-check: {enable: true, url: "https://www.gstatic.com/generate_204"}
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(flow, listOf("example.com"))
        val hcLines = out.lineSequence().filter { it.contains("health-check:") }.count()
        assertEquals(1, hcLines)
    }

    @Test
    fun `4空格模板 - 组注入后结构合法`() {
        val groups4Space =
            """
            |proxies:
            |    - name: "jp-01"
            |      type: ss
            |      server: 1.2.3.4
            |      port: 8388
            |proxy-groups:
            |    - name: "自动选择"
            |      type: url-test
            |      proxies:
            |        - jp-01
            |rules:
            |    - MATCH,自动选择
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(groups4Space, listOf("example.com"))
        val doc = parseOk(out)
        val group = (doc["proxy-groups"] as List<Map<String, Any?>>)[0]
        assertEquals(healthCheckUrl, group["url"])
        assertEquals(5_000, group["timeout"])
    }

    @Test
    fun `空域名列表 - 仍注入测速配置`() {
        val out = SubscriptionSanitizer.sanitize(templateWithGroups, emptyList())
        val doc = parseOk(out)
        val group = (doc["proxy-groups"] as List<Map<String, Any?>>)[0]
        assertEquals(healthCheckUrl, group["url"])
    }

    @Test
    fun `重复清洗幂等 - 测速配置不重复注入`() {
        val once = SubscriptionSanitizer.sanitize(templateWithProviders, listOf("example.com"))
        val twice = SubscriptionSanitizer.sanitize(once, listOf("example.com"))
        val doc = parseOk(twice)
        val group = (doc["proxy-groups"] as List<Map<String, Any?>>)[0]
        assertEquals(healthCheckUrl, group["url"])
        val provider = (doc["proxy-providers"] as Map<String, Any?>)["airport"] as Map<String, Any?>
        assertEquals(healthCheckUrl, (provider["health-check"] as Map<String, Any?>)["url"])
        // 注入的测速 url 恰好两条（组 url + health-check url），provider 自身 fetch url 不计入
        val injectedUrlLines =
            twice
                .lineSequence()
                .filter { it.trim() == "url: $healthCheckUrl" }
                .count()
        assertEquals(2, injectedUrlLines)
    }

    @Test
    fun `flow风格 rules - 跳过注入且不崩溃`() {
        val flow =
            """
            |mixed-port: 7890
            |rules: [MATCH,节点选择]
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(flow, listOf("example.com"))
        val doc = parseOk(out)
        assertEquals(0, doc["mixed-port"])
        // flow 风格下不注入（避免破坏结构）；YAML flow 序列按逗号切分
        assertEquals(listOf("MATCH", "节点选择"), doc["rules"])
    }

    @Test
    fun `无 rules 段 - 不崩溃且端口仍清零`() {
        val noRules =
            """
            |socks-port: 7891
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            |    cipher: aes-128-gcm
            |    password: "x"
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(noRules, listOf("example.com"))
        val doc = parseOk(out)
        assertEquals(0, doc["socks-port"])
        val proxies = doc["proxies"] as List<Map<String, Any?>>
        assertEquals(8388, proxies[0]["port"])
    }

    @Test
    fun `空输入与非法文本 - 安全返回`() {
        assertEquals("", SubscriptionSanitizer.sanitize("", listOf("example.com")))
        val garbage = "not: [valid: yaml\n:::"
        // 结构异常的行一律原样放行：既不抛异常，也不做任何注入
        val out = SubscriptionSanitizer.sanitize(garbage, listOf("example.com"))
        assertEquals(garbage, out)
        assertFalse(out.contains("DOMAIN-SUFFIX"))
    }

    @Test
    fun `重复清洗幂等 - 不重复注入`() {
        val once = SubscriptionSanitizer.sanitize(template2Space, listOf("example.com"))
        val twice = SubscriptionSanitizer.sanitize(once, listOf("example.com"))
        val doc = parseOk(twice)
        val rules = doc["rules"] as List<String>
        assertEquals(1, rules.count { it == "DOMAIN-SUFFIX,example.com,DIRECT" })
        val filter = (doc["dns"] as Map<String, Any?>)["fake-ip-filter"] as List<String>
        assertEquals(1, filter.count { it == "+.example.com" })
    }

    @Test
    fun `内容校验 - 接受含 proxies 的 Clash 配置`() {
        assertTrue(SubscriptionSanitizer.isValidSubscribeYaml(template2Space))
        assertTrue(SubscriptionSanitizer.isValidSubscribeYaml("  proxies: []"))
        assertTrue(SubscriptionSanitizer.isValidSubscribeYaml("proxies:\n- name: a"))
    }

    @Test
    fun `内容校验 - 拒绝空体与错误响应`() {
        assertFalse(SubscriptionSanitizer.isValidSubscribeYaml(""))
        assertFalse(SubscriptionSanitizer.isValidSubscribeYaml("   "))
        assertFalse(SubscriptionSanitizer.isValidSubscribeYaml("{\"data\":null,\"message\":\"已过期\"}"))
        assertFalse(SubscriptionSanitizer.isValidSubscribeYaml("<html><body>Not Found</body></html>"))
        assertFalse(SubscriptionSanitizer.isValidSubscribeYaml("proxies 缺失的普通文本"))
    }

    @Test
    fun `内联fake-ip-filter - 不重复注入且不崩溃`() {
        val inline =
            """
            |dns:
            |  enable: true
            |  fake-ip-filter: ["*.lan", "*.local"]
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(inline, listOf("example.com"))
        // 内联形式不展开成块、不产生重复键（豁免由内核兜底）
        val filterLines = out.lineSequence().filter { it.trim().startsWith("fake-ip-filter:") }.count()
        assertEquals(1, filterLines)
        assertTrue(out.contains("fake-ip-filter: [\"*.lan\", \"*.local\"]"))
    }

    /** 带控制面键的订阅：清空 external-controller/secret/external-ui 等，避免把内核 REST API 暴露出去 */
    @Test
    fun `控制面中和 - 清空控制API标量键`() {
        val src =
            """
            |external-controller: 0.0.0.0:9090
            |external-controller-tls: 0.0.0.0:9443
            |secret: hunter2
            |external-ui: /srv/ui
            |external-ui-url: https://evil.example/ui.zip
            |mixed-port: 7890
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            """.trimMargin()
        val doc = parseOk(SubscriptionSanitizer.sanitize(src, listOf("example.com")))
        assertEquals("", doc["external-controller"])
        assertEquals("", doc["external-controller-tls"])
        assertEquals("", doc["secret"])
        assertEquals("", doc["external-ui"])
        assertEquals("", doc["external-ui-url"])
        assertEquals(0, doc["mixed-port"])
    }

    @Test
    fun `控制面中和 - 删除hosts与script块且不误伤其他键`() {
        val src =
            """
            |hosts:
            |  'bank.example': 10.0.0.1
            |  '*.tracker.example': 127.0.0.1
            |script:
            |  code: |
            |    def main(ctx, md):
            |      return md
            |web:
            |  external-ui: /srv/ui
            |mixed-port: 7890
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            |rules:
            |  - MATCH,DIRECT
            """.trimMargin()
        val doc = parseOk(SubscriptionSanitizer.sanitize(src, listOf("example.com")))
        assertFalse(doc.containsKey("hosts"))
        assertFalse(doc.containsKey("script"))
        assertFalse(doc.containsKey("web"))
        assertEquals(0, doc["mixed-port"])
        assertTrue((doc["rules"] as List<*>).contains("MATCH,DIRECT"))
    }

    @Test
    fun `控制面中和 - 强制关闭tun并沿用块内缩进`() {
        val src =
            """
            |tun:
            |    enable: true
            |    stack: gvisor
            |    dns-hijack:
            |      - any:53
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            """.trimMargin()
        val out = SubscriptionSanitizer.sanitize(src, listOf("example.com"))
        val tun = parseOk(out)["tun"] as Map<String, Any?>
        assertEquals(false, tun["enable"])
        assertEquals("gvisor", tun["stack"])
        // 4 空格缩进被沿用：固定写 2 空格会产出混合缩进、直接变成非法 YAML
        assertTrue(out.contains("    enable: false"))
    }

    @Test
    fun `控制面中和 - flow风格与缺enable的tun都被关闭`() {
        val flow =
            """
            |tun: {enable: true, stack: gvisor}
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            """.trimMargin()
        val missing =
            """
            |tun:
            |    stack: system
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            """.trimMargin()
        assertEquals(false, (parseOk(SubscriptionSanitizer.sanitize(flow, listOf("example.com")))["tun"] as Map<String, Any?>)["enable"])
        assertEquals(false, (parseOk(SubscriptionSanitizer.sanitize(missing, listOf("example.com")))["tun"] as Map<String, Any?>)["enable"])
    }

    @Test
    fun `控制面中和 - authentication两种写法均被清空`() {
        val flow =
            """
            |authentication: ["admin:hunter2"]
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            """.trimMargin()
        val block =
            """
            |authentication:
            |  - "admin:hunter2"
            |  - "root:toor"
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            """.trimMargin()
        val flowDoc = parseOk(SubscriptionSanitizer.sanitize(flow, listOf("example.com")))
        assertEquals(emptyList<Any?>(), flowDoc["authentication"])
        val blockDoc = parseOk(SubscriptionSanitizer.sanitize(block, listOf("example.com")))
        assertFalse(blockDoc.containsKey("authentication"))
    }

    @Test
    fun `控制面中和 - 缩进的同名子键不受影响`() {
        val src =
            """
            |proxy-providers:
            |  provider1:
            |    type: http
            |    url: https://example.com/sub
            |    secret: keep-me
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            """.trimMargin()
        val doc = parseOk(SubscriptionSanitizer.sanitize(src, listOf("example.com")))
        assertFalse(doc.containsKey("secret"))
        val provider = (doc["proxy-providers"] as Map<String, Any?>)["provider1"] as Map<String, Any?>
        assertEquals("keep-me", provider["secret"])
    }

    @Test
    fun `控制面中和 - 幂等`() {
        val src =
            """
            |external-controller: 0.0.0.0:9090
            |secret: hunter2
            |hosts:
            |  'bank.example': 10.0.0.1
            |tun:
            |    enable: true
            |proxies:
            |  - name: "x"
            |    type: ss
            |    server: 1.2.3.4
            |    port: 8388
            |rules:
            |  - MATCH,DIRECT
            """.trimMargin()
        val once = SubscriptionSanitizer.sanitize(src, listOf("example.com"))
        assertEquals(once, SubscriptionSanitizer.sanitize(once, listOf("example.com")))
    }

    /** 清洗结果会被原子覆盖到生效配置上：必须始终可被 YAML 解析器接受 */
    @Test
    fun `清洗后输出始终是合法YAML`() {
        listOf(template2Space, template4Space, templateWithGroups).forEach { src ->
            parseOk(SubscriptionSanitizer.sanitize(src, listOf("example.com")))
        }
    }
}
