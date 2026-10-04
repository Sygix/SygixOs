/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.data.AppIconCache
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.ui.hero.LocalSourceIcons
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HeroInfoTest {

    @get:Rule
    val compose = createComposeRule()

    private val icons = AppIconCache { pkg -> if (pkg == WITH_ICON) Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888) else null }

    private var shown by mutableStateOf<HeroItem?>(null)

    private fun show(item: HeroItem) {
        if (shown == null) {
            shown = item
            compose.setContent {
                CompositionLocalProvider(LocalSourceIcons provides icons) {
                    TestHome(catalog = catalogOf(dock = emptyList(), grid = emptyList()), hero = heroStateOf(items = listOfNotNull(shown)))
                }
            }
        } else {
            compose.runOnIdle { shown = item.copy(id = item.id + shownCount++) }
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForIdle()
    }

    private var shownCount = 0

    private fun program(
        kind: ProgramKind,
        app: String? = "Appli fictive",
        pkg: String = WITH_ICON,
        season: String? = null,
        episode: String? = null,
        duration: Long? = null,
        position: Long? = null,
        progress: Float? = null,
    ) = HeroItem(
        id = "p",
        title = "Titre fictif",
        sourcePackage = pkg,
        sourceLabel = app,
        launchUri = "intent://example/1",
        kind = kind,
        season = season,
        episode = episode,
        durationMillis = duration,
        positionMillis = position,
        progress = progress,
    )

    private fun label() = compose.onNodeWithTag("hero-header-label", useUnmergedTree = true)

    private fun bottom(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.bottom

    @Test
    fun `program in progress shows continue, details and remaining time`() {
        show(
            program(
                ProgramKind.CONTINUE,
                season = "2",
                episode = "5",
                duration = 42 * MINUTE,
                position = 17 * MINUTE,
                progress = 17f / 42f,
            ),
        )
        label().assertTextEquals("Continuer dans Appli fictive")
        compose.onNodeWithTag("hero-source-icon", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("hero-details", useUnmergedTree = true).assertTextEquals("Saison 2 · Épisode 5 · 42 min")
        compose.onNodeWithTag("hero-remaining", useUnmergedTree = true).assertTextEquals("Reste 25 min")
        compose.onNodeWithTag("hero-progress-bar", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `next episode, new and watchlist programs name their type`() {
        show(program(ProgramKind.NEXT))
        label().assertTextEquals("Épisode suivant dans Appli fictive")
        show(program(ProgramKind.NEW))
        label().assertTextEquals("Nouveau dans Appli fictive")
        show(program(ProgramKind.WATCHLIST))
        label().assertTextEquals("À regarder dans Appli fictive")
    }

    @Test
    fun `program highlighted by the app and watch next program without type show the app name only`() {
        show(program(ProgramKind.FEATURED))
        label().assertTextEquals("Appli fictive")
        show(program(ProgramKind.WATCH_NEXT))
        label().assertTextEquals("Appli fictive")
    }

    @Test
    fun `missing app name keeps the icon and the type`() {
        show(program(ProgramKind.CONTINUE, app = null))
        label().assertTextEquals("Continuer")
        compose.onNodeWithTag("hero-source-icon", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `missing icon keeps the label alone`() {
        show(program(ProgramKind.NEW, pkg = "com.example.noicon"))
        label().assertTextEquals("Nouveau dans Appli fictive")
        compose.onNodeWithTag("hero-source-icon", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `featured program without name keeps the icon alone`() {
        show(program(ProgramKind.FEATURED, app = null))
        compose.onNodeWithTag("hero-source-icon", useUnmergedTree = true).assertExists()
        label().assertDoesNotExist()
    }

    @Test
    fun `featured program without name nor icon has no header line`() {
        show(program(ProgramKind.FEATURED, app = null, pkg = "com.example.noicon"))
        compose.onNodeWithTag("hero-header", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `details only show what the app provides`() {
        show(program(ProgramKind.FEATURED, episode = "3"))
        compose.onNodeWithTag("hero-details", useUnmergedTree = true).assertTextEquals("Épisode 3")
        show(program(ProgramKind.FEATURED, duration = 95 * MINUTE))
        compose.onNodeWithTag("hero-details", useUnmergedTree = true).assertTextEquals("1 h 35 min")
    }

    @Test
    fun `no details leaves no empty line under the title`() {
        show(program(ProgramKind.FEATURED))
        compose.onNodeWithTag("hero-details", useUnmergedTree = true).assertDoesNotExist()
        val title = compose.onNodeWithText("Titre fictif", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(title.bottom, bottom("hero-metadata"), 0.5f)
    }

    @Test
    fun `progress without position or duration shows the bar without remaining time`() {
        show(program(ProgramKind.CONTINUE, progress = 0.5f))
        compose.onNodeWithTag("hero-progress-bar", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("hero-remaining", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `no progress shows neither bar nor remaining time`() {
        show(program(ProgramKind.CONTINUE, duration = 42 * MINUTE))
        compose.onNodeWithTag("hero-progress", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("hero-remaining", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("hero-details", useUnmergedTree = true).assertTextEquals("42 min")
    }

    @Test
    fun `remaining time over an hour shows hours and minutes`() {
        show(program(ProgramKind.CONTINUE, duration = 130 * MINUTE, position = 58 * MINUTE, progress = 58f / 130f))
        compose.onNodeWithTag("hero-remaining", useUnmergedTree = true).assertTextEquals("Reste 1 h 12 min")
    }

    private companion object {
        const val WITH_ICON = "com.example.player"
        const val MINUTE = 60_000L
    }
}
