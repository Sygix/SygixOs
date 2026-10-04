/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.SygixOsApp
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class MascotAnimationSourceTest {

    private var decodes = 0
    private val drawable: Drawable = ColorDrawable(Color.WHITE)

    private fun source(dispatcher: TestDispatcher) = RawMascotAnimationSource(dispatcher) {
        decodes++
        drawable
    }

    @Test
    fun `prefetched animation is decoded once and handed to the splash`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mascot = source(dispatcher)
        mascot.prefetch(backgroundScope)
        testScheduler.runCurrent()
        assertEquals(1, decodes)
        assertSame(drawable, mascot.load().getOrNull())
        assertEquals(1, decodes)
    }

    @Test
    fun `memory trim before the splash drops the prefetched animation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mascot = source(dispatcher)
        mascot.prefetch(backgroundScope)
        mascot.trimMemory()
        testScheduler.advanceUntilIdle()
        assertEquals(0, decodes)
        assertFalse(mascot.prefetched)
    }

    @Test
    fun `memory trim during the splash keeps the animation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mascot = source(dispatcher)
        mascot.prefetch(backgroundScope)
        testScheduler.runCurrent()
        assertSame(drawable, mascot.load().getOrNull())
        mascot.trimMemory()
        assertTrue(mascot.prefetched)
        mascot.release()
        assertFalse(mascot.prefetched)
    }

    @Test
    fun `test application never prefetches the animation`() {
        val app = ApplicationProvider.getApplicationContext<SygixOsApp>()
        assertFalse(app.mascotAnimation.prefetched)
    }

    @Test
    @Config(application = SygixOsApp::class)
    fun `main process prefetches the animation when it starts`() {
        val app = ApplicationProvider.getApplicationContext<SygixOsApp>()
        assertTrue(app.mascotAnimation.prefetched)
    }
}
