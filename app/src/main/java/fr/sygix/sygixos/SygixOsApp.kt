/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import android.os.SystemClock
import android.provider.Settings
import fr.sygix.sygixos.data.AccessibilityConnection
import fr.sygix.sygixos.data.AndroidLauncherSystemGateway
import fr.sygix.sygixos.data.LauncherSystemIntents
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.domain.AccessibilityCheck
import fr.sygix.sygixos.domain.ImageBounds
import fr.sygix.sygixos.domain.BootStartDispatcher
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.ConnectivityNetworkStatus
import fr.sygix.sygixos.data.ContextSystemScreenLauncher
import fr.sygix.sygixos.data.ForegroundTracker
import fr.sygix.sygixos.data.GitHubReleaseSource
import fr.sygix.sygixos.data.HomeScreenStateReader
import fr.sygix.sygixos.data.HomeStateReader
import fr.sygix.sygixos.data.HttpsUrlTransport
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.PackageManagerApkInspector
import fr.sygix.sygixos.data.RawMascotAnimationSource
import fr.sygix.sygixos.data.SystemPackageInstallerGateway
import fr.sygix.sygixos.data.TvProviderHeroSource
import fr.sygix.sygixos.data.UpdateInstaller
import fr.sygix.sygixos.data.UpdatePrefs
import fr.sygix.sygixos.data.UpdateRelaunchReceiver
import fr.sygix.sygixos.data.UpdateRepository
import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.domain.StartupSession

open class SygixOsApp : Application(), ImageLoaderFactory {

    // Conteneur app-scope : une seule instance partagée par les deux ViewModel Factories.
    val installedAppsSource: InstalledAppsSource by lazy { InstalledAppsSource(this) }
    val launcherPrefs: LauncherPrefs by lazy { LauncherPrefs(this) }
    val appCatalogRepository: AppCatalogRepository by lazy { AppCatalogRepository(installedAppsSource, launcherPrefs) }
    val tvProviderHeroSource: TvProviderHeroSource by lazy { TvProviderHeroSource(this) }
    val appArtwork: AppArtworkSource by lazy {
        val tile = Dimens.gridTileWidthPx(resources.configuration.screenWidthDp, resources.displayMetrics.density)
        AppArtworkSource(packageManager, bounds = ImageBounds.tile(tile))
    }
    val startupSession = StartupSession()
    val foregroundTracker = ForegroundTracker()
    val updatePrefs: UpdatePrefs by lazy { UpdatePrefs(this) }
    val updateRepository: UpdateRepository by lazy { createUpdateRepository() }
    val mascotAnimation: RawMascotAnimationSource by lazy { RawMascotAnimationSource(resources) }
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val defaultLauncherController by lazy {
        val reader = createHomeStateReader()
        val access = fr.sygix.sygixos.data.AdbAccessGateway.create(this)
        val store = fr.sygix.sygixos.data.DefaultLauncherPrefs(this)
        val operations = fr.sygix.sygixos.data.PrivilegedShell(
            access, reader, fr.sygix.sygixos.data.HomeScreenStateReader.currentUser(),
            { Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) },
            { Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1 },
            { store.loadMemory().previousHome },
        )
        fr.sygix.sygixos.domain.DefaultLauncherController(
            access, operations, reader, store, appScope,
            enableBoot = {
                launcherPrefs.setBootStartEnabled(true, bootCount())
                kotlinx.coroutines.withTimeout(10_000) { launcherPrefs.launcherSystem.first { it.bootStart.enabled } }
            },
            refreshManual = {},
            openDebug = {
                listOf(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, Settings.ACTION_DEVICE_INFO_SETTINGS).any { action ->
                    runCatching {
                        val intent = android.content.Intent(action).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (packageManager.resolveActivity(intent, 0) == null) false else { startActivity(intent); true }
                    }.getOrDefault(false)
                }
            },
        )
    }
    val bootStartDispatcher: BootStartDispatcher by lazy {
        BootStartDispatcher(
            store = launcherPrefs,
            bootCount = ::bootCount,
            sinceBootMs = SystemClock::elapsedRealtime,
            foreground = { foregroundTracker.isForeground },
            openHome = { runCatching { startActivity(LauncherSystemIntents.boot(this)) }.isSuccess },
        )
    }
    val accessibilityCheck: AccessibilityCheck by lazy {
        AccessibilityCheck(
            enabled = { AndroidLauncherSystemGateway.serviceEnabledIn(this) },
            connected = AccessibilityConnection.connected,
            scope = appScope,
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (prefetchesMascot && Application.getProcessName() == packageName) {
            mascotAnimation.prefetch(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        }
        registerActivityLifecycleCallbacks(foregroundTracker)
        watchPackageChanges()
        updateRepository.coldStart()
        if (checksAccessibility && Application.getProcessName() == packageName) accessibilityCheck.check()
    }

    private fun watchPackageChanges() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                intent.data?.schemeSpecificPart?.let(appArtwork::invalidate)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun onAccessibilityConnected() {
        appScope.launch(Dispatchers.IO) { bootStartDispatcher.onServiceConnected() }
    }

    protected open val prefetchesMascot: Boolean get() = true

    protected open val checksAccessibility: Boolean get() = true

    internal open fun bootCount(): Int? = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1).takeIf { it >= 0 }

    protected open fun createHomeStateReader(): HomeStateReader = HomeScreenStateReader(this)

    internal open fun createLauncherSystemStore(): LauncherSystemStore = launcherPrefs

    override fun onTerminate() {
        appScope.cancel()
        super.onTerminate()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        mascotAnimation.trimMemory()
    }

    private fun createUpdateRepository(): UpdateRepository {
        val transport = HttpsUrlTransport()
        val inspector = PackageManagerApkInspector(packageManager, packageName)
        val gateway = SystemPackageInstallerGateway(this)
        val userAgentValue by lazy {
            "SygixOs/${runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()}"
        }
        val userAgent = { userAgentValue }
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
            installedVersionCode = { UpdateRelaunchReceiver.installedVersionCode(this) ?: 0L },
            scope = appScope,
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
