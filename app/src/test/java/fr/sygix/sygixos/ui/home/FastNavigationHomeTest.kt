/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performKeyPress
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.FastNavigationHost
import fr.sygix.sygixos.core.designsystem.Motion
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

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class FastNavigationHomeTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun offlineImages() = Coil.setImageLoader(UnreachableImageLoader())

    @After
    fun resetImages() = Coil.reset()

    private val apps = (0 until 50).map { app("com.a%02d".format(it), "App $it") }
    private val withPosters = listOf("com.a05", "com.a10", "com.a25")
    private val bottomMargin = 508f
    private var time = 10_000L

    private fun showGrid(upNext: UpNextState? = null, zone: Zone = Zone.GRID) {
        compose.setContent {
            FastNavigationHost {
                TestHome(
                    catalog = catalogOf(dock = emptyList(), grid = apps),
                    hero = heroStateOf(
                        items = withPosters.map { heroItem("h-$it", "Titre", imageUrl = "uri-$it", sourcePackage = it) },
                        validated = withPosters.map { "uri-$it" }.toSet(),
                    ),
                    initialZone = zone,
                    upNext = upNext,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun send(key: Key, action: Int, down: Long, repeat: Int) {
        compose.onRoot().performKeyPress(KeyEvent(NativeKeyEvent(down, time, action, key.nativeKeyCode, repeat)))
    }

    private fun burst(key: Key, count: Int) {
        compose.mainClock.autoAdvance = false
        time += 5_000
        val down = time
        repeat(count) { index ->
            send(key, NativeKeyEvent.ACTION_DOWN, down, index)
            compose.mainClock.advanceTimeBy(REPEAT_MS)
            time += REPEAT_MS
        }
        send(key, NativeKeyEvent.ACTION_UP, down, 0)
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeByFrame()
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).getBoundsInRoot()

    private fun width(tag: String) = bounds(tag).let { it.right - it.left }.value

    @Test
    fun `a held key in the grid ends on the right tile, lifted, with the page already at its target`() {
        showGrid()
        compose.onNodeWithTag("app-tile-com.a00").assertIsFocused()
        burst(Key.DirectionDown, 8)
        compose.onNodeWithTag("app-tile-com.a40").assertIsFocused()
        assertEquals(bottomMargin, bounds("app-tile-com.a40").bottom.value, 1f)
        assertEquals(width("app-tile-art-com.a41") * Dimens.TileFocusScale, width("app-tile-art-com.a40"), 1f)
        assertEquals(width("app-tile-art-com.a41"), width("app-tile-art-com.a35"), 1f)
    }

    @Test
    fun `a held key in Up Next ends on the right card, fully visible, without waiting for the scroll`() {
        val source = UpNextSourceEntry("com.app", "App")
        val cards = UpNextContent.Items((1L..12L).map { UpNextItem(it, source, UpNextContentType.MOVIE, "Movie $it") })
        showGrid(UpNextState(cards, UpNextPosition.BEFORE_APPS), zone = Zone.HERO)
        press(Key.DirectionDown)
        compose.onNodeWithTag("upnext-card-com.app:1").assertIsFocused()
        burst(Key.DirectionRight, 8)
        compose.onNodeWithTag("upnext-card-com.app:9").assertIsFocused()
        val card = bounds("upnext-card-com.app:9")
        assertEquals(width("upnext-card-com.app:9") * Dimens.TileFocusScale, width("upnext-card-art-com.app:9"), 1f)
        assertEquals(width("upnext-card-com.app:8"), width("upnext-card-art-com.app:8"), 1f)
        val screen = compose.onRoot().getBoundsInRoot()
        assertTrue("$card", card.left >= screen.left && card.right <= screen.right)
        compose.mainClock.advanceTimeBy(Motion.FOCUS_MS * 2L)
        compose.waitForIdle()
        compose.onNodeWithTag("upnext-card-com.app:9").assertIsFocused()
        assertEquals(card.left.value, bounds("upnext-card-com.app:9").left.value, 0.5f)
    }

    private companion object {
        const val REPEAT_MS = 50L
    }
}
