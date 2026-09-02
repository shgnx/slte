package com.slte.app.data.remote.config

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * ApiFailoverInterceptor 集成测试：本地假服务器模拟多地址，验证
 * 故障切换、幂等限制、伪成功识别与熔断跳过的完整链路。
 */
class ApiFailoverInterceptorTest {

    private lateinit var server1: MockWebServer
    private lateinit var server2: MockWebServer
    private lateinit var selector: EndpointSelector
    private lateinit var client: OkHttpClient

    private val primary: String get() = server1.url("/").toString()
    private val backup: String get() = server2.url("/").toString()

    private fun failoverConfig() = object : FailoverConfig {
        override val apiBaseUrl: String = primary
        override fun apiCandidates(primary: String): List<String> = listOf(primary, backup)
    }

    private fun ok() = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("{}")

    @Before
    fun setup() {
        server1 = MockWebServer().apply { start() }
        server2 = MockWebServer().apply { start() }
        selector = EndpointSelector()
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(ApiFailoverInterceptor(failoverConfig(), selector))
            .build()
    }

    @After
    fun teardown() {
        server1.shutdown()
        server2.shutdown()
    }

    @Test
    fun `GET主地址500时failover到备选`() {
        server1.enqueue(MockResponse().setResponseCode(500))
        server2.enqueue(ok())
        client.newCall(Request.Builder().url(server1.url("/api/v1/user/info")).build())
            .execute().use { assertEquals(200, it.code) }
        assertEquals(1, server1.requestCount)
        assertEquals(1, server2.requestCount)
    }

    @Test
    fun `POST故障码不自动重试`() {
        server1.enqueue(MockResponse().setResponseCode(500))
        val body = "{}".toRequestBody(null)
        client.newCall(
            Request.Builder().url(server1.url("/api/v1/user/order")).post(body).build()
        ).execute().use { assertEquals(500, it.code) }
        // 写操作禁止重放：备选不应收到请求
        assertEquals(0, server2.requestCount)
    }

    @Test
    fun `订阅YAML响应不误判为伪成功`() {
        // 订阅接口返回 YAML（text/plain），非 JSON 声明不参与伪成功判定
        server1.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "text/plain")
                .setBody("proxies:\n  - name: HK01\n    type: ss\n")
        )
        client.newCall(Request.Builder().url(server1.url("/api/v1/client/subscribe")).build())
            .execute().use { assertEquals(200, it.code) }
        assertEquals(1, server1.requestCount)
        assertEquals(0, server2.requestCount)
    }

    @Test
    fun `声明JSON返回HTML视为劫持并failover`() {
        server1.enqueue(
            MockResponse().setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("<html><body>captive portal</body></html>")
        )
        server2.enqueue(ok())
        client.newCall(Request.Builder().url(server1.url("/api/v1/user/info")).build())
            .execute().use { assertEquals(200, it.code) }
        assertEquals(1, server1.requestCount)
        assertEquals(1, server2.requestCount)
    }

    @Test
    fun `连续失败触发熔断后跳过该地址`() {
        // 连续 3 次 500：主地址进入熔断退避
        repeat(3) {
            server1.enqueue(MockResponse().setResponseCode(500))
            server2.enqueue(ok())
            client.newCall(Request.Builder().url(server1.url("/api/v1/user/info")).build())
                .execute().use { assertEquals(200, it.code) }
        }
        // 第 4 次：主地址已熔断，直接使用备选
        server2.enqueue(ok())
        client.newCall(Request.Builder().url(server1.url("/api/v1/user/info")).build())
            .execute().use { assertEquals(200, it.code) }
        assertEquals(3, server1.requestCount)
        assertEquals(4, server2.requestCount)
    }

    @Test
    fun `候选列表重复时全部故障不抛异常`() {
        // 回归：候选列表含同一地址两次时，前一个故障响应必须在重试前关闭，
        // 否则 OkHttp 抛 "previous response is still open" 的 IllegalStateException
        val dupConfig = object : FailoverConfig {
            override val apiBaseUrl: String = primary
            override fun apiCandidates(primary: String): List<String> = listOf(primary, primary)
        }
        val dupClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(ApiFailoverInterceptor(dupConfig, selector))
            .build()
        server1.enqueue(MockResponse().setResponseCode(500))
        server1.enqueue(MockResponse().setResponseCode(500))
        dupClient.newCall(Request.Builder().url(server1.url("/api/v1/user/info")).build())
            .execute().use { assertEquals(500, it.code) }
        assertEquals(2, server1.requestCount)
    }

    @Test
    fun `单候选故障时直接返回故障响应`() {
        // 仅一个候选地址（远程配置单地址的常规形态）：故障时返回响应本身，不做二次重试
        val singleConfig = object : FailoverConfig {
            override val apiBaseUrl: String = primary
            override fun apiCandidates(primary: String): List<String> = listOf(primary)
        }
        val singleClient = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .addInterceptor(ApiFailoverInterceptor(singleConfig, selector))
            .build()
        server1.enqueue(MockResponse().setResponseCode(500))
        singleClient.newCall(Request.Builder().url(server1.url("/api/v1/user/info")).build())
            .execute().use { assertEquals(500, it.code) }
        assertEquals(1, server1.requestCount)
    }
}
