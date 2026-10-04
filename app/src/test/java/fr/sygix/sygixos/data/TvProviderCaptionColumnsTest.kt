/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.database.MatrixCursor
import android.media.tv.TvContract.PreviewPrograms
import android.media.tv.TvContract.WatchNextPrograms
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.ProgramKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TvProviderCaptionColumnsTest {

    private val source = TvProviderHeroSource(ApplicationProvider.getApplicationContext<Context>())

    private val watchNextColumns = arrayOf(
        WatchNextPrograms._ID,
        WatchNextPrograms.COLUMN_PACKAGE_NAME,
        WatchNextPrograms.COLUMN_TITLE,
        WatchNextPrograms.COLUMN_POSTER_ART_URI,
        WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
        WatchNextPrograms.COLUMN_DURATION_MILLIS,
        WatchNextPrograms.COLUMN_WATCH_NEXT_TYPE,
        WatchNextPrograms.COLUMN_SEASON_DISPLAY_NUMBER,
        WatchNextPrograms.COLUMN_EPISODE_DISPLAY_NUMBER,
    )

    private fun watchNext(type: Int?, season: String? = null, episode: String? = null, position: Long? = null, duration: Long? = null) =
        MatrixCursor(watchNextColumns).apply {
            addRow(arrayOf<Any?>(1L, "com.example.player", "Titre fictif", "https://example.invalid/p.jpg", position, duration, type, season, episode))
            moveToFirst()
        }

    @Test
    fun `watch next type gives the program kind`() {
        assertEquals(ProgramKind.CONTINUE, source.mapWatchNextRow(watchNext(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE))?.kind)
        assertEquals(ProgramKind.NEXT, source.mapWatchNextRow(watchNext(WatchNextPrograms.WATCH_NEXT_TYPE_NEXT))?.kind)
        assertEquals(ProgramKind.NEW, source.mapWatchNextRow(watchNext(WatchNextPrograms.WATCH_NEXT_TYPE_NEW))?.kind)
        assertEquals(ProgramKind.WATCHLIST, source.mapWatchNextRow(watchNext(WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST))?.kind)
    }

    @Test
    fun `missing or unknown watch next type is a watch next program without type`() {
        assertEquals(ProgramKind.WATCH_NEXT, source.mapWatchNextRow(watchNext(null))?.kind)
        assertEquals(ProgramKind.WATCH_NEXT, source.mapWatchNextRow(watchNext(42))?.kind)
    }

    @Test
    fun `season, episode, duration and position are read when published`() {
        val item = source.mapWatchNextRow(
            watchNext(WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE, season = "2", episode = "5", position = 1_020_000L, duration = 2_520_000L),
        )
        assertEquals("2", item?.season)
        assertEquals("5", item?.episode)
        assertEquals(2_520_000L, item?.durationMillis)
        assertEquals(1_020_000L, item?.positionMillis)
    }

    @Test
    fun `missing season, episode or duration stay absent`() {
        val item = source.mapWatchNextRow(watchNext(WatchNextPrograms.WATCH_NEXT_TYPE_NEXT, season = " ", episode = null, duration = 0L))
        assertNull(item?.season)
        assertNull(item?.episode)
        assertNull(item?.durationMillis)
        assertNull(item?.positionMillis)
    }

    @Test
    fun `preview program is a featured program with its season, episode and duration`() {
        val columns = arrayOf(
            PreviewPrograms._ID,
            PreviewPrograms.COLUMN_PACKAGE_NAME,
            PreviewPrograms.COLUMN_TITLE,
            PreviewPrograms.COLUMN_POSTER_ART_URI,
            PreviewPrograms.COLUMN_DURATION_MILLIS,
            PreviewPrograms.COLUMN_SEASON_DISPLAY_NUMBER,
            PreviewPrograms.COLUMN_EPISODE_DISPLAY_NUMBER,
        )
        val cursor = MatrixCursor(columns).apply {
            addRow(arrayOf<Any?>(3L, "com.example.player", "Titre fictif", "https://example.invalid/p.jpg", 3_600_000L, "1", "3"))
            moveToFirst()
        }
        val item = source.mapPreviewRow(cursor)
        assertEquals(ProgramKind.FEATURED, item?.kind)
        assertEquals("1", item?.season)
        assertEquals("3", item?.episode)
        assertEquals(3_600_000L, item?.durationMillis)
    }
}
