/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.domain.UpNextContent
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.model.*
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class UpNextHomeTest {
    @get:Rule val compose = createComposeRule()
    private val source = UpNextSourceEntry("com.source", "Source")
    private fun item(id: Long) = UpNextItem(id, source, UpNextContentType.MOVIE, "Movie $id")
    private val row = mutableStateOf<UpNextState?>(UpNextState(UpNextContent.Items((1L..7L).map(::item)), UpNextPosition.BEFORE_APPS))
    private var opened: UpNextSourceEntry? = null

    private fun home(position: UpNextPosition = UpNextPosition.BEFORE_APPS, apps: Boolean = true) {
        row.value = row.value?.copy(position = position)
        compose.setContent {
            val pm = LocalContext.current.packageManager
            val artwork = remember(pm) { AppArtworkSource(pm, Dispatchers.Unconfined) }
            CompositionLocalProvider(LocalAppArtwork provides artwork) {
                MaterialTheme {
                    LauncherHome(catalog = catalogOf(emptyList(), if (apps) listOf(app("com.app", "App"), app("com.second", "Second"), app("com.last", "Last")) else emptyList()),
                        hero = heroStateOf(emptyList()), onTogglePin = {}, glassBlur = false,
                        upNext = row.value, onOpenUpNextSource = { _, entry -> opened = entry })
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
    private fun focused(id: Long) {
        compose.onNodeWithTag("upnext-card-${source.packageName}:$id").assertIsFocused()
    }

    @Test fun `before apps navigates down up and back`() {
        home()
        focused(1)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        press(Key.DirectionUp)
        focused(1)
        press(Key.Back)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }
    @Test fun `movie placeholder preserves title without source action badge or progress`() {
        row.value = row.value!!.copy(content = UpNextContent.Items(listOf(item(1))))
        home()
        compose.onNodeWithTag("upnext-card-placeholder-com.source:1").assertExists()
        compose.onNodeWithText("Movie 1").assertExists()
        compose.onNodeWithText("Source").assertDoesNotExist()
        compose.onNodeWithTag("upnext-card-badge-com.source:1").assertDoesNotExist()
        compose.onNodeWithTag("upnext-card-progress-com.source:1").assertDoesNotExist()
    }
    @Test fun `episode exposes badge progress and episode line with absent artwork`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val icon = BitmapDrawable(context.resources, Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888))
        val episode = item(1).copy(source = source.copy(icon = icon), type = UpNextContentType.EPISODE,
            seriesTitle = "Series", season = "1", episode = "2", episodeTitle = "Episode",
            watchNextType = ProgramKind.CONTINUE, positionMillis = 50, durationMillis = 100)
        row.value = row.value!!.copy(content = UpNextContent.Items(listOf(episode)))
        home()
        compose.onNodeWithTag("upnext-card-placeholder-com.source:1").assertExists()
        compose.onNodeWithTag("upnext-card-badge-com.source:1").assertExists()
        compose.onNodeWithTag("upnext-card-progress-com.source:1").assertExists()
        compose.onNodeWithText("Series").assertExists()
        compose.onNodeWithText("S01E02 · Episode").assertExists()
        compose.onNodeWithText("Source").assertDoesNotExist()
    }
    @Test fun `empty home has no section titles and keeps empty apps message`() {
        row.value = null
        home(apps = false)
        compose.onNodeWithTag("section-title-upnext").assertDoesNotExist()
        compose.onNodeWithTag("section-title-apps").assertDoesNotExist()
        compose.onNodeWithText("Aucune app TV détectée").assertExists()
    }
    @Test fun `after apps navigates in configured order`() {
        home(UpNextPosition.AFTER_APPS)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
        press(Key.DirectionDown)
        focused(1)
        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
    }
    @Test fun `menu opens for one source and back restores card`() {
        home()
        press(Key.Menu)
        compose.onNodeWithTag("upnext-menu").assertExists()
        compose.onNodeWithTag("upnext-menu-entry-com.source").assertIsFocused()
        compose.onNodeWithTag("upnext-menu-title").assertTextEquals("Movie 1")
        compose.onNodeWithTag("upnext-menu-subtitle").assertTextEquals("Ouvrir avec…")
        press(Key.Back)
        compose.onNodeWithTag("upnext-menu").assertDoesNotExist()
        focused(1)
    }
    @Test fun `menu preserves source order and OK selects its own source`() {
        val second = UpNextSourceEntry("com.second", "Second", intentUri = "https://example.org/second")
        row.value = row.value!!.copy(content = UpNextContent.Items(listOf(item(1).copy(sources = listOf(source, second)))))
        home()
        press(Key.Menu)
        compose.onNodeWithTag("upnext-menu-entry-com.source").assertIsFocused()
        assertTrue(compose.span("upnext-menu-entry-com.source").top < compose.span("upnext-menu-entry-com.second").top)
        press(Key.DirectionDown)
        compose.onNodeWithTag("upnext-menu-entry-com.second").assertIsFocused()
        press(Key.Enter)
        assertEquals(second, opened)
        compose.onNodeWithTag("upnext-menu").assertDoesNotExist()
        focused(1)
    }
    @Test fun `horizontal scroll and return retain selected card`() {
        home()
        repeat(5) { press(Key.DirectionRight) }
        focused(6)
        val card = compose.onNodeWithTag("upnext-card-com.source:6").fetchSemanticsNode()
        assertTrue(card.positionInRoot.x >= 0)
        assertTrue(card.positionInRoot.x + card.size.width <= compose.onRoot().fetchSemanticsNode().size.width)
        press(Key.Back)
        press(Key.DirectionDown)
        focused(6)
    }
    @Test fun `removed middle card selects following card`() {
        home()
        press(Key.DirectionRight)
        focused(2)
        compose.runOnIdle { row.value = row.value!!.copy(content = UpNextContent.Items((1L..7L).filter { it != 2L }.map(::item))) }
        compose.waitForIdle()
        focused(3)
    }
    @Test fun `removed last card selects previous card`() {
        row.value = row.value!!.copy(content = UpNextContent.Items(listOf(item(1), item(2))))
        home()
        press(Key.DirectionRight)
        compose.runOnIdle { row.value = row.value!!.copy(content = UpNextContent.Items(listOf(item(1)))) }
        compose.waitForIdle()
        focused(1)
    }
    @Test fun `row removed falls back to apps before`() {
        home()
        compose.runOnIdle { row.value = null }
        compose.waitForIdle()
        compose.onNodeWithTag("section-title-upnext").assertDoesNotExist()
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
    }
    @Test fun `row removed falls back to apps after`() {
        home(UpNextPosition.AFTER_APPS)
        press(Key.DirectionDown)
        compose.runOnIdle { row.value = null }
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.last").assertIsFocused()
    }
    @Test fun `row removed without apps returns to hero`() {
        home(apps = false)
        compose.onNodeWithTag("section-title-apps").assertDoesNotExist()
        compose.runOnIdle { row.value = null }
        compose.waitForIdle()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }
    @Test fun `skeleton is not focusable and has five cards`() {
        row.value = row.value!!.copy(content = UpNextContent.Skeleton)
        home()
        compose.onAllNodesWithTag("upnext-skeleton").assertCountEquals(5)
        compose.onNodeWithTag("app-tile-com.app").assertIsFocused()
    }
    @Test fun `error is a single focusable card`() {
        row.value = row.value!!.copy(content = UpNextContent.Error)
        home()
        compose.onAllNodesWithTag("upnext-error").assertCountEquals(1)
        compose.onNodeWithTag("upnext-error").assertIsFocused()
    }
    @Test fun `titles are ordered and first title is visible`() {
        home()
        assertTrue(compose.span("section-title-upnext").top < compose.span("section-title-apps").top)
        assertTrue(compose.span("section-title-upnext").top >= 0f)
        assertFalse(compose.onNodeWithTag("section-title-upnext").fetchSemanticsNode().config.contains(SemanticsProperties.Focused))
        assertFalse(compose.onNodeWithTag("section-title-apps").fetchSemanticsNode().config.contains(SemanticsProperties.Focused))
        compose.onNodeWithTag("upnext-card-placeholder-com.source:1", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("upnext-card-progress-com.source:1", useUnmergedTree = true).assertDoesNotExist()
    }
}
