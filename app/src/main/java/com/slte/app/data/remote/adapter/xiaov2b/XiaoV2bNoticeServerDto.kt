package com.slte.app.data.remote.adapter.xiaov2b

import com.slte.app.domain.model.Notice
import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.ServerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class XiaoV2bNoticeData(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val tags: List<String>? = null,
    val show: Int = 1,
    @SerialName("img_url")
    val imgUrl: String? = null,
    @SerialName("created_at")
    val createdAt: Long = 0,
    @SerialName("updated_at")
    val updatedAt: Long = 0
) {
    fun toDomain() = Notice(
        id = id,
        title = title,
        body = content,
        tags = tags ?: emptyList(),
        createdAt = createdAt
    )
}


@Serializable
data class XiaoV2bServerData(
    val id: Int = 0,
    val name: String = "",
    val type: String = "",
    val host: String = "",
    val port: Int = 0,
    @SerialName("server_port") val serverPort: Int = 0,
    val cipher: String? = null,
    val password: String? = null,
    val uuid: String? = null,
    @SerialName("alter_id") val alterId: Int = 0,
    val network: String? = null,
    @SerialName("network_settings") val networkSettings: kotlinx.serialization.json.JsonElement? = null,
    val tls: Int = 0,
    @SerialName("tls_settings") val tlsSettings: kotlinx.serialization.json.JsonElement? = null,
    val obfs: String? = null,
    @SerialName("obfs_settings") val obfsSettings: kotlinx.serialization.json.JsonElement? = null,
    val flow: String? = null,
    val method: String? = null,
    val protocol: String? = null,
    @SerialName("obfs_password") val obfsPassword: String? = null,
    // group_id 可能为数组或 Int，用 JsonElement 容错
    @SerialName("group_id") val groupId: kotlinx.serialization.json.JsonElement? = null,
    val show: Int = 1,
    @SerialName("is_online") val isOnline: Int = 1,
    val description: String? = null
) {
    private fun resolveType(): com.slte.app.domain.model.ServerType {
        // v2node 是 V2Board 通用节点，实际协议由 protocol 字段决定
        if (type == "v2node" && protocol != null) {
            return when (protocol) {
                "hysteria2" -> com.slte.app.domain.model.ServerType.HYSTERIA2
                "hysteria" -> com.slte.app.domain.model.ServerType.HYSTERIA
                "shadowsocks" -> com.slte.app.domain.model.ServerType.SHADOWSOCKS
                "vmess" -> com.slte.app.domain.model.ServerType.VMESS
                "vless" -> com.slte.app.domain.model.ServerType.VLESS
                "trojan" -> com.slte.app.domain.model.ServerType.TROJAN
                "tuic" -> com.slte.app.domain.model.ServerType.TUIC
                "anytls" -> com.slte.app.domain.model.ServerType.ANYTLS
                else -> com.slte.app.domain.model.ServerType.SHADOWSOCKS
            }
        }
        return when (type) {
            "shadowsocks" -> com.slte.app.domain.model.ServerType.SHADOWSOCKS
            "vmess" -> com.slte.app.domain.model.ServerType.VMESS
            "vless" -> com.slte.app.domain.model.ServerType.VLESS
            "trojan" -> com.slte.app.domain.model.ServerType.TROJAN
            "tuic" -> com.slte.app.domain.model.ServerType.TUIC
            "hysteria" -> com.slte.app.domain.model.ServerType.HYSTERIA
            "hysteria2" -> com.slte.app.domain.model.ServerType.HYSTERIA2
            "anytls" -> com.slte.app.domain.model.ServerType.ANYTLS
            else -> com.slte.app.domain.model.ServerType.SHADOWSOCKS
        }
    }

    /** 从 tls_settings JSON 中提取 server_name 作为 SNI */
    private fun extractSni(): String {
        val settings = tlsSettings ?: return ""
        val obj = when (settings) {
            is kotlinx.serialization.json.JsonObject -> settings
            is kotlinx.serialization.json.JsonPrimitive -> runCatching {
                kotlinx.serialization.json.Json.parseToJsonElement(settings.content) as? kotlinx.serialization.json.JsonObject
            }.getOrNull()
            else -> null
        } ?: return ""
        return (obj["server_name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
    }

    fun toServerNode() = com.slte.app.domain.model.ServerNode(
        id = id, name = name,
        type = resolveType(),
        host = host, port = port, serverPort = serverPort,
        cipher = cipher ?: method ?: "",
        password = password.orEmpty(),
        obfsPassword = obfsPassword.orEmpty(),
        uuid = uuid ?: "",
        alterId = alterId, network = network ?: "tcp",
        networkSettings = networkSettings?.let { jsonElementToStr(it) },
        tls = tls == 1, tlsSettings = tlsSettings?.let { jsonElementToStr(it) },
        obfs = obfs ?: "", obfsSettings = obfsSettings?.let { jsonElementToStr(it) },
        flow = flow ?: "",
        sni = extractSni(),
        groupId = when (groupId) {
            is kotlinx.serialization.json.JsonArray -> (groupId.firstOrNull() as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull() ?: 0
            is kotlinx.serialization.json.JsonPrimitive -> groupId.content.toIntOrNull() ?: 0
            else -> 0
        }
    )
}

/** 将 JsonElement 转成字符串：原始字符串去引号，对象/数组 toString */
private fun jsonElementToStr(el: kotlinx.serialization.json.JsonElement): String = when (el) {
    is kotlinx.serialization.json.JsonPrimitive -> el.content
    else -> el.toString()
}
