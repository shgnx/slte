package com.slte.app.data.remote.api.dto

/**
 * 套餐列表项（来自 [com.slte.app.data.remote.api.AuthApi.fetchPlans]）。
 *
 * @param id 套餐 ID
 * @param name 套餐名称
 * @param monthPrice 月付价格（分），null 表示不支持
 * @param quarterPrice 季付价格（分）
 * @param halfYearPrice 半年付价格（分）
 * @param yearPrice 年付价格（分）
 * @param twoYearPrice 两年付价格（分）
 * @param threeYearPrice 三年付价格（分）
 * @param onetimePrice 一次性价格（分）
 * @param resetPrice 重置流量价格（分）
 * @param speedLimit 限速（Mbps），null 表示不限速
 * @param deviceLimit 设备数限制，null 表示不限制
 * @param content 套餐描述（HTML 格式）
 * @param show 是否在前台展示
 * @param renew 是否可续费
 * @param sort 排序
 */
data class PlanInfoDto(
    val id: Int,
    val name: String,
    val monthPrice: Long? = null,
    val quarterPrice: Long? = null,
    val halfYearPrice: Long? = null,
    val yearPrice: Long? = null,
    val twoYearPrice: Long? = null,
    val threeYearPrice: Long? = null,
    val onetimePrice: Long? = null,
    val resetPrice: Long? = null,
    val speedLimit: Int? = null,
    val deviceLimit: Int? = null,
    val content: String? = null,
    val transferEnable: Int = 0,
    val show: Boolean = true,
    val renew: Boolean = true,
    val sort: Int? = null,
)
