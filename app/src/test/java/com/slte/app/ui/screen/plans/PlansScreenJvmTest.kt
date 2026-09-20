package com.slte.app.ui.screen.plans

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.slte.app.data.remote.ApiException
import com.slte.app.data.remote.api.dto.PlanInfoDto
import com.slte.app.data.repository.GiftCardRepository
import com.slte.app.data.repository.OrderRepository
import com.slte.app.support.FakeAuthApi
import com.slte.app.support.RobolectricTestApplication
import com.slte.app.ui.screen.giftcard.GiftCardRedeemViewModel
import com.slte.app.ui.theme.SlteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh-rCN-w411dp-h891dp-420dpi", application = RobolectricTestApplication::class)
class PlansScreenJvmTest {
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

    private fun content() {
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
    }

    @Test
    fun 渲染套餐卡并可打开购买弹层() {
        api.plans = listOf(plan(1))
        viewModel.enterAndRefresh()
        content()
        composeRule.waitUntil(5_000) { viewModel.data.value.plans.isNotEmpty() }

        composeRule.onNodeWithText("进阶套餐").assertIsDisplayed()
        composeRule.onNode(hasText("订阅") and hasClickAction()).performClick()

        composeRule.onNodeWithText("有优惠券？").assertIsDisplayed()
    }

    @Test
    fun 加载失败显示错误态() {
        api.plansError = java.io.IOException("boom")
        viewModel.retry()
        content()
        composeRule.waitUntil(5_000) { viewModel.data.value.errorMessageRes != null }

        composeRule.onNodeWithText("重试").assertIsDisplayed()
    }

    @Test
    fun 右上角礼品卡入口打开兑换弹窗() {
        api.plans = listOf(plan(1))
        viewModel.enterAndRefresh()
        content()
        composeRule.waitUntil(5_000) { viewModel.data.value.plans.isNotEmpty() }

        composeRule.onNodeWithContentDescription("礼品卡兑换").performClick()

        composeRule.onNodeWithText("请输入兑换码").assertIsDisplayed()
    }

    @Test
    fun 兑换成功后关闭弹窗并弹出礼品卡兑换成功提示() {
        api.plans = listOf(plan(1))
        viewModel.enterAndRefresh()
        content()
        composeRule.waitUntil(5_000) { viewModel.data.value.plans.isNotEmpty() }

        composeRule.onNodeWithContentDescription("礼品卡兑换").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("SLTE2026")
        composeRule.onNode(hasText("兑换") and hasClickAction()).performClick()
        composeRule.waitUntil(5_000) { !giftCardViewModel.state.value.visible }
        composeRule.waitUntil(5_000) { ShadowToast.getTextOfLatestToast() != null }

        composeRule.onNodeWithText("请输入兑换码").assertDoesNotExist()
        assertEquals("礼品卡兑换成功", ShadowToast.getTextOfLatestToast())
    }

    @Test
    fun 兑换失败弹出后端返回的提示() {
        api.plans = listOf(plan(1))
        api.giftCardRedeemError = ApiException("兑换码不存在")
        viewModel.enterAndRefresh()
        content()
        composeRule.waitUntil(5_000) { viewModel.data.value.plans.isNotEmpty() }

        composeRule.onNodeWithContentDescription("礼品卡兑换").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("BADCODE")
        composeRule.onNode(hasText("兑换") and hasClickAction()).performClick()
        composeRule.waitUntil(5_000) { ShadowToast.getTextOfLatestToast() != null }

        composeRule.onNodeWithText("BADCODE").assertIsDisplayed()
        assertEquals("兑换码无效或已过期", ShadowToast.getTextOfLatestToast())
    }
}
