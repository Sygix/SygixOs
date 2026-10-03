/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Dimens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class TvFocusStyleTest {

    @get:Rule
    val compose = createComposeRule()

    private val grid = (0 until 15).map { app("com.g%02d".format(it), "Grille $it") }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun rect(tag: String): Rect {
        val coordinates = compose.onNodeWithTag(tag).fetchSemanticsNode().layoutInfo.coordinates
        return coordinates.findRootCoordinates().localBoundingBoxOf(coordinates, clipBounds = false)
    }

    private fun dp(rect: Rect): Rect {
        val density = compose.onRoot().fetchSemanticsNode().layoutInfo.density.density
        return Rect(rect.left / density, rect.top / density, rect.right / density, rect.bottom / density)
    }

    private fun art(pkg: String) = dp(rect("app-tile-art-$pkg"))

    private fun tile(pkg: String): Rect {
        val node = compose.onNodeWithTag("app-tile-$pkg").fetchSemanticsNode()
        val position = node.positionInRoot
        return dp(Rect(position.x, position.y, position.x + node.size.width, position.y + node.size.height))
    }

    private fun screen(): Rect {
        val root = compose.onRoot().fetchSemanticsNode().size
        return dp(Rect(0f, 0f, root.width.toFloat(), root.height.toFloat()))
    }

    private fun contains(outer: Rect, inner: Rect) =
        inner.left >= outer.left - 0.5f && inner.top >= outer.top - 0.5f &&
            inner.right <= outer.right + 0.5f && inner.bottom <= outer.bottom + 0.5f

    private fun showGrid() {
        compose.setContent {
            TestHome(catalog = catalogOf(dock = emptyList(), grid = grid), hero = heroStateOf(items = emptyList()), initialZone = Zone.GRID)
        }
        compose.waitForIdle()
    }

    private fun focusGridTile(): String {
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        compose.onNodeWithTag("app-tile-com.g06").assertIsFocused()
        return "com.g06"
    }

    @Test
    fun `focused grid tile zooms without touching its neighbours or leaving the screen`() {
        showGrid()
        val focused = focusGridTile()
        val lifted = art(focused)
        val layout = tile(focused)
        assertEquals(layout.width * Dimens.TileFocusScale, lifted.width, 0.5f)
        listOf("com.g01", "com.g05", "com.g07", "com.g11").forEach { neighbour ->
            assertFalse("$focused overlaps $neighbour", lifted.overlaps(art(neighbour)))
        }
        assertTrue("$lifted in ${screen()}", contains(screen(), lifted))
    }

    @Test
    fun `no tile shows the app name, focused or not`() {
        showGrid()
        focusGridTile()
        grid.forEach { app ->
            compose.onAllNodes(hasText(app.label), useUnmergedTree = true).assertCountEquals(0)
        }
    }

    @Test
    fun `focused tiles on every edge of the grid stay whole`() {
        showGrid()
        compose.onNodeWithTag("app-tile-com.g00").assertIsFocused()
        assertWhole("com.g00")
        repeat(4) { press(Key.DirectionRight) }
        compose.onNodeWithTag("app-tile-com.g04").assertIsFocused()
        assertWhole("com.g04")
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.g14").assertIsFocused()
        assertWhole("com.g14")
        repeat(4) { press(Key.DirectionLeft) }
        compose.onNodeWithTag("app-tile-com.g10").assertIsFocused()
        assertWhole("com.g10")
    }

    @Test
    fun `first and last tiles of a full dock stay whole when focused`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = (0 until 6).map { app("com.d$it", "Dock $it") }, grid = grid),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.waitForIdle()
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.d0").assertIsFocused()
        assertWhole("com.d0")
        assertTrue(contains(dp(rect("dock-glass")), art("com.d0")))
        repeat(5) { press(Key.DirectionRight) }
        compose.onNodeWithTag("app-tile-com.d5").assertIsFocused()
        assertWhole("com.d5")
        assertTrue(contains(dp(rect("dock-glass")), art("com.d5")))
    }

    private fun assertWhole(pkg: String) {
        val lifted = art(pkg)
        val visible = dp(compose.onNodeWithTag("app-tile-art-$pkg").fetchSemanticsNode().boundsInRoot)
        assertEquals(Dimens.TileFocusScale, lifted.width / tile(pkg).width, 0.01f)
        assertEquals(lifted.left, visible.left, 0.5f)
        assertEquals(lifted.top, visible.top, 0.5f)
        assertEquals(lifted.right, visible.right, 0.5f)
        assertEquals(lifted.bottom, visible.bottom, 0.5f)
        assertTrue(contains(screen(), lifted))
    }

    @Test
    fun `dock tiles keep one fixed size smaller than the grid and the dock stays centred`() {
        var count by mutableStateOf(1)
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = (0 until count).map { app("com.d$it", "Dock $it") }, grid = grid),
                hero = heroStateOf(items = emptyList()),
            )
        }
        val sizes = listOf(1, 3, 6).map { n ->
            compose.runOnIdle { count = n }
            compose.waitForIdle()
            val glass = dp(rect("dock-glass"))
            assertEquals(screen().width / 2f, (glass.left + glass.right) / 2f, 0.5f)
            (0 until n).map { tile("com.d$it").size }.distinct().single()
        }
        assertEquals(1, sizes.distinct().size)
        val dockTile = sizes.first()
        assertEquals(Dimens.DockTileWidth.value, dockTile.width, 0.5f)
        val gridTile = tile("com.g00").size
        assertTrue(dockTile.width < gridTile.width && dockTile.height < gridTile.height)
    }

    @Test
    fun `focused dock tile stays whole inside the dock glass`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = (0 until 3).map { app("com.d$it", "Dock $it") }, grid = grid),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.waitForIdle()
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        compose.onNodeWithTag("app-tile-com.d1").assertIsFocused()
        val lifted = art("com.d1")
        assertEquals(Dimens.DockTileWidth.value * Dimens.TileFocusScale, lifted.width, 0.5f)
        assertTrue(contains(dp(rect("dock-glass")), lifted))
        assertFalse(lifted.overlaps(art("com.d0")))
        assertFalse(lifted.overlaps(art("com.d2")))
    }
}
