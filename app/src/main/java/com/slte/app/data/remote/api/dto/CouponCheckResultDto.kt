package com.slte.app.data.remote.api.dto

/**
 * 优惠券校验结果。
 *
 * @param name 优惠券名称
 * @param type 2=百分比折扣，1=固定金额减扣
 * @param value 百分比时表示折扣百分数，固定金额时表示减扣金额（分）
 */
data class CouponCheckResultDto(
    val name: String,
    val type: Int,
    val value: Int,
)
