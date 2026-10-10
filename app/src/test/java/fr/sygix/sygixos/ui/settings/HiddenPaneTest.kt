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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
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

    @After
    fun clearViewModel() = harness.clear()

    private fun send(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
    }

    private fun press(key: Key) {
        send(key)
        compose.waitForIdle()
    }

    private fun openHiddenPane(rows: Int) {
        compose.showSettings(harness)
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hiddenRow).fetchSemanticsNodes().size == rows
        }
    }

    private val hiddenRow = SemanticsMatcher("hidden row") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("hidden-row-") == true
    }

    private fun inPane(tag: String) = hasTestTag(tag) and hasAnyAncestor(hasTestTag("hidden-pane"))

    private fun top(tag: String) = compose.onNodeWithTag(tag).getBoundsInRoot().top.value

    @Test
    fun `pane lists hidden apps under unhide-all, most recently hidden first, without validation`() {
        harness.install("Alpha", "Beta", "Gamma", "Keep")
        harness.hideInOrder("Beta", "Gamma", "Alpha")
        openHiddenPane(rows = 3)
        val tags = listOf("unhide-all", "hidden-row-com.alpha", "hidden-row-com.gamma", "hidden-row-com.beta")
        tags.forEach { compose.onNode(inPane(it)).assertIsDisplayed() }
        val tops = tags.map(::top)
        assertTrue(tops.zipWithNext().all { (a, b) -> a < b })
        assertEquals(emptyList<String>(), harness.toggled)
        assertEquals(0, harness.unhideAllCalls)
    }

    @Test
    fun `right focuses the first row and up reaches unhide-all`() {
        harness.install("Alpha", "Beta", "Keep")
        harness.hideInOrder("Alpha", "Beta")
        openHiddenPane(rows = 2)
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("unhide-all").assertIsFocused()
    }

    @Test
    fun `ok unhides a row in place and a second ok hides it again`() {
        harness.install("Alpha", "Beta", "Keep")
        harness.hideInOrder("Alpha", "Beta")
        openHiddenPane(rows = 2)
        press(Key.DirectionRight)
        press(Key.Enter)
        assertEquals(listOf("com.beta"), harness.toggled)
        compose.waitForToggle("hidden-row-com.beta", on = false)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsOff().assertIsFocused()
        assertEquals(setOf("com.alpha"), harness.hiddenNow())

        press(Key.Enter)
        assertEquals(listOf("com.beta", "com.beta"), harness.toggled)
        compose.waitForToggle("hidden-row-com.beta", on = true)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsOn().assertIsFocused()
        assertEquals(setOf("com.alpha", "com.beta"), harness.hiddenNow())
        assertTrue(top("hidden-row-com.beta") < top("hidden-row-com.alpha"))
    }

    @Test
    fun `unhide-all keeps every row visible and the focus on the button`() {
        harness.install("Alpha", "Beta", "Keep")
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
        harness.install("Alpha")
        openHiddenPane(rows = 0)
        compose.onNodeWithTag("hidden-empty").assertIsDisplayed()
        compose.onNodeWithTag("unhide-all").assertDoesNotExist()
        press(Key.DirectionRight)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.Back)
        assertEquals(1, harness.backs)
    }

    @Test
    fun `edges do not loop, left returns to the category and back closes`() {
        harness.install("Alpha", "Beta", "Keep")
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
    fun `coming back from the categories keeps the rows and focuses the first one`() {
        harness.install("Alpha", "Beta", "Gamma", "Keep")
        harness.hideInOrder("Alpha", "Beta", "Gamma")
        openHiddenPane(rows = 3)
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        press(Key.Enter)
        compose.waitForToggle("hidden-row-com.beta", on = false)
        press(Key.DirectionDown)
        compose.onNodeWithTag("hidden-row-com.alpha").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.gamma").assertIsFocused()
        compose.onNodeWithTag("hidden-row-com.beta").assertIsOff()
        val tops = listOf("hidden-row-com.gamma", "hidden-row-com.beta", "hidden-row-com.alpha").map(::top)
        assertTrue(tops.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun `a long list scrolls to keep the focused row on screen`() {
        val labels = (1..16).map { "App%02d".format(it) }
        harness.install(*(labels + "Keep").toTypedArray())
        harness.hideInOrder(*labels.reversed().toTypedArray())
        compose.showSettings(harness)
        press(Key.DirectionDown)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("hidden-row-com.app01").fetchSemanticsNodes().isNotEmpty() }
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.app01").assertIsFocused().assertIsDisplayed()
        compose.onNodeWithTag("hidden-row-com.app16").assertDoesNotExist()
        repeat(labels.size - 1) { press(Key.DirectionDown) }
        compose.onNodeWithTag("hidden-row-com.app16").assertIsFocused().assertIsDisplayed()
        repeat(labels.size) { press(Key.DirectionUp) }
        compose.onNodeWithTag("unhide-all").assertIsFocused().assertIsDisplayed()
    }

    @Test
    fun `an app uninstalled under the focus moves it to the row now in its place, then to the category when the list empties`() {
        harness.install("Alpha", "Beta", "Gamma", "Keep")
        harness.hideInOrder("Alpha", "Beta", "Gamma")
        openHiddenPane(rows = 3)
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsFocused()

        harness.uninstall("Beta")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("hidden-row-com.beta").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
        compose.onNodeWithTag("hidden-row-com.alpha").assertIsFocused()

        harness.uninstall("Alpha")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("hidden-row-com.alpha").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
        compose.onNodeWithTag("hidden-row-com.gamma").assertIsFocused()

        harness.uninstall("Gamma")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("hidden-empty").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
        press(Key.Back)
        assertEquals(1, harness.backs)
    }

    @Test
    fun `an app uninstalled while unhide-all has the focus leaves the focus on the button`() {
        harness.install("Alpha", "Beta", "Keep")
        harness.hideInOrder("Alpha", "Beta")
        openHiddenPane(rows = 2)
        press(Key.DirectionRight)
        press(Key.DirectionUp)
        compose.onNodeWithTag("unhide-all").assertIsFocused()
        harness.uninstall("Beta")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("hidden-row-com.beta").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
        compose.onNodeWithTag("unhide-all").assertIsFocused()
    }

    @Test
    fun `coming back to the category never shows the previous list, even for one frame`() {
        harness.install("Alpha", "Beta", "Keep")
        harness.hideInOrder("Alpha", "Beta")
        openHiddenPane(rows = 2)
        press(Key.DirectionRight)
        press(Key.Enter)
        compose.waitForToggle("hidden-row-com.beta", on = false)
        press(Key.DirectionLeft)
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-HOME_SCREEN").assertIsFocused()
        compose.mainClock.autoAdvance = false
        send(Key.DirectionUp)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("hidden-pane").assertExists()
        compose.onNodeWithTag("hidden-row-com.beta").assertDoesNotExist()
        compose.onNodeWithTag("hidden-row-com.alpha").assertExists()
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun `hidden row is one accessible switch announced with its name as hidden`() {
        harness.install("Alpha", "Keep")
        harness.hideInOrder("Alpha")
        openHiddenPane(rows = 1)
        val row = compose.onNodeWithTag("hidden-row-com.alpha")
        row.assertIsOn()
        row.assert(hasClickAction())
        row.assert(hasText("Alpha"))
        row.assert(hasStateDescription("Cachée"))
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        compose.onNodeWithTag("hidden-switch-com.alpha", useUnmergedTree = true)
            .assertExists()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun `hidden rows load the real icon from PackageManager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context.packageManager).setApplicationIcon(
            "com.iconed",
            BitmapDrawable(context.resources, Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)),
        )
        harness.install("Iconed", "Missing", "Keep")
        harness.hideInOrder("Iconed", "Missing")
        openHiddenPane(rows = 2)
        compose.waitUntil(5_000) {
            compose.onAllNodesWithTag("app-icon-com.iconed", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("app-icon-com.missing", useUnmergedTree = true).assertDoesNotExist()
    }
}
