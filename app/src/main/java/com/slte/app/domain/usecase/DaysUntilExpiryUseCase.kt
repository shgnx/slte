package com.slte.app.domain.usecase

import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * 计算套餐到期剩余天数。
 *
 * 已到期或未设置到期时间（时间戳 <= 0）时返回 0，结果不小于 0。
 */
class DaysUntilExpiryUseCase
@Inject
constructor() {
    operator fun invoke(expiredAtEpochSeconds: Long): Int {
        if (expiredAtEpochSeconds <= 0L) return 0
        val now = Instant.now()
        val target = Instant.ofEpochSecond(expiredAtEpochSeconds)
        val days = ChronoUnit.DAYS.between(now, target).toInt()
        return days.coerceAtLeast(0)
    }
}
