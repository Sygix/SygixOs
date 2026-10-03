/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.domain.StartupPhase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class StartupSlowMotionTest : StartupHostTest(motionScale = 2f) {

    @Test
    fun `slowed system animations let the fade finish before the splash goes away`() {
        launch()
        advanceTo(100)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(600)
        assertEquals(StartupPhase.FadingOut, controller.phase.value)
        advanceTo(600 + Motion.SPLASH_FADE_MS + 200L)
        assertSplash(true)
        assertEquals(StartupPhase.FadingOut, controller.phase.value)
        advanceTo(600 + 2L * Motion.SPLASH_FADE_MS + 100L)
        frames(8)
        assertSplash(false)
        assertEquals(StartupPhase.Done, controller.phase.value)
        compose.onNodeWithTag("zone-hero").assertExists()
    }
}
