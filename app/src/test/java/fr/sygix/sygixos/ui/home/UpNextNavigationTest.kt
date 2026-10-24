/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.domain.UpNextContent
import fr.sygix.sygixos.domain.UpNextReturn
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextSourceEntry
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class UpNextNavigationTest {
    @get:Rule val compose = createComposeRule()
    private val source = UpNextSourceEntry("com.app", "App")
    private fun item(id: Long) = UpNextItem(id, source, UpNextContentType.MOVIE, "Movie $id")
    private val row = mutableStateOf<UpNextState?>(null)
    private val opened = mutableListOf<UpNextItem>()
    private val moves = mutableListOf<Int>()
    private val dock = listOf(app("com.d1", "Dock 1"), app("com.d2", "Dock 2"))
    private val apps = listOf(app("com.app", "App"), app("com.second", "Second"), app("com.last", "Last"))

    @Before fun offlineImages() = Coil.setImageLoader(UnreachableImageLoader())

    @After fun resetImages() = Coil.reset()

    private fun cards(vararg ids: Long) = UpNextContent.Items(ids.map(::item))

    private fun home(
        content: UpNextContent? = cards(1, 2, 3, 4, 5, 6),
        position: UpNextPosition = UpNextPosition.BEFORE_APPS,
        dockApps: List<TvApp> = dock,
        gridApps: List<TvApp> = apps,
        hero: HeroState = heroStateOf(emptyList()),
    ) {
        row.value = content?.let { UpNextState(it, position) }
        compose.setContent {
            val pm = LocalContext.current.packageManager
            val artwork = remember(pm) { AppArtworkSource(pm, Dispatchers.Unconfined) }
            CompositionLocalProvider(LocalAppArtwork provides artwork) {
                MaterialTheme {
                    LauncherHome(
                        catalog = catalogOf(dockApps, gridApps), hero = hero, onTogglePin = {}, glassBlur = false,
                        upNext = row.value, onOpenUpNext = { opened += it },
                        onMoveInGrid = { _, delta -> moves += delta },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun longPress() {
        compose.mainClock.autoAdvance = false
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS + 100)
        compose.onRoot().performKeyInput { keyUp(Key.DirectionCenter) }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    private fun card(id: Long) = "upnext-card-com.app:$id"

    private fun density() = compose.onRoot().fetchSemanticsNode().layoutInfo.density.density

    private fun rect(tag: String): Rect {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        val d = density()
        val p = node.positionInRoot
        return Rect(p.x / d, p.y / d, (p.x + node.size.width) / d, (p.y + node.size.height) / d)
    }

    private fun screen(): Rect {
        val size = compose.onRoot().fetchSemanticsNode().size
        return Rect(0f, 0f, size.width / density(), size.height / density())
    }

    @Test
    fun `with a dock descent goes hero dock row apps and ascent comes back the same way`() {
        home()
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.d1").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag(card(1)).assertIsFocused()
        assertTrue(rect("section-title-upnext").bottom <= rect("zone-upnext").top + 0.5f)
        assertEquals(Dimens.GridTopMargin.value, rect("section-title-upnext").top, 1f)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        assertTrue(rect("section-title-apps").bottom <= rect("app-tile-com.app").top + 0.5f)
        press(Key.DirectionUp)
        compose.onNodeWithTag(card(1)).assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.d1").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `hidden or disabled row is skipped with its title from the dock in both directions`() {
        home(content = null)
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        compose.onNodeWithTag("section-title-upnext").assertDoesNotExist()
        compose.onNodeWithTag("zone-upnext").assertDoesNotExist()
        assertEquals(Dimens.GridTopMargin.value, rect("section-title-apps").top, 1f)
        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.d1").assertIsFocused()
    }

    @Test
    fun `apps title is whole between the margins on arrival on the first app row`() {
        home(gridApps = (1..20).map { app("com.g%02d".format(it), "G$it") })
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.g01").assertIsFocused()
        val title = rect("section-title-apps")
        assertTrue(title.top >= Dimens.GridTopMargin.value - 1f)
        assertTrue(rect("app-tile-com.g01").bottom <= screen().bottom - Dimens.GridRowSpacing.value + 1f)
        press(Key.DirectionUp)
        compose.onNodeWithTag(card(1)).assertIsFocused()
        assertTrue(rect("section-title-upnext").top >= Dimens.GridTopMargin.value - 1f)
    }

    @Test
    fun `card and deep tile are restored at the same screen position after leaving the grid`() {
        home(gridApps = (1..20).map { app("com.g%02d".format(it), "G$it") })
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        repeat(2) { press(Key.DirectionRight) }
        compose.onNodeWithTag(card(3)).assertIsFocused()
        val cardBefore = rect(card(3))
        press(Key.Back)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag(card(3)).assertIsFocused()
        assertEquals(cardBefore.top, rect(card(3)).top, 1f)
        assertEquals(cardBefore.left, rect(card(3)).left, 1f)
        repeat(2) { press(Key.DirectionLeft) }
        compose.onNodeWithTag(card(1)).assertIsFocused()
        repeat(4) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.g16").assertIsFocused()
        val tileBefore = rect("app-tile-com.g16")
        press(Key.Back)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.g16").assertIsFocused()
        assertEquals(tileBefore.top, rect("app-tile-com.g16").top, 1f)
        assertEquals(tileBefore.left, rect("app-tile-com.g16").left, 1f)
    }

    @Test
    fun `returning from an app refocuses the origin card by key after a reorder then its neighbour once gone`() {
        home()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        repeat(2) { press(Key.DirectionRight) }
        press(Key.Enter)
        assertEquals(listOf(3L), opened.map { it.id })
        val position = rect("zone-upnext").top
        compose.runOnIdle { row.value = UpNextState(cards(3, 1, 2, 4, 5, 6), UpNextPosition.BEFORE_APPS) }
        compose.waitForIdle()
        compose.onNodeWithTag(card(3)).assertIsFocused()
        assertEquals(position, rect("zone-upnext").top, 1f)
        compose.runOnIdle { row.value = UpNextState(cards(1, 2, 4, 5, 6), UpNextPosition.BEFORE_APPS) }
        compose.waitForIdle()
        compose.onNodeWithTag(card(1)).assertIsFocused()
        assertEquals(position, rect("zone-upnext").top, 1f)
    }

    private fun openCard(position: Int, content: UpNextContent): Float {
        home(content = content)
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        repeat(position - 1) { press(Key.DirectionRight) }
        press(Key.Enter)
        return rect("zone-upnext").top
    }

    private fun returnWith(content: UpNextContent, focus: UpNextReturn?) {
        compose.runOnIdle { row.value = UpNextState(content, UpNextPosition.BEFORE_APPS, focus) }
        compose.waitForIdle()
    }

    @Test
    fun `returning from the fourth card puts its content first with the focus at the start of the row`() {
        val position = openCard(4, cards(1, 2, 3, 4, 5, 6))
        assertEquals(listOf(4L), opened.map { it.id })
        returnWith(cards(4, 1, 2, 3, 5, 6), UpNextReturn(card(4).removePrefix("upnext-card-"), 1))
        compose.onNodeWithTag(card(4)).assertIsFocused()
        assertEquals(Dimens.ScreenMarginH.value, rect(card(4)).left, 1f)
        assertEquals(position, rect("zone-upnext").top, 1f)
    }

    @Test
    fun `returning from a scrolled card brings the row back to its start`() {
        val position = openCard(7, cards(1, 2, 3, 4, 5, 6, 7, 8))
        assertTrue(compose.onAllNodesWithTag(card(1)).fetchSemanticsNodes().isEmpty() || rect(card(1)).left < Dimens.ScreenMarginH.value)
        returnWith(cards(7, 1, 2, 3, 4, 5, 6, 8), UpNextReturn(card(7).removePrefix("upnext-card-"), 2))
        compose.onNodeWithTag(card(7)).assertIsFocused()
        assertEquals(Dimens.ScreenMarginH.value, rect(card(7)).left, 1f)
        assertEquals(position, rect("zone-upnext").top, 1f)
    }

    @Test
    fun `returning after the app published another episode focuses the series under its new card key`() {
        val position = openCard(4, cards(1, 2, 3, 4, 5, 6))
        returnWith(cards(9, 1, 2, 3, 5, 6), UpNextReturn(card(9).removePrefix("upnext-card-"), 3))
        compose.onNodeWithTag(card(9)).assertIsFocused()
        assertEquals(Dimens.ScreenMarginH.value, rect(card(9)).left, 1f)
        assertEquals(position, rect("zone-upnext").top, 1f)
    }

    @Test
    fun `returning after the opened content disappeared focuses the card now at its position`() {
        val position = openCard(4, cards(1, 2, 3, 4, 5, 6))
        returnWith(cards(1, 2, 3, 5, 6), null)
        compose.onNodeWithTag(card(5)).assertIsFocused()
        assertEquals(position, rect("zone-upnext").top, 1f)
    }

    @Test
    fun `returning to the launcher with the origin card gone at the end focuses the previous card`() {
        home(content = cards(1, 2, 3))
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        repeat(2) { press(Key.DirectionRight) }
        press(Key.Enter)
        press(Key.Back)
        compose.runOnIdle { row.value = UpNextState(cards(1, 2), UpNextPosition.BEFORE_APPS) }
        compose.waitForIdle()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag(card(2)).assertIsFocused()
    }

    @Test
    fun `no Top Shelf panel opens on the row while an app tile of the same package opens it`() {
        val posters = listOf(heroItem("h1", "Titre", imageUrl = "uri-1", sourcePackage = "com.app"))
        home(hero = heroStateOf(posters, validated = setOf("uri-1")), dockApps = emptyList())
        press(Key.DirectionDown)
        compose.onNodeWithTag(card(1)).assertIsFocused()
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS + 500)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-panel").assertDoesNotExist()
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS + 500)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-panel").assertExists()
    }

    @Test
    fun `after the apps down from the dock reaches apps first and down from the row does nothing`() {
        home(position = UpNextPosition.AFTER_APPS)
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        assertTrue(rect("section-title-apps").top < rect("section-title-upnext").top)
        press(Key.DirectionDown)
        compose.onNodeWithTag(card(1)).assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag(card(1)).assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
    }

    @Test
    fun `no app message is centred in the space left below the row and alone without it`() {
        home(gridApps = emptyList(), dockApps = emptyList())
        compose.onNodeWithTag("section-title-apps").assertDoesNotExist()
        press(Key.DirectionDown)
        compose.onNodeWithTag(card(1)).assertIsFocused()
        val message = rect("no-tv-apps")
        val spaceTop = rect("zone-upnext").bottom + Dimens.GridRowSpacing.value
        val spaceBottom = screen().bottom - Dimens.GridTopMargin.value
        assertEquals((spaceTop + spaceBottom) / 2f, message.center.y, 1f)
        assertEquals(screen().center.x, message.center.x, 1f)
    }

    @Test
    fun `no app message is centred on the grid screen without the row`() {
        home(content = null, gridApps = emptyList(), dockApps = emptyList())
        compose.onNodeWithTag("section-title-apps").assertDoesNotExist()
        val zone = rect("zone-grid")
        assertEquals(zone.top + screen().height / 2f, rect("no-tv-apps").center.y, 1f)
    }

    @Test
    fun `move mode only moves app tiles and never focuses the row`() {
        home()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        longPress()
        press(Key.DirectionDown)
        compose.onNodeWithTag("menu-action-move").assertIsFocused()
        press(Key.Enter)
        compose.onNodeWithTag("move-banner").assertExists()
        press(Key.DirectionUp)
        assertEquals(listOf(-Dimens.GridColumns), moves)
        compose.onNodeWithTag(card(1)).assertIsNotFocused()
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
    }

    private fun emptiedWhileAway(position: UpNextPosition, expected: String) {
        home(position = position)
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        if (position == UpNextPosition.AFTER_APPS) press(Key.DirectionDown)
        compose.onNodeWithTag(card(1)).assertIsFocused()
        press(Key.Back)
        compose.runOnIdle { row.value = null }
        compose.waitForIdle()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("section-title-upnext").assertDoesNotExist()
        compose.onNodeWithTag(expected).assertIsFocused()
    }

    @Test
    fun `row emptied while away before the apps hands focus to the first app on return`() = emptiedWhileAway(UpNextPosition.BEFORE_APPS, "app-tile-com.app")

    @Test
    fun `row emptied while away after the apps hands focus to the last app on return`() = emptiedWhileAway(UpNextPosition.AFTER_APPS, "app-tile-com.last")

    @Test
    fun `up from the first app row goes to the dock once the row above has been emptied`() {
        home()
        repeat(3) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        compose.runOnIdle { row.value = null }
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.d1").assertIsFocused()
    }

    @Test
    fun `up from the first app row reaches a row that appeared above it`() {
        home(content = null)
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        compose.runOnIdle { row.value = UpNextState(cards(1, 2, 3), UpNextPosition.BEFORE_APPS) }
        compose.waitForIdle()
        press(Key.DirectionUp)
        compose.onNodeWithTag(card(1)).assertIsFocused()
    }

    @Test
    fun `up from the first app row under a loading skeleton goes to the dock`() {
        home(content = UpNextContent.Skeleton)
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.d1").assertIsFocused()
    }

    @Test
    fun `up from any column of the apps returns to the last visited card of a scrolled row`() {
        home(content = cards(1, 2, 3, 4, 5, 6, 7))
        repeat(2) { press(Key.DirectionDown) }
        repeat(5) { press(Key.DirectionRight) }
        compose.onNodeWithTag(card(6)).assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.last").assertIsFocused()
        repeat(2) { press(Key.DirectionLeft) }
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag(card(6)).assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.last").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("app-tile-com.second").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag(card(6)).assertIsFocused()
    }
}
