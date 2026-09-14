package com.slte.app.data.remote.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 白名单是凭据外发的唯一边界，锁定「放行/拒绝」判定语义；
 * 具体白名单内容由构建期注入（测试构建下仅占位域 example.com）。
 */
class AllowedHostsTest {
    @Test
    fun `白名单域及其子域放行`() {
        assertTrue(AllowedHosts.isAllowedHost("example.com"))
        assertTrue(AllowedHosts.isAllowedHost("api.example.com"))
        assertTrue(AllowedHosts.isAllowedHost("a.b.example.com"))
        assertTrue(AllowedHosts.isAllowedHost("API.EXAMPLE.COM")) // 大小写不敏感
    }

    @Test
    fun `非白名单域被拒绝_含后缀伪装`() {
        assertFalse(AllowedHosts.isAllowedHost("attacker.tld"))
        assertFalse(AllowedHosts.isAllowedHost("notexample.com"))
        assertFalse(AllowedHosts.isAllowedHost("example.com.attacker.tld"))
        assertFalse(AllowedHosts.isAllowedHost(""))
        assertFalse(AllowedHosts.isAllowedHost(null))
    }

    @Test
    fun `订阅地址必须https且在白名单内`() {
        assertTrue(AllowedHosts.isAllowedUrl("https://api.example.com/api/v1/client/subscribe?token=x"))
        assertFalse(AllowedHosts.isAllowedUrl("http://api.example.com/subscribe"))
        assertFalse(AllowedHosts.isAllowedUrl("https://attacker.tld/subscribe?token=x"))
        assertFalse(AllowedHosts.isAllowedUrl("not a url"))
        assertFalse(AllowedHosts.isAllowedUrl(null))
    }
}
