package com.slte.app.domain.model

/**
 * 公告信息。
 *
 * @param body 公告正文，HTML 格式
 * @param createdAt 创建时间（Unix 秒级时间戳）
 */
data class Notice(
    val id: Int,
    val title: String,
    val body: String,
    val tags: List<String>,
    val createdAt: Long,
) {
    companion object {
        /** 后端公告字段 → 领域模型（两个后端适配器共用，避免映射漂移） */
        fun fromAnnouncement(
            id: Int,
            title: String,
            content: String,
            tags: List<String>?,
            createdAt: Long,
        ) = Notice(
            id = id,
            title = title,
            body = content,
            tags = tags ?: emptyList(),
            createdAt = createdAt,
        )
    }
}
