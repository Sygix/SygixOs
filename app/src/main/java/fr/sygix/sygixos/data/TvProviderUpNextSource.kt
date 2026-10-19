/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.os.CancellationSignal
import android.media.tv.TvContract.WatchNextPrograms as W
import fr.sygix.sygixos.domain.UpNextSource
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow

class TvProviderUpNextSource(
    private val context: Context,
    private val timeoutMillis: Long = 2_000,
    private val debounceMillis: Long = 500,
    private val query: (CancellationSignal) -> Cursor? = { signal ->
        context.contentResolver.query(W.CONTENT_URI, PROJECTION, null, null, null, signal)
    },
    private val installed: (String) -> Boolean = { pkg -> runCatching { context.packageManager.getApplicationInfo(pkg, 0) }.isSuccess },
) : UpNextSource {
    private val apps = ConcurrentHashMap<String, SourceApp>()

    private data class SourceApp(val label: String, val icon: Drawable?)

    override suspend fun load(): Result<List<UpNextItem>> = providerQuery(timeoutMillis) { signal ->
        val cursor = query(signal) ?: error("Watch Next provider unavailable")
        cursor.use { c -> buildList { while (c.moveToNext()) mapRow(c)?.let(::add) } }
    }.map { accessible(it) }

    override suspend fun accessible(items: List<UpNextItem>): List<UpNextItem> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val available = items.map { it.source.packageName }.distinct().associateWith(installed)
        items.filter { available[it.source.packageName] == true }
    }

    override fun changes(): Flow<Unit> = context.contentResolver.changes(listOf(W.CONTENT_URI), debounceMillis)

    internal fun mapRow(c: Cursor): UpNextItem? {
        if (c.optInt(W.COLUMN_BROWSABLE, 1) == 0) return null
        val season = c.optString(W.COLUMN_SEASON_DISPLAY_NUMBER)
        val episode = c.optString(W.COLUMN_EPISODE_DISPLAY_NUMBER)
        val type = when (c.optInt(W.COLUMN_TYPE, -1)) {
            W.TYPE_TV_EPISODE -> UpNextContentType.EPISODE
            W.TYPE_MOVIE -> UpNextContentType.MOVIE
            -1 -> if (season != null && episode != null) UpNextContentType.EPISODE else UpNextContentType.MOVIE
            else -> return null
        }
        val published = c.optString(W.COLUMN_TITLE)
        val episodeTitle = c.optString(W.COLUMN_EPISODE_TITLE)
        val title = published ?: episodeTitle?.takeIf { type == UpNextContentType.EPISODE } ?: return null
        val pkg = c.optString(W.COLUMN_PACKAGE_NAME) ?: return null
        val app = apps.getOrPut(pkg) { sourceApp(pkg) }
        val source = UpNextSourceEntry(pkg, app.label, app.icon, c.optString(W.COLUMN_INTENT_URI))
        val image = landscapeImage(c, W.COLUMN_POSTER_ART_URI, W.COLUMN_POSTER_ART_ASPECT_RATIO, W.COLUMN_THUMBNAIL_URI)
        return UpNextItem(
            id = c.optLong(W._ID),
            source = source,
            type = type,
            title = title,
            seriesTitle = published?.takeIf { type == UpNextContentType.EPISODE },
            season = season,
            episode = episode,
            episodeTitle = episodeTitle,
            titleFromEpisode = published == null,
            imageUrl = image,
            portrait = image != null && image == c.optString(W.COLUMN_POSTER_ART_URI) && isPortraitRatio(c.optInt(W.COLUMN_POSTER_ART_ASPECT_RATIO, -1)),
            positionMillis = c.optLong(W.COLUMN_LAST_PLAYBACK_POSITION_MILLIS).takeIf { it > 0 },
            durationMillis = c.optLong(W.COLUMN_DURATION_MILLIS).takeIf { it > 0 },
            watchNextType = watchNextKind(c.optInt(W.COLUMN_WATCH_NEXT_TYPE, -1)),
            engagement = c.optLong(W.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS),
            internalProviderId = c.optString(W.COLUMN_INTERNAL_PROVIDER_ID),
            contentId = c.optString(W.COLUMN_CONTENT_ID),
            posterUrl = c.optString(W.COLUMN_POSTER_ART_URI),
            thumbnailUrl = c.optString(W.COLUMN_THUMBNAIL_URI),
            posterAspectRatio = c.optInt(W.COLUMN_POSTER_ART_ASPECT_RATIO, -1),
            releaseDate = c.optString(W.COLUMN_RELEASE_DATE),
            year = c.optString(W.COLUMN_RELEASE_DATE)?.let { Regex("^\\d{4}").find(it)?.value?.toIntOrNull() },
        )
    }

    private fun sourceApp(pkg: String): SourceApp {
        val pm = context.packageManager
        val info = runCatching { pm.getApplicationInfo(pkg, 0) }.getOrNull()
        return SourceApp(
            info?.let { runCatching { pm.getApplicationLabel(it).toString() }.getOrNull() } ?: pkg,
            info?.let { runCatching { pm.getApplicationIcon(it) }.getOrNull() },
        )
    }

    internal companion object {
        val PROJECTION = arrayOf(
            W._ID, W.COLUMN_PACKAGE_NAME, W.COLUMN_TITLE, W.COLUMN_EPISODE_TITLE,
            W.COLUMN_SEASON_DISPLAY_NUMBER, W.COLUMN_EPISODE_DISPLAY_NUMBER,
            W.COLUMN_TYPE, W.COLUMN_BROWSABLE, W.COLUMN_WATCH_NEXT_TYPE,
            W.COLUMN_POSTER_ART_URI, W.COLUMN_POSTER_ART_ASPECT_RATIO, W.COLUMN_THUMBNAIL_URI,
            W.COLUMN_LAST_PLAYBACK_POSITION_MILLIS, W.COLUMN_DURATION_MILLIS,
            W.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS, W.COLUMN_INTENT_URI,
            W.COLUMN_INTERNAL_PROVIDER_ID, W.COLUMN_CONTENT_ID, W.COLUMN_RELEASE_DATE,
        )
    }
}
