package com.slte.app.kernel

import android.content.Context
import com.maxmind.db.Reader
import com.slte.app.utils.AppLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 本地 GeoIP 解析：读取内核自带的 geoip.metadb（mmdb 容器，Meta-geoip0 数据），
 * 将 IP 解析为国家 ISO 码。文件缺失或解析失败返回 null。
 */
@Singleton
class GeoIpResolver
@Inject
constructor(
    @ApplicationContext private val context: Context,
) {
    @Volatile
    private var reader: Reader? = null

    /** IP → 国家 ISO 码（小写）；解析失败返回 null */
    fun countryCode(ip: String): String? {
        val r =
            reader ?: synchronized(this) {
                reader ?: load().also { reader = it }
            } ?: return null
        val address = runCatching { InetAddress.getByName(ip) }.getOrNull() ?: return null
        // Meta-geoip0 数据：标量国家码（"cn"），或 [国家码, 组织] 数组
        val scalar = runCatching { r.get(address, String::class.java) }.getOrNull()
        val code =
            if (!scalar.isNullOrBlank()) {
                scalar
            } else {
                val list = runCatching { r.get(address, List::class.java) }.getOrNull()
                (list as? List<*>)?.firstOrNull() as? String
            }
        // 只接受 ASCII 两位字母国家码（ISO 3166-1 子集；避免 Unicode 字母误放行），统一转小写
        return code?.takeIf { it.length == 2 && it.all { c -> c in 'a'..'z' || c in 'A'..'Z' } }?.lowercase()
    }

    /** 懒加载：metadb 由内核解压在后台异步完成，首次调用时可能尚未就绪 */
    private fun load(): Reader? = runCatching {
        Reader(File(context.filesDir, "clash/geoip.metadb"))
    }.getOrElse {
        AppLog.w("SLTE-Kernel", "GeoIpResolver: metadb 加载失败: ${it.message}")
        null
    }
}
