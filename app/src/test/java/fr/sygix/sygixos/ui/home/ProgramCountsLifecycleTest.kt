/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.ui.settings.SettingsHarness
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class ProgramCountsLifecycleTest {

    @get:Rule
    val compose = createComposeRule()

    private var starts = 0
    private var stops = 0

    private val harness = SettingsHarness(
        flow {
            starts++
            emit(mapOf("com.a" to starts))
            awaitCancellation()
        }.onCompletion { stops++ },
    )

    private val owner = object : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }

    @After
    fun clearViewModel() = harness.clear()

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    @Test
    fun `counting runs only while the settings are open and pauses when the launcher stops`() {
        val counts = harness.viewModel.counts
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                TestHome(
                    catalog = catalogOf(dock = emptyList(), grid = listOf(app("com.a", "A"))),
                    hero = heroStateOf(items = emptyList()),
                    counts = counts,
                )
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitForIdle()
        assertEquals(0, starts)

        press(Key.DirectionUp)
        press(Key.Enter)
        compose.onNodeWithTag("settings-screen").assertExists()
        compose.waitUntil(5_000) { counts.value == mapOf("com.a" to 1) }

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.waitUntil(5_000) { stops == 1 }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitUntil(5_000) { counts.value == mapOf("com.a" to 2) }

        press(Key.Back)
        compose.onNodeWithTag("settings-screen").assertDoesNotExist()
        compose.waitUntil(5_000) { stops == 2 }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitForIdle()
        assertEquals(2, starts)
    }
}
