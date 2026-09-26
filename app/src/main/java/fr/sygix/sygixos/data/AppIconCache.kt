/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

class AppIconCache(private val load: (String) -> Bitmap?) {

    private val cache = LruCache<String, Bitmap>(MAX_ENTRIES)

    fun cached(packageName: String): Bitmap? = cache[packageName]

    fun get(packageName: String): Bitmap? =
        cached(packageName) ?: load(packageName)?.also { cache.put(packageName, it) }

    companion object {
        private const val MAX_ENTRIES = 64

        fun forPackageManager(pm: PackageManager) = AppIconCache { packageName ->
            runCatching { pm.getApplicationIcon(packageName).toBitmap() }.getOrNull()
        }
    }
}
