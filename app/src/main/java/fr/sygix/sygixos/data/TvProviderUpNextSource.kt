/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.os.CancellationSignal
import android.media.tv.TvContract.WatchNextPrograms as W
import fr.sygix.sygixos.domain.UpNextSource
import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout

class TvProviderUpNextSource(
    private val context: Context,
    private val timeoutMillis: Long = 2_000,
    private val debounceMillis: Long = 500,
    private val query: (CancellationSignal) -> Cursor? = { signal ->
        context.contentResolver.query(W.CONTENT_URI, PROJECTION, null, null, null, signal)
    },
) : UpNextSource {
    private val apps = ConcurrentHashMap<String, SourceApp>()

    private data class SourceApp(val label: String, val icon: Drawable?)

    override suspend fun load(): Result<List<UpNextItem>> = try {
        Result.success(withTimeout(timeoutMillis) { queryCancellably() })
    } catch (error: TimeoutCancellationException) {
        Result.failure(error)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    private suspend fun queryCancellably(): List<UpNextItem> = coroutineScope {
        val signal = CancellationSignal()
        val done = AtomicBoolean(false)
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                if (!done.get()) signal.cancel()
            }
        }
        try {
            runInterruptible(Dispatchers.IO) {
                val cursor = query(signal) ?: error("Watch Next provider unavailable")
                cursor.use { c -> buildList { while (c.moveToNext()) mapRow(c)?.let(::add) } }
            }
        } finally {
            done.set(true)
            watcher.cancel()
        }
    }

    override fun changes(): Flow<Unit> = channelFlow {
        val signals = Channel<Unit>(Channel.CONFLATED)
        val resolver = context.contentResolver
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) { signals.trySend(Unit) }
        }
        val observing = runCatching { resolver.registerContentObserver(W.CONTENT_URI, true, observer) }.isSuccess
        try {
            send(Unit)
            while (observing) {
                signals.receive()
                delay(debounceMillis)
                signals.tryReceive()
                send(Unit)
            }
        } finally {
            runCatching { resolver.unregisterContentObserver(observer) }
            signals.close()
        }
    }

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
            watchNextType = when (c.optInt(W.COLUMN_WATCH_NEXT_TYPE, -1)) {
                W.WATCH_NEXT_TYPE_CONTINUE -> ProgramKind.CONTINUE
                W.WATCH_NEXT_TYPE_NEXT -> ProgramKind.NEXT
                W.WATCH_NEXT_TYPE_NEW -> ProgramKind.NEW
                W.WATCH_NEXT_TYPE_WATCHLIST -> ProgramKind.WATCHLIST
                else -> ProgramKind.WATCH_NEXT
            },
            engagement = c.optLong(W.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS),
            internalProviderId = c.optString(W.COLUMN_INTERNAL_PROVIDER_ID),
            contentId = c.optString(W.COLUMN_CONTENT_ID),
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
