/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.database.MatrixCursor
import android.media.tv.TvContract
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.domain.SettingsOrdering
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowContentResolver

private class FakeTvProvider : ContentProvider() {

    val previewRows = mutableListOf<Array<String>>()
    val watchNextRows = mutableListOf<Array<String>>()
    val denied = mutableSetOf<String>()

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor = MatrixCursor(arrayOf(TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME, TvContract.PreviewPrograms.COLUMN_BROWSABLE)).apply {
        if (uri.lastPathSegment in denied) throw SecurityException("Permission Denial: READ_TV_LISTINGS")
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

@Implements(ContentResolver::class)
class DeniedObserverShadowContentResolver : ShadowContentResolver() {
    @Implementation
    override fun registerContentObserver(uri: Uri, notifyForDescendents: Boolean, observer: ContentObserver) {
        throw SecurityException("Permission Denial: READ_TV_LISTINGS")
    }
}

@RunWith(AndroidJUnit4::class)
class TvProviderHeroSourceFlowTest {

    @Test
    @Config(shadows = [DeniedObserverShadowContentResolver::class])
    fun `programCountsFlow emits counts once and completes when observer registration is denied`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = FakeTvProvider()
        ShadowContentResolver.registerProviderInternal("android.media.tv", provider)
        provider.previewRows.add(arrayOf("com.a", "1"))

        val emissions = withTimeout(10_000) {
            TvProviderHeroSource(context).programCountsFlow(debounceMillis = 1).toList()
        }
        assertEquals(listOf(mapOf("com.a" to 1)), emissions)
    }

    @Test
    fun `programCounts combines channels and Watch Next per package`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = FakeTvProvider()
        ShadowContentResolver.registerProviderInternal("android.media.tv", provider)
        provider.previewRows.add(arrayOf("com.a", "1"))
        provider.watchNextRows.add(arrayOf("com.a", "1"))
        provider.watchNextRows.add(arrayOf("com.w", "1"))
        assertEquals(mapOf("com.a" to 2, "com.w" to 1), TvProviderHeroSource(context).programCounts())
    }

    @Test
    fun `Watch Next notification updates combined counts and cancellation unregisters both URIs`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = FakeTvProvider()
        ShadowContentResolver.registerProviderInternal("android.media.tv", provider)
        val resolver = org.robolectric.Shadows.shadowOf(context.contentResolver)
        val emissions = Channel<Map<String, Int>>(Channel.UNLIMITED)
        val job = launch { TvProviderHeroSource(context).programCountsFlow(debounceMillis = 1).collect { emissions.send(it) } }
        withTimeout(10_000) {
            assertEquals(emptyMap<String, Int>(), emissions.receive())
            val previewObservers = resolver.getContentObservers(TvContract.PreviewPrograms.CONTENT_URI)
            val watchObservers = resolver.getContentObservers(TvContract.WatchNextPrograms.CONTENT_URI)
            assertEquals(1, previewObservers.size)
            assertEquals(1, watchObservers.size)
            assertEquals(previewObservers.single(), watchObservers.single())
            provider.watchNextRows.add(arrayOf("com.watchonly", "1"))
            context.contentResolver.notifyChange(TvContract.WatchNextPrograms.CONTENT_URI, null)
            assertEquals(mapOf("com.watchonly" to 1), emissions.receive())
            job.cancelAndJoin()
        }
        assertEquals(0, resolver.getContentObservers(TvContract.PreviewPrograms.CONTENT_URI).size)
        assertEquals(0, resolver.getContentObservers(TvContract.WatchNextPrograms.CONTENT_URI).size)
    }

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

    @Test
    fun `programCounts counts Preview only apps and an empty provider`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = FakeTvProvider()
        ShadowContentResolver.registerProviderInternal("android.media.tv", provider)
        val source = TvProviderHeroSource(context)
        assertEquals(emptyMap<String, Int>(), source.programCounts())
        provider.previewRows.add(arrayOf("com.preview", "1"))
        provider.previewRows.add(arrayOf("com.preview", "1"))
        provider.previewRows.add(arrayOf("com.preview", "0"))
        assertEquals(mapOf("com.preview" to 2), source.programCounts())
    }

    @Test
    fun `programCounts survives SecurityException on one or both tables`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = FakeTvProvider()
        ShadowContentResolver.registerProviderInternal("android.media.tv", provider)
        provider.previewRows.add(arrayOf("com.a", "1"))
        provider.watchNextRows.add(arrayOf("com.w", "1"))
        val source = TvProviderHeroSource(context)
        provider.denied += "preview_program"
        assertEquals(mapOf("com.w" to 1), source.programCounts())
        provider.denied += "watch_next_program"
        assertEquals(emptyMap<String, Int>(), source.programCounts())
    }

    @Test
    fun `sources are sorted on the combined total with a Watch Next only app among content apps`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val provider = FakeTvProvider()
        ShadowContentResolver.registerProviderInternal("android.media.tv", provider)
        repeat(3) { provider.previewRows.add(arrayOf("com.big", "1")) }
        provider.previewRows.add(arrayOf("com.mixed", "1"))
        provider.watchNextRows.add(arrayOf("com.mixed", "1"))
        provider.watchNextRows.add(arrayOf("com.zed", "1"))
        val apps = listOf(TvApp("com.alpha", "Alpha"), TvApp("com.zed", "Zed"), TvApp("com.big", "Big"), TvApp("com.mixed", "Mixed"), TvApp("com.beta", "Beta"))
        val sorted = SettingsOrdering.sources(apps, TvProviderHeroSource(context).programCounts())
        assertEquals(listOf("Big", "Mixed", "Zed", "Alpha", "Beta"), sorted.map { it.label })
    }
}
