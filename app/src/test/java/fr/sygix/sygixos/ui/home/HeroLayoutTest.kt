/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.model.HeroItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HeroLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private var shown by mutableStateOf<HeroItem?>(null)

    private fun show(item: HeroItem) {
        if (shown == null) {
            shown = item
            compose.setContent {
                TestHome(catalog = catalogOf(dock = emptyList(), grid = emptyList()), hero = heroStateOf(items = listOfNotNull(shown)))
            }
        } else {
            compose.runOnIdle { shown = item }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("hero-open").assertIsFocused()
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top

    private fun bottom(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.bottom

    private fun gapAfter(tag: String) = top("hero-open") - bottom(tag)

    private fun program(progress: Float?) = HeroItem(
        id = if (progress == null) "h2" else "h1",
        title = "Titre court",
        sourcePackage = "com.source",
        sourceLabel = "Source",
        durationMillis = progress?.let { 1_000_000L },
        positionMillis = progress?.let { (it * 1_000_000L).toLong() },
    )

    @Test
    fun `metadata without progress sits as close to the button as the progress bar does`() {
        show(program(progress = 0.4f))
        val withProgress = gapAfter("hero-progress")
        assertEquals(bottom("hero-progress"), bottom("hero-metadata"), 0.5f)

        show(program(progress = null))
        compose.onNodeWithTag("hero-progress").assertDoesNotExist()
        assertEquals(withProgress, gapAfter("hero-metadata"), 0.5f)
        assertTrue(withProgress > 0f)
    }

    @Test
    fun `a one line title reserves no second line`() {
        show(program(progress = null))
        val title = compose.onNodeWithText("Titre court", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(title.bottom, bottom("hero-metadata"), 0.5f)
        assertTrue(title.height < 60f * 2)
    }
}
