/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.domain.ImageBounds
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.domain.AppCatalog
import fr.sygix.sygixos.domain.GridScroll
import fr.sygix.sygixos.domain.UpNextContent
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextSourceEntry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class RecordingImageLoader : ImageLoader {
    val requests = mutableListOf<String>()
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult {
        val size = request.sizeResolver.size()
        requests += "${request.data}@${size.width}x${size.height}"
        return SuccessResult(ColorDrawable(Color.WHITE), request, DataSource.MEMORY)
    }
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class WindowedGridTest {

    @get:Rule
    val compose = createComposeRule()

    private val images = RecordingImageLoader()

    @Before
    fun fakeImages() = Coil.setImageLoader(images)

    @After
    fun resetImages() = Coil.reset()

    private fun apps(count: Int) = (0 until count).map { app("com.a%03d".format(it), "App $it") }

    private fun composedTiles(): Int = compose.onAllNodes(
        SemanticsMatcher("grid tile") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("app-tile-com.") == true },
    ).fetchSemanticsNodes().size

    private fun settle(millis: Long = 1_000) {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
    }

    private fun press(key: Key, millis: Long = 1_000) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        settle(millis)
    }

    private fun showGrid(grid: Int, hero: HeroState = heroStateOf(emptyList()), upNext: UpNextState? = null) {
        compose.mainClock.autoAdvance = false
        compose.setContent { TestHome(catalogOf(emptyList(), apps(grid)), hero, initialZone = Zone.GRID, upNext = upNext) }
        settle()
        compose.onNodeWithTag("app-tile-com.a000").assertIsFocused()
    }

    private fun assertSamePlace(expected: Span, tag: String) {
        val span = compose.span(tag)
        assertEquals(expected.top, span.top, 1f)
        assertEquals(expected.bottom, span.bottom, 1f)
    }

    private fun assertWithinMargins(tag: String) {
        val span = compose.span(tag)
        assertTrue("$tag $span", span.top >= 0f && span.bottom <= compose.screenHeight())
    }

    @Test
    fun `every row is composed up to the threshold and a bounded number above it`() {
        var size by mutableIntStateOf(GridScroll.EAGER_ROWS * Dimens.GridColumns)
        compose.setContent { key(size) { TestHome(catalogOf(emptyList(), apps(size)), heroStateOf(emptyList()), initialZone = Zone.GRID) } }
        compose.waitForIdle()
        assertEquals(size, composedTiles())
        val bound = (GridScroll.PREFETCH_ROWS + 6) * Dimens.GridColumns
        for (count in listOf(size + 1, 200, 2_000)) {
            compose.runOnIdle { size = count }
            compose.waitForIdle()
            compose.onNodeWithTag("app-tile-com.a000").assertExists()
            compose.onNodeWithTag("app-tile-com.a%03d".format(count - 1)).assertDoesNotExist()
            val composed = composedTiles()
            assertTrue("$count apps compose $composed tiles", composed in Dimens.GridColumns..bound)
        }
    }

    @Test
    fun `a burst of presses reaches rows outside the window and a quick reversal comes back`() {
        showGrid(200)
        repeat(10) { press(Key.DirectionDown, millis = 32) }
        compose.onNodeWithTag("app-tile-com.a050").assertIsFocused()
        press(Key.DirectionUp, millis = 32)
        compose.onNodeWithTag("app-tile-com.a045").assertIsFocused()
        press(Key.DirectionDown, millis = 32)
        compose.onNodeWithTag("app-tile-com.a050").assertIsFocused()
        settle()
        assertWithinMargins("app-tile-com.a050")
        compose.onNodeWithTag("app-tile-com.a000").assertDoesNotExist()
        assertTrue(composedTiles() <= (GridScroll.PREFETCH_ROWS * 2 + 6) * Dimens.GridColumns)
    }

    @Test
    fun `a deep row is restored after the hero and after the context menu`() {
        showGrid(200)
        repeat(15) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.a075").assertIsFocused()
        val before = compose.span("app-tile-com.a075")
        press(Key.Back)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.a075").assertIsFocused().assertIsDisplayed()
        assertSamePlace(before, "app-tile-com.a075")

        openMenu()
        compose.onRoot().performKeyInput { keyDown(Key.Back); keyUp(Key.Back) }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        closeMenu()
        compose.onNodeWithTag("app-tile-com.a075").assertIsFocused()
        assertSamePlace(before, "app-tile-com.a075")
    }

    @Test
    fun `up next after a large grid is reached and restored`() {
        val source = UpNextSourceEntry("com.up", "Up")
        val cards = UpNextContent.Items((1L..3L).map { UpNextItem(it, source, UpNextContentType.MOVIE, "Movie $it") })
        showGrid(GridScroll.EAGER_ROWS * Dimens.GridColumns + 1, upNext = UpNextState(cards, UpNextPosition.AFTER_APPS))
        repeat(GridScroll.EAGER_ROWS + 1) { press(Key.DirectionDown) }
        compose.onNodeWithTag("upnext-card-com.up:1").assertIsFocused()
        assertWithinMargins("upnext-card-com.up:1")
        press(Key.Back)
        press(Key.DirectionDown)
        compose.onNodeWithTag("upnext-card-com.up:1").assertIsFocused()
        assertWithinMargins("upnext-card-com.up:1")
        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.a060").assertIsFocused()
    }

    private fun openMenu() {
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        settle(Motion.LONG_PRESS_MS + 100)
        compose.onRoot().performKeyInput { keyUp(Key.DirectionCenter) }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("menu-action-pin").assertIsFocused()
    }

    private fun closeMenu() {
        settle()
        compose.onNodeWithTag("app-menu").assertDoesNotExist()
    }

    private fun startMoving() {
        openMenu()
        compose.onRoot().performKeyInput { keyDown(Key.DirectionDown); keyUp(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNodeWithTag("menu-action-move").assertIsFocused()
        compose.onRoot().performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        compose.waitForIdle()
        closeMenu()
        compose.onNodeWithTag("move-banner").assertExists()
    }

    @Test
    fun `moving an app across windows keeps it focused and cancelling restores the order`() {
        val apps = apps(200).associateBy { it.packageName }
        var order by mutableStateOf(apps.keys.toList())
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalogOf(emptyList(), order.map { apps.getValue(it) }), heroStateOf(emptyList()), initialZone = Zone.GRID,
                onMoveInGrid = { app, delta -> order = AppCatalog.move(order, app.packageName, delta) },
                onRestoreOrder = { order = it },
            )
        }
        settle()
        repeat(15) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.a075").assertIsFocused()
        startMoving()
        compose.mainClock.autoAdvance = true
        repeat(10) { press(Key.DirectionDown) }
        assertEquals(125, order.indexOf("com.a075"))
        compose.onNodeWithTag("app-tile-com.a075").assertIsFocused()
        assertWithinMargins("app-tile-com.a075")
        press(Key.Enter)
        compose.onNodeWithTag("move-banner").assertDoesNotExist()
        val confirmed = order
        startMoving()
        repeat(4) { press(Key.DirectionDown) }
        assertEquals(145, order.indexOf("com.a075"))
        press(Key.Back)
        assertEquals(confirmed, order)
        compose.onNodeWithTag("move-banner").assertDoesNotExist()
        compose.onNodeWithTag("app-tile-com.a075").assertIsFocused()
        assertWithinMargins("app-tile-com.a075")
    }

    @Test
    fun `the top shelf opens above a deep row of a large grid`() {
        showGrid(200, heroStateOf(listOf(heroItem("h", "Titre", imageUrl = "uri-100", sourcePackage = "com.a100")), validated = setOf("uri-100"), checked = setOf("uri-100")))
        repeat(20) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.a100").assertIsFocused()
        settle(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS + 200)
        val panel = compose.span("shelf-panel")
        val tile = compose.span("app-tile-com.a100")
        assertTrue("$panel $tile", panel.top >= 0f && panel.bottom <= tile.top && tile.bottom <= compose.screenHeight())
    }

    @Test
    fun `the first poster is warmed before the shelf opens and a late validation keeps the current poster`() {
        var validated by mutableStateOf(setOf("u1", "u2"))
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalogOf(emptyList(), apps(1)),
                heroStateOf(listOf("u1", "u2", "u3").map { heroItem("h-$it", "Titre", imageUrl = it, sourcePackage = "com.a000") }, validated = validated, checked = validated),
                initialZone = Zone.GRID,
            )
        }
        val zoomed = (960.dp - Dimens.ScreenMarginH * 2) * ShelfPosterZoom
        val panel = with(Density(2f)) { ImageBounds.screen(zoomed.roundToPx(), (zoomed / Dimens.ShelfAspectRatio).roundToPx()) }
        fun posters() = images.requests.filter { it.endsWith("@${panel.width}x${panel.height}") }.map { it.substringBefore("@") }
        settle(Motion.SHELF_OPEN_DELAY_MS / 2)
        compose.onNodeWithTag("shelf-panel").assertDoesNotExist()
        assertEquals(images.requests.toString(), listOf("u1"), posters())
        settle(Motion.SHELF_OPEN_DELAY_MS / 2 + Motion.SHELF_EXPAND_MS + 7_000)
        compose.onNodeWithTag("shelf-panel").assertExists()
        assertEquals(listOf("u1", "u1", "u2"), posters())
        compose.runOnIdle { validated = setOf("u1", "u2", "u3") }
        settle(2_000)
        assertEquals(listOf("u1", "u1", "u2"), posters())
        settle(6_000)
        assertEquals(listOf("u1", "u1", "u2", "u3"), posters())
    }

    @Test
    fun `the grid zone stays the haze source and spans every row of a large grid`() {
        showGrid(200)
        val grid = compose.onNodeWithTag("zone-grid").fetchSemanticsNode()
        assertTrue(grid.layoutInfo.getModifierInfo().any { it.modifier::class.java.name.contains("haze", ignoreCase = true) })
        val pitch = compose.span("app-tile-com.a005").top - compose.span("app-tile-com.a000").top
        assertTrue(grid.size.height >= pitch * (200 / Dimens.GridColumns - 1))
        assertTrue(compose.span("zone-grid").top <= 0f)
    }
}
