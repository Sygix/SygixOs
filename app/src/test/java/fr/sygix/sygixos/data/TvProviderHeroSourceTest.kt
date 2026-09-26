/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.database.MatrixCursor
import android.media.tv.TvContract
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.HeroItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TvProviderHeroSourceTest {

    private val source = TvProviderHeroSource(ApplicationProvider.getApplicationContext<Context>())

    private val watchNextColumns = arrayOf(
        TvContract.WatchNextPrograms._ID,
        TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME,
        TvContract.WatchNextPrograms.COLUMN_TITLE,
        TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
        TvContract.WatchNextPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
        TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI,
        TvContract.WatchNextPrograms.COLUMN_PREVIEW_VIDEO_URI,
        TvContract.WatchNextPrograms.COLUMN_INTENT_URI,
        TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
        TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS,
        TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
        TvContract.WatchNextPrograms.COLUMN_BROWSABLE,
    )

    private val previewColumns = arrayOf(
        TvContract.PreviewPrograms._ID,
        TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME,
        TvContract.PreviewPrograms.COLUMN_TITLE,
        TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
        TvContract.PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO,
        TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI,
        TvContract.PreviewPrograms.COLUMN_PREVIEW_VIDEO_URI,
        TvContract.PreviewPrograms.COLUMN_INTENT_URI,
        TvContract.PreviewPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
        TvContract.PreviewPrograms.COLUMN_DURATION_MILLIS,
        TvContract.PreviewPrograms.COLUMN_BROWSABLE,
    )

    private fun cursor(columns: Array<String>, vararg row: Any?): MatrixCursor =
        MatrixCursor(columns).apply { addRow(row); moveToFirst() }

    @Test
    fun `watch next row maps to hero item with progress, intent and package`() {
        val c = cursor(
            watchNextColumns,
            1L, "org.jellyfin.androidtv", "Film X", "https://srv/poster.jpg", TvContract.PreviewPrograms.ASPECT_RATIO_16_9, null, null, "intent://detail/1", 30L, 100L, 500L, 1,
        )
        val item = source.mapWatchNextRow(c)
        assertEquals("watchnext-1", item?.id)
        assertEquals("Film X", item?.title)
        assertEquals("https://srv/poster.jpg", item?.imageUrl)
        assertEquals("org.jellyfin.androidtv", item?.sourcePackage)
        assertEquals(0.3f, item?.progress)
        assertEquals("intent://detail/1", item?.launchUri)
        assertEquals(500L, item?.engagement)
        assertNull(item?.videoUrl)
    }

    @Test
    fun `portrait poster prefers landscape thumbnail for the fullscreen hero`() {
        val c = cursor(
            watchNextColumns,
            2L, "app.pkg", "Série Y", "https://srv/portrait.jpg", 5 /* ASPECT_RATIO_MOVIE_POSTER */, "https://srv/thumb16x9.jpg", null, "intent://detail/2", 0L, 0L, 1L, 1,
        )
        val item = source.mapWatchNextRow(c)
        assertEquals("https://srv/thumb16x9.jpg", item?.imageUrl)
        assertNull(item?.progress)
    }

    @Test
    fun `portrait poster without thumbnail is kept`() {
        val c = cursor(
            watchNextColumns,
            3L, "app.pkg", "Film Z", "https://srv/portrait.jpg", TvContract.PreviewPrograms.ASPECT_RATIO_2_3, null, null, null, 0L, 0L, 1L, 1,
        )
        assertEquals("https://srv/portrait.jpg", source.mapWatchNextRow(c)?.imageUrl)
    }

    @Test
    fun `row without any visual is skipped`() {
        val c = cursor(watchNextColumns, 4L, "app.pkg", "Vide", null, null, null, null, "intent://x", 0L, 0L, 1L, 1)
        assertNull(source.mapWatchNextRow(c))
    }

    @Test
    fun `non browsable row is skipped`() {
        val c = cursor(watchNextColumns, 5L, "app.pkg", "Caché", "https://srv/p.jpg", null, null, null, null, 0L, 0L, 1L, 0)
        assertNull(source.mapWatchNextRow(c))
    }

    @Test
    fun `preview row maps package, preview video and progress`() {
        val c = cursor(
            previewColumns,
            7L, "com.netflix.ninja", "Nouveauté", "https://cdn/p.jpg", TvContract.PreviewPrograms.ASPECT_RATIO_16_9, null, "https://cdn/preview.mp4", "intent://n/7", 50L, 200L, 1,
        )
        val item = source.mapPreviewRow(c)
        assertEquals("preview-7", item?.id)
        assertEquals("com.netflix.ninja", item?.sourcePackage)
        assertEquals("https://cdn/preview.mp4", item?.videoUrl)
        assertEquals("https://cdn/p.jpg", item?.imageUrl)
        assertEquals(0.25f, item?.progress)
    }

    @Test
    fun `preview row with only a video is kept`() {
        val c = cursor(previewColumns, 8L, "app.pkg", "Trailer", null, null, null, "https://cdn/t.mp4", null, 0L, 0L, 1)
        assertEquals("https://cdn/t.mp4", source.mapPreviewRow(c)?.videoUrl)
    }

    @Test
    fun `tv input preview uri is not a playable video, poster is kept`() {
        val c = cursor(
            previewColumns,
            9L, "org.videolan.vlc", "S5:E4", "https://srv/thumb.jpg", null, null, "content://android.media.tv/preview_program/0?input=org.videolan.vlc%2F.PreviewVideoInputService", null, 0L, 0L, 1,
        )
        val item = source.mapPreviewRow(c)
        assertNull(item?.videoUrl)
        assertEquals("https://srv/thumb.jpg", item?.imageUrl)
    }

    @Test
    fun `app content provider preview video is kept`() {
        val c = cursor(previewColumns, 10L, "app.pkg", "Clip", null, null, null, "content://app.pkg.provider/clip/1", null, 0L, 0L, 1)
        assertEquals("content://app.pkg.provider/clip/1", source.mapPreviewRow(c)?.videoUrl)
    }

    @Test
    fun `progress ratio guards against zero duration and completion`() {
        assertNull(HeroOrdering.progressRatio(0L, 0L))
        assertNull(HeroOrdering.progressRatio(100L, 100L))
        assertNull(HeroOrdering.progressRatio(10L, 0L))
        assertEquals(0.5f, HeroOrdering.progressRatio(50L, 100L))
    }

    @Test
    fun `ordering puts in-progress items first then by most recent engagement`() {
        val items = listOf(
            HeroItem("a", "récent", engagement = 900L),
            HeroItem("b", "en cours", progress = 0.4f, engagement = 100L),
            HeroItem("c", "ancien", engagement = 300L),
        )
        val sorted = HeroOrdering.sort(items)
        assertEquals(listOf("b", "a", "c"), sorted.map { it.id })
        assertTrue(sorted.first().progress != null)
    }
}
