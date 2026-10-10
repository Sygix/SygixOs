/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.tvFocusable
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.model.UpNextPosition
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HomeScreenContentTest {
    @get:Rule val compose = createComposeRule()
    private val visible = mutableStateOf(true)
    private val position = mutableStateOf(UpNextPosition.BEFORE_APPS)
    private val enabled = mutableStateOf(true)
    private fun content(extra: Boolean = false) {
        compose.setContent {
            val focus = remember { FocusRequester() }
            MaterialTheme {
                HomeScreenContent(visible.value, position.value,
                    HomeScreenActions({ visible.value = !visible.value }, { position.value = it }),
                    enabled.value, focus, additionalRows = { active ->
                        if (extra) repeat(3) { index ->
                            Spacer(Modifier.height(12.dp))
                            Text("Control", Modifier.testTag("system-control-$index").tvFocusable(enabled = active).focusable())
                        }
                    })
            }
            LaunchedEffect(Unit) { withFrameNanos { }; focus.tryRequestFocus() }
        }
        compose.waitForIdle()
    }
    private fun press(key: Key, tag: String = "settings-home-screen") {
        compose.onNodeWithTag(tag).performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }
    @Test fun `extension follows P6 rows and focus crosses boundary both ways`() {
        content(extra = true)
        compose.onNodeWithTag("setting-upnext-visible").assertIsFocused().assertIsOn()
        press(Key.DirectionDown)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("system-control-0").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        repeat(4) { press(Key.DirectionDown) }
        compose.onNodeWithTag("system-control-2").assertIsFocused()
    }
    @Test fun `position remains enabled while visibility is off`() {
        content()
        press(Key.Enter)
        compose.onNodeWithTag("setting-upnext-visible").assertIsOff()
        press(Key.DirectionDown)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        press(Key.Enter)
        compose.onNodeWithTag("setting-upnext-position-BEFORE_APPS").assertIsFocused().assertIsSelected()
        press(Key.DirectionDown, "setting-upnext-position-list")
        press(Key.Enter, "setting-upnext-position-list")
        assertEquals(UpNextPosition.AFTER_APPS, position.value)
        compose.onNodeWithTag("setting-upnext-position-list").assertDoesNotExist()
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
    }
    @Test fun `dropdown back restores row without changing value`() {
        content(extra = true)
        press(Key.DirectionDown)
        press(Key.Enter)
        press(Key.DirectionDown, "setting-upnext-position-list")
        press(Key.Back, "setting-upnext-position-list")
        assertEquals(UpNextPosition.BEFORE_APPS, position.value)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
    }
    @Test fun `dropdown direction boundaries do not wrap or escape`() {
        content(extra = true)
        press(Key.DirectionDown)
        press(Key.Enter)
        press(Key.DirectionUp, "setting-upnext-position-list")
        press(Key.DirectionLeft, "setting-upnext-position-list")
        press(Key.DirectionRight, "setting-upnext-position-list")
        compose.onNodeWithTag("setting-upnext-position-BEFORE_APPS").assertIsFocused()
        repeat(2) { press(Key.DirectionDown, "setting-upnext-position-list") }
        compose.onNodeWithTag("setting-upnext-position-AFTER_APPS").assertIsFocused()
    }
    @Test fun `content disabled closes dropdown and disables extension`() {
        content(extra = true)
        press(Key.DirectionDown)
        press(Key.Enter)
        compose.runOnIdle { enabled.value = false }
        compose.waitForIdle()
        compose.onNodeWithTag("setting-upnext-position-list").assertDoesNotExist()
        compose.onNodeWithTag("system-control-0").assertIsNotFocused()
        compose.runOnIdle { enabled.value = true }
        compose.waitForIdle()
        compose.onNodeWithTag("setting-upnext-position-list").assertDoesNotExist()
    }
}
