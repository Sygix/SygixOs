/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import android.media.tv.TvContract.PreviewPrograms as P
import android.media.tv.TvContract.WatchNextPrograms as W
import android.net.Uri
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.HeroRead
import fr.sygix.sygixos.domain.ArtworkAge
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.ProgramKind
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TvProviderHeroSource(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
    private val installed: (String) -> Boolean = { pkg -> runCatching { context.packageManager.getApplicationInfo(pkg, 0) }.isSuccess },
    private val onConfirmed: () -> Unit = {},
    private val onSnapshot: suspend (String, List<HeroItem>) -> Unit = { _, _ -> },
    private val query: (Uri, Array<String>, android.os.CancellationSignal) -> Cursor? = { uri, projection, signal ->
        context.contentResolver.query(uri, projection, null, null, null, signal)
    },
    private val queries: ProviderQueries = ProviderQueries.Shared,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : HeroContentProvider {

    private val labels = mutableMapOf<String, String?>()
    private val snapshots = mutableMapOf<Uri, List<HeroItem>>()

    override suspend fun load(): List<HeroItem> = withContext(io) {
        read().items
    }

    override suspend fun read(): HeroRead = withContext(io) {
        val errors = mutableListOf<Throwable>()
        for ((uri, projection, mapper) in listOf(
            Triple(P.CONTENT_URI, PREVIEW_PROJECTION, ::mapPreviewRow),
            Triple(W.CONTENT_URI, WATCH_NEXT_PROJECTION, ::mapWatchNextRow),
        )) {
            val result = rowsResult(uri, projection, mapper)
            result.onSuccess { items ->
                snapshots[uri] = items.map { it.copy(expiresAt = clock() + ArtworkAge.FRESH_MS) }
                onSnapshot(if (uri == P.CONTENT_URI) "preview" else "watchnext", items)
                onConfirmed()
            }
                .onFailure { error ->
                    errors += error
                    if (error is SecurityException) snapshots.clear()
                }
        }
        val now = clock()
        val available = snapshots.values.flatten().mapNotNull { it.sourcePackage }.distinct().associateWith(installed)
        snapshots.replaceAll { _, items -> items.filter { it.expiresAt > now && it.sourcePackage?.let { pkg -> available[pkg] } != false } }
        val items = snapshots.values.flatten()
            .map { it.copy(sourceLabel = labelOf(it.sourcePackage)) }
        HeroRead(HeroOrdering.sort(items), errors)
    }

    override fun changes(): Flow<Unit> = context.contentResolver.changes(PROGRAM_URIS)

    suspend fun programCounts(): Map<String, Int> = PROGRAM_URIS
        .flatMap { uri ->
            rows(uri, arrayOf(P.COLUMN_PACKAGE_NAME, P.COLUMN_BROWSABLE)) { c ->
                c.optString(P.COLUMN_PACKAGE_NAME).takeIf { c.optInt(P.COLUMN_BROWSABLE, 1) != 0 }
            }
        }
        .groupingBy { it }
        .eachCount()

    fun programCountsFlow(debounceMillis: Long = 500): Flow<Map<String, Int>> =
        context.contentResolver.changes(PROGRAM_URIS, debounceMillis).map { programCounts() }

    private suspend fun <T : Any> rows(uri: Uri, projection: Array<String>, map: (Cursor) -> T?): List<T> =
        rowsResult(uri, projection, map).getOrDefault(emptyList())

    private suspend fun <T : Any> rowsResult(uri: Uri, projection: Array<String>, map: (Cursor) -> T?): Result<List<T>> =
        queries.run(2_000) { signal ->
            val cursor = query(uri, projection, signal)
                ?: error("TV provider unavailable")
            cursor.use { c -> buildList { while (c.moveToNext()) map(c)?.let(::add) } }
        }

    internal fun mapPreviewRow(c: Cursor): HeroItem? = mapRow(c, "preview", ProgramKind.FEATURED)

    internal fun mapWatchNextRow(c: Cursor): HeroItem? = mapRow(c, "watchnext", watchNextKind(c.optInt(W.COLUMN_WATCH_NEXT_TYPE, -1)))

    private fun mapRow(c: Cursor, idPrefix: String, kind: ProgramKind): HeroItem? {
        if (c.optInt(P.COLUMN_BROWSABLE, 1) == 0) return null
        val image = landscapeImage(c, P.COLUMN_POSTER_ART_URI, P.COLUMN_POSTER_ART_ASPECT_RATIO, P.COLUMN_THUMBNAIL_URI)
        val video = playableVideo(c.optString(P.COLUMN_PREVIEW_VIDEO_URI))
        return HeroItem(
            id = "$idPrefix-${c.getLong(c.getColumnIndexOrThrow(P._ID))}",
            title = c.optString(P.COLUMN_TITLE).orEmpty(),
            videoUrl = video,
            imageUrl = image,
            sourcePackage = c.optString(P.COLUMN_PACKAGE_NAME),
            launchUri = c.optString(P.COLUMN_INTENT_URI),
            engagement = c.optLong(W.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS),
            kind = kind,
            season = c.optString(P.COLUMN_SEASON_DISPLAY_NUMBER),
            episode = c.optString(P.COLUMN_EPISODE_DISPLAY_NUMBER),
            durationMillis = c.optLong(P.COLUMN_DURATION_MILLIS).takeIf { it > 0 },
            positionMillis = c.optLong(P.COLUMN_LAST_PLAYBACK_POSITION_MILLIS).takeIf { it > 0 },
            posterUrl = c.optString(P.COLUMN_POSTER_ART_URI),
            thumbnailUrl = c.optString(P.COLUMN_THUMBNAIL_URI),
            posterAspectRatio = c.optInt(P.COLUMN_POSTER_ART_ASPECT_RATIO, -1),
            internalProviderId = c.optString(P.COLUMN_INTERNAL_PROVIDER_ID),
            contentId = c.optString(P.COLUMN_CONTENT_ID),
            seriesId = c.optString(P.COLUMN_SERIES_ID),
            releaseDate = c.optString(P.COLUMN_RELEASE_DATE),
            episodeArtwork = c.optInt(P.COLUMN_TYPE, -1) == P.TYPE_TV_EPISODE || c.optString(P.COLUMN_SEASON_DISPLAY_NUMBER) != null || c.optString(P.COLUMN_EPISODE_DISPLAY_NUMBER) != null,
        )
    }

    private fun playableVideo(uri: String?): String? = uri?.takeIf { raw ->
        val parsed = Uri.parse(raw)
        parsed.scheme == "http" || parsed.scheme == "https" ||
            (parsed.scheme == "content" && parsed.authority != TvContract.AUTHORITY)
    }

    private fun labelOf(packageName: String?): String? = packageName?.let { pkg ->
        labels.getOrPut(pkg) {
            runCatching {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
            }.getOrNull()
        }
    }

    private companion object {
        val PROGRAM_URIS = listOf(P.CONTENT_URI, W.CONTENT_URI)

        val PREVIEW_PROJECTION = arrayOf(
            P._ID,
            P.COLUMN_PACKAGE_NAME,
            P.COLUMN_TITLE,
            P.COLUMN_POSTER_ART_URI,
            P.COLUMN_POSTER_ART_ASPECT_RATIO,
            P.COLUMN_THUMBNAIL_URI,
            P.COLUMN_PREVIEW_VIDEO_URI,
            P.COLUMN_INTENT_URI,
            P.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
            P.COLUMN_DURATION_MILLIS,
            P.COLUMN_SEASON_DISPLAY_NUMBER,
            P.COLUMN_EPISODE_DISPLAY_NUMBER,
            P.COLUMN_BROWSABLE,
            P.COLUMN_TYPE,
            P.COLUMN_INTERNAL_PROVIDER_ID,
            P.COLUMN_CONTENT_ID,
            P.COLUMN_SERIES_ID,
            P.COLUMN_RELEASE_DATE,
        )
        val WATCH_NEXT_PROJECTION = PREVIEW_PROJECTION + arrayOf(W.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, W.COLUMN_WATCH_NEXT_TYPE)
    }
}
