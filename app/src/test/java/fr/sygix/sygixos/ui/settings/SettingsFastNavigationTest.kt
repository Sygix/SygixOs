/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasParent
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyPress
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.FastNavigationHost
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.ui.home.WithTestArtwork
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class SettingsFastNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private val entered = mutableListOf<SettingsCategory>()
    private var time = 10_000L

    private fun show() {
        compose.setContent {
            WithTestArtwork {
                MaterialTheme {
                    FastNavigationHost {
                        SettingsScreen(
                            state = SettingsState(version = "0.1.0"),
                            actions = SettingsActions(onCategoryEntered = { entered += it }),
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
        compose.mainClock.autoAdvance = false
    }

    private fun send(action: Int, down: Long, repeat: Int) {
        val code = Key.DirectionDown.nativeKeyCode
        compose.onRoot().performKeyPress(KeyEvent(NativeKeyEvent(down, time, action, code, repeat)))
    }

    private fun emphasized(category: SettingsCategory): Boolean {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNode(hasParent(hasTestTag("settings-category-${category.name}")), useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single().layoutInput.style.fontWeight == TextStyles.RowFocused.fontWeight
    }

    @Test
    fun `a held key moves the focus at once and shows the pane of the category where it rests`() {
        show()
        val down = time
        repeat(8) { index ->
            send(NativeKeyEvent.ACTION_DOWN, down, index)
            compose.mainClock.advanceTimeBy(REPEAT_MS)
            time += REPEAT_MS
        }
        send(NativeKeyEvent.ACTION_UP, down, 0)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("settings-category-ABOUT").assertIsFocused()
        assertTrue(emphasized(SettingsCategory.ABOUT))
        assertFalse(emphasized(SettingsCategory.LANGUAGE))
        compose.onNodeWithTag("settings-about").assertDoesNotExist()
        assertEquals(listOf(SettingsCategory.HIDDEN), entered)
        compose.mainClock.advanceTimeBy(Motion.FOCUS_MS.toLong() + REPEAT_MS)
        compose.onNodeWithTag("settings-about").assertExists()
        compose.onNodeWithTag("settings-category-ABOUT").assertIsFocused()
        assertEquals(listOf(SettingsCategory.HIDDEN, SettingsCategory.ABOUT), entered)
    }

    @Test
    fun `an isolated press after a pause shows its pane at once and animates the pill`() {
        show()
        send(NativeKeyEvent.ACTION_DOWN, time, 0)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        assertEquals(listOf(SettingsCategory.HIDDEN), entered)
        assertFalse(emphasized(SettingsCategory.HIDDEN))
        compose.mainClock.advanceTimeBy(Motion.FOCUS_MS.toLong())
        assertTrue(emphasized(SettingsCategory.HIDDEN))
    }

    private companion object {
        const val REPEAT_MS = 50L
    }
}
