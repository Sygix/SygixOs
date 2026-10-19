/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CompletableDeferred

internal class ArtworkScheduler(
    private val parallelism: Int = 3,
    private val capacity: Int = 64,
) : Closeable {
    private val threads = ThreadFactory { task -> Thread(task, "artwork-worker").apply { isDaemon = true } }
    private val workers = Executors.newFixedThreadPool(parallelism, threads)
    private val deadlines = ScheduledThreadPoolExecutor(1, threads).apply { removeOnCancelPolicy = true }
    private val cancellations = ThreadPoolExecutor(2, 2, 0, TimeUnit.MILLISECONDS, ArrayBlockingQueue(64), threads, ThreadPoolExecutor.DiscardPolicy())
    private val pending = mutableListOf<Work<*>>()
    private val active = mutableSetOf<String>()
    private val quarantined = mutableSetOf<String>()
    private var closed = false
    private var lastApp: String? = null

    private class Work<T>(val app: String, val cancel: () -> Unit, val read: () -> T) {
        val result = CompletableDeferred<Result<T>>()
        var started = false
    }

    suspend fun <T> run(app: String, timeoutMillis: Long, cancel: () -> Unit = {}, read: () -> T): Result<T> {
        val work = Work(app, cancel, read)
        val timeout = synchronized(this) {
            if (closed || pending.size >= capacity || quarantined.size == parallelism) {
                return Result.failure(ArtworkBusyException())
            }
            pending += work
            deadlines.schedule({ expire(work) }, timeoutMillis.coerceAtLeast(1), TimeUnit.MILLISECONDS).also { pump() }
        }
        return try { work.result.await() } finally {
            timeout.cancel(false)
            synchronized(this) {
                if (!work.result.isCompleted) {
                    work.result.cancel()
                    pending.remove(work)
                    if (work.started) quarantined += work.app
                    cancellations.execute(work.cancel)
                }
            }
        }
    }

    @Synchronized private fun expire(work: Work<*>) {
        if (work.result.complete(Result.failure(TimeoutException("Artwork deadline exceeded")))) {
            pending.remove(work)
            if (work.started) quarantined += work.app
            cancellations.execute(work.cancel)
        }
    }

    private fun pump() {
        while (!closed && active.size < parallelism) {
            val preferred = pending.indexOfFirst { it.app !in active && it.app != lastApp }
            val index = preferred.takeIf { it >= 0 } ?: pending.indexOfFirst { it.app !in active }
            if (index < 0) return
            start(pending.removeAt(index))
        }
    }

    private fun <T> start(work: Work<T>) {
        active += work.app
        lastApp = work.app
        work.started = true
        workers.execute {
            try {
                if (!work.result.isCompleted) work.result.complete(runCatching(work.read))
            } finally {
                synchronized(this) {
                    active -= work.app
                    quarantined -= work.app
                    lastApp = work.app
                    pump()
                }
            }
        }
    }

    @Synchronized override fun close() {
        closed = true
        pending.forEach { it.result.complete(Result.failure(ArtworkBusyException())) }
        pending.clear()
        deadlines.shutdownNow()
        workers.shutdownNow()
        cancellations.shutdownNow()
    }
}

internal class ArtworkBusyException : java.io.IOException("Artwork workers unavailable")
