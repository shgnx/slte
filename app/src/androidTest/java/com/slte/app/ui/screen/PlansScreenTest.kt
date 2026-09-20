package com.slte.app.ui.screen.plans

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.slte.app.data.remote.api.dto.PlanInfoDto
import com.slte.app.data.repository.GiftCardRepository
import com.slte.app.data.repository.OrderRepository
import com.slte.app.support.FakeAuthApi
import com.slte.app.ui.screen.giftcard.GiftCardRedeemViewModel
import com.slte.app.ui.theme.SlteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlansScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val api = FakeAuthApi()
    private val repository = OrderRepository(api)
    private val viewModel = PlansViewModel(repository)
    private val giftCardViewModel = GiftCardRedeemViewModel(GiftCardRepository(api))
    private val purchaseViewModel =
        PurchaseViewModel(
            couponChecker = CouponChecker(repository),
            paymentLoader = OrderPaymentLoader(repository),
            poller = OrderPaymentPoller(repository),
            orderCreator = OrderCreator(repository),
            paymentCheckout = PaymentCheckout(repository),
        )

    private fun plan(id: Int) = PlanInfoDto(
        id = id,
        name = "进阶套餐",
        monthPrice = 5_000,
        transferEnable = 400,
        show = true,
    )

    @Test
    fun 渲染套餐卡并可打开购买弹层() {
        api.plans = listOf(plan(1))
        viewModel.enterAndRefresh()

        composeRule.setContent {
            SlteTheme {
                PlansScreen(
                    onBack = {},
                    viewModel = viewModel,
                    purchaseViewModel = purchaseViewModel,
                    giftCardViewModel = giftCardViewModel,
                )
            }
        }
        composeRule.waitUntil(5_000) { viewModel.data.value.plans.isNotEmpty() }

        composeRule.onNodeWithText("进阶套餐").assertIsDisplayed()
        composeRule.onNode(hasText("订阅") and hasClickAction()).performClick()

        composeRule.onNodeWithText("有优惠券？").assertIsDisplayed()
    }

    @Test
    fun 加载失败显示错误态() {
        api.plansError = java.io.IOException("boom")
        viewModel.retry()

        composeRule.setContent {
            SlteTheme {
                PlansScreen(
                    onBack = {},
                    viewModel = viewModel,
                    purchaseViewModel = purchaseViewModel,
                    giftCardViewModel = giftCardViewModel,
                )
            }
        }
        composeRule.waitUntil(5_000) { viewModel.data.value.errorMessageRes != null }

        composeRule.onNodeWithText("重试").assertIsDisplayed()
    }
}
