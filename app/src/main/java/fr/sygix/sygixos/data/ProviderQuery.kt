/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.CancellationSignal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
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

internal fun ContentResolver.changes(uris: List<Uri>, debounceMillis: Long = 500): Flow<Unit> = channelFlow {
    val signals = Channel<Unit>(Channel.CONFLATED)
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) { signals.trySend(Unit) }
    }
    val observing = uris.map { uri -> runCatching { registerContentObserver(uri, true, observer) }.isSuccess }.any { it }
    try {
        send(Unit)
        while (observing) {
            signals.receive()
            delay(debounceMillis)
            signals.tryReceive()
            send(Unit)
        }
    } finally {
        runCatching { unregisterContentObserver(observer) }
        signals.close()
    }
}
