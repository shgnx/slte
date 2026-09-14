package com.slte.app.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 加密凭证存储：记住登录账号（邮箱 + 密码）。
 *
 * 单槽位：登录哪个账号就覆盖保存哪个；登出后保留，供下次登录预填。
 * 加密密钥由 AndroidKeyStore 管理，设备级安全。
 *
 * 依赖收窄为 [SharedPreferences]（同上，见 [SessionStore]）：加密实现由 DI 注入。
 */
@Singleton
class CredentialStore
@Inject
constructor(
    @CredentialPrefs private val prefs: SharedPreferences,
) {

    /** 保存登录账号（覆盖上一个账号） */
    fun save(
        email: String,
        password: String,
    ) {
        prefs.edit {
            putString(KEY_EMAIL, email)
            putString(KEY_PASSWORD, password)
        }
    }

    fun clear() {
        prefs.edit {
            remove(KEY_EMAIL)
            remove(KEY_PASSWORD)
        }
    }

    /** 读取保存的邮箱，未保存时返回 null */
    fun getSavedEmail(): String? = prefs.getString(KEY_EMAIL, null)

    /** 读取保存的密码，未保存时返回 null */
    fun getSavedPassword(): String? = prefs.getString(KEY_PASSWORD, null)

    internal companion object {
        internal const val PREFS_NAME = "slte_credential_store"
        internal const val KEY_ALIAS = "slte_credential_master_key"
        private const val KEY_EMAIL = "email"
        private const val KEY_PASSWORD = "password"
    }
}
