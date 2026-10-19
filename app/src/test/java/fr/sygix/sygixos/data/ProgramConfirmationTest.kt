/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.database.MatrixCursor
import android.media.tv.TvContract.PreviewPrograms as P
import android.media.tv.TvContract.WatchNextPrograms as W
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.domain.ArtworkAge
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProgramConfirmationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private var now = 1_000L
    private var previewFails = false
    private var watchFails = false
    private var denied = false
    private var present = true
    private var installed = true
    private fun source() = TvProviderHeroSource(context, { now }, { installed }) { uri, projection, _ ->
        if (denied) throw SecurityException()
        if (uri == P.CONTENT_URI && previewFails || uri == W.CONTENT_URI && watchFails) throw java.io.IOException()
        MatrixCursor(projection).apply {
            if (present) addRow(projection.map { column -> when (column) {
                P._ID -> 1L
                P.COLUMN_TITLE -> "Fiction"
                P.COLUMN_PACKAGE_NAME -> "app"
                P.COLUMN_INTENT_URI -> "intent://fiction"
                P.COLUMN_POSTER_ART_URI -> "content://artwork/poster"
                P.COLUMN_THUMBNAIL_URI -> "content://artwork/thumbnail"
                P.COLUMN_BROWSABLE -> 1
                else -> null
            } }.toTypedArray<Any?>())
        }
    }

    @Test fun `table confirmations are independent and expire at exactly twenty four hours`() = runBlocking {
        val source = source()
        assertEquals(2, source.read().items.size)
        previewFails = true
        now += ArtworkAge.FRESH_MS - 1
        assertEquals(2, source.read().items.size)
        now++
        val boundary = source.read()
        assertEquals(listOf("watchnext-1"), boundary.items.map { it.id })
        assertEquals(1, boundary.errors.size)
        watchFails = true
        now += ArtworkAge.FRESH_MS
        assertTrue(source.read().items.isEmpty())
    }

    @Test fun `successful empty withdraws immediately while denial and uninstall do not use a grace period`() = runBlocking {
        val source = source()
        assertEquals(2, source.read().items.size)
        present = false
        assertTrue(source.read().items.isEmpty())
        previewFails = true
        watchFails = true
        assertTrue(source.read().items.isEmpty())
        previewFails = false
        watchFails = false
        present = true
        assertEquals(2, source.read().items.size)
        denied = true
        assertTrue(source.read().items.isEmpty())
        denied = false
        installed = false
        assertTrue(source.read().items.isEmpty())
    }

    @Test fun `poster and thumbnail survive mapping without relying on published ratios`() = runBlocking {
        val mapped = source().read().items.first()
        assertEquals("content://artwork/poster", mapped.posterUrl)
        assertEquals("content://artwork/thumbnail", mapped.thumbnailUrl)
        assertEquals(2, mapped.artwork.candidates.size)
    }
}
