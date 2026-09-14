package com.slte.app.domain.model

/**
 * 套餐领域模型。
 *
 * @param id 套餐 ID
 * @param name 套餐名称
 * @param transferEnable 流量（GB）
 * @param speedLimit 限速（Mbps），null 表示不限速
 * @param deviceLimit 设备数限制，null 表示不限制
 * @param content 套餐描述（HTML 格式）
 * @param periodPrices 可用周期价格列表
 */
data class PlanInfo(
    val id: Long,
    val name: String,
    val transferEnable: Int = 0,
    val speedLimit: Int? = null,
    val deviceLimit: Int? = null,
    val content: String? = null,
    val show: Boolean = true,
    val periodPrices: List<PeriodPrice>,
) {
    data class PeriodPrice(
        val period: String,
        val price: String,
    )

    val availablePeriods: List<String>
        get() = periodPrices.map { it.period }

    fun priceForPeriod(period: String): String? = periodPrices.find { it.period == period }?.price
}
