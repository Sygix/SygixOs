/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.drawable.Drawable
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.data.MascotAnimationSource
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class PendingMascotSource : MascotAnimationSource {
    val drawable = FakeMascotDrawable()
    val ready = CompletableDeferred<Unit>()
    override suspend fun load(): Result<Drawable> {
        ready.await()
        return Result.success(drawable)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class StartupMascotTest : StartupHostTest(motionScale = 1f) {

    @Test
    fun `home is composed only once the mascot has faded in`() {
        launch()
        catalogReady()
        advanceTo(Motion.SPLASH_APPEAR_MS - 60L)
        compose.onAllNodesWithTag("zone-hero").assertCountEquals(0)
        advanceTo(Motion.SPLASH_APPEAR_MS + 60L)
        compose.onAllNodesWithTag("zone-hero").assertCountEquals(1)
    }

    @Test
    fun `home waits a short while for the window focus before being composed`() {
        windowFocused.value = false
        launch()
        catalogReady()
        advanceTo(Motion.SPLASH_APPEAR_MS + 100L)
        compose.onAllNodesWithTag("zone-hero").assertCountEquals(0)
        windowFocused.value = true
        Snapshot.sendApplyNotifications()
        frames()
        compose.onAllNodesWithTag("zone-hero").assertCountEquals(1)
    }

    @Test
    fun `permission dialog holding the window focus does not block the home`() {
        windowFocused.value = false
        launch()
        catalogReady()
        advanceTo(Motion.SPLASH_APPEAR_MS + Motion.SPLASH_FOCUS_WAIT_MS - 60L)
        compose.onAllNodesWithTag("zone-hero").assertCountEquals(0)
        advanceTo(Motion.SPLASH_APPEAR_MS + Motion.SPLASH_FOCUS_WAIT_MS + 60L)
        compose.onAllNodesWithTag("zone-hero").assertCountEquals(1)
        controller.heroVisualReady()
        advanceTo(Motion.SPLASH_APPEAR_MS + Motion.SPLASH_FOCUS_WAIT_MS + 100L + Motion.SPLASH_FADE_MS)
        frames(8)
        assertSplash(false)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `minimum duration counts from the first frame showing a late mascot`() {
        val source = PendingMascotSource()
        launch(mascot = source)
        assertNull(mascotAt)
        compose.mainClock.advanceTimeBy(800, ignoreFrameDuration = true)
        frames(1)
        source.ready.complete(Unit)
        repeat(10) { if (mascotAt == null) frames(1) }
        assertNotNull(mascotAt)
        assertTrue(checkNotNull(mascotAt) - checkNotNull(shownAt) >= 800)
        assertTrue(source.drawable.isRunning)
        catalogReady()
        controller.heroVisualReady()
        advanceTo(590)
        assertSplash(true)
        advanceTo(600)
        frames()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }
}
