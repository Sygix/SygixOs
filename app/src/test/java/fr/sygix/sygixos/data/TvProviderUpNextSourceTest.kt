/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.OperationCanceledException
import android.database.MatrixCursor
import android.media.tv.TvContract.WatchNextPrograms as W
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.domain.UpNextFeed
import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.model.UpNextContentType
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.robolectric.Shadows.shadowOf
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
class TvProviderUpNextSourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val source = TvProviderUpNextSource(context)
    private val row: Map<String, Any?> = mapOf(
        W._ID to 1L, W.COLUMN_PACKAGE_NAME to "app", W.COLUMN_TITLE to "Série",
        W.COLUMN_EPISODE_TITLE to "Épisode", W.COLUMN_SEASON_DISPLAY_NUMBER to "1",
        W.COLUMN_EPISODE_DISPLAY_NUMBER to "2", W.COLUMN_TYPE to W.TYPE_TV_EPISODE,
        W.COLUMN_BROWSABLE to 1, W.COLUMN_WATCH_NEXT_TYPE to W.WATCH_NEXT_TYPE_CONTINUE,
        W.COLUMN_POSTER_ART_URI to "content://app/poster", W.COLUMN_POSTER_ART_ASPECT_RATIO to W.ASPECT_RATIO_2_3,
        W.COLUMN_THUMBNAIL_URI to "content://app/thumbnail", W.COLUMN_LAST_PLAYBACK_POSITION_MILLIS to 40L,
        W.COLUMN_DURATION_MILLIS to 100L, W.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS to 500L,
        W.COLUMN_INTENT_URI to "intent://app/item", W.COLUMN_INTERNAL_PROVIDER_ID to "internal",
        W.COLUMN_CONTENT_ID to "content", W.COLUMN_RELEASE_DATE to "1982-10-10",
    )

    private fun cursor(values: Map<String, Any?> = row): MatrixCursor =
        MatrixCursor(values.keys.toTypedArray()).apply { addRow(values.values.toTypedArray()); moveToFirst() }

    @Test
    fun `every optional column can be missing individually without rejecting item except title`() {
        for (column in TvProviderUpNextSource.PROJECTION.filter { it != W._ID && it != W.COLUMN_PACKAGE_NAME }) {
            val actual = cursor(row - column).use(source::mapRow)
            assertNotNull(column, actual)
        }
    }

    @Test
    fun `mapping keeps all metadata and prefers thumbnail to portrait poster`() {
        val actual = cursor().use(source::mapRow)!!
        assertEquals(1L, actual.id)
        assertEquals("Série", actual.displayTitle)
        assertEquals("Épisode", actual.episodeTitle)
        assertEquals("1", actual.season)
        assertEquals("2", actual.episode)
        assertEquals(UpNextContentType.EPISODE, actual.type)
        assertEquals(ProgramKind.CONTINUE, actual.watchNextType)
        assertEquals(0.4f, actual.progress)
        assertEquals(500L, actual.engagement)
        assertEquals(1982, actual.year)
        assertEquals("internal", actual.internalProviderId)
        assertEquals("content", actual.contentId)
        assertEquals("content://app/thumbnail", actual.imageUrl)
        assertFalse(actual.portrait)
        assertEquals("intent://app/item", actual.sources.single().intentUri)
        assertEquals("app", actual.source.label)
        assertTrue(actual.externalIds.isEmpty())
    }

    @Test
    fun `type inference requires both season and episode and published episode overrides absence`() {
        val absent = row - W.COLUMN_TYPE
        assertEquals(UpNextContentType.EPISODE, cursor(absent).use(source::mapRow)?.type)
        for (values in listOf(absent - W.COLUMN_SEASON_DISPLAY_NUMBER, absent - W.COLUMN_EPISODE_DISPLAY_NUMBER, absent - W.COLUMN_SEASON_DISPLAY_NUMBER - W.COLUMN_EPISODE_DISPLAY_NUMBER)) {
            assertEquals(UpNextContentType.MOVIE, cursor(values).use(source::mapRow)?.type)
        }
        assertEquals(UpNextContentType.EPISODE, cursor(row - W.COLUMN_SEASON_DISPLAY_NUMBER - W.COLUMN_EPISODE_DISPLAY_NUMBER).use(source::mapRow)?.type)
        assertNull(cursor(row + (W.COLUMN_TYPE to W.TYPE_CLIP)).use(source::mapRow))
    }

    @Test
    fun `blank missing and unknown fields degrade without excluding item`() {
        assertNull(cursor(row + (W.COLUMN_TITLE to "  ") - W.COLUMN_EPISODE_TITLE).use(source::mapRow))
        assertNull(cursor(row + (W.COLUMN_BROWSABLE to 0)).use(source::mapRow))
        assertNotNull(cursor(row - W.COLUMN_BROWSABLE).use(source::mapRow))
        for (watch in listOf(null, -9, 99)) {
            assertEquals(ProgramKind.WATCH_NEXT, cursor(row + (W.COLUMN_WATCH_NEXT_TYPE to watch)).use(source::mapRow)?.watchNextType)
        }
        assertNull(cursor(row - W.COLUMN_DURATION_MILLIS).use(source::mapRow)?.progress)
        assertNull(cursor(row - W.COLUMN_LAST_PLAYBACK_POSITION_MILLIS).use(source::mapRow)?.progress)
        assertEquals(0L, cursor(row - W.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS).use(source::mapRow)?.engagement)
        val noImage = cursor(row - W.COLUMN_POSTER_ART_URI - W.COLUMN_THUMBNAIL_URI).use(source::mapRow)!!
        assertNull(noImage.imageUrl)
        val portrait = cursor(row - W.COLUMN_THUMBNAIL_URI).use(source::mapRow)!!
        assertTrue(portrait.portrait)
    }

    @Test
    fun `load distinguishes successful empty from denied absent and timeout`() = runBlocking {
        assertEquals(emptyList<Any>(), TvProviderUpNextSource(context, query = { MatrixCursor(W.COLUMN_TITLE.let { arrayOf(it) }) }).load().getOrThrow())
        assertTrue(TvProviderUpNextSource(context, query = { null }).load().isFailure)
        assertTrue(TvProviderUpNextSource(context, query = { throw SecurityException("denied") }).load().exceptionOrNull() is SecurityException)
        val latch = CountDownLatch(1)
        assertTrue(TvProviderUpNextSource(context, timeoutMillis = 50, query = { latch.await(); null }).load().exceptionOrNull() is TimeoutCancellationException)
    }

    @Test
    fun `load runs query off main thread closes cursor and only reads Watch Next`() = runBlocking {
        val c = cursor()
        c.moveToPosition(-1)
        val caller = Thread.currentThread()
        var queryThread: Thread? = null
        val actual = TvProviderUpNextSource(context, query = { queryThread = Thread.currentThread(); c }).load().getOrThrow()
        assertEquals(1, actual.size)
        assertNotEquals(caller, queryThread)
        assertTrue(c.isClosed)
    }

    @Test
    fun `inferred episode merges with a published episode from another app at level 3`() {
        val published = cursor(row + (W.COLUMN_PACKAGE_NAME to "app.a") + (W._ID to 1L)).use(source::mapRow)!!
        val inferred = cursor(row - W.COLUMN_TYPE + (W.COLUMN_PACKAGE_NAME to "app.b") + (W._ID to 2L) + (W.COLUMN_SEASON_DISPLAY_NUMBER to "01") + (W.COLUMN_WATCH_NEXT_TYPE to W.WATCH_NEXT_TYPE_NEXT)).use(source::mapRow)!!
        assertEquals(UpNextContentType.EPISODE, inferred.type)
        val merged = UpNextFeed.build(listOf(inferred, published)).single()
        assertEquals(1L, merged.id)
        assertEquals(listOf("app.a", "app.b"), merged.sources.map { it.packageName })
    }

    @Test
    fun `inferred movie never merges with a published episode of the same title`() {
        val published = cursor(row + (W.COLUMN_PACKAGE_NAME to "app.a") + (W._ID to 1L)).use(source::mapRow)!!
        val inferred = cursor(row - W.COLUMN_TYPE - W.COLUMN_EPISODE_DISPLAY_NUMBER + (W.COLUMN_PACKAGE_NAME to "app.b") + (W._ID to 2L)).use(source::mapRow)!!
        assertEquals(UpNextContentType.MOVIE, inferred.type)
        assertEquals(2, UpNextFeed.build(listOf(inferred, published)).size)
    }

    @Test
    fun `timeout cancels the provider query through its cancellation signal`() = runBlocking {
        var cancelled = false
        val slow = TvProviderUpNextSource(context, timeoutMillis = 50, query = { signal ->
            val released = CountDownLatch(1)
            signal.setOnCancelListener { released.countDown() }
            while (released.count > 0) {
                try { released.await() } catch (_: InterruptedException) { }
            }
            cancelled = signal.isCanceled
            throw OperationCanceledException()
        })
        val result = withTimeout(5_000) { slow.load() }
        assertTrue(result.isFailure)
        assertTrue(cancelled)
    }

    @Test
    fun `identical loads produce equal items with the same cached icon and label`() = runBlocking {
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            packageName = "app"
            applicationInfo = ApplicationInfo().apply { packageName = "app"; nonLocalizedLabel = "App" }
        })
        val provider = TvProviderUpNextSource(context, query = { cursor().apply { moveToPosition(-1) } })
        val first = provider.load().getOrThrow()
        val second = provider.load().getOrThrow()
        assertNotNull(first.single().source.icon)
        assertEquals("App", first.single().source.label)
        assertEquals(first, second)
    }

    @Test
    fun `episode without title keeps its episode title as displayed title while a program with neither title is excluded`() {
        val fallback = cursor(row - W.COLUMN_TITLE).use(source::mapRow)!!
        assertEquals("Épisode", fallback.displayTitle)
        assertNull(fallback.seriesTitle)
        assertTrue(fallback.titleFromEpisode)
        assertEquals(UpNextContentType.EPISODE, cursor(row - W.COLUMN_TITLE - W.COLUMN_TYPE).use(source::mapRow)?.type)
        assertNull(cursor(row - W.COLUMN_TITLE - W.COLUMN_EPISODE_TITLE).use(source::mapRow))
        assertNull(cursor(row - W.COLUMN_TITLE + (W.COLUMN_TYPE to W.TYPE_MOVIE)).use(source::mapRow))
        val other = cursor(row - W.COLUMN_TITLE + (W.COLUMN_PACKAGE_NAME to "app.b") + (W._ID to 2L) + (W.COLUMN_SEASON_DISPLAY_NUMBER to "3")).use(source::mapRow)!!
        val titled = cursor(row + (W.COLUMN_TITLE to "épisode") + (W.COLUMN_PACKAGE_NAME to "app.c") + (W._ID to 3L) + (W.COLUMN_SEASON_DISPLAY_NUMBER to "3")).use(source::mapRow)!!
        assertEquals(1, UpNextFeed.build(listOf(other, titled)).size)
    }
}
