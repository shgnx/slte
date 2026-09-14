package com.slte.app.data.remote.adapter.xboard

import com.slte.app.domain.model.Notice
import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.ServerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val updatedAt: Long = 0,
)

fun XboardNoticeData.toDomain() = Notice.fromAnnouncement(
    id = id,
    title = title,
    content = content,
    tags = tags,
    createdAt = createdAt,
)

// 服务器节点 DTO（Xboard 节点列表仅含元数据，连接参数由订阅 YAML 提供）

@Serializable
data class XboardServerData(
    val id: Int = 0,
    // Xboard 节点 type 为协议名（shadowsocks/vmess/...）
    val type: String = "",
    val name: String = "",
) {
    /** 节点列表仅展示用：host 留空，连接配置来自订阅 YAML */
    fun toServerNode() = ServerNode(
        id = id,
        name = name,
        type = ServerType.fromProtocolName(type),
        host = "",
    )
}
