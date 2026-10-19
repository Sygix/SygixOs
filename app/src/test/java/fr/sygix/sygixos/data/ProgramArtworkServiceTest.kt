/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import coil.disk.DiskCache
import fr.sygix.sygixos.domain.ArtworkAge
import fr.sygix.sygixos.domain.ArtworkDimensions
import fr.sygix.sygixos.domain.ArtworkPolicy
import fr.sygix.sygixos.model.ProgramArtwork
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProgramArtworkServiceTest {
    @Test fun `leaving one destination does not cancel a source still requested by another`() = runBlocking {
        val reads = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        ProgramArtworkService(store, ArtworkTransport { _, _ -> reads.incrementAndGet(); entered.countDown(); release.await(); image },
            clock = { now }, diskDispatcher = kotlinx.coroutines.Dispatchers.Unconfined).use { service ->
            val first = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { service.load(source, ArtworkPolicy.Hero) }
            val second = async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) { service.load(source, ArtworkPolicy.Card(396)) }
            try {
                assertTrue(entered.await(2, TimeUnit.SECONDS))
                first.cancel()
                first.join()
                release.countDown()
                assertTrue(withTimeout(3_000) { second.await() } is ArtworkResult.Ready)
                assertEquals(1, reads.get())
            } finally { release.countDown() }
        }
    }

    @Test fun `a refreshed version rejected by the decoder restores the previous bytes and original date`() = runBlocking {
        var bytes = image
        ProgramArtworkService(store, ArtworkTransport { _, _ -> bytes }, clock = { now }).use { service ->
            val original = (service.load(source) as ArtworkResult.Ready).artwork
            now += ArtworkAge.FRESH_MS
            bytes = image.copy(bytes = byteArrayOf(4, 5, 6))
            val before = service.changes.value
            assertArrayEquals(original.bytes, (service.load(source) as ArtworkResult.Ready).artwork.bytes)
            withTimeout(3_000) { service.changes.first { it > before } }
            val updated = (service.load(source) as ArtworkResult.Ready).artwork
            assertNotEquals(original.version, updated.version)
            val restored = service.decodeFailed(source, updated)!!
            assertArrayEquals(original.bytes, restored.bytes)
            assertEquals(original.fetchedAt, restored.fetchedAt)
            assertArrayEquals(original.bytes, (service.load(source) as ArtworkResult.Ready).artwork.bytes)
        }
    }

    @Test fun `same title movies do not share an association while explicitly shared episode artwork does`() = runBlocking {
        val reads = AtomicInteger()
        ProgramArtworkService(store, ArtworkTransport { _, _ -> reads.incrementAndGet(); image }, clock = { now }).use { service ->
            val first = fr.sygix.sygixos.model.HeroItem("watchnext-1", "Same title", imageUrl = source.candidates.first(), sourcePackage = "app")
            val other = first.copy(id = "watchnext-2")
            assertTrue(service.load(first.artwork) is ArtworkResult.Ready)
            assertTrue(service.load(other.artwork) is ArtworkResult.Ready)
            assertEquals(2, reads.get())
            val episode = first.copy(id = "watchnext-3", season = "1", episode = "1", episodeArtwork = true)
            assertTrue(service.load(episode.artwork) is ArtworkResult.Ready)
            assertTrue(service.load(episode.copy(id = "watchnext-4", episode = "2").artwork) is ArtworkResult.Ready)
            assertEquals(3, reads.get())
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test fun `already requested artwork notifies at freshness and retention deadlines without renewing its date`() = runBlocking {
        val scheduler = kotlinx.coroutines.test.TestCoroutineScheduler()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.test.UnconfinedTestDispatcher(scheduler))
        var offline = false
        ProgramArtworkService(store, ArtworkTransport { _, _ -> if (offline) throw IOException() else image },
            clock = { scheduler.currentTime + 1_000 }, scope = scope).use { service ->
            assertTrue(service.load(source) is ArtworkResult.Ready)
            val initial = service.changes.value
            offline = true
            scheduler.advanceTimeBy(ArtworkAge.FRESH_MS - 1)
            scheduler.runCurrent()
            assertEquals(initial, service.changes.value)
            scheduler.advanceTimeBy(1)
            scheduler.runCurrent()
            assertTrue(service.changes.value > initial)
            assertEquals(1_000L, (service.load(source) as ArtworkResult.Ready).artwork.fetchedAt)
            scheduler.advanceTimeBy(ArtworkAge.RETAIN_MS - ArtworkAge.FRESH_MS)
            scheduler.runCurrent()
            assertTrue(service.load(source) is ArtworkResult.Failed)
        }
    }

    @Test fun `late physical result after a deadline never commits source bytes`() = runBlocking {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val exited = CountDownLatch(1)
        ProgramArtworkService(store, ArtworkTransport { _, _ ->
            entered.countDown()
            try { release.await(); image } finally { exited.countDown() }
        }, clock = { now }, timeoutMillis = 100).use { service ->
            try {
                assertEquals(ArtworkResult.Failed(ArtworkFailure.TIMEOUT), service.load(source))
                assertTrue(entered.await(2, TimeUnit.SECONDS))
            } finally { release.countDown() }
            assertTrue(exited.await(2, TimeUnit.SECONDS))
        }
        ProgramArtworkService(store, ArtworkTransport { _, _ -> throw IOException() }, clock = { now }).use { service ->
            assertEquals(ArtworkResult.Failed(ArtworkFailure.TRANSPORT), service.load(source))
        }
    }

    @get:Rule val folder = TemporaryFolder()
    private val source = ProgramArtwork("app", listOf("content://artwork/poster"), "film-one")
    private val disk by lazy { DiskCache.Builder().directory(folder.newFolder()).build() }
    private val store by lazy { ArtworkDiskStore(disk) }
    private var now = 1_000L
    private val image = ArtworkBytes(byteArrayOf(1, 2, 3), ArtworkDimensions(640, 360))

    @Test fun `three destinations share bytes and dimensions but not verdicts`() = runBlocking {
        val reads = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        ProgramArtworkService(store, ArtworkTransport { _, _ -> reads.incrementAndGet(); entered.countDown(); release.await(); image }, clock = { now }).use { service ->
            val hero = async { service.load(source, ArtworkPolicy.Hero) }
            val card = async { service.load(source, ArtworkPolicy.Card(396)) }
            val shelf = async { service.load(source, ArtworkPolicy.Shelf(1600)) }
            val ready = async(kotlinx.coroutines.Dispatchers.IO) { assertTrue(entered.await(2, TimeUnit.SECONDS)); release.countDown() }
            try {
                ready.await()
                assertTrue(withTimeout(3_000) { hero.await() } is ArtworkResult.Ready)
                assertTrue(card.await() is ArtworkResult.Ready)
                assertEquals(ArtworkResult.Failed(ArtworkFailure.QUALITY), shelf.await())
                assertEquals(1, reads.get())
            } finally { release.countDown() }
        }
    }

    @Test fun `recreated service serves content bytes offline without renewing dates`() = runBlocking {
        ProgramArtworkService(store, ArtworkTransport { _, _ -> image }, clock = { now }).use { service ->
            assertTrue(service.load(source) is ArtworkResult.Ready)
        }
        val fetched = now
        now += ArtworkAge.FRESH_MS - 1
        val reads = AtomicInteger()
        ProgramArtworkService(store, ArtworkTransport { _, _ -> reads.incrementAndGet(); throw IOException() }, clock = { now }).use { service ->
            val cached = service.load(source) as ArtworkResult.Ready
            assertArrayEquals(image.bytes, cached.artwork.bytes)
            assertEquals(fetched, cached.artwork.fetchedAt)
            assertEquals(0, reads.get())
            now = fetched + ArtworkAge.RETAIN_MS
            assertEquals(ArtworkResult.Failed(ArtworkFailure.TRANSPORT), service.load(source))
            assertEquals(1, reads.get())
        }
    }

    @Test fun `failed refresh preserves stale bytes while independent content cannot inherit them`() = runBlocking {
        ProgramArtworkService(store, ArtworkTransport { _, _ -> image }, clock = { now }).use { service -> service.load(source) }
        val fetched = now
        now += ArtworkAge.FRESH_MS
        ProgramArtworkService(store, ArtworkTransport { _, _ -> throw IOException() }, clock = { now }).use { service ->
            assertEquals(fetched, (service.load(source) as ArtworkResult.Ready).artwork.fetchedAt)
            assertEquals(ArtworkResult.Failed(ArtworkFailure.TRANSPORT), service.load(source.copy(identity = "film-two")))
            assertEquals(fetched, (service.load(source) as ArtworkResult.Ready).artwork.fetchedAt)
        }
    }

    @Test fun `alternative survives rejection of the preferred candidate`() = runBlocking {
        val reads = mutableListOf<String>()
        ProgramArtworkService(store, ArtworkTransport { uri, _ ->
            synchronized(reads) { reads += uri }
            if (uri.endsWith("poster")) image.copy(dimensions = ArtworkDimensions(200, 100)) else image
        }, clock = { now }).use { service ->
            val alternatives = source.copy(candidates = source.candidates + "content://artwork/thumbnail")
            assertTrue(service.load(alternatives, ArtworkPolicy.Card(396)) is ArtworkResult.Ready)
            assertEquals(alternatives.candidates, reads)
            assertTrue(service.load(alternatives, ArtworkPolicy.Card(396)) is ArtworkResult.Ready)
            assertEquals(2, reads.size)
        }
    }
}
