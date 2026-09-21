package fr.sygix.sygixos.data

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.tv.TvContract
import android.net.Uri
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TvProviderHeroSource(private val context: Context) : HeroContentProvider {

    private val labels = mutableMapOf<String, String?>()

    override suspend fun load(): List<HeroItem> = withContext(Dispatchers.IO) {
        runCatching { queryAll() }.getOrDefault(emptyList())
    }

    private fun queryAll(): List<HeroItem> {
        val resolver = context.contentResolver
        val items = query(resolver, TvContract.PreviewPrograms.CONTENT_URI, PREVIEW_PROJECTION, ::mapPreviewRow) +
            query(resolver, TvContract.WatchNextPrograms.CONTENT_URI, WATCH_NEXT_PROJECTION, ::mapWatchNextRow)
        return HeroOrdering.sort(items.map { it.copy(sourceLabel = labelOf(it.sourcePackage)) })
    }

    private fun query(
        resolver: ContentResolver,
        uri: android.net.Uri,
        projection: Array<String>,
        map: (Cursor) -> HeroItem?,
    ): List<HeroItem> {
        val items = mutableListOf<HeroItem>()
        runCatching {
            resolver.query(uri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) map(c)?.let(items::add)
            }
        }
        return items
    }

    internal fun mapPreviewRow(c: Cursor): HeroItem? {
        if (c.optInt(TvContract.PreviewPrograms.COLUMN_BROWSABLE, 1) == 0) return null
        val image = landscapeImage(c, TvContract.PreviewPrograms.COLUMN_POSTER_ART_URI, TvContract.PreviewPrograms.COLUMN_POSTER_ART_ASPECT_RATIO, TvContract.PreviewPrograms.COLUMN_THUMBNAIL_URI)
        val video = playableVideo(c.optString(TvContract.PreviewPrograms.COLUMN_PREVIEW_VIDEO_URI))
        val intentUri = c.optString(TvContract.PreviewPrograms.COLUMN_INTENT_URI)
        if (image == null && video == null) return null
        return HeroItem(
            id = "preview-${c.getLong(c.getColumnIndexOrThrow(TvContract.PreviewPrograms._ID))}",
            title = c.optString(TvContract.PreviewPrograms.COLUMN_TITLE).orEmpty(),
            videoUrl = video,
            imageUrl = image,
            sourcePackage = c.optString(TvContract.PreviewPrograms.COLUMN_PACKAGE_NAME),
            progress = HeroOrdering.progressRatio(
                c.optLong(TvContract.PreviewPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS),
                c.optLong(TvContract.PreviewPrograms.COLUMN_DURATION_MILLIS),
            ),
            launchUri = intentUri,
        )
    }

    internal fun mapWatchNextRow(c: Cursor): HeroItem? {
        if (c.optInt(TvContract.WatchNextPrograms.COLUMN_BROWSABLE, 1) == 0) return null
        val image = landscapeImage(c, TvContract.WatchNextPrograms.COLUMN_POSTER_ART_URI, TvContract.WatchNextPrograms.COLUMN_POSTER_ART_ASPECT_RATIO, TvContract.WatchNextPrograms.COLUMN_THUMBNAIL_URI)
        val video = playableVideo(c.optString(TvContract.WatchNextPrograms.COLUMN_PREVIEW_VIDEO_URI))
        val intentUri = c.optString(TvContract.WatchNextPrograms.COLUMN_INTENT_URI)
        if (image == null && video == null) return null
        return HeroItem(
            id = "watchnext-${c.getLong(c.getColumnIndexOrThrow(TvContract.WatchNextPrograms._ID))}",
            title = c.optString(TvContract.WatchNextPrograms.COLUMN_TITLE).orEmpty(),
            videoUrl = video,
            imageUrl = image,
            sourcePackage = c.optString(TvContract.WatchNextPrograms.COLUMN_PACKAGE_NAME),
            progress = HeroOrdering.progressRatio(
                c.optLong(TvContract.WatchNextPrograms.COLUMN_LAST_PLAYBACK_POSITION_MILLIS),
                c.optLong(TvContract.WatchNextPrograms.COLUMN_DURATION_MILLIS),
            ),
            launchUri = intentUri,
            engagement = c.optLong(TvContract.WatchNextPrograms.COLUMN_LAST_ENGAGEMENT_TIME_UTC_MILLIS),
        )
    }

    private fun playableVideo(uri: String?): String? = uri?.takeIf { raw ->
        val parsed = Uri.parse(raw)
        parsed.scheme == "http" || parsed.scheme == "https" ||
            (parsed.scheme == "content" && parsed.authority != TvContract.AUTHORITY)
    }

    private fun landscapeImage(c: Cursor, posterColumn: String, aspectColumn: String, thumbnailColumn: String): String? {
        val poster = c.optString(posterColumn)
        val thumbnail = c.optString(thumbnailColumn)
        val portrait = c.optInt(aspectColumn, -1) in PORTRAIT_RATIOS
        return if (portrait && thumbnail != null) thumbnail else poster ?: thumbnail
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
        const val ASPECT_RATIO_MOVIE_POSTER = 5
        val PORTRAIT_RATIOS = setOf(TvContract.PreviewPrograms.ASPECT_RATIO_2_3, ASPECT_RATIO_MOVIE_POSTER)
        val PREVIEW_PROJECTION = arrayOf(
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
        val WATCH_NEXT_PROJECTION = arrayOf(
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
    }
}

private fun Cursor.optString(column: String): String? =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getString)?.takeIf { it.isNotBlank() }

private fun Cursor.optLong(column: String): Long =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getLong) ?: 0L

private fun Cursor.optInt(column: String, default: Int): Int =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getInt) ?: default
