/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.FLAG_LONG_PRESS
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvClickableTest {

    @get:Rule
    val compose = createComposeRule()

    private var clicks = 0
    private var longClicks = 0

    private fun showFocused() {
        compose.setContent {
            val focus = remember { FocusRequester() }
            Box(Modifier.size(40.dp).testTag("target").focusRequester(focus).tvClickable(onClick = { clicks++ }, onLongClick = { longClicks++ }))
            LaunchedEffect(Unit) { focus.requestFocus() }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("target").assertIsFocused()
        compose.mainClock.autoAdvance = false
    }

    private fun center(action: Int, repeat: Int = 0, flags: Int = 0) {
        compose.onRoot().performKeyPress(KeyEvent(android.view.KeyEvent(0, 0, action, KEYCODE_DPAD_CENTER, repeat, 0, 0, 0, flags)))
    }

    @Test
    fun `a short press clicks once and never fires the long press`() {
        showFocused()
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter); keyUp(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS * 2)
        assertEquals(1, clicks)
        assertEquals(0, longClicks)
    }

    @Test
    fun `holding select fires the long press once after the delay and the release does not click`() {
        showFocused()
        center(ACTION_DOWN)
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS - 50)
        assertEquals(0, longClicks)
        compose.mainClock.advanceTimeBy(100)
        assertEquals(1, longClicks)
        center(ACTION_UP)
        assertEquals(0, clicks)
    }

    @Test
    fun `a system long press repeat fires the long press immediately and only once`() {
        showFocused()
        center(ACTION_DOWN)
        center(ACTION_DOWN, repeat = 1, flags = FLAG_LONG_PRESS)
        assertEquals(1, longClicks)
        center(ACTION_DOWN, repeat = 2, flags = FLAG_LONG_PRESS)
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS * 2)
        center(ACTION_UP)
        assertEquals(1, longClicks)
        assertEquals(0, clicks)
    }
}
