package fr.sygix.sygixos.data

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.media.tv.TvContract
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Remonte les programmes que les apps installées publient dans le TV Provider
 * système (watch next + preview programs) : reprises de lecture, récemment
 * ajoutés, recommandations. Aucune liste d'apps codée en dur : dès qu'une app
 * publie (Jellyfin, Netflix, Prime…), son contenu apparaît.
 */
class TvProviderHeroSource(private val context: Context) : HeroContentProvider {

    override suspend fun load(): List<HeroItem> = withContext(Dispatchers.IO) {
        runCatching { queryAll() }.getOrDefault(emptyList())
    }

    private fun queryAll(): List<HeroItem> {
        val resolver = context.contentResolver
        val packagesByChannel = queryPreviewChannelPackages(resolver)
        val preview = queryPreviewPrograms(resolver, packagesByChannel)
        val watchNext = queryWatchNext(resolver)
        return HeroOrdering.sort(preview + watchNext)
    }

    private fun queryPreviewChannelPackages(resolver: ContentResolver): Map<Long, String> {
        resolver.query(
            TvContract.Channels.CONTENT_URI,
            arrayOf(TvContract.Channels._ID, TvContract.Channels.COLUMN_PACKAGE_NAME),
            null,
            null,
            null,
        )?.use { c ->
            val map = mutableMapOf<Long, String>()
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(TvContract.Channels._ID))
                val pkg = c.getString(c.getColumnIndexOrThrow(TvContract.Channels.COLUMN_PACKAGE_NAME))
                if (pkg != null) map[id] = pkg
            }
            return map
        }
        return emptyMap()
    }

    private fun queryPreviewPrograms(
        resolver: ContentResolver,
        packagesByChannel: Map<Long, String>,
    ): List<HeroItem> {
        val items = mutableListOf<HeroItem>()
        resolver.query(
            TvContract.PreviewPrograms.CONTENT_URI,
            PREVIEW_PROJECTION,
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val channelId = c.getLong(c.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_CHANNEL_ID))
                mapPreviewRow(c, packagesByChannel[channelId])?.let(items::add)
            }
        }
        return items
    }

    private fun queryWatchNext(resolver: ContentResolver): List<HeroItem> {
        val items = mutableListOf<HeroItem>()
        resolver.query(
            TvContract.WatchNextPrograms.CONTENT_URI,
            WATCH_NEXT_PROJECTION,
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                mapWatchNextRow(c)?.let(items::add)
            }
        }
        return items
    }

    internal fun mapPreviewRow(cursor: Cursor, packageName: String?): HeroItem? {
        val title = cursor.getString(cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_TITLE))
        val poster = cursor.getString(cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI))
            ?: cursor.getString(cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI))
        val intentUri = cursor.getString(cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms.COLUMN_INTENT_URI))
        if (packageName == null && poster == null && intentUri == null) return null
        return HeroItem(
            id = "preview-${cursor.getString(cursor.getColumnIndexOrThrow(TvContract.PreviewPrograms._ID))}",
            title = title ?: "",
            imageUrl = poster,
            sourcePackage = packageName,
            launchUri = intentUri,
        )
    }

    internal fun mapWatchNextRow(cursor: Cursor): HeroItem? {
        val title = cursor.getString(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms.COLUMN_TITLE))
        val poster = cursor.getString(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI))
            ?: cursor.getString(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI))
        val intentUri = cursor.getString(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms.COLUMN_INTENT_URI))
        val position = cursor.getLong(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS))
        val duration = cursor.getLong(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS))
        val engagement = cursor.getLong(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS))
        if (poster == null && intentUri == null) return null
        return HeroItem(
            id = "watchnext-${cursor.getString(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms._ID))}",
            title = title ?: "",
            imageUrl = poster,
            sourcePackage = cursor.getString(cursor.getColumnIndexOrThrow(TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME)),
            progress = HeroOrdering.progressRatio(position, duration),
            launchUri = intentUri,
            engagement = engagement,
        )
    }

    private val artCache = java.util.concurrent.ConcurrentHashMap<String, List<String>>()

    /**
     * Posters publiés par une app donnée (preview programs + watch next),
     * pour le panneau Top Shelf au focus d'une tuile. Cache mémoire.
     */
    suspend fun posterUrisFor(packageName: String): List<String> = withContext(Dispatchers.IO) {
        artCache.getOrPut(packageName) {
            runCatching { queryPosterUrisFor(packageName) }.getOrDefault(emptyList())
        }
    }

    private fun queryPosterUrisFor(packageName: String): List<String> {
        val resolver = context.contentResolver
        val channelIds = queryPreviewChannelPackages(resolver)
            .filterValues { it == packageName }.keys
        val uris = mutableListOf<String>()
        if (channelIds.isNotEmpty()) {
            val idList = channelIds.joinToString(",") { it.toString() }
            resolver.query(
                TvContract.PreviewPrograms.CONTENT_URI,
                arrayOf(TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI, TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI, TvContract.PreviewPrograms.COLUMN_CHANNEL_ID),
                "${TvContract.PreviewPrograms.COLUMN_CHANNEL_ID} IN ($idList)",
                null,
                null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val poster = c.getString(0) ?: c.getString(1)
                    if (poster != null) uris.add(poster)
                }
            }
        }
        resolver.query(
            TvContract.WatchNextPrograms.CONTENT_URI,
            arrayOf(TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI, TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI, TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME),
            "${TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME} = ?",
            arrayOf(packageName),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val poster = c.getString(0) ?: c.getString(1)
                if (poster != null) uris.add(poster)
            }
        }
        return uris.distinct()
    }

    private companion object {
        val PREVIEW_PROJECTION = arrayOf(
            TvContract.PreviewPrograms._ID,
            TvContract.PreviewPrograms.COLUMN_CHANNEL_ID,
            TvContract.PreviewPrograms.COLUMN_TITLE,
            TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI,
            TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI,
            TvContract.PreviewPrograms.COLUMN_INTENT_URI,
        )
        val WATCH_NEXT_PROJECTION = arrayOf(
            TvContract.WatchNextPrograms._ID,
            TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME,
            TvContract.WatchNextPrograms.COLUMN_TITLE,
            TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI,
            TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI,
            TvContract.WatchNextPrograms.COLUMN_INTENT_URI,
            TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS,
            TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS,
            TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS,
        )
    }
}

internal object HeroOrdering {

    fun progressRatio(positionMillis: Long, durationMillis: Long): Float? =
        if (durationMillis > 0 && positionMillis in 1 until durationMillis) {
            (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
        } else {
            null
        }

    /** Reprises en cours d'abord (progression connue), puis le plus récent engagement. */
    fun sort(items: List<HeroItem>): List<HeroItem> =
        items.sortedWith(
            compareByDescending<HeroItem> { it.progress != null }
                .thenByDescending { it.engagement },
        )
}
