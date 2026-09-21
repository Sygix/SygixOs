package fr.sygix.sygixos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers

/** Coil sobre pour une TV : RGB565, peu de décodeurs simultanés, cache disque généreux. */
class SygixOsApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .allowRgb565(true)
        .crossfade(false)
        .respectCacheHeaders(false)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.15).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("images"))
                .maxSizeBytes(256L * 1024 * 1024)
                .build()
        }
        .decoderDispatcher(Dispatchers.IO.limitedParallelism(2))
        .fetcherDispatcher(Dispatchers.IO.limitedParallelism(4))
        .build()
}
