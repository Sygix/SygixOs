/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import coil.disk.DiskCache
import fr.sygix.sygixos.domain.ArtworkAge
import fr.sygix.sygixos.domain.ArtworkPolicy
import fr.sygix.sygixos.model.ProgramArtwork
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

internal fun artworkBytes(width: Int = 800, height: Int = 450, color: Int = android.graphics.Color.BLUE): ByteArray {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.eraseColor(color)
    return ByteArrayOutputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); bitmap.recycle(); output.toByteArray() }
}

internal class ArtworkFixture(directory: java.io.File, val read: ImageBytesReader, val now: AtomicLong = AtomicLong(1_000), localReader: ImageBytesReader? = null,
    timeoutMillis: Long = IMAGE_REQUEST_TIMEOUT_MS, onRendered: (coil.request.SuccessResult) -> Unit = {}) : Closeable {
    val context: Context = ApplicationProvider.getApplicationContext()
    val disk = DiskCache.Builder().directory(directory).build()
    val db = Room.inMemoryDatabaseBuilder(context, ArtworkDatabase::class.java).build()
    val repository = ArtworkRepository(context, db, disk, read, clock = now::get, requestTimeoutMillis = timeoutMillis,
        localReader = localReader ?: LocalProgramImageReader(context.contentResolver))
    val loader = coil.ImageLoader.Builder(context).diskCache(disk).allowHardware(false).eventListener(object : coil.EventListener {
        override fun onSuccess(request: coil.request.ImageRequest, result: coil.request.SuccessResult) {
            if (result.drawable is android.graphics.drawable.BitmapDrawable) onRendered(result)
        }
    }).components {
        add(ProgramArtworkInterceptor(repository))
        add(ProgramImageFetcher.Factory(disk, repository))
    }.build().also { loader -> repository.imageLoader = { loader } }
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    override fun close() { repository.close(); loader.shutdown(); disk.clear() }
}

@RunWith(RobolectricTestRunner::class)
@OptIn(coil.annotation.ExperimentalCoilApi::class)
class ArtworkRepositoryTest {
    @get:Rule val main = fr.sygix.sygixos.MainDispatcherRule()
    @get:Rule val folder = TemporaryFolder()
    private val source = ProgramArtwork("app", listOf("content://fixture/poster"), "film", "preview-1", "film")
    private val pixels by lazy { artworkBytes() }

