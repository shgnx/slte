package com.slte.app.utils

import com.slte.app.R
import okhttp3.MediaType.Companion.toMediaType

/**
 * API 层错误消息的字符串资源 ID，随 [com.slte.app.data.remote.ApiException] 一起抛给 UI。
 *
 * [com.slte.app.utils.ErrorMessages] 优先采用异常自带的资源 ID，仅在缺失时回退到
 * 关键词匹配——适配器在抛出点就知道语义，比按后端文案猜类型可靠。
 */
object ApiErrors {
    val EMPTY_DATA = R.string.api_error_empty_data
    val REGISTER_CONFIG = R.string.api_error_register_config
    val USER_INFO = R.string.api_error_user_info
    val INVITE_INFO = R.string.api_error_invite_info
    val CREATE_ORDER = R.string.api_error_create_order
    val ORDER_DETAIL = R.string.api_error_order_detail
    val COUPON_INVALID = R.string.error_coupon_invalid
    val CHECKOUT = R.string.api_error_checkout
    val NETWORK = R.string.error_network

    /** 响应 JSON 结构/类型与客户端不符（与网络失败区分，避免用户误以为网络问题反复重试） */
    val SERIALIZATION = R.string.api_error_bad_response

    val UNSUPPORTED_BACKEND = R.string.api_error_unsupported_backend
}

object Constants {
    /** 网络请求单项超时（秒）：连接/读/写各自的上限 */
    const val API_TIMEOUT_SECONDS = 30L

    /**
     * 单次请求的整体预算（秒）：略高于单项超时，容纳一次偏慢但正常的请求，
     * 同时封顶「多候选地址串行 failover + OkHttp 自动重试」的叠加等待
     * （无此上限时最坏可达单项超时的数倍，用户侧表现为永不结束的转圈）。
     */
    const val API_CALL_TIMEOUT_SECONDS = 45L

    /** Retrofit Kotlin Serialization 所需 MediaType */
    val JSON_MEDIA_TYPE = "application/json".toMediaType()

    /** 未连接内核时的兜底代理模式 */
    const val DEFAULT_PROXY_MODE = "规则"
    const val PROXY_MODE_GLOBAL = "全局"

    /** 内核可能返回但用户不可选的模式（仅用于展示映射） */
    const val PROXY_MODE_DIRECT = "直连"
    const val PROXY_MODE_SCRIPT = "脚本"
    const val PLACEHOLDER_DASH = "--"

    /**
     * 测速超时哨兵值：表示该节点本轮未完成测速（内核返回的失败/未测通标记），
     * 非真实延迟。kernel 桥接层与 UI 层共用，禁止在两处各自硬编码 999。
     */
    const val DELAY_TIMEOUT = 999

    /**
     * 内核判定为无效延迟的下限：mihomo 用 2^16-1=65535（及以下边界巧值）表示不可达，
     * [com.slte.app.kernel.KernelProxy.normalizeDelay] 据此将 [>= 本值] 归一化为 [DELAY_TIMEOUT]。
     */
    const val DELAY_INVALID_MAX = 65535
}

/**
 * TGS 动态贴纸资源路径。
 * 文件位于 src/main/assets/stickers/，运行时本地加载，无需网络。
 */
object Stickers {
    const val LOGIN = "stickers/login.tgs"
    const val FORGOT_PASSWORD = "stickers/forgot.tgs"
    const val REGISTER = "stickers/register.tgs"
    const val EMPTY = "stickers/empty.tgs"
    const val ERROR = "stickers/error.tgs"
    const val UPDATE = "stickers/update.tgs"
    const val FORCE_UPDATE = "stickers/force_update.tgs"
    const val INVITE = "stickers/invite.tgs"
}
