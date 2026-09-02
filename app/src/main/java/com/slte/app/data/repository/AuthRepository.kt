package com.slte.app.data.repository

import com.slte.app.data.local.CredentialStore
import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.data.remote.api.dto.LoginResponseDto
import com.slte.app.domain.model.EmailCodePurpose
import com.slte.app.domain.model.RegisterConfig
import com.slte.app.domain.model.SessionManager
import com.slte.app.domain.model.SessionState
import com.slte.app.domain.model.User
import com.slte.app.utils.Constants
import com.slte.app.utils.sanitizeLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.slte.app.utils.AppLog

/**
 * 认证仓库：登录/注册/找回密码/登出。
 * 会话状态见 [SessionManager]，订阅/用户/公告数据见 [SubscribeRepository]。
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
    private val credentialStore: CredentialStore,
    private val sessionManager: SessionManager,
    private val subscribeRepository: SubscribeRepository,
) {
    /** 会话状态（登录/登出），供登录表单等 UI 观察 */
    val sessionState: StateFlow<SessionState>
        get() = sessionManager.sessionState

    // 记住密码（加密本地存储，供登录表单自动填充）

    fun saveCredentials(email: String, password: String) = credentialStore.save(email, password)

    fun clearCredentials() = credentialStore.clear()

    fun clearSavedPassword() = credentialStore.clearPassword()

    fun savedEmail(): String? = credentialStore.getSavedEmail()

    fun savedPassword(): String? = credentialStore.getSavedPassword()

    // 登出时尽力而为的服务端会话吊销，不阻塞本地登出
    private val revokeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun login(email: String, password: String): Result<User> = runApi {
        val response = authApi.login(email.trim(), password)
        val user = response.toDomainUser(email.trim())
        sessionManager.setLoggedIn(user)
        subscribeRepository.invalidateCache()
        user
    }

    suspend fun register(
        email: String,
        password: String,
        emailCode: String? = null,
        inviteCode: String? = null
    ): Result<User> = runApi {
        val response = authApi.register(email.trim(), password, emailCode, inviteCode)
        val user = response.toDomainUser(email.trim())
        sessionManager.setLoggedIn(user)
        subscribeRepository.invalidateCache()
        user
    }

    suspend fun fetchRegisterConfig(): Result<RegisterConfig> = runApi {
        authApi.fetchRegisterConfig()
    }

    suspend fun forgotPassword(email: String, emailCode: String, newPassword: String): Result<Unit> = runApi {
        authApi.forgotPassword(email.trim(), emailCode.trim(), newPassword)
    }

    suspend fun sendEmailCode(email: String, purpose: EmailCodePurpose = EmailCodePurpose.FORGOT_PASSWORD): Result<Unit> = runApi {
        authApi.sendEmailCode(email.trim(), purpose)
    }

    /** 更新到期邮件提醒开关（服务端下发邮件），成功后同步本地用户缓存 */
    suspend fun updateRemindExpire(enabled: Boolean): Result<Unit> = runApi {
        authApi.updateRemindExpire(enabled)
        val current = sessionManager.sessionState.value as? SessionState.LoggedIn
        if (current != null) {
            val updated = current.user.copy(remindExpire = if (enabled) 1 else 0)
            sessionManager.updateUser(updated)
            subscribeRepository.updateCachedUserInfo(updated)
        }
    }

    /** 更新流量邮件提醒开关（服务端下发邮件），成功后同步本地用户缓存 */
    suspend fun updateRemindTraffic(enabled: Boolean): Result<Unit> = runApi {
        authApi.updateRemindTraffic(enabled)
        val current = sessionManager.sessionState.value as? SessionState.LoggedIn
        if (current != null) {
            val updated = current.user.copy(remindTraffic = if (enabled) 1 else 0)
            sessionManager.updateUser(updated)
            subscribeRepository.updateCachedUserInfo(updated)
        }
    }

    /**
     * 修改密码：服务端会清除全部活跃会话（包括本机），
     * 因此改密成功后立即用新密码静默重新登录换新 token——
     * 其他设备被挤出，本机保持登录态不退出；同时刷新本地缓存。重登失败时清会话保证状态一致。
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<Unit> = runApi {
        authApi.changePassword(oldPassword, newPassword)
        val current = sessionManager.sessionState.value as? SessionState.LoggedIn
        if (current != null) {
            val email = current.user.email
            val response = try {
                authApi.login(email.trim(), newPassword)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                sessionManager.clearSession()
                throw e
            }
            val user = response.toDomainUser(email.trim())
            sessionManager.setLoggedIn(user)
            subscribeRepository.invalidateCache()
            if (credentialStore.getSavedEmail() != null) {
                credentialStore.save(email.trim(), newPassword)
            }
        }
    }

    /**
     * 退出登录：先尽力吊销服务端会话（显式携带 Authorization 头），再清除本地会话；
     * 吊销失败不阻塞本地登出，但留痕可观测。吊销前校验会话未被重登。
     */
    fun logout() {
        val authData = sessionStore.getAuthData()
        if (authData != null) {
            revokeScope.launch {
                runCatching {
                    val current = sessionStore.getAuthData()
                    if (current == null || current == authData) {
                        authApi.revokeActiveSessions(authData)
                    }
                }.onFailure { e ->
                    AppLog.w("SLTE-Repo", "revokeActiveSessions failed: ${sanitizeLog(e.message ?: "Unknown")}")
                }
            }
        }
        sessionManager.clearSession()
    }

    private fun LoginResponseDto.toDomainUser(email: String) = User(
        id = token,
        displayName = Constants.DEFAULT_USER_NAME,
        email = email,
        authData = authData,
        subscribeToken = token
    )
}
