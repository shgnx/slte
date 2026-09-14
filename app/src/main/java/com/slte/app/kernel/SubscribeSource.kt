package com.slte.app.kernel

import okhttp3.ResponseBody

/** 订阅源数据访问抽象：内核配置导入所需（token、邮箱、订阅 YAML 下载、更新时间），由 data 层实现 */
interface SubscribeSource {
    fun getEmail(): String?

    suspend fun fetchSubscribeYaml(): ResponseBody?

    fun saveSubscriptionUpdatedAt()
}
