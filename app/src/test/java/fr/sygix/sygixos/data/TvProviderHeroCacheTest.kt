/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.database.MatrixCursor
import android.media.tv.TvContract.PreviewPrograms as P
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.domain.ArtworkAge
import fr.sygix.sygixos.model.HeroItem
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TvProviderHeroCacheTest {

    @get:Rule val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val now = AtomicLong(1_000_000)

    private val fixtures = mutableListOf<ArtworkFixture>()

    @After fun close() = fixtures.forEach { it.close() }

    private inner class RoomStore : HeroProgramSnapshots {
        val repository = ArtworkFixture(folder.newFolder(), ImageBytesReader { _, _ -> error("no image read") }, now = now).also { fixtures += it }.repository
        var writes = 0
        override suspend fun read(): Map<String, List<HeroItem>> = repository.read()
        override suspend fun write(table: String, items: List<HeroItem>) { writes++; repository.write(table, items) }
    }

    private fun preview(projection: Array<String>, vararg titles: String) = MatrixCursor(projection).apply {
        titles.forEachIndexed { index, title ->
            newRow().add(P._ID, index + 1L).add(P.COLUMN_PACKAGE_NAME, "com.example").add(P.COLUMN_TITLE, title)
                .add(P.COLUMN_POSTER_ART_URI, "https://example.invalid/$index.jpg").add(P.COLUMN_BROWSABLE, 1)
        }
    }

    private fun source(store: HeroProgramSnapshots, installed: Boolean = true, onSnapshot: suspend (String, List<HeroItem>) -> Unit = { _, _ -> },
        query: (android.net.Uri, Array<String>) -> MatrixCursor?) = TvProviderHeroSource(context, clock = now::get, installed = { installed },
        onSnapshot = onSnapshot, query = { uri, projection, _ -> query(uri, projection) }, queries = threadedQueries(), io = Dispatchers.Unconfined, store = store)

    @Test fun bothTablesAreReadAtTheSameTime() = runBlocking {
        val entered = CountDownLatch(2)
        val read = source(RoomStore()) { _, projection ->
            entered.countDown()
            check(entered.await(5, TimeUnit.SECONDS)) { "tables read one after the other" }
            preview(projection)
        }.read()
        assertEquals(emptyList<Throwable>(), read.errors)
    }

    @Test fun aNewProcessShowsTheLastConfirmedProgramsUntilTheyExpireOrTheirAppIsGone() = runBlocking {
        val store = RoomStore()
        source(store) { uri, projection -> if (uri == P.CONTENT_URI) preview(projection, "Cached fiction") else preview(projection) }.read()
        val offline: (android.net.Uri, Array<String>) -> MatrixCursor? = { _, _ -> error("provider not queried") }
        assertEquals(listOf("Cached fiction"), source(store, query = offline).cached()?.items?.map { it.title })
        assertNull(source(store, installed = false, query = offline).cached())
        now.addAndGet(ArtworkAge.FRESH_MS)
        assertNull(source(store, query = offline).cached())
    }

    @Test fun anUnchangedConfirmationIsPersistedAgainOnlyOncePerHour() = runBlocking {
        val store = RoomStore()
        val programs = source(store) { uri, projection -> if (uri == P.CONTENT_URI) preview(projection, "Fiction") else preview(projection) }
        programs.read()
        val first = store.writes
        programs.read()
        assertEquals(first, store.writes)
        now.addAndGet(60 * 60_000L)
        programs.read()
        assertEquals(first + 2, store.writes)
        assertEquals(now.get() + ArtworkAge.FRESH_MS, store.read().getValue("preview").single().expiresAt)
    }

    @Test fun aDeniedProviderForgetsThePersistedPrograms() = runBlocking {
        val store = RoomStore()
        source(store) { uri, projection -> if (uri == P.CONTENT_URI) preview(projection, "Fiction") else preview(projection) }.read()
        val denied = source(store) { _, _ -> throw SecurityException("Permission Denial: READ_TV_LISTINGS") }.read()
        assertTrue(denied.items.isEmpty())
        assertEquals(emptyMap<String, List<HeroItem>>(), store.read())
    }

    @Test fun theRoomIndexKeepsEveryDisplayedFieldInPublicationOrder() = runBlocking {
        val item = HeroItem("watchnext-4", "Fiction", imageUrl = "https://example.invalid/i.jpg", sourcePackage = "com.example", launchUri = "intent://4",
            engagement = 12, kind = fr.sygix.sygixos.model.ProgramKind.CONTINUE, season = "2", episode = "5", durationMillis = 100, positionMillis = 40,
            posterUrl = "https://example.invalid/p.jpg", thumbnailUrl = "https://example.invalid/t.jpg", posterAspectRatio = 0, expiresAt = now.get() + 99,
            internalProviderId = "i", contentId = "c", releaseDate = "2026", episodeArtwork = true, seriesId = "s")
        val plain = HeroItem("watchnext-1", "Plain", expiresAt = now.get() + 99)
        val store = RoomStore()
        store.write("watchnext", listOf(item, plain))
        assertEquals(mapOf("watchnext" to listOf(item, plain)), store.read())
        store.write("watchnext", listOf(plain))
        assertEquals(mapOf("watchnext" to listOf(plain)), store.read())
    }
}
