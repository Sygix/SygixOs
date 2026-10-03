/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ComponentName
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import fr.sygix.sygixos.model.TvApp

data class AppArtwork(val bitmap: Bitmap, val isBanner: Boolean, val blurred: Bitmap)

class AppArtworkSource(private val pm: PackageManager) {

    private val cache = java.util.concurrent.ConcurrentHashMap<String, java.util.Optional<AppArtwork>>()

    fun load(app: TvApp): AppArtwork? =
        cache.getOrPut(app.packageName) { java.util.Optional.ofNullable(resolve(app)) }.orElse(null)

    fun cached(app: TvApp): AppArtwork? = cache[app.packageName]?.orElse(null)

    fun preload(apps: List<TvApp>) = apps.forEach { load(it) }

    private fun resolve(app: TvApp): AppArtwork? {
        val banner = banner(app)
        val bitmap = banner ?: runCatching { pm.getApplicationIcon(app.packageName).toBitmap() }.getOrNull() ?: return null
        return AppArtwork(
            bitmap = bitmap,
            isBanner = banner != null,
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
