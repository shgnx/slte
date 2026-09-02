package com.slte.app.data.remote.adapter.xboard

import com.slte.app.data.remote.api.dto.LoginResponseDto
import com.slte.app.data.remote.api.dto.SubscribeInfoDto
import com.slte.app.data.remote.api.dto.UserInfoDto
import com.slte.app.domain.model.CommissionRecord
import com.slte.app.domain.model.InviteCodeInfo
import com.slte.app.domain.model.InviteInfo
import com.slte.app.domain.model.InviteStat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class XboardLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class XboardRegisterRequest(
    val email: String,
    val password: String,
    val email_code: String? = null,
    val invite_code: String? = null
)

@Serializable
data class XboardForgotRequest(
    val email: String,
    val password: String,
    val email_code: String
)

@Serializable
data class XboardSendCodeRequest(
    val email: String,
    val isforget: Int
)

@Serializable
data class XboardTransferRequest(
    @SerialName("transfer_amount")
    val transferAmount: Int
)

@Serializable
data class XboardWithdrawRequest(
    @SerialName("withdraw_method")
    val withdrawMethod: String,
    @SerialName("withdraw_account")
    val withdrawAccount: String
)

@Serializable
data class XboardRemoveSessionRequest(
    @SerialName("session_id")
    val sessionId: String
)

@Serializable
data class XboardUpdateUserRequest(
    @SerialName("remind_expire")
    val remindExpire: Int? = null,
    @SerialName("remind_traffic")
    val remindTraffic: Int? = null
)

@Serializable
data class XboardChangePasswordRequest(
    @SerialName("old_password")
    val oldPassword: String,
    @SerialName("new_password")
    val newPassword: String
)


@Serializable
data class XboardResponse<T>(
    val data: T? = null,
    val message: String? = null
)


@Serializable
data class XboardLoginData(
    val token: String,
    val auth_data: String
)

/** 登录/注册响应 → 领域 DTO（auth_data 即后续请求的 JWT） */
fun XboardLoginData.toDomainLoginResponse() = LoginResponseDto(
    token = token,
    authData = auth_data
)

/** Xboard 站点配置响应，仅提取注册相关字段 */
@Serializable
data class XboardSiteConfig(
    val is_email_verify: Int? = 0,
    val is_invite_force: Int? = 0
)

@Serializable
data class XboardUserInfoData(
    val email: String = "",
    val balance: Int = 0,
    @SerialName("plan_id")
    val planId: Int = 0,
    @SerialName("expired_at")
    val expiredAt: Long = 0L,
    @SerialName("transfer_enable")
    val transferEnable: Long = 0L,
    @SerialName("remind_expire")
    val remindExpire: Boolean = false,
    @SerialName("remind_traffic")
    val remindTraffic: Boolean = false
)

fun XboardUserInfoData.toDomainUserInfo() = UserInfoDto(
    email = email,
    balance = balance,
    planId = planId,
    expiredAt = expiredAt,
    transferEnable = transferEnable,
    remindExpire = if (remindExpire) 1 else 0,
    remindTraffic = if (remindTraffic) 1 else 0
)

/** 订阅信息响应：包含套餐详情 */
@Serializable
data class XboardSubscribeData(
    @SerialName("plan_id")
    val planId: Int = 0,
    @SerialName("expired_at")
    val expiredAt: Long = 0L,
    @SerialName("transfer_enable")
    val transferEnable: Long = 0L,
    val u: Long = 0L,
    val d: Long = 0L,
    @SerialName("reset_day")
    val resetDay: Int? = null,
    val plan: XboardPlanData? = null
)

fun XboardSubscribeData.toDomainSubscribeInfo() = SubscribeInfoDto(
    planId = planId,
    planName = plan?.name ?: "",
    expiredAt = expiredAt,
    transferEnable = transferEnable,
    upload = u,
    download = d,
    resetDay = resetDay
)


@Serializable
data class XboardInviteCodeData(
    val id: Int = 0,
    @SerialName("user_id")
    val userId: Int = 0,
    val code: String = "",
    // Xboard 返回布尔状态
    val status: Boolean = false,
    val pv: Int = 0,
    @SerialName("created_at")
    val createdAt: Long = 0L,
    @SerialName("updated_at")
    val updatedAt: Long = 0L
)

fun XboardInviteCodeData.toDomain() = InviteCodeInfo(
    id = id,
    code = code,
    pv = pv,
    status = if (status) 1 else 0,
    createdAt = createdAt
)

/**
 * 邀请信息响应。
 *
 * stat 数组结构：[已注册用户数, 有效佣金总额, 确认中佣金, 佣金比例, 可用佣金余额]
 */
@Serializable
data class XboardInviteData(
    val codes: List<XboardInviteCodeData> = emptyList(),
    val stat: List<Int> = emptyList()
)

fun XboardInviteData.toDomain() = InviteInfo(
    codes = codes.map { it.toDomain() },
    stat = InviteStat(
        registeredUsers = stat.getOrElse(0) { 0 },
        totalCommission = stat.getOrElse(1) { 0 },
        pendingCommission = stat.getOrElse(2) { 0 },
        commissionRate = stat.getOrElse(3) { 0 },
        availableBalance = stat.getOrElse(4) { 0 }
    )
)

@Serializable
data class XboardCommissionRecordData(
    val id: Int = 0,
    @SerialName("trade_no")
    val tradeNo: String = "",
    @SerialName("order_amount")
    val orderAmount: Int = 0,
    @SerialName("get_amount")
    val getAmount: Int = 0,
    @SerialName("created_at")
    val createdAt: Long = 0L
)

fun XboardCommissionRecordData.toDomain() = CommissionRecord(
    id = id,
    tradeNo = tradeNo,
    orderAmount = orderAmount,
    getAmount = getAmount,
    createdAt = createdAt
)
