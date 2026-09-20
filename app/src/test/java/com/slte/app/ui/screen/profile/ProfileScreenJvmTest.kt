package com.slte.app.ui.screen.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.slte.app.data.local.SessionManager
import com.slte.app.data.repository.AuthRepository
import com.slte.app.data.repository.GiftCardRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.domain.model.SessionState
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.usecase.DaysUntilExpiryUseCase
import com.slte.app.support.FakeAuthApi
import com.slte.app.support.RobolectricTestApplication
import com.slte.app.ui.screen.giftcard.GiftCardRedeemViewModel
import com.slte.app.ui.theme.SlteTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "zh-rCN-w411dp-h891dp-420dpi", application = RobolectricTestApplication::class)
class ProfileScreenJvmTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val subscribeRepository = mockk<SubscribeRepository>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val expiryUseCase = mockk<DaysUntilExpiryUseCase>(relaxed = true)
    private val api = FakeAuthApi()
    private val giftCardViewModel = GiftCardRedeemViewModel(GiftCardRepository(api))

    private fun content() {
        every { subscribeRepository.subscribeInfo } returns MutableStateFlow<SubscribeInfo?>(null)
        every { sessionManager.sessionState } returns MutableStateFlow(SessionState.LoggedOut)
        val viewModel = ProfileViewModel(subscribeRepository, sessionManager, authRepository, expiryUseCase)

        composeRule.setContent {
            SlteTheme {
                ProfileScreen(
                    onBack = {},
                    viewModel = viewModel,
                    giftCardViewModel = giftCardViewModel,
                )
            }
        }
    }

    @Test
    fun 右上角礼品卡入口打开兑换弹窗() {
        content()

        composeRule.onNodeWithContentDescription("礼品卡兑换").performClick()

        composeRule.onNodeWithText("请输入兑换码").assertIsDisplayed()
    }
}
