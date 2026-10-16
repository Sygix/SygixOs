/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.os.CancellationSignal
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout

internal suspend fun <T> providerQuery(timeoutMillis: Long, query: (CancellationSignal) -> T): Result<T> = try {
    Result.success(withTimeout(timeoutMillis) {
        coroutineScope {
            val signal = CancellationSignal()
            val done = AtomicBoolean(false)
            val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    awaitCancellation()
                } finally {
                    if (!done.get()) signal.cancel()
                }
            }
            try {
                runInterruptible(Dispatchers.IO) { query(signal) }
            } finally {
                done.set(true)
                watcher.cancel()
            }
        }
    })
} catch (error: TimeoutCancellationException) {
    Result.failure(error)
} catch (error: CancellationException) {
    throw error
} catch (error: Exception) {
    Result.failure(error)
}
