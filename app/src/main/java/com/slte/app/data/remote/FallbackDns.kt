package com.slte.app.data.remote

import okhttp3.Dns
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.UnknownHostException
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import com.slte.app.utils.AppLog

/** 系统 DNS 失败时轮询备用 DNS 的兜底解析器 */
@Singleton
class FallbackDns @Inject constructor() : Dns {

    /** 解析缓存（域名 → IP 列表 + 写入时间），TTL 过期后重新解析 */
    private val cache = ConcurrentHashMap<String, CachedEntry>()

    private val fallbackServers = listOf(
        "114.114.114.114",   // 中国电信公共 DNS
        "223.5.5.5",         // 阿里 DNS
        "8.8.8.8",           // Google DNS
        "1.1.1.1",           // Cloudflare DNS
    )

    override fun lookup(hostname: String): List<InetAddress> {
        val now = System.currentTimeMillis()
        cache[hostname]?.let { entry ->
            if (now - entry.timestamp < CACHE_TTL_MS) return entry.ips
            cache.remove(hostname)
        }

        try {
            val result = Dns.SYSTEM.lookup(hostname)
            cache[hostname] = CachedEntry(result, now)
            return result
        } catch (_: UnknownHostException) {
            // 不输出解析的域名：业务 API 域属敏感信息
            AppLog.w("SLTE-Dns", "FallbackDns: 系统 DNS 解析失败，尝试备用 DNS")
        }

        // 备用 DNS：整体超时兜底
        val deadline = now + FALLBACK_TIMEOUT_MS
        for (dnsStr in fallbackServers) {
            if (System.currentTimeMillis() > deadline) break
            try {
                val dnsServer = InetAddress.getByName(dnsStr)
                val result = queryDns(hostname, dnsServer, deadline - System.currentTimeMillis())
                if (result.isNotEmpty()) {
                    cache[hostname] = CachedEntry(result, System.currentTimeMillis())
                    return result
                }
            } catch (e: Exception) {
                AppLog.w("SLTE-Dns", "FallbackDns: 备用 DNS $dnsStr 失败")
            }
        }

        throw UnknownHostException("FallbackDns: 所有 DNS 均无法解析")
    }

    /**
     * 清空解析缓存：VPN 建立后调用，强制 API 域名重新解析。
     * 缓存里是 VPN 前的真实 IP——继续使用会让业务流量以纯 IP 流进入 TUN，
     * 使注入的 DOMAIN-SUFFIX 直连规则无法匹配（域名信息丢失）。
     */
    fun clearCache() {
        cache.clear()
    }

    private data class CachedEntry(val ips: List<InetAddress>, val timestamp: Long)

    private companion object {
            const val CACHE_TTL_MS = 5 * 60_000L

            const val FALLBACK_TIMEOUT_MS = 8_000L

            const val QUERY_TIMEOUT_MS = 5_000L

            /** DNS 名称指针压缩最大跳转次数（防环） */
            const val MAX_POINTER_JUMPS = 16

            /** DNS 名称最大长度（RFC 1035 上限） */
            const val MAX_NAME_LENGTH = 253
    }

    /** UDP 查询 A 记录；remainingMs 为整体超时的剩余时间，socket 超时取单次上限与剩余时间的较小值 */
    private fun queryDns(hostname: String, dnsServer: InetAddress, remainingMs: Long): List<InetAddress> {
        val socket = DatagramSocket()
        socket.soTimeout = minOf(QUERY_TIMEOUT_MS, remainingMs.coerceAtLeast(1)).toInt()

        try {
            val id = (SecureRandom().nextInt(65536) and 0xFFFF).toShort()
            val req = buildQueryPacket(id, hostname)
            socket.send(DatagramPacket(req, req.size, dnsServer, 53))

            val resp = ByteArray(512)
            val pkt = DatagramPacket(resp, resp.size)
            socket.receive(pkt)
            // 响应必须来自查询的服务器
            if (pkt.address != dnsServer) {
                throw UnknownHostException("DNS response source mismatch")
            }

            return parseResponse(resp, pkt.length, id, hostname)
        } finally {
            socket.close()
        }
    }

