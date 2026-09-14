package com.slte.app.data.remote.api

/**
 * 面板响应信封：`{ data, message }`。
 *
 * Xboard 与 V2Board 的响应结构完全一致，差别只在负载类型，因此两套后端各自的
 * `@Serializable` 信封都实现本接口。这样 [com.slte.app.data.remote.adapter.AdapterExecute]
 * 只写一份取用逻辑——此前它靠每个适配器传入 `{ data to message }` 适配 lambda，
 * 一旦某处漏传或改错，该后端的错误提示会静默失效。
 */
interface ApiResponse<out T> {
    val data: T?
    val message: String?
}
