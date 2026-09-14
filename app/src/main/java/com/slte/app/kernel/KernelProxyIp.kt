package com.slte.app.kernel

import com.slte.app.utils.AppLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * KernelProxy 出口 IP 扩展：双栈（IPv4/IPv6）并发探测。
 * 与 KernelProxy 同包，访问其 internal 成员。
 */

/** 查询当前出口公网 IP（走内核隧道），失败返回 null；国家代码本地 GeoIP 解析 */
suspend fun KernelProxy.fetchPublicIp(): IpGeoInfo? = withContext(Dispatchers.IO) {
    try {
        // 双栈并发探测：IPv4 保底，IPv6 端点失败自动降级（规则/全局模式下请求都走节点）
        val (ipv4, ipv6) =
            coroutineScope {
                val v4 = async { queryIp(ipClient, KernelProxy.IPIFY_V4_URL) }
                val v6 = async { queryIp(ipClient, KernelProxy.IPIFY_V6_URL) }
                v4.await() to v6.await()
            }
        val ip = ipv4 ?: ipv6 ?: return@withContext null
        IpGeoInfo(ip = ip, ipv6 = ipv6, countryCode = geoIpResolver.countryCode(ip))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // 双栈探测各自的失败已在 queryIp 内留痕；只有并发编排本身出错才打这里
        e.logAsFault("SLTE-IP")
        null
    }
}

/** 单端点 IP 查询；失败/非 2xx 返回 null */
internal fun KernelProxy.queryIp(
    client: OkHttpClient,
    url: String,
): String? = try {
    client
        .newCall(Request.Builder().url(url).build())
        .execute()
        .use { response ->
            if (!response.isSuccessful) {
                AppLog.d("SLTE-IP", "queryIp($url): HTTP ${response.code}")
                null
            } else {
                response.body
                    ?.string()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
            }
        }
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    // 单端点偶发失败（尤其 IPv6 端点常不可用）属预期，记 W 即可，避免误报
    e.logAsFault("SLTE-IP")
    null
}
