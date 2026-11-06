/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class ManualArtworkTimer {
    private class Task(val at: Long, val action: Runnable)

    private val lock = ReentrantLock()
    private val changed = lock.newCondition()
    private val tasks = mutableListOf<Task>()
    private var now = 0L

    fun schedule(delay: Long, action: Runnable): () -> Unit = lock.withLock {
        val task = Task(now + delay, action)
        tasks += task
        changed.signalAll()
        val cancel: () -> Unit = { lock.withLock { tasks.remove(task) } }
        cancel
    }

    fun awaitScheduled(count: Int) = lock.withLock {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (tasks.size < count) {
            val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
            check(remaining > 0) { "Expected $count scheduled deadlines, found ${tasks.size}" }
            changed.await(remaining, TimeUnit.MILLISECONDS)
        }
    }

    fun advance(millis: Long) {
        val due = lock.withLock {
            now += millis
            tasks.filter { it.at <= now }.also { tasks.removeAll(it) }
        }
        due.forEach { it.action.run() }
    }

    fun executors(parallelism: Int): ArtworkExecutors = ArtworkExecutors.threads(parallelism).let {
        ArtworkExecutors(it.workers, it.cancellations, ::schedule, it.shutdown)
    }
}
