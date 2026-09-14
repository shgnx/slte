package com.slte.app.kernel

import com.maxmind.db.Reader
import java.io.File
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * GeoIP 数据库（metadb）解码冒烟测试：验证标量/数组 record 及私有网段的原始返回值。
 */
class GeoIpResolverTest {
    private val metadb = File("src/main/assets/geoip.metadb")

    @Test
    fun `metadb 标量与数组 record 均能解码国家码`() {
        assumeTrue("metadb 文件不存在，跳过", metadb.exists())
        Reader(metadb).use { reader ->
            val scalar = reader.get(InetAddress.getByName("114.114.114.114"), String::class.java)
            assertEquals("cn", scalar)
            // 数组 record：首元素为国家码
            val arr = reader.get(InetAddress.getByName("8.8.8.8"), List::class.java)
            assertEquals("us", (arr as? List<*>)?.firstOrNull())
            // 私有网段：数据返回 "private" 标记（非两字母码，Resolver 的长度过滤会剔除）
            assertEquals("private", reader.get(InetAddress.getByName("10.0.0.1"), String::class.java))
        }
    }
}
