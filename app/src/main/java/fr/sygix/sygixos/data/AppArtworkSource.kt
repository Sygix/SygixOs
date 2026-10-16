/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ComponentName
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import fr.sygix.sygixos.domain.ImageBounds
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.CoroutineDispatcher
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

class AppArtwork(val bitmap: Bitmap, val isBanner: Boolean) {
    val image: ImageBitmap = bitmap.asImageBitmap()
}

open class AppArtworkSource(
    private val pm: PackageManager,
    val dispatcher: CoroutineDispatcher = BackgroundDecoding,
    maxBytes: Int = 8 * 1024 * 1024,
) {

    private val cache = object : LruCache<String, AppArtwork>(maxBytes) {
        override fun sizeOf(key: String, value: AppArtwork): Int = value.bitmap.allocationByteCount
    }
    private val pending = ConcurrentHashMap<String, FutureTask<AppArtwork?>>()

    fun load(app: TvApp): AppArtwork? {
        cache[app.packageName]?.let { return it }
        val task = pending.getOrPut(app.packageName) {
            FutureTask { cache[app.packageName] ?: resolve(app)?.also { cache.put(app.packageName, it) } }
        }
        try {
            task.run()
            return task.get()
        } catch (failure: ExecutionException) {
            throw failure.cause ?: failure
        } finally {
            if (task.isDone) pending.remove(app.packageName, task)
        }
    }

    open fun cached(app: TvApp): AppArtwork? = cache[app.packageName]

    fun preload(apps: List<TvApp>) = apps.forEach { load(it) }

    private fun resolve(app: TvApp): AppArtwork? {
        val banner = banner(app)
        val bitmap = banner ?: runCatching { bounded(pm.getApplicationIcon(app.packageName)) }.getOrNull() ?: return null
        return AppArtwork(bitmap = bitmap, isBanner = banner != null)
    }

    private fun banner(app: TvApp): Bitmap? = runCatching {
        val activity = app.activityName?.let { pm.getActivityInfo(ComponentName(app.packageName, it), 0) }
        val owner = activity?.applicationInfo ?: pm.getApplicationInfo(app.packageName, 0)
        val resource = activity?.bannerResource?.takeIf { it != 0 } ?: owner.banner
        if (resource == 0) null else decodeBounded(owner, resource)
    }.getOrNull()

    private fun decodeBounded(owner: ApplicationInfo, resource: Int): Bitmap? {
        val resources = pm.getResourcesForApplication(owner)
        val sampled = runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(resources, resource)) { decoder, info, _ ->
                val target = ImageBounds.artwork(info.size.width, info.size.height)
                decoder.setTargetSize(target.width, target.height)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrNull()
        return sampled ?: pm.getDrawable(owner.packageName, resource, owner)?.let(::bounded)
    }

    private fun bounded(drawable: Drawable): Bitmap? {
        if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) return null
        val target = ImageBounds.artwork(drawable.intrinsicWidth, drawable.intrinsicHeight)
        return drawable.toBitmap(target.width, target.height)
    }
}
