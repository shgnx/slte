package com.slte.app.data.remote.adapter.xboard

import com.slte.app.BuildConfig
import com.slte.app.data.remote.ApiException
import com.slte.app.utils.ApiErrors
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException
import java.io.IOException

/**
 * 统一请求执行：空 data 校验 + 异常映射（脱敏留痕）。
 * 供 XboardAuthApi 各方法复用。
 */
internal suspend fun <T> executeXboard(block: suspend () -> XboardResponse<T>): XboardResponse<T> {
    return try {
        val response = block()
        if (response.data == null && response.message != null) {
            if (BuildConfig.DEBUG) {
                AppLog.w("SLTE-Api", "execute: data=null, message=${sanitizeLog(response.message)}")
            }
            throw ApiException(response.message)
        }
        response
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        throw e
    } catch (e: HttpException) {
        val errorBody = e.response()?.errorBody()?.string()
        // 网络/服务端错误无条件留痕（脱敏），release 也需可排查；DEBUG 时附带截断脱敏后的响应体
        if (BuildConfig.DEBUG) {
            AppLog.e("SLTE-Api", "execute HttpException: code=${e.code()}, body=${AppLog.sanitize(errorBody?.take(500) ?: "")}")
        } else {
            AppLog.w("SLTE-Api", "execute HttpException: code=${e.code()}")
        }
        val errorMessage = try {
            errorBody?.let {
                Json.parseToJsonElement(it)
                    .jsonObject["message"]?.let { m -> (m as? JsonPrimitive)?.content }
            }
        } catch (_: Exception) { null }
        throw ApiException(errorMessage ?: "请求失败，请检查网络连接", ApiErrors.NETWORK)
    } catch (e: IOException) {
        // 网络异常无条件留痕（脱敏）：IOException 消息含完整请求 URL，需 sanitize
        AppLog.w("SLTE-Api", "execute IOException: ${sanitizeLog(e.message ?: "Unknown")}")
        throw ApiException("请求失败，请检查网络连接", ApiErrors.NETWORK)
    } catch (e: Exception) {
        AppLog.w("SLTE-Api", "execute unexpected ${e.javaClass.simpleName}: ${sanitizeLog(e.message ?: "Unknown")}")
        throw ApiException("服务器响应异常", ApiErrors.NETWORK)
    }
}
