/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.os.CancellationSignal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout

internal suspend fun <T> providerQuery(timeoutMillis: Long = Long.MAX_VALUE, query: (CancellationSignal) -> T): Result<T> = try {
    Result.success(withTimeout(timeoutMillis) {
        coroutineScope {
            val signal = CancellationSignal()
            val result = async(Dispatchers.IO) { runInterruptible { query(signal) } }
            try {
                result.await()
            } catch (cancelled: CancellationException) {
                signal.cancel()
                throw cancelled
            }
        }
    })
} catch (error: Exception) {
    currentCoroutineContext().ensureActive()
    Result.failure(error)
}
