package com.slte.app

import com.slte.app.ui.screen.plans.computeCouponDiscount
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 优惠券折扣计算测试：百分比/固定金额、负值钳制与未知类型回退。
 */
class PurchaseDiscountTest {
    @Test
    fun `百分比折扣按原价计算`() {
        assertEquals(60, computeCouponDiscount(type = 2, value = 50, priceCents = 120))
        assertEquals(0, computeCouponDiscount(type = 2, value = 0, priceCents = 120))
        assertEquals(120, computeCouponDiscount(type = 2, value = 100, priceCents = 120))
    }

    @Test
    fun `固定金额折扣直接扣减`() {
        assertEquals(30, computeCouponDiscount(type = 1, value = 30, priceCents = 120))
    }

    @Test
    fun `折扣不为负`() {
        // 异常负值输入钳制为 0；超额折扣由 finalPrice 封顶，不在折扣计算内
        assertEquals(0, computeCouponDiscount(type = 1, value = -5, priceCents = 120))
        assertEquals(0, computeCouponDiscount(type = 2, value = -10, priceCents = 120))
        assertEquals(240, computeCouponDiscount(type = 2, value = 200, priceCents = 120))
        assertEquals(999, computeCouponDiscount(type = 1, value = 999, priceCents = 120))
    }

    @Test
    fun `未知类型按固定金额处理`() {
        assertEquals(10, computeCouponDiscount(type = 0, value = 10, priceCents = 120))
    }
}
