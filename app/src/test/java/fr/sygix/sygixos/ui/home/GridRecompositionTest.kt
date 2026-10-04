/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.tooling.CompositionObserver
import androidx.compose.runtime.tooling.ObservableComposition
import androidx.compose.runtime.tooling.setObserver
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalComposeRuntimeApi::class)
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class GridRecompositionTest {

    @get:Rule
    val compose = createComposeRule()

    private val grid = (0 until 20).map { app("com.g$it", "Grille $it") }
    private val entered = mutableListOf<RecomposeScope>()
    private var recording = false
    private var validated by mutableStateOf(emptySet<String>())

    private val observer = object : CompositionObserver {
        override fun onBeginComposition(composition: ObservableComposition) = Unit
        override fun onScopeEnter(scope: RecomposeScope) {
            if (recording) entered += scope
        }
        override fun onReadInScope(scope: RecomposeScope, value: Any) = Unit
        override fun onScopeExit(scope: RecomposeScope) = Unit
        override fun onEndComposition(composition: ObservableComposition) = Unit
        override fun onScopeInvalidated(scope: RecomposeScope, value: Any?) = Unit
        override fun onScopeDisposed(scope: RecomposeScope) = Unit
    }

    private val programs = listOf(
        heroItem("p1", "Programme un", imageUrl = "https://example.invalid/p1.jpg", sourcePackage = "com.g7"),
        heroItem("p2", "Programme deux", imageUrl = "https://example.invalid/p2.jpg", sourcePackage = "com.g12"),
    )

    private fun showGrid() {
        compose.setContent {
            val composition = currentComposer.composition
            DisposableEffect(composition) {
                val handle = composition.setObserver(observer)
                onDispose { handle?.dispose() }
            }
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = grid),
                hero = heroStateOf(items = programs, validated = validated, checked = validated),
                initialZone = Zone.GRID,
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.g0").assertIsFocused()
    }

    private fun recorded(action: () -> Unit): Int {
        entered.clear()
        recording = true
        action()
        compose.waitForIdle()
        recording = false
        return entered.size
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
    }

    @Test
    fun `moving the focus along a row recomposes only a few scopes`() {
        showGrid()
        val count = recorded { press(Key.DirectionRight) }
        compose.onNodeWithTag("app-tile-com.g1").assertIsFocused()
        assertTrue("$count scopes recomposed", count <= MOVE_BUDGET)
    }

    @Test
    fun `moving the focus to the next row recomposes only a few scopes`() {
        showGrid()
        val count = recorded { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.g5").assertIsFocused()
        assertTrue("$count scopes recomposed", count <= MOVE_BUDGET)
    }

    @Test
    fun `a checked visual of another app recomposes no row of the grid`() {
        showGrid()
        compose.mainClock.advanceTimeBy(Motion.SHELF_PREPARE_DELAY_MS + 100)
        val count = recorded { validated = setOf("https://example.invalid/p2.jpg") }
        assertTrue("$count scopes recomposed", count <= VALIDATION_BUDGET)
    }

    private companion object {
        const val MOVE_BUDGET = 30
        const val VALIDATION_BUDGET = 40
    }
}
