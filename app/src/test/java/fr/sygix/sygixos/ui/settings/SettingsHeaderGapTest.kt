/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Dimens
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class SettingsHeaderGapTest {
    @get:Rule val compose = createComposeRule()
    private val harness = SettingsHarness()

    @After
    fun clear() = harness.clear()

    private fun press(key: Key) {
        compose.onNodeWithTag("settings-screen").performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun gap(description: SemanticsMatcher, firstRow: String): Float {
        val density = compose.onNodeWithTag("settings-screen").fetchSemanticsNode().layoutInfo.density.density
        val text = compose.onNode(description, useUnmergedTree = true).fetchSemanticsNode()
        val row = compose.onNodeWithTag(firstRow).fetchSemanticsNode()
        return (row.positionInRoot.y - (text.positionInRoot.y + text.size.height)) / density
    }

    @Test
    fun `every settings pane puts its first row at the shared header gap below its description`() {
        harness.install("Alpha", "Beta")
        harness.hideInOrder("Beta")
        compose.showSettings(harness)
        val expected = Dimens.SettingsHeaderGap.value
        assertEquals(expected, gap(hasText("Choisissez les applications qui alimentent le héro, le Top Shelf et Up Next."), "source-row-com.alpha"), 0.5f)
        press(Key.DirectionDown)
        assertEquals(expected, gap(hasText("Les applications cachées n'apparaissent plus dans la grille ni dans le dock."), "unhide-all"), 0.5f)
        press(Key.DirectionDown)
        assertEquals(expected, gap(hasText("Choisissez ce qu'affiche l'accueil de SygixOs."), "setting-upnext-visible"), 0.5f)
        press(Key.DirectionDown)
        assertEquals(expected, gap(hasText("SygixOs est un logiciel libre", substring = true), "update-check"), 0.5f)
    }
}
