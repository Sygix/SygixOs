/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.ConnectivityNetworkStatus
import fr.sygix.sygixos.data.ContextSystemScreenLauncher
import fr.sygix.sygixos.data.ForegroundTracker
import fr.sygix.sygixos.data.GitHubReleaseSource
import fr.sygix.sygixos.data.HttpsUrlTransport
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.PackageManagerApkInspector
import fr.sygix.sygixos.data.SystemPackageInstallerGateway
import fr.sygix.sygixos.data.TvProviderHeroSource
import fr.sygix.sygixos.data.UpdateInstaller
import fr.sygix.sygixos.data.UpdatePrefs
import fr.sygix.sygixos.data.UpdateRepository

class SygixOsApp : Application(), ImageLoaderFactory {

    // Conteneur app-scope : une seule instance partagée par les deux ViewModel Factories.
    val installedAppsSource: InstalledAppsSource by lazy { InstalledAppsSource(this) }
    val launcherPrefs: LauncherPrefs by lazy { LauncherPrefs(this) }
    val appCatalogRepository: AppCatalogRepository by lazy { AppCatalogRepository(installedAppsSource, launcherPrefs) }
    val tvProviderHeroSource: TvProviderHeroSource by lazy { TvProviderHeroSource(this) }
    val foregroundTracker = ForegroundTracker()
    val updatePrefs: UpdatePrefs by lazy { UpdatePrefs(this) }
    val updateRepository: UpdateRepository by lazy { createUpdateRepository() }

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(foregroundTracker)
        updateRepository.coldStart()
    }

    private fun createUpdateRepository(): UpdateRepository {
        val transport = HttpsUrlTransport()
        val inspector = PackageManagerApkInspector(packageManager, packageName)
        val gateway = SystemPackageInstallerGateway(this)
        val userAgent = "SygixOs/${runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()}"
        val source = GitHubReleaseSource(transport, userAgent)
        val updatesDir = cacheDir.resolve("updates")
        return UpdateRepository(
            store = updatePrefs,
            source = source,
            installer = UpdateInstaller(
                source = source,
                transport = transport,
                inspector = inspector,
                gateway = gateway,
                store = updatePrefs,
                foreground = foregroundTracker,
                updatesDir = updatesDir,
                freeSpace = { cacheDir.usableSpace },
                selfPackage = packageName,
                userAgent = userAgent,
            ),
            gateway = gateway,
            network = ConnectivityNetworkStatus(this),
            foreground = foregroundTracker,
            screens = ContextSystemScreenLauncher(this),
            installedVersionCode = { inspector.installed()?.versionCode ?: 0L },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
    }

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
