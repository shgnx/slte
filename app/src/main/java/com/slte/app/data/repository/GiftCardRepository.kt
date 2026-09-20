package com.slte.app.data.repository

import com.slte.app.data.remote.api.AuthApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GiftCardRepository
@Inject
constructor(
    private val authApi: AuthApi,
) {
    suspend fun redeem(code: String): Result<Unit> = runApi {
        authApi.redeemGiftCard(code.trim())
    }
}
