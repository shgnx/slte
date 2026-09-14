package com.slte.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 服务器节点（来自 V2Board API）。
 *
 * 仅用于列表展示：连接配置由订阅 YAML 提供，故只保留展示字段。
 */
@Serializable
data class ServerNode(
    val id: Int,
    val name: String,
    val type: ServerType,
    val host: String,
)

@Serializable
enum class ServerType {
    SHADOWSOCKS,
    VMESS,
    VLESS,
    TROJAN,
    TUIC,
    HYSTERIA,
    HYSTERIA2,
    ANYTLS,
    ;

    companion object {
        /** 后端协议名 → ServerType（未知协议回退 SHADOWSOCKS；两个后端适配器共用，避免映射漂移） */
        fun fromProtocolName(name: String?): ServerType = when (name) {
            "shadowsocks" -> SHADOWSOCKS
            "vmess" -> VMESS
            "vless" -> VLESS
            "trojan" -> TROJAN
            "tuic" -> TUIC
            "hysteria" -> HYSTERIA
            "hysteria2" -> HYSTERIA2
            "anytls" -> ANYTLS
            else -> SHADOWSOCKS
        }
    }
}
