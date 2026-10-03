/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class GlassSurfaceTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `switching the glass off and on keeps the same content`() {
        var active by mutableStateOf(true)
        var entered = 0
        var left = 0
        compose.setContent {
            val haze = rememberHazeState()
            CompositionLocalProvider(LocalHazeState provides haze) {
                Box(Modifier.size(400.dp).hazeSource(haze)) {
                    GlassSurface(Modifier.testTag("glass"), active = active) {
                        DisposableEffect(Unit) {
                            entered++
                            onDispose { left++ }
                        }
                        Box(Modifier.size(40.dp))
                    }
                }
            }
        }
        compose.onNodeWithTag("glass").assert(SemanticsMatcher.expectValue(GlassActive, true))
        compose.runOnIdle { active = false }
        compose.onNodeWithTag("glass").assert(SemanticsMatcher.expectValue(GlassActive, false))
        compose.runOnIdle { active = true }
        compose.onNodeWithTag("glass").assert(SemanticsMatcher.expectValue(GlassActive, true))
        assertEquals(1, entered)
        assertEquals(0, left)
    }

    @Test
    fun `without a haze state the surface is never glass`() {
        compose.setContent {
            GlassSurface(Modifier.testTag("glass"), active = true) { Box(Modifier.size(40.dp)) }
        }
        compose.onNodeWithTag("glass").assert(SemanticsMatcher.expectValue(GlassActive, false))
    }
}
