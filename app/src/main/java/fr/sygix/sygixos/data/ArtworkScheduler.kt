/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CompletableDeferred

internal class ArtworkExecutors(
    val workers: Executor,
    val cancellations: Executor,
    val schedule: (Long, Runnable) -> () -> Unit,
    val shutdown: () -> Unit,
) {
    companion object {
        val Shared: ArtworkExecutors by lazy {
            val threads = threads(SHARED_PARALLELISM)
            ArtworkExecutors(threads.workers, threads.cancellations, threads.schedule) {}
        }

        fun threads(parallelism: Int): ArtworkExecutors {
            val threads = ThreadFactory { task -> Thread(task, "artwork-worker").apply { isDaemon = true } }
            val workers = ThreadPoolExecutor(parallelism, parallelism, IDLE_MS, TimeUnit.MILLISECONDS, LinkedBlockingQueue(), threads)
                .apply { allowCoreThreadTimeOut(true) }
            val deadlines = ScheduledThreadPoolExecutor(1, threads).apply { removeOnCancelPolicy = true }
            val cancellations = ThreadPoolExecutor(2, 2, IDLE_MS, TimeUnit.MILLISECONDS, ArrayBlockingQueue(64), threads, ThreadPoolExecutor.DiscardPolicy())
                .apply { allowCoreThreadTimeOut(true) }
            return ArtworkExecutors(
                workers = workers,
                cancellations = cancellations,
                schedule = { delay, action -> deadlines.schedule(action, delay, TimeUnit.MILLISECONDS).let { future -> { future.cancel(false); Unit } } },
                shutdown = { deadlines.shutdownNow(); workers.shutdownNow(); cancellations.shutdownNow() },
            )
        }

        private const val SHARED_PARALLELISM = 6
        private const val IDLE_MS = 30_000L
    }
}

internal class ArtworkScheduler(
    private val parallelism: Int = 3,
    private val capacity: Int = 64,
    internal val executors: ArtworkExecutors = ArtworkExecutors.Shared,
) : Closeable {
    private val pending = mutableListOf<Work<*>>()
    private val active = mutableSetOf<String>()
    private val quarantined = mutableSetOf<String>()
    private var closed = false
    private var lastApp: String? = null

    private class Work<T>(val app: String, val key: String?, val timeoutMillis: Long, var priority: Boolean, val cancel: () -> Unit, val read: () -> T) {
        val result = CompletableDeferred<Result<T>>()
        var started = false
        var timeout: (() -> Unit)? = null
    }

    @Synchronized fun promote(key: String) { pending.filter { it.key == key }.forEach { it.priority = true } }

    suspend fun <T> run(app: String, timeoutMillis: Long, cancel: () -> Unit = {}, priority: Boolean = true, key: String? = null, read: () -> T): Result<T> {
        val work = Work(app, key, timeoutMillis, priority, cancel, read)
        synchronized(this) {
            if (closed || pending.size >= capacity || quarantined.size == parallelism || app in quarantined) {
                return Result.failure(ArtworkBusyException())
            }
            pending += work
            pump()
        }
        return try { work.result.await() } finally {
            work.timeout?.invoke()
            synchronized(this) {
                if (!work.result.isCompleted) {
                    work.result.cancel()
                    pending.remove(work)
                    if (work.started) quarantined += work.app
                    executors.cancellations.execute(work.cancel)
                }
            }
        }
    }

    @Synchronized private fun expire(work: Work<*>) {
        if (work.result.complete(Result.failure(TimeoutException("Artwork deadline exceeded")))) {
            pending.remove(work)
            if (work.started) quarantined += work.app
            executors.cancellations.execute(work.cancel)
        }
    }

    private fun pump() {
        while (!closed && active.size < parallelism) {
            val fairVisible = pending.indexOfFirst { it.app !in active && it.priority && it.app != lastApp }
            val visible = fairVisible.takeIf { it >= 0 } ?: pending.indexOfFirst { it.app !in active && it.priority }
            val preferred = visible.takeIf { it >= 0 } ?: pending.indexOfFirst { it.app !in active && it.app != lastApp }
            val index = preferred.takeIf { it >= 0 } ?: pending.indexOfFirst { it.app !in active }
            if (index < 0) return
            start(pending.removeAt(index))
        }
    }

    private fun <T> start(work: Work<T>) {
        active += work.app
        lastApp = work.app
        work.started = true
        executors.workers.execute {
            try {
                work.timeout = executors.schedule(work.timeoutMillis.coerceAtLeast(1)) { expire(work) }
                if (!work.result.isCompleted) work.result.complete(runCatching(work.read))
            } finally {
                work.timeout?.invoke()
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
        executors.shutdown()
    }
}

internal class ArtworkBusyException : java.io.IOException("Artwork workers unavailable")
