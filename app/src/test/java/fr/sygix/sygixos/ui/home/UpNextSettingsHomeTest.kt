/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.domain.UpNextController
import fr.sygix.sygixos.domain.UpNextSource
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextSourceEntry
import fr.sygix.sygixos.ui.settings.SettingsHarness
import fr.sygix.sygixos.ui.settings.waitForToggle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class UpNextSettingsHomeTest {
    @get:Rule val compose = createComposeRule()
    private val harness = SettingsHarness()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val items = listOf(
        UpNextItem(1, UpNextSourceEntry("com.alpha", "Alpha"), UpNextContentType.MOVIE, "Movie 1"),
        UpNextItem(2, UpNextSourceEntry("com.beta", "Beta"), UpNextContentType.MOVIE, "Movie 2"),
    )
    private val source = object : UpNextSource {
        override suspend fun load() = Result.success(items)
        override fun changes() = emptyFlow<Unit>()
    }

    @Before
    fun setup() {
        Coil.setImageLoader(UnreachableImageLoader())
        runBlocking {
            harness.prefs.setUpNextVisible(true)
            harness.prefs.setUpNextPosition(UpNextPosition.BEFORE_APPS)
        }
        harness.install("Alpha", "Beta")
    }

    @After
    fun tearDown() {
        scope.cancel()
        harness.clear()
        Coil.reset()
    }

    private fun home() {
        val controller = UpNextController(source, harness.repo.upNextVisible, harness.repo.upNextPosition, harness.repo.disabledSources, scope)
        controller.refresh()
        compose.setContent {
            val settings by harness.viewModel.state.collectAsState()
            val upNext by controller.state.collectAsState()
            MaterialTheme {
                LauncherHome(
                    catalog = catalogOf(emptyList(), listOf(app("com.alpha", "Alpha"), app("com.beta", "Beta"))),
                    hero = heroStateOf(emptyList()), onTogglePin = {}, glassBlur = false,
                    settings = settings, onToggleSource = harness.viewModel::toggleSource,
                    onSettingsCategory = harness.viewModel::enterCategory, homeScreenActions = harness.viewModel.homeScreenActions,
                    upNext = upNext,
                )
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("upnext-card-com.alpha:1").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun press(key: Key, tag: String = "settings-or-home") {
        val target = if (tag == "settings-or-home") {
            if (compose.onAllNodesWithTag("settings-screen").fetchSemanticsNodes().isNotEmpty()) "settings-screen" else "zone-grid"
        } else tag
        compose.onNodeWithTag(target).performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun node(tag: String): SemanticsNodeInteraction = compose.onNodeWithTag(tag)

    private fun density() = node("zone-grid").fetchSemanticsNode().layoutInfo.density.density

    private fun rect(tag: String): Rect {
        val n = node(tag).fetchSemanticsNode()
        val d = density()
        val p = n.positionInRoot
        return Rect(p.x / d, p.y / d, (p.x + n.size.width) / d, (p.y + n.size.height) / d)
    }

    private fun screenRect(tag: String): Rect {
        val n = node(tag).fetchSemanticsNode()
        val d = density()
        val p = n.positionOnScreen
        return Rect(p.x / d, p.y / d, (p.x + n.size.width) / d, (p.y + n.size.height) / d)
    }

    private fun openHomeScreenCategory() {
        press(Key.DirectionUp)
        node("settings-gear").assertIsFocused()
        press(Key.Enter)
        node("settings-category-SOURCES").assertIsFocused()
        repeat(2) { press(Key.DirectionDown) }
        node("settings-category-HOME_SCREEN").assertIsFocused()
    }

    @Test
    fun `switching a source off removes its card from the home without restart`() {
        home()
        press(Key.DirectionUp)
        press(Key.Enter)
        press(Key.DirectionRight)
        node("source-row-com.alpha").assertIsFocused()
        press(Key.Enter)
        compose.waitForToggle("source-row-com.alpha", false)
        press(Key.Back)
        node("zone-hero").assertIsFocused()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("upnext-card-com.alpha:1").fetchSemanticsNodes().isEmpty() }
        node("upnext-card-com.beta:2").assertExists()
    }

    @Test
    fun `home screen pane has its header and two rows and follows the settings page navigation`() {
        home()
        openHomeScreenCategory()
        val header = hasAnyAncestor(hasTestTag("settings-home-screen"))
        compose.onNode(header and hasText("Écran d'accueil")).assertExists()
        compose.onNode(header and hasText("Choisissez ce qu'affiche l'accueil de SygixOs.")).assertExists()
        assertTrue(rect("setting-upnext-visible").top < rect("setting-upnext-position").top)
        press(Key.DirectionRight)
        node("setting-upnext-visible").assertIsFocused()
        press(Key.DirectionUp)
        node("setting-upnext-visible").assertIsFocused()
        press(Key.DirectionRight)
        node("setting-upnext-visible").assertIsFocused()
        press(Key.DirectionDown)
        node("setting-upnext-position").assertIsFocused()
        press(Key.DirectionDown)
        node("setting-upnext-position").assertIsFocused()
        press(Key.DirectionRight)
        node("setting-upnext-position").assertIsFocused()
        press(Key.DirectionLeft)
        node("settings-category-HOME_SCREEN").assertIsFocused()
        press(Key.DirectionRight)
        node("setting-upnext-visible").assertIsFocused()
        assertEquals(true, runBlocking { harness.prefs.upNextVisible.first() })
        assertEquals(UpNextPosition.BEFORE_APPS, runBlocking { harness.prefs.upNextPosition.first() })
        press(Key.Back)
        node("settings-screen").assertDoesNotExist()
        node("zone-hero").assertIsFocused()
    }

    @Test
    fun `rows keep their rectangle with and without focus and the closed position row shows value and chevron`() {
        home()
        openHomeScreenCategory()
        val visibleRest = rect("setting-upnext-visible")
        val positionRest = rect("setting-upnext-position")
        assertTrue(visibleRest.height >= Dimens.SettingsRowHeight.value - 0.5f)
        press(Key.DirectionRight)
        assertEquals(visibleRest, rect("setting-upnext-visible"))
        press(Key.DirectionDown)
        assertEquals(positionRest, rect("setting-upnext-position"))
        node("setting-upnext-position-value").assertTextEquals("Avant les applications")
        node("setting-upnext-position-chevron").assertExists()
        assertTrue(rect("setting-upnext-position-value").right <= rect("setting-upnext-position-chevron").left)
        press(Key.DirectionUp)
        press(Key.Enter)
        compose.waitForToggle("setting-upnext-visible", false)
        press(Key.DirectionDown)
        node("setting-upnext-position").assertIsFocused()
        assertEquals(positionRest, rect("setting-upnext-position"))
    }

    @Test
    fun `position list shows both values in order with the check on the current one below the row in the pane`() {
        home()
        openHomeScreenCategory()
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        press(Key.Enter)
        val list = "setting-upnext-position-list"
        node("setting-upnext-position-BEFORE_APPS").assertIsFocused().assertIsSelected()
        node("setting-upnext-position-AFTER_APPS").assertIsNotSelected()
        assertTrue(screenRect("setting-upnext-position-BEFORE_APPS").top < screenRect("setting-upnext-position-AFTER_APPS").top)
        assertEquals(1, compose.onAllNodesWithTag("setting-upnext-position-check", useUnmergedTree = true).fetchSemanticsNodes().size)
        compose.onNode(hasTestTag("setting-upnext-position-check") and hasAnyAncestor(hasTestTag("setting-upnext-position-BEFORE_APPS")), useUnmergedTree = true).assertExists()
        val panel = screenRect(list)
        val row = screenRect("setting-upnext-position")
        val pane = screenRect("settings-home-screen")
        assertTrue("$panel below $row", panel.top >= row.bottom)
        assertTrue("$panel in $pane", panel.left >= pane.left && panel.right <= pane.right)
        press(Key.DirectionLeft, list)
        press(Key.DirectionRight, list)
        node("setting-upnext-position-BEFORE_APPS").assertIsFocused()
        press(Key.Back, list)
        node(list).assertDoesNotExist()
        node("settings-screen").assertExists()
        node("setting-upnext-position").assertIsFocused()
        assertEquals(UpNextPosition.BEFORE_APPS, runBlocking { harness.prefs.upNextPosition.first() })
    }

    @Test
    fun `position and visibility changes reposition and hide the row and its title on the home`() {
        home()
        openHomeScreenCategory()
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        press(Key.Enter)
        press(Key.DirectionDown, "setting-upnext-position-list")
        press(Key.Enter, "setting-upnext-position-list")
        node("setting-upnext-position").assertIsFocused()
        compose.waitUntil(5_000) { runBlocking { harness.prefs.upNextPosition.first() } == UpNextPosition.AFTER_APPS }
        node("setting-upnext-position-value").assertTextEquals("Après les applications")
        press(Key.Back)
        press(Key.DirectionDown)
        node("app-tile-com.alpha").assertIsFocused()
        assertTrue(rect("section-title-apps").top < rect("section-title-upnext").top)
        press(Key.Back)
        openHomeScreenCategory()
        press(Key.DirectionRight)
        press(Key.Enter)
        compose.waitForToggle("setting-upnext-visible", false)
        press(Key.DirectionDown)
        press(Key.Enter)
        node("setting-upnext-position-AFTER_APPS").assertIsFocused()
        press(Key.DirectionUp, "setting-upnext-position-list")
        press(Key.Enter, "setting-upnext-position-list")
        compose.waitUntil(5_000) { runBlocking { harness.prefs.upNextPosition.first() } == UpNextPosition.BEFORE_APPS }
        press(Key.Back)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("section-title-upnext").fetchSemanticsNodes().isEmpty() }
        node("zone-upnext").assertDoesNotExist()
        press(Key.DirectionDown)
        node("app-tile-com.alpha").assertIsFocused()
        press(Key.Back)
        openHomeScreenCategory()
        press(Key.DirectionRight)
        press(Key.Enter)
        compose.waitForToggle("setting-upnext-visible", true)
        press(Key.Back)
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("section-title-upnext").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(rect("section-title-upnext").top < rect("section-title-apps").top)
    }
}
