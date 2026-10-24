/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import fr.sygix.sygixos.domain.UpNextContentKey
import fr.sygix.sygixos.model.UpNextContentType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class HiddenContentsPaneTest {
    @get:Rule
    val compose = createComposeRule()

    private val harness = SettingsHarness()

    @After
    fun clearViewModel() = harness.clear()

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun openHidden(expected: String) {
        compose.showSettings(harness)
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(expected).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun row(key: UpNextContentKey) = "hidden-content-row-${key.serialized}"

    private fun action(key: UpNextContentKey) = "hidden-content-action-${key.serialized}"

    private fun top(tag: String) = compose.onNodeWithTag(tag).getBoundsInRoot().top.value

    private fun texts(tag: String): List<String> =
        compose.onAllNodes(hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true).fetchSemanticsNodes()
            .flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty().map { text -> text.text } }

    @Test
    fun `removed contents form a second group below the hidden apps, most recent first`() {
        harness.install("Alpha", "Beta", "Jelly", "Keep")
        harness.hideInOrder("Alpha", "Beta")
        val series = harness.removeContent("Serie", UpNextContentType.EPISODE, "com.jelly", "com.gone")
        val film = harness.removeContent("Film", UpNextContentType.MOVIE)
        openHidden(row(film))
        val order = listOf("hidden-row-com.beta", "hidden-row-com.alpha", "hidden-group-upnext", row(film), row(series))
        assertTrue(order.map(::top).zipWithNext().all { (a, b) -> a < b })
        assertTrue(texts(row(series)).contains("Série · Jelly"))
        assertTrue(texts(row(film)).contains("Film"))
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsFocused()
        compose.onNodeWithTag(action(film)).assertDoesNotExist()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag(row(film)).assertIsFocused()
        compose.onNodeWithTag(action(film)).assertExists()
        compose.onNodeWithTag(action(series)).assertDoesNotExist()
        press(Key.DirectionUp)
        compose.onNodeWithTag("hidden-row-com.alpha").assertIsFocused()
    }

    @Test
    fun `restoring a content removes its line and moves the focus to the item now in its place`() {
        harness.install("Alpha", "Keep")
        harness.hideInOrder("Alpha")
        val first = harness.removeContent("Premier")
        val second = harness.removeContent("Second")
        openHidden(row(first))
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        compose.onNodeWithTag(row(second)).assertIsFocused()
        press(Key.Enter)
        assertEquals(listOf(second), harness.restored)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(row(second)).fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
        assertEquals(listOf(first), harness.removedNow())
        compose.onNodeWithTag(row(first)).assertIsFocused()
        press(Key.Enter)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("hidden-group-upnext").fetchSemanticsNodes().isEmpty() }
        compose.waitForIdle()
        compose.onNodeWithTag("hidden-row-com.alpha").assertIsFocused()
    }

    @Test
    fun `restoring the last content of an otherwise empty pane shows the empty state and focuses the category`() {
        val only = harness.removeContent("Seul")
        openHidden(row(only))
        compose.onNodeWithTag("unhide-all").assertDoesNotExist()
        press(Key.DirectionRight)
        compose.onNodeWithTag(row(only)).assertIsFocused()
        press(Key.Enter)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("hidden-empty").fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
    }

    @Test
    fun `each group shows three lines then a show more line that expands until the category is entered again`() {
        val labels = listOf("Alpha", "Beta", "Gamma", "Delta", "Omega")
        harness.install(*(labels + "Keep").toTypedArray())
        harness.hideInOrder(*labels.toTypedArray())
        val contents = (1..4).map { harness.removeContent("Contenu $it") }
        openHidden("hidden-show-more-apps")
        assertEquals(3, compose.onAllNodes(hasTestTagPrefix("hidden-row-")).fetchSemanticsNodes().size)
        assertTrue(texts("hidden-show-more-apps").contains("Afficher les 2 autres"))
        press(Key.DirectionRight)
        repeat(3) { press(Key.DirectionDown) }
        compose.onNodeWithTag("hidden-show-more-apps").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag(row(contents[3])).assertIsFocused()
        repeat(3) { press(Key.DirectionDown) }
        compose.onNodeWithTag("hidden-show-more-upnext").assertIsFocused()
        assertTrue(texts("hidden-show-more-upnext").contains("Afficher l'autre"))
        assertEquals(3, compose.onAllNodes(hasTestTagPrefix("hidden-content-row-")).fetchSemanticsNodes().size)
        assertTrue(top(row(contents[1])) < top("hidden-show-more-upnext"))
        compose.onNodeWithTag(row(contents.first())).assertDoesNotExist()
        repeat(4) { press(Key.DirectionUp) }
        compose.onNodeWithTag("hidden-show-more-apps").assertIsFocused()
        press(Key.Enter)
        compose.onNodeWithTag("hidden-show-more-apps").assertDoesNotExist()
        assertEquals(5, compose.onAllNodes(hasTestTagPrefix("hidden-row-")).fetchSemanticsNodes().size)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsFocused()
        press(Key.DirectionLeft)
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("hidden-show-more-apps").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(3, compose.onAllNodes(hasTestTagPrefix("hidden-row-")).fetchSemanticsNodes().size)
        press(Key.DirectionRight)
        press(Key.DirectionUp)
        compose.onNodeWithTag("unhide-all").assertIsFocused()
        press(Key.Enter)
        assertEquals(1, harness.unhideAllCalls)
        compose.waitForToggle("hidden-row-com.omega", on = false)
        assertEquals(4, harness.removedNow().size)
    }

    @Test
    fun `contents alone show neither unhide-all nor app rows and right focuses the content`() {
        val only = harness.removeContent("Seul", UpNextContentType.MOVIE)
        openHidden(row(only))
        compose.onNodeWithTag("hidden-group-upnext").assertExists()
        assertEquals(0, compose.onAllNodes(hasTestTagPrefix("hidden-row-")).fetchSemanticsNodes().size)
        press(Key.DirectionRight)
        compose.onNodeWithTag(row(only)).assertIsFocused()
    }

    private fun hasTestTagPrefix(prefix: String) = androidx.compose.ui.test.SemanticsMatcher("tag $prefix") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith(prefix) == true
    }
}
