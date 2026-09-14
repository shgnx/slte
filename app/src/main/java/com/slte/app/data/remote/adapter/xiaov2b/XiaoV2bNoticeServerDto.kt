package com.slte.app.data.remote.adapter.xiaov2b

import com.slte.app.domain.model.Notice
import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.ServerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val updatedAt: Long = 0,
) {
    fun toDomain() = Notice.fromAnnouncement(
        id = id,
        title = title,
        content = content,
        tags = tags,
        createdAt = createdAt,
    )
}

// 服务器节点 DTO：列表仅含元数据，连接配置来自订阅 YAML；
// 协议相关字段仅用于 resolveType，未知字段由 ignoreUnknownKeys 忽略

@Serializable
data class XiaoV2bServerData(
    val id: Int = 0,
    val name: String = "",
    val type: String = "",
    val host: String = "",
    // v2node 是 V2Board 通用节点，实际协议由 protocol 字段决定
    val protocol: String? = null,
) {
    private fun resolveType(): ServerType = if (type == "v2node" && protocol != null) {
        ServerType.fromProtocolName(protocol)
    } else {
        ServerType.fromProtocolName(type)
    }

    /** 节点列表仅展示用：host 取自列表元数据，连接配置来自订阅 YAML */
    fun toServerNode() = ServerNode(
        id = id,
        name = name,
        type = resolveType(),
        host = host,
    )
}
