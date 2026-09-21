package fr.sygix.sygixos.data

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import fr.sygix.sygixos.domain.AccentColor
import fr.sygix.sygixos.model.TvApp

/**
 * Visuel d'une tuile : bannière Android TV 16:9 de préférence, sinon icône ; couleur
 * dominante pour le halo de focus ; miniature très réduite qui, agrandie, sert de flou gratuit.
 */
data class AppArtwork(val bitmap: Bitmap, val isBanner: Boolean, val accent: Int, val blurred: Bitmap)

class AppArtworkSource(private val pm: PackageManager) {

    private val cache = java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<AppArtwork>>()

    fun load(app: TvApp): AppArtwork? =
        cache.getOrPut(app.packageName) { java.util.Optional.ofNullable(resolve(app)) }.orElse(null)

    /** Déjà en cache ? Lecture immédiate sur le thread UI, sans aller-retour asynchrone. */
    fun cached(app: TvApp): AppArtwork? = cache[app.packageName]?.orElse(null)

    /** Chargement séquentiel (le PackageManager n'aime pas 40 appels parallèles), dock d'abord. */
    fun preload(apps: List<TvApp>) = apps.forEach { load(it) }

    private fun resolve(app: TvApp): AppArtwork? {
        val banner = banner(app)
        val bitmap = banner ?: runCatching { pm.getApplicationIcon(app.packageName).toBitmap() }.getOrNull() ?: return null
        val sample = Bitmap.createScaledBitmap(bitmap, 16, 9, true)
        val pixels = IntArray(16 * 9).also { sample.getPixels(it, 0, 16, 0, 0, 16, 9) }
        return AppArtwork(
            bitmap = bitmap,
            isBanner = banner != null,
            accent = AccentColor.vivid(AccentColor.compute(pixels)),
            blurred = Bitmap.createScaledBitmap(bitmap, 24, 14, true),
        )
    }

    private fun banner(app: TvApp): Bitmap? = runCatching {
        val drawable = app.activityName
            ?.let { pm.getActivityInfo(ComponentName(app.packageName, it), 0).loadBanner(pm) }
            ?: pm.getApplicationInfo(app.packageName, 0).loadBanner(pm)
        drawable?.takeIf { it.intrinsicWidth > 0 && it.intrinsicHeight > 0 }?.toBitmap()
    }.getOrNull()
}
