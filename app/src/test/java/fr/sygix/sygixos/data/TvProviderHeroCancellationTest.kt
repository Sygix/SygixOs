/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.os.CancellationSignal
import androidx.test.core.app.ApplicationProvider
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TvProviderHeroCancellationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun cancellingHeroLoadCancelsTheRunningProviderQuery() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val release = CountDownLatch(1)
        val source = TvProviderHeroSource(context, read = { _, _, signal ->
            signal.setOnCancelListener { cancelled.complete(Unit); release.countDown() }
            entered.complete(Unit)
            while (release.count > 0) {
                try { release.await() } catch (_: InterruptedException) { }
            }
            null
        })
        val load = async { source.load() }
        try {
            withTimeout(5_000) { entered.await() }
            load.cancel()
            withTimeout(5_000) { cancelled.await() }
        } finally {
            release.countDown()
            load.cancelAndJoin()
        }
    }

    @Test fun aTimedOutCountQueryIsCancelledAndDoesNotPreventTheOtherTable() = runBlocking {
        val release = CountDownLatch(1)
        val signals = mutableListOf<CancellationSignal>()
        val source = TvProviderHeroSource(context, timeoutMillis = 50, read = { _, _, signal ->
            signals += signal
            if (signals.size == 1) {
                signal.setOnCancelListener { release.countDown() }
                while (release.count > 0) {
                    try { release.await() } catch (_: InterruptedException) { }
                }
            }
            null
        })
        val load = async { source.programCounts() }
        try {
            assertTrue(withTimeout(5_000) { load.await() }.isEmpty())
            assertTrue(signals.first().isCanceled)
            assertTrue(signals.size == 2)
        } finally {
            release.countDown()
            load.cancelAndJoin()
        }
    }
}
