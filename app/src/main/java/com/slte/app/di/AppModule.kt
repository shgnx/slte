package com.slte.app.di

import com.slte.app.BuildConfig
import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.ApiBackend
import com.slte.app.data.remote.AuthInterceptor
import com.slte.app.data.remote.FallbackDns
import com.slte.app.data.remote.SubscribeSourceImpl
import com.slte.app.data.remote.XiaoV2b
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.data.remote.config.RemoteConfig
import com.slte.app.kernel.AppRemoteConfig
import com.slte.app.kernel.SpeedResultStore
import com.slte.app.kernel.SubscribeSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppRemoteConfig(remoteConfig: RemoteConfig): AppRemoteConfig = remoteConfig

    @Provides
    @Singleton
    fun provideSpeedResultStore(store: SessionStore): SpeedResultStore = store

    @Provides
    @Singleton
    fun provideSubscribeSource(source: SubscribeSourceImpl): SubscribeSource = source

    @Provides
    @Singleton
    fun provideAuthApi(
        authInterceptor: AuthInterceptor,
        fallbackDns: FallbackDns,
        remoteConfig: RemoteConfig
    ): AuthApi {
        val cfg = remoteConfig.data
        val backend = ApiBackend(
            type = cfg.apiType,
            baseUrl = cfg.apiBaseUrl
        )
        return XiaoV2b.createAuthApi(
            backend,
            isDebug = BuildConfig.DEBUG,
            authInterceptor = authInterceptor,
            dns = fallbackDns,
            remoteConfig = remoteConfig
        )
    }
}
