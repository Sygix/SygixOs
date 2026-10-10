/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.domain.UpNextContent
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextSourceEntry
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class UpNextCardsTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val icon = BitmapDrawable(context.resources, Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888))
    private val source = UpNextSourceEntry("com.app", "Source App")
    private fun movie(id: Long) = UpNextItem(id, source, UpNextContentType.MOVIE, "Movie $id")
    private val row = mutableStateOf<UpNextState?>(null)
    private var retries = 0
    private var chosen: UpNextSourceEntry? = null
    private val cardWidth = (960f - Dimens.ScreenMarginH.value * 2 - Dimens.GridSpacing.value * 3) / 4

    @Before fun offlineImages() = Coil.setImageLoader(UnreachableImageLoader())

    @After fun resetImages() = Coil.reset()

    private fun home(items: UpNextContent, position: UpNextPosition = UpNextPosition.BEFORE_APPS) {
        row.value = UpNextState(items, position)
        compose.setContent {
            val pm = LocalContext.current.packageManager
            val artwork = remember(pm) { AppArtworkSource(pm, Dispatchers.Unconfined) }
            CompositionLocalProvider(LocalAppArtwork provides artwork) {
                MaterialTheme {
                    LauncherHome(
                        catalog = catalogOf(emptyList(), listOf(app("com.app", "Source App"), app("com.other", "Other"))),
                        hero = heroStateOf(emptyList()), onTogglePin = {}, glassBlur = false,
                        upNext = row.value, onRetryUpNext = { retries++ }, onOpenUpNextSource = { _, entry -> chosen = entry },
                    )
                }
            }
        }
        compose.waitForIdle()
        press(Key.DirectionDown)
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun longPress() {
        compose.mainClock.autoAdvance = false
        compose.onRoot().performKeyInput { keyDown(Key.Enter) }
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS + 100)
        compose.onRoot().performKeyInput { keyUp(Key.Enter) }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    private fun density() = compose.onRoot().fetchSemanticsNode().layoutInfo.density.density

    private fun rect(tag: String): Rect {
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        val d = density()
        val p = node.positionInRoot
        return Rect(p.x / d, p.y / d, (p.x + node.size.width) / d, (p.y + node.size.height) / d)
    }

    private fun lifted(tag: String): Rect {
        val coordinates = compose.onNodeWithTag(tag).fetchSemanticsNode().layoutInfo.coordinates
        val box = coordinates.findRootCoordinates().localBoundingBoxOf(coordinates, clipBounds = false)
        val d = density()
        return Rect(box.left / d, box.top / d, box.right / d, box.bottom / d)
    }

    private fun texts(tag: String): List<String> =
        compose.onAllNodes(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Text), useUnmergedTree = true)
            .fetchSemanticsNodes().flatMap { node -> node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } }

    @Test
    fun `four whole cards of a quarter of the content width and a fifth crossing the right edge`() {
        home(UpNextContent.Items((1L..6L).map(::movie)))
        val cards = (1L..5L).map { rect("upnext-card-com.app:$it") }
        cards.forEach { card ->
            assertEquals(cardWidth, card.width, 0.5f)
            assertEquals(card.width * 9f / 16f, card.height, 0.5f)
        }
        assertEquals(Dimens.ScreenMarginH.value, cards.first().left, 0.5f)
        cards.zipWithNext().forEach { (a, b) -> assertEquals(Dimens.GridSpacing.value, b.left - a.right, 0.5f) }
        cards.take(4).forEach { assertTrue(it.right <= 960f + 0.5f) }
        assertTrue(cards[4].left < 960f && cards[4].right > 960f)
        repeat(4) { press(Key.DirectionRight) }
        compose.onNodeWithTag("upnext-card-com.app:5").assertIsFocused()
        val fifth = rect("upnext-card-com.app:5")
        assertTrue(fifth.left >= 0f && fifth.right <= 960f + 0.5f)
    }

    @Test
    fun `episode shows two text nodes movie one and no card shows an action or app name`() {
        val episode = movie(1).copy(source = source.copy(icon = icon), type = UpNextContentType.EPISODE, seriesTitle = "Series",
            season = "1", episode = "2", episodeTitle = "Pilot", watchNextType = ProgramKind.CONTINUE, positionMillis = 50, durationMillis = 100)
        home(UpNextContent.Items(listOf(episode, movie(2))))
        assertEquals(listOf("Series", "S01E02 · Pilot"), texts("upnext-card-com.app:1"))
        assertEquals(listOf("Movie 2"), texts("upnext-card-com.app:2"))
        compose.runOnIdle {
            row.value = UpNextState(UpNextContent.Items(listOf(episode.copy(title = "Pilot", seriesTitle = null, titleFromEpisode = true))), UpNextPosition.BEFORE_APPS)
        }
        compose.waitForIdle()
        assertEquals(listOf("Pilot"), texts("upnext-card-com.app:1"))
        compose.onNodeWithTag("upnext-card-badge-com.app:1", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("upnext-card-badge-com.app:2", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `progress bar exists only for continue items that have a progress`() {
        val withProgress = movie(1).copy(watchNextType = ProgramKind.CONTINUE, positionMillis = 50, durationMillis = 100)
        home(UpNextContent.Items(listOf(withProgress, movie(2).copy(watchNextType = ProgramKind.CONTINUE),
            movie(3).copy(watchNextType = ProgramKind.NEXT, positionMillis = 50, durationMillis = 100))))
        compose.onNodeWithTag("upnext-card-progress-com.app:1", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("upnext-card-progress-com.app:2", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("upnext-card-progress-com.app:3", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `focused card has exactly the transform of a focused app tile of the same size`() {
        val cardFocus = FocusRequester()
        val tileFocus = FocusRequester()
        val target = mutableStateOf(cardFocus)
        compose.setContent {
            val pm = LocalContext.current.packageManager
            val artwork = remember(pm) { AppArtworkSource(pm, Dispatchers.Unconfined) }
            CompositionLocalProvider(LocalAppArtwork provides artwork) {
                Row {
                    UpNextCard(movie(1), Modifier.width(198.dp), focusRequester = cardFocus)
                    AppTile(app("com.tile", "Tile"), focusEnabled = true, onClick = {}, onLongClick = {}, focusRequester = tileFocus, modifier = Modifier.width(198.dp))
                }
            }
            LaunchedEffect(target.value) { withFrameNanos { }; target.value.tryRequestFocus() }
        }
        compose.waitForIdle()
        val card = rect("upnext-card-com.app:1")
        val cardLift = lifted("upnext-card-art-com.app:1")
        compose.runOnIdle { target.value = tileFocus }
        compose.waitForIdle()
        val tile = rect("app-tile-com.tile")
        val tileLift = lifted("app-tile-art-com.tile")
        assertEquals(card.width, tile.width, 0.5f)
        assertEquals(card.height, tile.height, 0.5f)
        assertEquals(card.width * Dimens.TileFocusScale, cardLift.width, 0.5f)
        assertEquals(tileLift.width, cardLift.width, 0.5f)
        assertEquals(tileLift.height, cardLift.height, 0.5f)
        assertEquals(tileLift.left - tile.left, cardLift.left - card.left, 0.5f)
        assertEquals(tileLift.top - tile.top, cardLift.top - card.top, 0.5f)
    }

    private fun assertErrorCard(position: UpNextPosition) {
        home(UpNextContent.Error, position)
        if (position == UpNextPosition.AFTER_APPS) press(Key.DirectionDown)
        compose.onAllNodesWithTag("upnext-error").assertCountEquals(1)
        compose.onNodeWithTag("upnext-error").assertIsFocused()
        assertEquals(listOf("Impossible de charger", "OK pour réessayer"), texts("upnext-error"))
        val card = rect("upnext-error")
        assertEquals(cardWidth, card.width, 0.5f)
        assertEquals(cardWidth * 9f / 16f, card.height, 0.5f)
        assertTrue(rect("section-title-upnext").bottom <= card.top + 0.5f)
        press(Key.Enter)
        assertEquals(1, retries)
    }

    @Test
    fun `error card before the apps is a single focusable card under its title and OK retries`() = assertErrorCard(UpNextPosition.BEFORE_APPS)

    @Test
    fun `error card after the apps is a single focusable card under its title and OK retries`() = assertErrorCard(UpNextPosition.AFTER_APPS)

    @Test
    fun `skeleton shows five unfocusable cards of card size`() {
        home(UpNextContent.Skeleton)
        val nodes = compose.onAllNodesWithTag("upnext-skeleton").fetchSemanticsNodes()
        assertEquals(5, nodes.size)
        nodes.forEach { node ->
            assertEquals(cardWidth, node.size.width / density(), 0.5f)
            assertFalse(node.config.contains(SemanticsProperties.Focused))
        }
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
    }

    @Test
    fun `long press opens the menu with a single source thumbnail header and no move action`() {
        home(UpNextContent.Items(listOf(movie(1))))
        compose.onNodeWithTag("upnext-card-com.app:1").assertIsFocused()
        longPress()
        compose.onNodeWithTag("upnext-menu").assertExists()
        compose.onNodeWithTag("upnext-menu-entry-com.app").assertIsFocused()
        compose.onNodeWithTag("upnext-menu-thumbnail", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("upnext-menu-title", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("upnext-menu-subtitle", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("menu-action-move").assertDoesNotExist()
        compose.onAllNodes(hasText("Déplacer")).assertCountEquals(0)
        press(Key.Back)
        compose.onNodeWithTag("upnext-menu").assertDoesNotExist()
        compose.onNodeWithTag("upnext-card-com.app:1").assertIsFocused()
    }

    @Test
    fun `long press with several sources lists them in order without wrapping and OK opens the chosen one`() {
        val second = UpNextSourceEntry("com.second", "Second")
        val third = UpNextSourceEntry("com.third", "Third")
        home(UpNextContent.Items(listOf(movie(1).copy(sources = listOf(source, second, third)))))
        longPress()
        val entries = listOf("com.app", "com.second", "com.third").map { "upnext-menu-entry-$it" }
        compose.onNodeWithTag(entries[0]).assertIsFocused()
        entries.zipWithNext().forEach { (a, b) -> assertTrue(rect(a).top < rect(b).top) }
        press(Key.DirectionUp)
        compose.onNodeWithTag(entries[0]).assertIsFocused()
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag(entries[2]).assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag(entries[2]).assertIsFocused()
        press(Key.Enter)
        assertEquals(third, chosen)
        compose.onNodeWithTag("upnext-menu").assertDoesNotExist()
        compose.onNodeWithTag("upnext-card-com.app:1").assertIsFocused()
    }

    @Test
    fun `horizontal moves scroll just enough in both directions without snapping`() {
        home(UpNextContent.Items((1L..7L).map(::movie)))
        val end = 960f - cardWidth * (Dimens.TileFocusScale - 1f) / 2f
        repeat(4) { press(Key.DirectionRight) }
        compose.onNodeWithTag("upnext-card-com.app:5").assertIsFocused()
        assertTrue(lifted("upnext-card-art-com.app:5").right <= 960f + 0.5f)
        press(Key.DirectionRight)
        compose.onNodeWithTag("upnext-card-com.app:6").assertIsFocused()
        assertEquals(end, rect("upnext-card-com.app:6").right, 1f)
        assertEquals(end - cardWidth - Dimens.GridSpacing.value, rect("upnext-card-com.app:5").right, 1f)
        assertTrue(lifted("upnext-card-art-com.app:6").right <= 960f + 0.5f)
        press(Key.DirectionRight)
        compose.onNodeWithTag("upnext-card-com.app:7").assertIsFocused()
        assertEquals(end, rect("upnext-card-com.app:7").right, 1f)
        assertTrue(lifted("upnext-card-art-com.app:7").right <= 960f + 0.5f)
        assertTrue(lifted("upnext-card-art-com.app:7").left >= 0f)
        repeat(3) { press(Key.DirectionLeft) }
        compose.onNodeWithTag("upnext-card-com.app:4").assertIsFocused()
        assertEquals(end, rect("upnext-card-com.app:7").right, 1f)
        press(Key.DirectionLeft)
        compose.onNodeWithTag("upnext-card-com.app:3").assertIsFocused()
        assertEquals(Dimens.ScreenMarginH.value, rect("upnext-card-com.app:3").left, 1f)
        repeat(2) { press(Key.DirectionLeft) }
        compose.onNodeWithTag("upnext-card-com.app:1").assertIsFocused()
        assertEquals(Dimens.ScreenMarginH.value, rect("upnext-card-com.app:1").left, 1f)
        assertTrue(lifted("upnext-card-art-com.app:1").left >= 0f)
    }

    @Test
    fun `left and right stay on the error card`() {
        home(UpNextContent.Error)
        compose.onNodeWithTag("upnext-error").assertIsFocused()
        press(Key.DirectionRight)
        compose.onNodeWithTag("upnext-error").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("upnext-error").assertIsFocused()
    }

    private fun inMenu(tag: String): Rect {
        val node = compose.onNode(hasTestTag(tag) and hasAnyAncestor(hasTestTag("upnext-menu-thumbnail")), useUnmergedTree = true).fetchSemanticsNode()
        val d = density()
        val p = node.positionInRoot
        return Rect(p.x / d, p.y / d, (p.x + node.size.width) / d, (p.y + node.size.height) / d)
    }

    @Test
    fun `menu thumbnail placeholder is centred horizontally in the upper half like the card`() {
        home(UpNextContent.Items(listOf(movie(1))))
        val cardIcon = lifted("upnext-card-placeholder-icon-com.app:1")
        val card = lifted("upnext-card-art-com.app:1")
        assertEquals(card.center.x, cardIcon.center.x, 0.5f)
        assertEquals(card.top + card.height / 4f, cardIcon.center.y, 0.5f)
        assertEquals(Dimens.UpNextPlaceholderIcon.value * card.width / rect("upnext-card-com.app:1").width, cardIcon.width, 0.5f)
        longPress()
        val thumbnail = compose.onNodeWithTag("upnext-menu-thumbnail", useUnmergedTree = true).fetchSemanticsNode().let { node ->
            val d = density()
            Rect(node.positionInRoot.x / d, node.positionInRoot.y / d, (node.positionInRoot.x + node.size.width) / d, (node.positionInRoot.y + node.size.height) / d)
        }
        val icon = inMenu("upnext-card-placeholder-icon-com.app:1")
        assertEquals(thumbnail.center.x, icon.center.x, 0.5f)
        assertEquals(thumbnail.top + thumbnail.height / 4f, icon.center.y, 0.5f)
        assertEquals(icon.width, icon.height, 0.5f)
        assertTrue(icon.height <= thumbnail.height / 2f + 0.5f)
    }
}
