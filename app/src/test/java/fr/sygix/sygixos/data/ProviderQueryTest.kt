/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.os.OperationCanceledException
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProviderQueryTest {

    @Test
    fun `caller cancellation cancels the provider query without reporting a failure`() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        var returned: Result<Unit>? = null
        val caller = launch(Dispatchers.Default) {
            returned = providerQuery { signal ->
                val released = CountDownLatch(1)
                signal.setOnCancelListener { released.countDown() }
                entered.complete(Unit)
                while (released.count > 0) {
                    try { released.await() } catch (_: InterruptedException) { }
                }
                throw OperationCanceledException()
            }
        }
        withTimeout(5_000) {
            entered.await()
            caller.cancelAndJoin()
        }
        assertNull(returned)
    }
}
