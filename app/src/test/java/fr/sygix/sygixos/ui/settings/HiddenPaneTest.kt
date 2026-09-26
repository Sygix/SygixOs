/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HiddenPaneTest {

    @get:Rule
    val compose = createComposeRule()

    private val harness = SettingsHarness()

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun openHiddenPane(rows: Int) {
        compose.setContent { harness.Content() }
        compose.waitForIdle()
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hiddenRow).fetchSemanticsNodes().size == rows
        }
    }

    private val hiddenRow = SemanticsMatcher("hidden row") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("hidden-row-") == true
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag).getBoundsInRoot().top.value

    @Test
    fun `pane lists hidden apps under unhide-all, most recently hidden first, without sub screen`() {
        harness.seed("Alpha", "Beta", "Gamma")
        harness.hideInOrder("Beta", "Gamma", "Alpha")
        openHiddenPane(rows = 3)
        compose.onNode(hasTestTag("hidden-row-com.alpha") and hasAnyAncestor(hasTestTag("hidden-pane"))).assertIsDisplayed()
        compose.onNodeWithTag("hidden-pane").assertExists()
        val tops = listOf("unhide-all", "hidden-row-com.alpha", "hidden-row-com.gamma", "hidden-row-com.beta").map(::top)
        assertEquals(tops.sorted(), tops)
        assertTrue(tops.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun `right focuses the first row and up reaches unhide-all`() {
        harness.seed("Alpha", "Beta")
        harness.hideInOrder("Alpha", "Beta")
        openHiddenPane(rows = 2)
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("unhide-all").assertIsFocused()
    }

    @Test
    fun `ok unhides a row in place and a second ok hides it again`() {
        harness.seed("Alpha", "Beta")
        harness.hideInOrder("Alpha", "Beta")
        openHiddenPane(rows = 2)
        press(Key.DirectionRight)
        press(Key.Enter)
        assertEquals(listOf("com.beta"), harness.unhid)
        compose.waitForToggle("hidden-row-com.beta", on = false)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsOff().assertIsFocused()
        assertEquals(setOf("com.alpha"), harness.hiddenNow())

        press(Key.Enter)
        assertEquals(listOf("com.beta"), harness.hid)
        compose.waitForToggle("hidden-row-com.beta", on = true)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsOn().assertIsFocused()
        assertEquals(setOf("com.alpha", "com.beta"), harness.hiddenNow())
        assertTrue(top("hidden-row-com.beta") < top("hidden-row-com.alpha"))
    }

    @Test
    fun `unhide-all keeps every row visible and the focus on the button`() {
        harness.seed("Alpha", "Beta")
        harness.hideInOrder("Alpha", "Beta")
        openHiddenPane(rows = 2)
        press(Key.DirectionRight)
        press(Key.DirectionUp)
        press(Key.Enter)
        assertEquals(1, harness.unhideAllCalls)
        compose.waitForToggle("hidden-row-com.alpha", on = false)
        compose.waitForToggle("hidden-row-com.beta", on = false)
        compose.onNodeWithTag("unhide-all").assertIsFocused()
        assertEquals(emptySet<String>(), harness.hiddenNow())
    }

    @Test
    fun `empty pane shows only the empty message, right keeps the category and back closes`() {
        harness.seed("Alpha")
        openHiddenPane(rows = 0)
        compose.onNodeWithTag("hidden-empty").assertExists()
        compose.onNodeWithTag("unhide-all").assertDoesNotExist()
        press(Key.DirectionRight)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.Back)
        assertEquals(1, harness.backs)
    }

    @Test
    fun `edges do not loop, left returns to the category and back closes`() {
        harness.seed("Alpha", "Beta")
        harness.hideInOrder("Alpha", "Beta")
        openHiddenPane(rows = 2)
        press(Key.DirectionRight)
        press(Key.DirectionUp)
        press(Key.DirectionUp)
        compose.onNodeWithTag("unhide-all").assertIsFocused()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("hidden-row-com.alpha").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("hidden-row-com.alpha").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.DirectionRight)
        press(Key.Back)
        assertEquals(1, harness.backs)
    }

    @Test
    fun `hidden row is one accessible switch announced as hidden`() {
        harness.seed("Alpha")
        harness.hideInOrder("Alpha")
        openHiddenPane(rows = 1)
        val row = compose.onNodeWithTag("hidden-row-com.alpha")
        row.assertIsOn()
        row.assert(hasClickAction())
        row.assert(hasStateDescription("Cachée"))
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        compose.onNodeWithTag("hidden-switch-com.alpha", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun `hidden rows load the real icon from PackageManager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context.packageManager).setApplicationIcon(
            "com.iconed",
            BitmapDrawable(context.resources, Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)),
        )
        harness.seed("Iconed", "Missing")
        harness.hideInOrder("Iconed", "Missing")
        openHiddenPane(rows = 2)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("app-icon-com.iconed", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("app-icon-com.missing", useUnmergedTree = true).assertDoesNotExist()
    }
}
