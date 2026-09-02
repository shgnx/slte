package com.slte.app.data.remote.adapter.xboard

import com.slte.app.domain.model.Notice
import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.ServerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class XboardNoticeData(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val tags: List<String>? = null,
    val show: Boolean = true,
    @SerialName("img_url")
    val imgUrl: String? = null,
    @SerialName("created_at")
    val createdAt: Long = 0,
    @SerialName("updated_at")
    val updatedAt: Long = 0
)

fun XboardNoticeData.toDomain() = Notice(
    id = id,
    title = title,
    body = content,
    tags = tags ?: emptyList(),
    createdAt = createdAt
)

// 服务器节点 DTO（Xboard 节点列表仅含元数据，连接参数由订阅 YAML 提供）

@Serializable
data class XboardServerData(
    val id: Int = 0,
    // Xboard 节点 type 为协议名（shadowsocks/vmess/...）
    val type: String = "",
    val version: String? = null,
    val name: String = "",
    // rate 可能为小数，用 JsonElement 容错
    val rate: JsonElement? = null,
    val tags: List<String>? = null,
    @SerialName("is_online")
    val isOnline: Int = 1,
    @SerialName("cache_key")
    val cacheKey: String? = null,
    @SerialName("last_check_at")
    val lastCheckAt: Long = 0L
) {
    private fun resolveType(): ServerType {
        return when (type) {
            "shadowsocks" -> ServerType.SHADOWSOCKS
            "vmess" -> ServerType.VMESS
            "vless" -> ServerType.VLESS
            "trojan" -> ServerType.TROJAN
            "tuic" -> ServerType.TUIC
            "hysteria" -> ServerType.HYSTERIA
            "hysteria2" -> ServerType.HYSTERIA2
            "anytls" -> ServerType.ANYTLS
            else -> ServerType.SHADOWSOCKS
        }
    }

    /** 节点列表仅展示用：host/port 留空，连接配置来自订阅 YAML */
    fun toServerNode() = ServerNode(
        id = id, name = name,
        type = resolveType(),
        host = "", port = 0
    )
}
