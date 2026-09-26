/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.data.Catalog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class CatalogUpdateFocusTest {

    @get:Rule
    val compose = createComposeRule()

    private var catalog by mutableStateOf(
        catalogOf(
            dock = listOf(app("com.d1", "Dock 1"), app("com.d2", "Dock 2"), app("com.d3", "Dock 3")),
            grid = listOf(app("com.a", "Alpha"), app("com.b", "Beta"), app("com.c", "Gamma")),
        ),
    )

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun longPress() {
        compose.mainClock.autoAdvance = false
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS + 100)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("app-menu").assertExists()
    }

    private fun setContent(initialZone: Zone) {
        compose.setContent {
            TestHome(
                catalog = catalog,
                hero = heroStateOf(items = emptyList()),
                initialZone = initialZone,
                onTogglePin = { app -> catalog = catalog.withoutDock(app.packageName) },
                onHide = { app -> catalog = catalog.withoutGrid(app.packageName) },
            )
        }
        compose.waitForIdle()
    }

    private fun Catalog.withoutGrid(packageName: String) = copy(grid = grid.filterNot { it.packageName == packageName })

    private fun Catalog.withoutDock(packageName: String) = copy(dock = dock.filterNot { it.packageName == packageName })

    @Test
    fun `hiding the focused grid app removes its tile and focuses the next one`() {
        setContent(Zone.GRID)
        press(Key.DirectionRight)
        compose.onNodeWithTag("app-tile-com.b").assertIsFocused()

        longPress()
        repeat(3) { press(Key.DirectionRight) }
        compose.onNodeWithTag("menu-hide").assertIsFocused()
        press(Key.Enter)

        compose.onNodeWithTag("app-menu").assertDoesNotExist()
        compose.onNodeWithTag("app-tile-com.b").assertDoesNotExist()
        compose.onNodeWithTag("app-tile-com.c").assertIsFocused()
    }

    @Test
    fun `hiding the last grid app focuses the previous one`() {
        setContent(Zone.GRID)
        press(Key.DirectionRight)
        press(Key.DirectionRight)
        compose.onNodeWithTag("app-tile-com.c").assertIsFocused()

        longPress()
        repeat(3) { press(Key.DirectionRight) }
        compose.onNodeWithTag("menu-hide").assertIsFocused()
        press(Key.Enter)

        compose.onNodeWithTag("app-tile-com.c").assertDoesNotExist()
        compose.onNodeWithTag("app-tile-com.b").assertIsFocused()
    }

    @Test
    fun `unpinning the focused dock app removes its tile and focuses the next one`() {
        setContent(Zone.DOCK)
        press(Key.DirectionRight)
        compose.onNodeWithTag("app-tile-com.d2").assertIsFocused()

        longPress()
        press(Key.Enter)

        compose.onNodeWithTag("app-menu").assertDoesNotExist()
        compose.onNodeWithTag("app-tile-com.d2").assertDoesNotExist()
        compose.onNodeWithTag("app-tile-com.d3").assertIsFocused()
    }
}
