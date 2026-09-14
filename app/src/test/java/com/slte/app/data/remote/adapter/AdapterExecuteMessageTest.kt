package com.slte.app.data.remote.adapter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 非 2xx 响应体 → 可读原因的提取测试，用例来自真实面板响应：
 * 密码错误返回 500 + message（业务错误被包成服务器错误），422 时真因在 errors 字段。
 * 提取不到时返回 null，由调用方退回通用网络提示。
 */
class AdapterExecuteMessageTest {
    @Test
    fun `Laravel 校验错误 - 取具体字段文案而不是通用句`() {
        val body =
            """{"message":"The given data was invalid.","errors":{"password":["密码必须大于 8 个字符"]}}"""

        assertEquals("密码必须大于 8 个字符", AdapterExecute.extractServerMessage(body))
    }

    @Test
    fun `业务错误被包成 500 - 仍能取到 message`() {
        val body = """{"message":"邮箱或密码错误"}"""

        assertEquals("邮箱或密码错误", AdapterExecute.extractServerMessage(body))
    }

    @Test
    fun `errors 为多字段时取首个非空值`() {
        val body = """{"errors":{"email":["邮箱格式不正确"],"password":["密码必须大于 8 个字符"]}}"""

        assertEquals("邮箱格式不正确", AdapterExecute.extractServerMessage(body))
    }

    @Test
    fun `errors 值为嵌套对象或数组时递归取叶子`() {
        assertEquals(
            "密码必须大于 8 个字符",
            AdapterExecute.extractServerMessage("""{"errors":{"password":{"0":"密码必须大于 8 个字符"}}}"""),
        )
        assertEquals(
            "邮箱已存在",
            AdapterExecute.extractServerMessage("""{"errors":[{"email":["邮箱已存在"]}]}"""),
        )
    }

    @Test
    fun `errors 里全是空串时回退到 message`() {
        val body = """{"message":"服务繁忙","errors":{"password":[""]}}"""

        assertEquals("服务繁忙", AdapterExecute.extractServerMessage(body))
    }

    @Test
    fun `取不到可读原因时返回 null`() {
        assertNull(AdapterExecute.extractServerMessage(null))
        assertNull(AdapterExecute.extractServerMessage(""))
        assertNull(AdapterExecute.extractServerMessage("   "))
        // 网关 HTML / Cloudflare 页面
        assertNull(AdapterExecute.extractServerMessage("<html><body>502 Bad Gateway</body></html>"))
        // 合法 JSON 但没有 message/errors
        assertNull(AdapterExecute.extractServerMessage("""{"data":null}"""))
        // message 为空串
        assertNull(AdapterExecute.extractServerMessage("""{"message":""}"""))
        // JSON 数组（顶层不是对象）
        assertNull(AdapterExecute.extractServerMessage("""["oops"]"""))
        // message 不是字符串
        assertNull(AdapterExecute.extractServerMessage("""{"message":{"a":1}}"""))
    }

    @Test
    fun `文本前后空白被裁剪`() {
        assertEquals("密码错误", AdapterExecute.extractServerMessage("""{"message":"  密码错误  "}"""))
    }
}
