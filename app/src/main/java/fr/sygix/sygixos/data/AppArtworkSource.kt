package fr.sygix.sygixos.data

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import fr.sygix.sygixos.model.TvApp

/** Visuel d'une tuile : bannière Android TV 16:9 de préférence, sinon icône. */
data class AppArtwork(val bitmap: Bitmap, val isBanner: Boolean)

class AppArtworkSource(private val pm: PackageManager) {

    private val cache = java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<AppArtwork>>()

    fun load(app: TvApp): AppArtwork? =
        cache.getOrPut(app.packageName) { java.util.Optional.ofNullable(resolve(app)) }.orElse(null)

    /** Chargement séquentiel (le PackageManager n'aime pas 40 appels parallèles), dock d'abord. */
    fun preload(apps: List<TvApp>) = apps.forEach { load(it) }

    private fun resolve(app: TvApp): AppArtwork? {
        banner(app)?.let { return AppArtwork(it, isBanner = true) }
        return runCatching { pm.getApplicationIcon(app.packageName).toBitmap() }
            .getOrNull()?.let { AppArtwork(it, isBanner = false) }
    }

    private fun banner(app: TvApp): Bitmap? = runCatching {
        val drawable = app.activityName
            ?.let { pm.getActivityInfo(ComponentName(app.packageName, it), 0).loadBanner(pm) }
            ?: pm.getApplicationInfo(app.packageName, 0).loadBanner(pm)
        drawable?.takeIf { it.intrinsicWidth > 0 && it.intrinsicHeight > 0 }?.toBitmap()
    }.getOrNull()
}
