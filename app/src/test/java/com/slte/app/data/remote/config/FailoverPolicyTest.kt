package com.slte.app.data.remote.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 故障切换策略纯逻辑测试：幂等方法重放、故障状态码判定与 JSON 劫持识别。
 */
class FailoverPolicyTest {
    @Test
    fun `仅幂等方法允许重放`() {
        assertTrue(FailoverPolicy.isRetryableMethod("GET"))
        assertTrue(FailoverPolicy.isRetryableMethod("HEAD"))
        assertTrue(FailoverPolicy.isRetryableMethod("OPTIONS"))
        // 写操作禁止自动重试：避免重复下单/结算
        assertFalse(FailoverPolicy.isRetryableMethod("POST"))
        assertFalse(FailoverPolicy.isRetryableMethod("PUT"))
        assertFalse(FailoverPolicy.isRetryableMethod("DELETE"))
        assertFalse(FailoverPolicy.isRetryableMethod("PATCH"))
    }

    @Test
    fun `故障状态码判定`() {
        listOf(408, 425, 429, 500, 502, 503, 504, 522, 524, 530).forEach {
            assertTrue("$it 应为故障码", FailoverPolicy.isFailureCode(it))
        }
        // 业务拒绝类 4xx 不属于瞬时故障，切换地址无意义
        listOf(200, 301, 400, 401, 403, 404, 409, 422).forEach {
            assertFalse("$it 不应为故障码", FailoverPolicy.isFailureCode(it))
        }
    }

    @Test
    fun `JSON 声明与内容不匹配识别劫持页`() {
        assertFalse(FailoverPolicy.isJsonMismatch("application/json", '{'.code))
        assertFalse(FailoverPolicy.isJsonMismatch("application/json; charset=utf-8", '['.code))
        // 劫持页：声明 JSON 但返回 HTML
        assertTrue(FailoverPolicy.isJsonMismatch("application/json", '<'.code))
        // 非 JSON 声明（订阅 YAML 等）不参与判定，避免误伤合法响应
        assertFalse(FailoverPolicy.isJsonMismatch("text/html", '<'.code))
        assertFalse(FailoverPolicy.isJsonMismatch(null, '<'.code))
        // 空 body 不判定（留给上层解析失败处理）
        assertFalse(FailoverPolicy.isJsonMismatch("application/json", null))
    }
}
