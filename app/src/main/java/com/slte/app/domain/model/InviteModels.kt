package com.slte.app.domain.model

data class InviteCodeInfo(
    val id: Int = 0,
    val code: String = "",
    val pv: Int = 0,
    val status: Int = 0,
    val createdAt: Long = 0L,
)

/** 邀请统计（金额单位分） */
data class InviteStat(
    val registeredUsers: Int = 0,
    val totalCommission: Int = 0,
    val pendingCommission: Int = 0,
    val commissionRate: Int = 0,
    val availableBalance: Int = 0,
)

data class InviteInfo(
    val codes: List<InviteCodeInfo> = emptyList(),
    val stat: InviteStat = InviteStat(),
)

/** 佣金明细（金额单位分） */
data class CommissionRecord(
    val id: Int = 0,
    val tradeNo: String = "",
    val orderAmount: Int = 0,
    val getAmount: Int = 0,
    val createdAt: Long = 0L,
)
