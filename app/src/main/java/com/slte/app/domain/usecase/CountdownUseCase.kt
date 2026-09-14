package com.slte.app.domain.usecase

import com.slte.app.utils.VerificationCodeConfig
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 验证码重发倒计时。
 *
 * 从 VerificationCodeConfig.countdownSeconds 开始倒数，每秒发射一次剩余秒数，
 * 到 0 后发射 0 并结束 Flow。
 */
class CountdownUseCase
@Inject
constructor() {
    operator fun invoke(): Flow<Int> = flow {
        var remaining = VerificationCodeConfig.countdownSeconds
        while (remaining > 0) {
            emit(remaining)
            delay(VerificationCodeConfig.countdownIntervalMs)
            remaining--
        }
        emit(0)
    }
}
