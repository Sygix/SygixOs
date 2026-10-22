/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import java.util.concurrent.TimeUnit

internal class ManualArtworkTimer {
    private class Task(val at: Long, val action: Runnable)

    private val lock = Object()
    private val tasks = mutableListOf<Task>()
    private var now = 0L

    fun schedule(delay: Long, action: Runnable): () -> Unit = synchronized(lock) {
        val task = Task(now + delay, action)
        tasks += task
        lock.notifyAll()
        val cancel: () -> Unit = { synchronized(lock) { tasks.remove(task) } }
        cancel
    }

    fun awaitScheduled(count: Int) = synchronized(lock) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30)
        while (tasks.size < count) {
            val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
            check(remaining > 0) { "Expected $count scheduled deadlines, found ${tasks.size}" }
            lock.wait(remaining)
        }
    }

    fun advance(millis: Long) {
        val due = synchronized(lock) {
            now += millis
            tasks.filter { it.at <= now }.also { tasks.removeAll(it) }
        }
        due.forEach { it.action.run() }
    }

    fun executors(parallelism: Int): ArtworkExecutors = ArtworkExecutors.threads(parallelism).let {
        ArtworkExecutors(it.workers, it.cancellations, ::schedule, it.shutdown)
    }
}
