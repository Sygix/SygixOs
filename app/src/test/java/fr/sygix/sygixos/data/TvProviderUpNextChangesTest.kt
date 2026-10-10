/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.media.tv.TvContract.WatchNextPrograms
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TvProviderUpNextChangesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    @Config(shadows = [DeniedObserverShadowContentResolver::class])
    fun `observer registration refused for a denied or missing provider emits one initial load then completes without crash`() = runBlocking {
        val emissions = withTimeout(10_000) { TvProviderUpNextSource(context, debounceMillis = 1).changes().toList() }
        assertEquals(listOf(Unit), emissions)
    }

    @Test
    fun `a burst of notifications triggers a single reload after the debounce and cancellation unregisters`() = runTest {
        val resolver = shadowOf(context.contentResolver)
        val emissions = mutableListOf<Unit>()
        val job = backgroundScope.launch { TvProviderUpNextSource(context, debounceMillis = 500).changes().collect { emissions += it } }
        runCurrent()
        assertEquals(1, emissions.size)
        assertEquals(1, resolver.getContentObservers(WatchNextPrograms.CONTENT_URI).size)
        repeat(3) { context.contentResolver.notifyChange(WatchNextPrograms.CONTENT_URI, null) }
        runCurrent()
        advanceTimeBy(499)
        runCurrent()
        assertEquals(1, emissions.size)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, emissions.size)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(2, emissions.size)
        job.cancel()
        runCurrent()
        assertEquals(0, resolver.getContentObservers(WatchNextPrograms.CONTENT_URI).size)
    }
}
