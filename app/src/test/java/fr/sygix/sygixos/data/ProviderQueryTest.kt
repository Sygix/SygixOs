/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.os.OperationCanceledException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProviderQueryTest {

    @Test
    fun `caller cancellation cancels the provider query without reporting a failure`() = runTest {
        val entered = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        var returned: Result<Unit>? = null
        val queries = blockingQueries()
        val caller = launch(start = CoroutineStart.UNDISPATCHED) {
            returned = queries.run { signal ->
                val released = CountDownLatch(1)
                signal.setOnCancelListener { released.countDown() }
                entered.countDown()
                while (released.count > 0) {
                    try { released.await() } catch (_: InterruptedException) { }
                }
                cancelled.countDown()
                throw OperationCanceledException()
            }
        }
        assertTrue(entered.await(30, TimeUnit.SECONDS))
        caller.cancelAndJoin()
        assertTrue(cancelled.await(30, TimeUnit.SECONDS))
        assertNull(returned)
    }
}
