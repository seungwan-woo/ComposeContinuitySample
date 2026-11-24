package com.example.adaptivesettings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.awaitCancellation
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ResizeContinuityTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun focusedEditorAndDraftSurviveRepeatedPaneChanges() {
        val windowSize = mutableStateOf(DpSize(390.dp, 720.dp))
        showSettingsAt(windowSize)

        val list = compose.onNodeWithTag("settings-list")
        list.performScrollToKey("device")
        compose.onNodeWithText("디바이스 정보").performClick()
        val editor = compose.onNodeWithTag("device-name")
        editor.performTextReplacement("리사이즈 테스트")
        editor.assertIsFocused()

        for ((width, height) in listOf(760 to 420, 520 to 420, 810 to 420, 390 to 720)) {
            compose.runOnIdle { windowSize.value = DpSize(width.dp, height.dp) }
            if (width >= 600) {
                list.assertIsDisplayed()
            } else {
                list.assertDoesNotExist()
            }
            editor.assertIsDisplayed().assertIsFocused().assertTextContains("리사이즈 테스트")
        }

        compose.onNodeWithText("‹  설정 목록").performClick()
        list.assertIsDisplayed()
        compose.onNodeWithText("디바이스 정보").assertIsDisplayed().performClick()
        editor.assertTextContains("리사이즈 테스트")
    }

    @Test
    fun focusedEditorMovesThroughIntermediatePositionsWhenListAppears() {
        val windowSize = mutableStateOf(DpSize(390.dp, 720.dp))
        showSettingsAt(windowSize)
        compose.onNodeWithTag("settings-list").performScrollToKey("device")
        compose.onNodeWithText("디바이스 정보").performClick()
        val editor = compose.onNodeWithTag("device-name")
        editor.performTextReplacement("편집 중")
        val start = editor.getUnclippedBoundsInRoot()

        compose.mainClock.autoAdvance = false
        val middle = try {
            compose.runOnUiThread {
                Snapshot.withMutableSnapshot { windowSize.value = DpSize(760.dp, 420.dp) }
            }
            repeat(7) {
                compose.mainClock.advanceTimeByFrame()
                compose.waitForIdle()
            }
            editor.assertIsDisplayed().assertIsFocused().assertTextContains("편집 중")
            editor.getUnclippedBoundsInRoot()
        } finally {
            compose.mainClock.autoAdvance = true
        }

        val end = editor.getUnclippedBoundsInRoot()
        assertTrue(
            "Expected motion through an intermediate position: $start → $middle → $end",
            start.left < middle.left && middle.left < end.left,
        )
        assertTrue(
            "Expected the editor to resize without overshooting: $start → $middle → $end",
            middle.width in minOf(start.width, end.width)..maxOf(start.width, end.width),
        )
        editor.assertIsFocused().assertTextContains("편집 중")
    }

    private fun showSettingsAt(windowSize: State<DpSize>) {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(windowSize.value)) {
                // ForcedSize changes Compose bounds independently of the real keyboard.
                InterceptPlatformTextInput(interceptor = { _, _ -> awaitCancellation() }) {
                    SettingsTheme { SettingsScreen() }
                }
            }
        }
    }
}

private val DpRect.width get() = right - left
