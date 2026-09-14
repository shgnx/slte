package com.slte.app.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.slte.app.R
import com.slte.app.ui.theme.SlteTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 共享交互组件的仪器化测试（需连接设备/模拟器）：
 * 覆盖 SlteSwitch 无障碍语义、SlteInput 输入回传、SlteButton 禁用态不回调与 SlteRowCard 整行可点。
 */
@RunWith(AndroidJUnit4::class)
class SlteComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun switch_带开关语义与状态描述() {
        var checked by mutableStateOf(false)
        val offLabel = context.getString(R.string.switch_state_off)

        composeRule.setContent {
            SlteTheme {
                SlteSwitch(checked = checked, onCheckedChange = { checked = it })
            }
        }

        composeRule
            .onNode(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, offLabel))
            .assertIsDisplayed()
            .performClick()

        assertTrue("点击开关应切换为开启", checked)
    }

    @Test
    fun input_展示占位符且输入可回传() {
        var value by mutableStateOf("")
        composeRule.setContent {
            SlteTheme {
                SlteInput(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = "请输入邮箱",
                )
            }
        }

        composeRule.onNodeWithText("请输入邮箱").assertIsDisplayed()
        composeRule.onNode(hasSetTextAction()).performTextInput("user@example.com")

        assertEquals("user@example.com", value)
    }

    @Test
    fun button_禁用态不触发点击() {
        var clicks = 0
        composeRule.setContent {
            SlteTheme {
                Column {
                    SlteButton(text = "禁用", onClick = { clicks++ }, enabled = false)
                    SlteButton(text = "可用", onClick = { clicks++ }, enabled = true)
                }
            }
        }

        composeRule.onNodeWithText("禁用").performClick()
        assertEquals("禁用按钮不应触发回调", 0, clicks)

        composeRule.onNodeWithText("可用").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun rowCard_整行可点击() {
        var clicked = false
        composeRule.setContent {
            SlteTheme {
                SlteRowCard(
                    icon = Icons.Outlined.Info,
                    title = "关于软件",
                    chevron = true,
                    onClick = { clicked = true },
                )
            }
        }

        composeRule.onNodeWithText("关于软件").assertIsDisplayed().performClick()
        assertTrue("行卡片应可点击", clicked)
    }
}
