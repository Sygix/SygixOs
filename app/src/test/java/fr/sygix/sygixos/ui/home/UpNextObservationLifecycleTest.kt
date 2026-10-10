/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class UpNextObservationLifecycleTest {
    @get:Rule val compose = createComposeRule()
    private var starts = 0
    private var stops = 0
    private val updates = flow<Unit> {
        starts++
        awaitCancellation()
    }.onCompletion { stops++ }
    private val owner = object : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun `provider observation runs only while the launcher is in the foreground`() {
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                MaterialTheme {
                    LauncherHome(catalog = catalogOf(emptyList(), listOf(app("com.a", "A"))), hero = heroStateOf(emptyList()),
                        onTogglePin = {}, glassBlur = false, upNextUpdates = updates)
                }
            }
        }
        compose.waitUntil(5_000) { starts == 1 }
        assertEquals(0, stops)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.waitUntil(5_000) { stops == 1 }
        assertEquals(1, starts)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitUntil(5_000) { starts == 2 }
        assertEquals(1, stops)
    }
}
