package com.slte.app.data.repository

import com.slte.app.data.remote.api.AuthApi
import com.slte.app.domain.model.CommissionRecord
import com.slte.app.domain.model.InviteInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 邀请推广仓库：邀请码管理、佣金统计、提现、转账。
 *
 * 鉴权由 OkHttp 拦截器统一注入。
 */
@Singleton
class InviteRepository
@Inject
constructor(
    private val authApi: AuthApi,
) {
    /** 获取邀请信息（邀请码列表 + 统计数据） */
    suspend fun fetchInviteInfo(): Result<InviteInfo> = runApi {
        authApi.fetchInviteInfo()
    }

    suspend fun generateInviteCode(): Result<Boolean> = runApi {
        authApi.generateInviteCode()
    }

    /** 获取佣金明细记录（分页） */
    suspend fun fetchCommissionRecords(
        page: Int = 1,
        pageSize: Int = 10,
    ): Result<List<CommissionRecord>> = runApi {
        authApi.fetchCommissionRecords(page, pageSize)
    }

    /** 佣金转余额 */
    suspend fun transferCommission(transferAmountCents: Int): Result<Boolean> = runApi {
        authApi.transferCommission(transferAmountCents)
    }

    suspend fun withdrawCommission(
        method: String,
        account: String,
    ): Result<Boolean> = runApi {
        authApi.withdrawCommission(method, account)
    }

    suspend fun fetchWithdrawMethods(): Result<List<String>> = runApi {
        authApi.fetchWithdrawMethods()
    }
}
