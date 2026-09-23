/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.Dispatchers
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.TvProviderHeroSource

class SygixOsApp : Application(), ImageLoaderFactory {

    // Conteneur app-scope : une seule instance partagée par les deux ViewModel Factories.
    val installedAppsSource: InstalledAppsSource by lazy { InstalledAppsSource(this) }
    val launcherPrefs: LauncherPrefs by lazy { LauncherPrefs(this) }
    val appCatalogRepository: AppCatalogRepository by lazy { AppCatalogRepository(installedAppsSource, launcherPrefs) }
    val tvProviderHeroSource: TvProviderHeroSource by lazy { TvProviderHeroSource(this) }

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
