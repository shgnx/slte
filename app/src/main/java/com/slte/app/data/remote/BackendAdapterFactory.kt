package com.slte.app.data.remote

import com.slte.app.BuildConfig
import com.slte.app.data.remote.adapter.xboard.XboardAuthApi
import com.slte.app.data.remote.adapter.xboard.XboardAuthRetrofit
import com.slte.app.data.remote.adapter.xboard.XboardUserPlanRetrofit
import com.slte.app.data.remote.adapter.xboard.XboardUserRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bAuthApi
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bAuthRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bUserPlanRetrofit
import com.slte.app.data.remote.adapter.xiaov2b.XiaoV2bUserRetrofit
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.data.remote.config.ApiFailoverInterceptor
import com.slte.app.data.remote.config.RemoteConfig
import com.slte.app.utils.ApiErrors
import com.slte.app.utils.AppLog
import com.slte.app.utils.Constants
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** 订阅下载完整 URL = 当前主地址 + 构建期 SUBSCRIBE_PATH（与内核 profile source 一致）+ token */
internal fun subscribeFetchUrl(
    apiBaseUrl: String,
    token: String,
): String? = runCatching {
    (apiBaseUrl.trimEnd('/') + BuildConfig.SUBSCRIBE_PATH)
        .toHttpUrlOrNull()
        ?.newBuilder()
        ?.addQueryParameter("token", token)
        ?.build()
        ?.toString()
}.getOrNull()

/**
 * 后端适配器工厂：按面板类型创建对应的 [AuthApi] 实现。
 *
 * 两套面板的 DTO / 请求 / Retrofit 接口类型族不同（见 adapter/xboard 与 adapter/xiaov2b），
 * 因此各自独立实现；本工厂只负责装配共用的 OkHttp / Retrofit 配置。
 */
object BackendAdapterFactory {
    // coerceInputValues：后端返回 null 时用字段默认值
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

    fun createAuthApi(
        backend: ApiBackend,
        isDebug: Boolean,
        authInterceptor: AuthInterceptor,
        dns: okhttp3.Dns,
        remoteConfig: RemoteConfig,
    ): AuthApi = when (backend.type) {
        "xiaov2b" -> {
            val retrofit = buildRetrofit(backend, isDebug, authInterceptor, dns, remoteConfig)
            XiaoV2bAuthApi(
                authApi = retrofit.create(XiaoV2bAuthRetrofit::class.java),
                userApi = retrofit.create(XiaoV2bUserRetrofit::class.java),
                userPlanApi = retrofit.create(XiaoV2bUserPlanRetrofit::class.java),
            )
        }
        "xboard" -> {
            // Xboard 为 V2Board 同构重写，格式差异（布尔开关/会话列表）已隔离在 adapter/xboard 独立 DTO
            val retrofit = buildRetrofit(backend, isDebug, authInterceptor, dns, remoteConfig)
            XboardAuthApi(
                authApi = retrofit.create(XboardAuthRetrofit::class.java),
                userApi = retrofit.create(XboardUserRetrofit::class.java),
                userPlanApi = retrofit.create(XboardUserPlanRetrofit::class.java),
            )
        }
        else -> throw ApiException("不支持的后端类型: ${backend.type}", ApiErrors.UNSUPPORTED_BACKEND)
    }

    private fun buildRetrofit(
        backend: ApiBackend,
        isDebug: Boolean,
        authInterceptor: AuthInterceptor,
        dns: okhttp3.Dns,
        remoteConfig: RemoteConfig,
    ): Retrofit {
        val client =
            OkHttpClient
                .Builder()
                .connectTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                // 整体预算：单项超时无法约束「多候选地址串行 + OkHttp 自动重试」的叠加，
                // 最坏可达 connect/read 超时的数倍，用户侧就是一个不结束的转圈
                .callTimeout(Constants.API_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .dns(dns) // 系统 DNS 失败时走备用 DNS（单例，可随 VPN 状态清缓存）
                .addInterceptor(ApiFailoverInterceptor(remoteConfig, remoteConfig.endpointSelector))
                .addInterceptor(authInterceptor)
                .apply {
                    if (isDebug) {
                        // 自定义调试日志：只输出方法 + 路径——域名与 query 不进日志（订阅 URL 携带 token）
                        addInterceptor { chain ->
                            val request = chain.request()
                            val safePath = request.url.encodedPath
                            AppLog.d("SLTE-Api", "${request.method} $safePath")
                            val response = chain.proceed(request)
                            AppLog.d("SLTE-Api", "${request.method} ${response.code} $safePath")
                            response
                        }
                    }
                }.build()

        val baseUrl = backend.baseUrl.trimEnd('/') + backend.apiPrefix + "/"
        return Retrofit
            .Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(Constants.JSON_MEDIA_TYPE))
            .build()
    }
}
