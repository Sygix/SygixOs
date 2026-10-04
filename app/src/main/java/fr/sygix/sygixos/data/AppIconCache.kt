/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

class AppIconCache(private val load: (String) -> Bitmap?) {

    private val cache = LruCache<String, Bitmap>(MAX_ENTRIES)

    fun cached(packageName: String): Bitmap? = cache[packageName]

    fun get(packageName: String): Bitmap? =
        cached(packageName) ?: load(packageName)?.also { cache.put(packageName, it) }

    companion object {
        private const val MAX_ENTRIES = 64
        private const val SQUARE_ICON_PX = 96

        fun forPackageManager(pm: PackageManager) = AppIconCache { packageName ->
            runCatching { pm.getApplicationIcon(packageName).toBitmap() }.getOrNull()
        }

        fun squareFor(pm: PackageManager) = AppIconCache { packageName ->
            runCatching { fullBleed(pm.getApplicationIcon(packageName), SQUARE_ICON_PX) }.getOrNull()
        }

        internal fun fullBleed(drawable: Drawable, size: Int): Bitmap? {
            if (drawable !is AdaptiveIconDrawable) {
                if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) return null
                return drawable.toBitmap(size, size).also { it.prepareToDraw() }
            }
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val inset = size * AdaptiveIconDrawable.getExtraInsetFraction()
            val extent = (size + 2 * inset).toInt()
            val offset = -inset.toInt()
            listOfNotNull(drawable.background, drawable.foreground).forEach { layer ->
                layer.setBounds(offset, offset, offset + extent, offset + extent)
                layer.draw(canvas)
            }
            bitmap.prepareToDraw()
            return bitmap
        }
    }
}
