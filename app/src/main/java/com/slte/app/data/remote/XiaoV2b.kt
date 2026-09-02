package com.slte.app.data.remote

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
import com.slte.app.data.remote.ApiBackend
import com.slte.app.utils.ApiErrors
import com.slte.app.utils.AppLog
import com.slte.app.utils.Constants
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** 后端适配器工厂：按后端类型分发到对应适配器实现 */
object XiaoV2b {

    // coerceInputValues：后端返回 null 时用字段默认值
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    fun createAuthApi(
        backend: ApiBackend,
        isDebug: Boolean,
        authInterceptor: AuthInterceptor,
        dns: okhttp3.Dns,
        remoteConfig: RemoteConfig
    ): AuthApi = when (backend.type) {
        "xiaov2b" -> {
            val retrofit = buildRetrofit(backend, isDebug, authInterceptor, dns, remoteConfig)
            val authRetrofit = retrofit.create(XiaoV2bAuthRetrofit::class.java)
            val userRetrofit = retrofit.create(XiaoV2bUserRetrofit::class.java)
            val userPlanRetrofit = retrofit.create(XiaoV2bUserPlanRetrofit::class.java)
            XiaoV2bAuthApi(authRetrofit, userRetrofit, userPlanRetrofit)
        }
        "xboard" -> {
            // Xboard 为 V2Board 同构重写，格式差异（布尔开关/会话列表）已隔离在 adapter/xboard 独立 DTO
            val retrofit = buildRetrofit(backend, isDebug, authInterceptor, dns, remoteConfig)
            val authRetrofit = retrofit.create(XboardAuthRetrofit::class.java)
            val userRetrofit = retrofit.create(XboardUserRetrofit::class.java)
            val userPlanRetrofit = retrofit.create(XboardUserPlanRetrofit::class.java)
            XboardAuthApi(authRetrofit, userRetrofit, userPlanRetrofit)
        }
        else -> throw IllegalArgumentException("不支持的后端类型: ${backend.type}")
    }

    private fun buildRetrofit(
        backend: ApiBackend,
        isDebug: Boolean,
        authInterceptor: AuthInterceptor,
        dns: okhttp3.Dns,
        remoteConfig: RemoteConfig
    ): Retrofit {
        val client = OkHttpClient.Builder()
            .connectTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .dns(dns)  // 系统 DNS 失败时走备用 DNS（单例，可随 VPN 状态清缓存）
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
            }
            .build()

        val baseUrl = backend.baseUrl.trimEnd('/') + backend.apiPrefix + "/"
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(Constants.JSON_MEDIA_TYPE))
            .build()
    }
}
