package com.slte.app.data.remote.adapter

import com.slte.app.BuildConfig
import com.slte.app.data.remote.ApiException
import com.slte.app.data.remote.api.ApiResponse
import com.slte.app.utils.ApiErrors
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import retrofit2.HttpException

/** 适配器请求执行公共逻辑：异常映射（脱敏留痕）+ 空 data 校验，两个后端适配器共用 */
internal object AdapterExecute {
    suspend fun <R> raw(block: suspend () -> R): R = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ApiException) {
        throw e
    } catch (e: HttpException) {
        val errorBody = e.response()?.errorBody()?.string()
        if (BuildConfig.DEBUG) {
            AppLog.e("SLTE-Api", "execute HttpException: code=${e.code()}, body=${AppLog.sanitize(errorBody?.take(500) ?: "")}")
        } else {
            AppLog.w("SLTE-Api", "execute HttpException: code=${e.code()}")
        }
        val serverMessage = extractServerMessage(errorBody)
        if (serverMessage != null) {
            // 服务端给出可读原因时按业务错误抛出（不贴 ApiErrors.NETWORK）；
            // 否则 422 校验失败、部分面板把"密码错误"包成 500 的情形会被统一显示为
            // "请求失败"，真实原因仅存于 logcat，用户无法自查。
            throw ApiException(serverMessage)
        }
        throw ApiException("请求失败，请检查网络连接", ApiErrors.NETWORK)
    } catch (e: SerializationException) {
        // 解析失败必须与网络失败区分：后端字段类型变化（如 int 变 "12"）曾被打成
        // ApiErrors.NETWORK，用户看到"网络错误"反复重试，真实原因只留在 logcat。
        AppLog.e(
            "SLTE-Api",
            "响应解析失败（后端字段类型可能与客户端不一致）: ${e.javaClass.simpleName}: ${sanitizeLog(e.message ?: "Unknown")}",
        )
        throw ApiException("服务器响应格式异常，请稍后重试", ApiErrors.SERIALIZATION)
    } catch (e: IOException) {
        AppLog.w("SLTE-Api", "execute IOException: ${sanitizeLog(e.message ?: "Unknown")}")
        throw ApiException("请求失败，请检查网络连接", ApiErrors.NETWORK)
    } catch (e: Exception) {
        AppLog.w("SLTE-Api", "execute unexpected ${e.javaClass.simpleName}: ${sanitizeLog(e.message ?: "Unknown")}")
        throw ApiException("服务器响应异常", ApiErrors.NETWORK)
    }

    /**
     * 执行请求并校验响应信封：`data` 为 null 且 `message` 非空时抛 [ApiException]（message 即后端文案）。
     *
     * `data` 与 `message` 同时为 null 属契约边界情形，交由调用方处理（见 [orEmptyLogged] / [orFalseLogged]）。
     */
    suspend fun <T> typed(block: suspend () -> ApiResponse<T>): ApiResponse<T> {
        val response = raw(block)
        val message = response.message
        if (response.data == null && message != null) {
            if (BuildConfig.DEBUG) {
                AppLog.w("SLTE-Api", "execute: data=null, message=${sanitizeLog(message)}")
            }
            throw ApiException(message)
        }
        return response
    }

    /**
     * 从非 2xx 响应体提取可读原因，取不到返回 null。
     *
     * V2Board 系面板的 `errors.<字段>[0]` 承载真正有用的文案，`message` 常是无信息量的
     * 通用句，故优先取 `errors`，其次才取 `message`。
     */
    fun extractServerMessage(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        val root =
            try {
                Json.parseToJsonElement(errorBody) as? JsonObject
            } catch (_: Exception) {
                // 非 JSON 错误体（网关 HTML、Cloudflare 页面等），无可读原因
                return null
            } ?: return null

        // errors 可能是对象（Laravel 标准形态）或数组，递归取首个非空叶子
        val validationMessage = root["errors"]?.let { detailText(it) }
        if (!validationMessage.isNullOrBlank()) return validationMessage.trim()

        val genericMessage = (root["message"] as? JsonPrimitive)?.content
        return genericMessage?.takeIf { it.isNotBlank() }?.trim()
    }

    /** 校验错误值的文本：单值、数组、嵌套对象（取首个非空叶子） */
    private fun detailText(value: JsonElement): String? = when (value) {
        is JsonPrimitive -> value.content.takeIf { it.isNotBlank() }
        is JsonArray -> value.firstNotNullOfOrNull { detailText(it) }
        is JsonObject -> value.values.firstNotNullOfOrNull { detailText(it) }
        else -> null
    }
}

/**
 * 列表类响应的空值处理：data 为 null 时视为空列表并留痕。
 *
 * 仅当 data 与 message 同时为 null 时才会走到这里（message 非空已由 [AdapterExecute.typed] 抛错）。
 * 留痕是为了与"确实无数据"区分，避免服务端契约破裂时日志无任何线索。
 */
internal fun <T> List<T>?.orEmptyLogged(what: String): List<T> {
    if (this != null) return this
    AppLog.w("SLTE-Api", "$what: 响应 data 为空且无 message，按空结果处理")
    return emptyList()
}

/** 开关类响应的空值处理：null 视为 false 并留痕，语义边界同 [orEmptyLogged] */
internal fun Boolean?.orFalseLogged(what: String): Boolean {
    if (this != null) return this
    AppLog.w("SLTE-Api", "$what: 响应 data 为空且无 message，按 false 处理")
    return false
}

/**
 * 对象类响应的空值处理：null 原样返回并留痕，不臆断默认值。
 * 空对象的业务含义由调用方决定（如无提现配置 → 空列表）。
 */
internal fun <T : Any> T?.orNullLogged(what: String): T? {
    if (this == null) AppLog.w("SLTE-Api", "$what: 响应 data 为空且无 message，按空对象处理")
    return this
}