    private fun buildQueryPacket(id: Short, hostname: String): ByteArray {
        val buf = ByteArray(512)
        var pos = 0

        // Header (12 字节)
        buf[pos++] = (id.toInt() shr 8).toByte()
        buf[pos++] = id.toByte()
        buf[pos++] = 1   // flags: recursion desired
        buf[pos++] = 0
        buf[pos++] = 0   // QDCOUNT = 1
        buf[pos++] = 1
        buf[pos++] = 0   // ANCOUNT = 0
        buf[pos++] = 0
        buf[pos++] = 0   // NSCOUNT = 0
        buf[pos++] = 0
        buf[pos++] = 0   // ARCOUNT = 0
        buf[pos++] = 0

        // Question: 域名编码为长度前缀标签
        for (label in hostname.split(".")) {
            buf[pos++] = label.length.toByte()
            for (c in label.encodeToByteArray()) {
                buf[pos++] = c
            }
        }
        buf[pos++] = 0    // 标签结束
        buf[pos++] = 0    // QTYPE: A (1)
        buf[pos++] = 1
        buf[pos++] = 0    // QCLASS: IN (1)
        buf[pos++] = 1

        return buf.copyOf(pos)
    }

    private fun parseResponse(
        resp: ByteArray, len: Int, expectedId: Short, hostname: String
    ): List<InetAddress> {
        val data = resp.copyOfRange(0, len)
        val respId = ((resp[0].toInt() and 0xFF) shl 8) or (resp[1].toInt() and 0xFF)
        if (respId != (expectedId.toInt() and 0xFFFF)) {
            throw UnknownHostException("DNS response ID mismatch")
        }
        if (resp[2].toInt() and 0x80 == 0) {
            throw UnknownHostException("DNS response is not a reply")
        }

        val rcode = resp[3].toInt() and 0x0F
        if (rcode != 0) {
            throw UnknownHostException("DNS response code: $rcode")
        }

        val qdcount = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        val ancount = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)

        var pos = 12

        if (qdcount > 0) {
            val (qname, qend) = decodeName(data, pos)
            pos = qend
            pos += 4  // QTYPE + QCLASS
            // 问题区必须与查询域名一致：防止无关响应被接受
            if (!qname.equals(hostname, ignoreCase = true)) {
                throw UnknownHostException("DNS question mismatch")
            }
        }

        val result = mutableListOf<InetAddress>()
        for (i in 0 until ancount) {
            pos = skipName(data, pos)
            if (pos + 10 > data.size) throw UnknownHostException("DNS answer truncated")
            val type = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
            pos += 8
            val rdlength = ((data[pos].toInt() and 0xFF) shl 8) or (data[pos + 1].toInt() and 0xFF)
            pos += 2
            if (pos + rdlength > data.size) throw UnknownHostException("DNS answer truncated")

            if (type == 1 && rdlength == 4) {  // A 记录
                val addr = data.copyOfRange(pos, pos + 4)
                result.add(InetAddress.getByAddress(hostname, addr))
            }
            pos += rdlength
        }

        if (result.isEmpty()) {
            throw UnknownHostException("No A records found for $hostname")
        }
        return result
    }

    /** 解码 DNS 名称（支持指针压缩），返回 (名称, 名称字段结束位置) */
    private fun decodeName(buf: ByteArray, start: Int): Pair<String, Int> {
        var pos = start
        var jumped = false
        var end = start
        val labels = mutableListOf<String>()
        var jumps = 0
        var totalLen = 0
        while (true) {
            if (pos >= buf.size) throw UnknownHostException("DNS name overflow")
            val len = buf[pos].toInt() and 0xFF
            if (len == 0) {
                if (!jumped) end = pos + 1
                break
            }
            if ((len and 0xC0) == 0xC0) {
                if (pos + 1 >= buf.size) throw UnknownHostException("DNS pointer overflow")
                val ptr = ((len and 0x3F) shl 8) or (buf[pos + 1].toInt() and 0xFF)
                if (!jumped) end = pos + 2
                jumped = true
                // 指针环/深度防护：跳转次数与总标签长度设上限，防伪造响应死循环
                if (++jumps > MAX_POINTER_JUMPS) throw UnknownHostException("DNS pointer loop")
                pos = ptr
            } else {
                if (pos + 1 + len > buf.size) throw UnknownHostException("DNS name overflow")
                totalLen += len
                if (totalLen > MAX_NAME_LENGTH) throw UnknownHostException("DNS name too long")
                labels.add(String(buf, pos + 1, len, Charsets.US_ASCII))
                pos += len + 1
            }
        }
        return labels.joinToString(".") to end
    }

    private fun skipName(buf: ByteArray, start: Int): Int {
        var pos = start
        var jumps = 0
        while (pos < buf.size) {
            val len = buf[pos].toInt() and 0xFF
            if (len == 0) return pos + 1
            if ((len and 0xC0) == 0xC0) {
                if (++jumps > MAX_POINTER_JUMPS) return pos + 2
                return pos + 2
            }
            pos += len + 1
        }
        return pos
    }

}
