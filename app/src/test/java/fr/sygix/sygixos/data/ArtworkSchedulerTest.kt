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
    @Test fun `thirty queued images do not consume their request timeout before starting`() = runBlocking {
        ArtworkScheduler().use { scheduler ->
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val first = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                scheduler.run("app", 5_000) { entered.countDown(); release.await(); 0 }
            }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            val queued = (1..30).map { n -> async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { scheduler.run("app", 50) { n } } }
            try {
                assertNull(kotlinx.coroutines.withTimeoutOrNull(150) { queued.last().await() })
                release.countDown()
                assertEquals((1..30).toList(), withTimeout(5_000) { queued.map { it.await().getOrThrow() } })
            } finally { release.countDown() }
            assertEquals(0, first.await().getOrThrow())
        }
    }

    @Test fun `visible work precedes prevalidation and an abandoned queued request is never read`() = runBlocking {
        ArtworkScheduler().use { scheduler ->
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val order = java.util.concurrent.CopyOnWriteArrayList<String>()
            val first = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { scheduler.run("app", 5_000) { entered.countDown(); release.await() } }
            assertTrue(entered.await(2, TimeUnit.SECONDS))
            val ahead = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { scheduler.run("app", 1_000, priority = false) { order += "prevalidation" } }
            val cancelled = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { scheduler.run("app", 1_000) { error("Abandoned work started") } }
            val visible = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { scheduler.run("app", 1_000) { order += "visible" } }
            cancelled.cancel()
            cancelled.join()
            release.countDown()
            withTimeout(5_000) { first.await(); ahead.await().getOrThrow(); visible.await().getOrThrow() }
            assertEquals(listOf("visible", "prevalidation"), order)
        }
    }

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
                assertTrue(again.exceptionOrNull() is ArtworkBusyException)
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
