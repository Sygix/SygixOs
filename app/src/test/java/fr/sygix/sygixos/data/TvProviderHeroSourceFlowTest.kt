/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.media.tv.TvContract
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowContentResolver

// Fournisseur TV factice : état mutable pour simuler les publications des apps sources.
private class FakeTvProvider : ContentProvider() {

    val previewRows = mutableListOf<Array<String>>()
    val watchNextRows = mutableListOf<Array<String>>()

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(arrayOf(TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME, TvContract.PreviewPrograms.COLUMN_BROWSABLE)).apply {
        when (uri.lastPathSegment) {
            "preview_program" -> previewRows
            else -> watchNextRows
        }.forEach { addRow(it) }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

@RunWith(AndroidJUnit4::class)
class TvProviderHeroSourceFlowTest {

    @Test
    fun `programCountsFlow emits initial counts then re-queries on content change`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = FakeTvProvider()
        ShadowContentResolver.registerProviderInternal("android.media.tv", provider)
        provider.previewRows.add(arrayOf("com.a", "1"))
        val source = TvProviderHeroSource(context)

        val emissions = mutableListOf<Map<String, Int>>()
        withTimeout(10_000) {
            val job = launch {
                source.programCountsFlow(debounceMillis = 1).take(2).collect { emissions.add(it) }
            }
            while (emissions.isEmpty()) delay(5)
            assertEquals(mapOf("com.a" to 1), emissions[0])

            provider.previewRows.add(arrayOf("com.b", "1"))
            context.contentResolver.notifyChange(TvContract.PreviewPrograms.CONTENT_URI, null)
            while (emissions.size < 2) delay(5)
            job.join()
        }
        assertEquals(mapOf("com.a" to 1, "com.b" to 1), emissions[1])
    }
}
