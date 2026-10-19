/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class ArtworkSchedulerTest {
    @Test fun `four apps respect the global bound and a queued app precedes another image from the released app`() = runBlocking {
        ArtworkScheduler(3).use { scheduler ->
            val entered = CountDownLatch(3)
            val releaseA = CountDownLatch(1)
            val releaseOthers = CountDownLatch(1)
            val order = java.util.concurrent.CopyOnWriteArrayList<String>()
            val started = (1..3).map { index -> async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                scheduler.run("app$index", 3_000) {
                    entered.countDown()
                    if (index == 1) releaseA.await() else releaseOthers.await()
                    index
                }
            } }
            val sameApp = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { scheduler.run("app1", 3_000) { order += "app1"; 1 } }
            val fourth = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { scheduler.run("app4", 3_000) { order += "app4"; 4 } }
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS))
                assertTrue(order.isEmpty())
                releaseA.countDown()
                assertEquals(4, withTimeout(3_000) { fourth.await() }.getOrThrow())
                assertEquals(1, sameApp.await().getOrThrow())
                assertEquals(listOf("app4", "app1"), order)
            } finally { releaseA.countDown(); releaseOthers.countDown() }
            started.forEach { assertTrue(it.await().isSuccess) }
        }
    }

    @Test fun `blocked app times out without releasing its physical slot`() = runBlocking {
        ArtworkScheduler(2).use { scheduler ->
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            try {
                val blocked = async { scheduler.run("blocked", 100) { entered.countDown(); release.await(); "late" } }
                val healthy = async {
                    assertTrue(entered.await(2, TimeUnit.SECONDS))
                    scheduler.run("healthy", 1_000) { "ready" }
                }
                assertEquals("ready", withTimeout(3_000) { healthy.await() }.getOrThrow())
                assertTrue(withTimeout(3_000) { blocked.await() }.exceptionOrNull() is TimeoutException)
                var reopened = false
                val again = scheduler.run("blocked", 100) { reopened = true; "bad" }
                assertTrue(again.exceptionOrNull() is TimeoutException)
                assertFalse(reopened)
            } finally { release.countDown() }
        }
    }

    @Test fun `blocking cancellation cannot stop later deadlines`() = runBlocking {
        ArtworkScheduler(3).use { scheduler ->
            val release = CountDownLatch(1)
            try {
                val jobs = (1..3).map { app -> async { scheduler.run("$app", 100, cancel = { release.await() }) { release.await() } } }
                withTimeout(3_000) { jobs.forEach { assertTrue(it.await().exceptionOrNull() is TimeoutException) } }
                assertTrue(scheduler.run("fourth", 100) { error("must not start") }.exceptionOrNull() is ArtworkBusyException)
            } finally { release.countDown() }
        }
    }
}