    @Test fun concurrentPublicationsOfTheSameHashBecomeGenericRegardlessOfTheirCompletionOrder() = runBlocking {
        repeat(6) { order ->
            ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> pixels }).use { fixture ->
                val sources = (1..3).shuffled(kotlin.random.Random(order)).map { n ->
                    source.copy(candidates = listOf("content://fixture/image$n"), identity = "film$n", sharedIdentity = "film$n")
                }
                val jobs = sources.map { item -> async { fixture.repository.resolve(item, ArtworkPolicy.Hero) } }
                withTimeout(5_000) { jobs.forEach { it.await() } }
                assertEquals(3, fixture.db.artwork().images().count { it.state == "generic" })
                sources.forEach { assertNull(fixture.repository.resolve(it, ArtworkPolicy.Hero).first) }
            }
        }
    }

    @Test fun localReadIsClosedOnTimeoutAndTheOtherAppStillProgresses() = runBlocking {
        val entered = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val exited = CountDownLatch(1)
        val closes = AtomicInteger()
        val local = ImageBytesReader { uri, cancellation ->
            val input = if (uri.path == "/blocked") object : java.io.InputStream() {
                override fun read(): Int { entered.countDown(); closed.await(); exited.countDown(); return -1 }
                override fun close() { closes.incrementAndGet(); closed.countDown() }
            } else java.io.ByteArrayInputStream(pixels)
            try { readImageBytes(cancellation.own(input), cancellation) } finally { cancellation.release() }
        }
        ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> error("Local fetch only") }, localReader = local, timeoutMillis = 100).use { f ->
            val blocked = source.copy(candidates = listOf("file:///blocked"))
            assertEquals(ArtworkFailure.TIMEOUT, f.repository.resolve(blocked, ArtworkPolicy.Hero).second)
            assertTrue(entered.await(1, TimeUnit.SECONDS))
            assertTrue(exited.await(1, TimeUnit.SECONDS))
            assertEquals(1, closes.get())
            assertNotNull(f.repository.resolve(blocked.copy(packageName = "other", candidates = listOf("file:///ready")), ArtworkPolicy.Hero).first)
        }
    }

    @Test fun oversizedLocalSourcesAreBoundedAndClosed() = runBlocking {
        val closes = AtomicInteger()
        val local = ImageBytesReader { _, cancellation ->
            val input = object : java.io.ByteArrayInputStream(ByteArray(16 * 1024 * 1024 + 1)) {
                override fun close() { closes.incrementAndGet(); super.close() }
            }
            try { readImageBytes(cancellation.own(input), cancellation) } finally { cancellation.release() }
        }
        ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> error("Local fetch only") }, localReader = local).use { f ->
            assertNull(f.repository.resolve(source.copy(candidates = listOf("file:///oversized")), ArtworkPolicy.Hero).first)
            assertEquals(1, closes.get())
        }
    }

    @Test fun threeDestinationsShareOnePhysicalRead() = runBlocking {
        val reads = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> reads.incrementAndGet(); entered.countDown(); release.await(); pixels }).use { f ->
            val hero = async { f.repository.resolve(source, ArtworkPolicy.Hero) }
            val card = async { f.repository.resolve(source, ArtworkPolicy.Card(396)) }
            val shelf = async { f.repository.resolve(source, ArtworkPolicy.Shelf(1728)) }
            val unblock = async(kotlinx.coroutines.Dispatchers.IO) { assertTrue(entered.await(3, TimeUnit.SECONDS)); release.countDown() }
            try {
                unblock.await()
                assertNotNull(withTimeout(5_000) { hero.await() }.first)
                assertNotNull(card.await().first)
                assertNotNull(shelf.await().first)
                assertEquals(1, reads.get())
            } finally { release.countDown() }
        }
    }

    @Test fun cancellationOfOneSubscriberDoesNotCancelAnother() = runBlocking {
        val reads = AtomicInteger()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> reads.incrementAndGet(); entered.countDown(); release.await(); pixels }).use { f ->
            val first = async { f.repository.resolve(source, ArtworkPolicy.Hero) }
            val second = async { f.repository.resolve(source, ArtworkPolicy.Card(396)) }
            val unblock = async(kotlinx.coroutines.Dispatchers.IO) { assertTrue(entered.await(3, TimeUnit.SECONDS)); first.cancel(); release.countDown() }
            try { unblock.await(); assertNotNull(withTimeout(5_000) { second.await() }.first); assertEquals(1, reads.get()) }
            finally { release.countDown() }
        }
    }

    @Test fun knownUriRequiresReadForNewIdentityButSharedSeriesDoesNot() = runBlocking {
        val reads = AtomicInteger()
        ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> reads.incrementAndGet(); pixels }).use { f ->
            assertNotNull(f.repository.resolve(source, ArtworkPolicy.Hero).first)
            assertNotNull(f.repository.resolve(source.copy(identity = "episode", sharedIdentity = "film"), ArtworkPolicy.Card(396)).first)
            assertEquals(1, reads.get())
            assertNotNull(f.repository.resolve(source.copy(identity = "other", sharedIdentity = "other"), ArtworkPolicy.Hero).first)
            assertEquals(2, reads.get())
        }
    }

    @Test fun aSingleChangedUriDoesNotReloadOtherProgramsOrForegroundImages() = runBlocking {
        val reads = mutableListOf<String>()
        ArtworkFixture(folder.newFolder(), ImageBytesReader { uri, _ -> synchronized(reads) { reads += uri.toString() }; pixels }).use { f ->
            val second = source.copy(candidates = listOf("content://fixture/second"), identity = "second", publicationId = "preview-2", sharedIdentity = "second")
            f.repository.confirm("preview", listOf(source to "full", second to "full2"))
            f.repository.resolve(source, ArtworkPolicy.Hero)
            val original = f.repository.resolve(second, ArtworkPolicy.Hero).first
            val changed = source.copy(candidates = listOf("content://fixture/new"), identity = "new")
            f.repository.confirm("preview", listOf(changed to "changed", second to "progress"))
            f.repository.resolve(changed, ArtworkPolicy.Hero)
            assertEquals(original, f.repository.resolve(second, ArtworkPolicy.Hero).first)
            f.repository.checkExpiry()
            assertEquals(listOf("content://fixture/poster", "content://fixture/second", "content://fixture/new"), reads)
        }
    }

    @Test fun onlyTheExpiredImageIsRefetchedWhileTheFreshNeighborKeepsItsVersionAndSignal() = runBlocking {
        val reads = java.util.concurrent.CopyOnWriteArrayList<String>()
        ArtworkFixture(folder.newFolder(), ImageBytesReader { uri, _ -> reads += uri.toString(); pixels }).use { f ->
            val second = source.copy(candidates = listOf("content://fixture/second"), identity = "neighbor", sharedIdentity = "neighbor")
            val first = f.repository.resolve(source, ArtworkPolicy.Hero).first!!
            f.now.addAndGet(60 * 60_000)
            val neighbor = f.repository.resolve(second, ArtworkPolicy.Hero).first!!
            f.repository.watch(source, ArtworkPolicy.Hero).use {
                f.repository.watch(second, ArtworkPolicy.Hero).use {
                    f.now.set(first.fetchedAt + ArtworkAge.FRESH_MS)
                    f.repository.checkExpiry()
                    withTimeout(5_000) { f.db.artwork().observeImages().first { images -> images.any { it.uri == first.uri && it.fetchedAt > first.fetchedAt } } }
                    assertEquals(neighbor, f.repository.resolve(second, ArtworkPolicy.Hero).first)
                    assertNull(f.repository.signal(second.identity).value)
                    assertEquals(listOf(source.candidates.single(), second.candidates.single(), source.candidates.single()), reads)
                }
            }
        }
    }

    @Test fun ttlRefreshKeepsOldBytesUntilValidatedReplacementAndDoesNotRenewFailedDate() = runBlocking {
        var served = pixels
        ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> served }).use { f ->
            val original = f.repository.resolve(source, ArtworkPolicy.Hero).first!!
            f.repository.watch(source, ArtworkPolicy.Hero).use {
                f.now.addAndGet(ArtworkAge.FRESH_MS)
                served = "error page".toByteArray()
                assertEquals(original.hash, f.repository.resolve(source, ArtworkPolicy.Hero).first?.hash)
                withTimeout(5_000) { f.db.artwork().observeImages().first { images -> images.any { it.retryAt > f.now.get() } } }
                assertEquals(original.fetchedAt, f.db.artwork().image("app", source.candidates.first())?.fetchedAt)
                assertNotNull(f.disk.openSnapshot(source.candidates.first())?.also { it.close() })
                f.now.set(original.fetchedAt + ArtworkAge.RETAIN_MS)
                assertNull(f.repository.resolve(source, ArtworkPolicy.Hero).first)
            }
        }
    }

    @Test fun sameBytesAcrossThreeIdentitiesAreGenericButSeriesEpisodesCountOnce() = runBlocking {
        ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> pixels }).use { f ->
            repeat(2) { n -> assertNotNull(f.repository.resolve(source.copy(identity = "film$n", sharedIdentity = "film$n"), ArtworkPolicy.Hero).first) }
            assertNull(f.repository.resolve(source.copy(identity = "film2", sharedIdentity = "film2"), ArtworkPolicy.Hero).first)
            assertEquals("generic", f.db.artwork().image("app", source.candidates.first())?.state)
            assertEquals(f.now.get() + ArtworkRepository.GENERIC_RETRY_MS, f.db.artwork().image("app", source.candidates.first())?.retryAt)
        }
        ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> pixels }).use { f ->
            repeat(5) { n -> assertNotNull(f.repository.resolve(source.copy(identity = "episode$n", sharedIdentity = "series"), ArtworkPolicy.Hero).first) }
            assertEquals("valid", f.db.artwork().image("app", source.candidates.first())?.state)
        }
    }

    @Test fun externalResourceIsNeverReadAndSmallIconNeverReplacesValidCopy() = runBlocking {
        var served = pixels
        val reads = AtomicInteger()
        ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> reads.incrementAndGet(); served }).use { f ->
            assertNull(f.repository.resolve(source.copy(candidates = listOf("android.resource://other.app/drawable/tile")), ArtworkPolicy.Card(396)).first)
            assertEquals(0, reads.get())
            val original = f.repository.resolve(source, ArtworkPolicy.Hero).first!!
            f.repository.watch(source, ArtworkPolicy.Hero).use {
                served = artworkBytes(48, 48)
                f.now.addAndGet(ArtworkAge.FRESH_MS)
                f.repository.resolve(source, ArtworkPolicy.Hero)
                withTimeout(5_000) { f.db.artwork().observeImages().first { images -> images.any { it.retryAt > f.now.get() } } }
                assertEquals(original.hash, f.repository.resolve(source, ArtworkPolicy.Hero).first?.hash)
                assertEquals(original.fetchedAt, f.db.artwork().image("app", source.candidates.first())?.fetchedAt)
            }
        }
    }

    @Test fun corruptAndErrorResponsesAreNotCommitted() = runBlocking {
        for (bytes in listOf("<html>error</html>".toByteArray(), byteArrayOf(1, 2, 3), pixels.copyOf(pixels.size - 16))) {
            ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> bytes }).use { f ->
                assertNull(f.repository.resolve(source, ArtworkPolicy.Hero).first)
                assertNull(f.disk.openSnapshot(source.candidates.first()))
            }
        }
    }

    @Test fun recreatedIndexServesDiskOfflineWithoutRenewingDimensionsOrDatesAndEvictionFetchesAgain() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val disk = DiskCache.Builder().directory(folder.newFolder()).build()
        val now = AtomicLong(1_000)
        val name = "artwork-persistence"
        val first = ArtworkRepository(context, ArtworkDatabase.create(context, name), disk, ImageBytesReader { _, _ -> pixels }, clock = now::get)
        first.confirm("preview", listOf(source to "full"))
        val original = first.resolve(source, ArtworkPolicy.Hero).first!!
        first.close()
        now.addAndGet(100)
        val reads = AtomicInteger()
        ArtworkRepository(context, ArtworkDatabase.create(context, name), disk, ImageBytesReader { _, _ -> reads.incrementAndGet(); throw java.io.IOException() }, clock = now::get).use { repository ->
            assertEquals(original, repository.resolve(source, ArtworkPolicy.Hero).first)
            assertEquals(0, reads.get())
            disk.remove(source.candidates.first())
            assertNull(repository.resolve(source, ArtworkPolicy.Hero).first)
            assertEquals(1, reads.get())
        }
        disk.clear()
    }

    @Test fun alternativeSurvivesQualityRejection() = runBlocking {
        ArtworkFixture(folder.newFolder(), ImageBytesReader { uri, _ -> if (uri.path == "/poster") artworkBytes(200, 100) else pixels }).use { f ->
            val result = f.repository.resolve(source.copy(candidates = source.candidates + "content://fixture/thumbnail"), ArtworkPolicy.Hero)
            assertEquals("content://fixture/thumbnail", result.first?.uri)
        }
    }
}
