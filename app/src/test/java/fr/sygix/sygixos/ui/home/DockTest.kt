/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.model.TvApp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DockTest {

    @get:Rule
    val compose = createComposeRule()

    private fun setContentDock(apps: List<TvApp>) {
        compose.setContent {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { withFrameNanos { }; focusRequester.tryRequestFocus() }
            MaterialTheme {
                Dock(
                    apps = apps,
                    alpha = 1f,
                    focusEnabled = true,
                    focusRequester = focusRequester,
                    onTileClick = {},
                    onTileLongClick = {},
                )
            }
        }
    }

    @Test
    fun `dock renders one tile per pinned app`() {
        setContentDock(listOf(app("com.a", "Alpha"), app("com.b", "Beta")))
        compose.onNodeWithTag("app-tile-com.a").assertExists()
        compose.onNodeWithTag("app-tile-com.b").assertExists()
    }

    @Test
    fun `dock gives initial focus to its first tile`() {
        setContentDock(listOf(app("com.a", "Alpha"), app("com.b", "Beta")))
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.a").assertIsFocused()
    }

    @Test
    fun `empty dock shows pin hint and no tiles`() {
        setContentDock(emptyList())
        compose.onNodeWithText("Épinglez des apps depuis la grille (appui long)").assertExists()
        compose.onNodeWithTag("app-tile-com.a").assertDoesNotExist()
    }
}
