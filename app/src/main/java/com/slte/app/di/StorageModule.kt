package com.slte.app.di

import android.content.Context
import android.content.SharedPreferences
import com.slte.app.data.local.CredentialPrefs
import com.slte.app.data.local.CredentialStore
import com.slte.app.data.local.SecurePreferences
import com.slte.app.data.local.SessionPrefs
import com.slte.app.data.local.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 加密偏好存储的绑定。
 *
 * 把「Android 加密实现 + Keystore 自愈降级」留在 DI 边界，[SessionStore] /
 * [CredentialStore] 只依赖 [SharedPreferences] 抽象，便于用内存实现做纯 JVM 单测
 * （见 SessionStoreTest）。
 *
 * 风险提示：别名（KEY_ALIAS）变更会使旧加密数据无法打开，改动别名必须走
 * [SecurePreferences] 的迁移路径，否则升级后会话/凭证会丢失。
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    @SessionPrefs
    fun provideSessionPrefs(
        @ApplicationContext context: Context,
    ): SharedPreferences = SecurePreferences.create(context, SessionStore.PREFS_NAME, SessionStore.KEY_ALIAS)

    @Provides
    @Singleton
    @CredentialPrefs
    fun provideCredentialPrefs(
        @ApplicationContext context: Context,
    ): SharedPreferences = SecurePreferences.create(context, CredentialStore.PREFS_NAME, CredentialStore.KEY_ALIAS)
}
