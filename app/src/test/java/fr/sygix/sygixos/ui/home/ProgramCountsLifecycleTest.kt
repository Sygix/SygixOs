/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
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

    @Test
    fun `counting starts with the home, pauses when the launcher stops and counts again when it starts`() {
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
        compose.waitUntil(5_000) { counts.value == mapOf("com.a" to 1) }
        assertEquals(0, stops)

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.waitUntil(5_000) { stops == 1 }
        assertEquals(1, starts)

        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitUntil(5_000) { counts.value == mapOf("com.a" to 2) }
        assertEquals(2, starts)
        assertEquals(1, stops)
    }
}
