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
import fr.sygix.sygixos.data.ProgramArtworkInterceptor
import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.domain.StartupSession

open class SygixOsApp : Application(), ImageLoaderFactory, fr.sygix.sygixos.data.InstallStatusRouting {

    val installedAppsSource: InstalledAppsSource by lazy { InstalledAppsSource(this) }
    val launcherPrefs: LauncherPrefs by lazy { LauncherPrefs(this) }
    val appCatalogRepository: AppCatalogRepository by lazy { AppCatalogRepository(installedAppsSource, launcherPrefs) }
    val tvProviderHeroSource: TvProviderHeroSource by lazy {
        TvProviderHeroSource(this, onSnapshot = { table, items -> programArtwork.confirm(table, items.map { it.artwork to it.fullFingerprint }) })
    }
    val appArtwork: AppArtworkSource by lazy {
        val tile = Dimens.gridTileWidthPx(resources.configuration.screenWidthDp, resources.displayMetrics.density)
        AppArtworkSource(packageManager, bounds = ImageBounds.tile(tile))
    }
    val startupSession = StartupSession()
    val heroOrder = fr.sygix.sygixos.domain.HeroSessionOrder()
    val foregroundTracker = ForegroundTracker()
    private val imageDisk by lazy { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(256L * 1024 * 1024).build() }
    private val imageHttp by lazy { okhttp3.OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build() }
    private val programArtworkInstance = lazy {
        fr.sygix.sygixos.data.ArtworkRepository(this, fr.sygix.sygixos.data.ArtworkDatabase.create(this), imageDisk,
            fr.sygix.sygixos.data.ProgramImageReader(contentResolver, imageHttp),
            cardWidth = (((resources.configuration.screenWidthDp - 2 * Dimens.ScreenMarginH.value - 3 * Dimens.GridSpacing.value) / 4) * resources.displayMetrics.density).toInt())
    }
    internal val programArtwork by programArtworkInstance
    internal val videoCache by lazy { fr.sygix.sygixos.data.HeroVideoCache(this) }
    val updatePrefs: UpdatePrefs by lazy { UpdatePrefs(this) }
    val updateRepository: UpdateRepository by lazy { createUpdateRepository() }
    val mascotAnimation: RawMascotAnimationSource by lazy { RawMascotAnimationSource(resources) }
    private val appScope = CoroutineScope(SupervisorJob() + applicationDispatcher)
    val tracking by lazy { fr.sygix.sygixos.tracking.TrackingRuntime(this, appCatalogRepository, appScope,
        onArtwork = { items -> programArtwork.confirm("tracking", items) }, onIdentities = launcherPrefs::enrich) }
    private val userAgentValue by lazy {
        "SygixOs/${runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()}"
    }
    private val userAgent: () -> String = { userAgentValue }
    val tsuyuReleases by lazy {
        fr.sygix.sygixos.data.TsuyuReleaseSource(
            GitHubReleaseSource(HttpsUrlTransport(), userAgent, apiBase = fr.sygix.sygixos.data.TsuyuReleaseSource.API_BASE),
            fr.sygix.sygixos.data.TsuyuReleasePrefs(this),
        )
    }
    val tsuyuInstaller by lazy {
        fr.sygix.sygixos.data.TsuyuApkInstaller(
            releases = tsuyuReleases,
            includePrereleases = { updatePrefs.data.first().includePrereleases },
            transport = HttpsUrlTransport(),
            inspector = PackageManagerApkInspector(packageManager, packageName),
            gateway = SystemPackageInstallerGateway(this, fr.sygix.sygixos.domain.TsuyuPackage),
            downloadDir = cacheDir.resolve("tsuyu"),
            userAgent = userAgent,
            showUserAction = { intent -> if (!launchFromForeground(intent)) throw android.content.ActivityNotFoundException() },
        )
    }
    override val installStatusRouter by lazy {
        fr.sygix.sygixos.data.InstallStatusRouter(listOf(tsuyuInstaller), updateRepository::onInstallStatus)
    }
    private val shizukuApi by lazy { fr.sygix.sygixos.data.AndroidShizukuApi(this) }
    val tsuyuLink by lazy { fr.sygix.sygixos.data.ShizukuTsuyuLink(shizukuApi, packageManager, ::launchFromForeground) }
    private val privilegedAccess by lazy {
        fr.sygix.sygixos.data.SwitchingAccess(tsuyuLink, fr.sygix.sygixos.data.TsuyuAccess(shizukuApi), fr.sygix.sygixos.data.AdbAccessGateway.create(this))
    }

    private fun launchFromForeground(intent: Intent): Boolean {
        val activity = foregroundTracker.resumed?.get() ?: return false
        return runCatching {
            if (intent.resolveActivity(packageManager) == null) false else { activity.startActivity(intent); true }
        }.getOrDefault(false)
    }

    private fun openSettings(vararg actions: String): Boolean = actions.any { action ->
        runCatching {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (packageManager.resolveActivity(intent, 0) == null) false else { startActivity(intent); true }
        }.getOrDefault(false)
    }

    val defaultLauncherController by lazy {
        val reader = createHomeStateReader()
        val access = privilegedAccess
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
            openDebug = { openSettings(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS, Settings.ACTION_DEVICE_INFO_SETTINGS) },
            openAbout = { openSettings(Settings.ACTION_DEVICE_INFO_SETTINGS, Settings.ACTION_SETTINGS) },
            devOptions = { Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0 },
            tsuyu = fr.sygix.sygixos.domain.TsuyuSupport(
                link = tsuyuLink,
                releases = tsuyuReleases,
                installer = tsuyuInstaller,
                includePrereleases = { updatePrefs.data.first().includePrereleases },
                portReachable = { fr.sygix.sygixos.data.DadbLocalTransport(noBackupFilesDir).reachable() },
                usingTsuyu = { privilegedAccess.usingTsuyu },
                onForegroundReturn = { tsuyuInstaller.onForegroundReturn() },
                canInstallPackages = { packageManager.canRequestPackageInstalls() },
            ),
            bootCount = ::bootCount,
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
                appArtwork.invalidate(intent.data?.schemeSpecificPart)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        ContextCompat.registerReceiver(this, receiver, IntentFilter(Intent.ACTION_LOCALE_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun onAccessibilityConnected() {
        appScope.launch(Dispatchers.IO) { bootStartDispatcher.onServiceConnected() }
    }

    protected open val prefetchesMascot: Boolean get() = true

    protected open val checksAccessibility: Boolean get() = true

    protected open val applicationDispatcher: kotlinx.coroutines.CoroutineDispatcher get() = Dispatchers.Main.immediate

    internal open fun bootCount(): Int? = Settings.Global.getInt(contentResolver, Settings.Global.BOOT_COUNT, -1).takeIf { it >= 0 }

    protected open fun createHomeStateReader(): HomeStateReader = HomeScreenStateReader(this)

    internal open fun createLauncherSystemStore(): LauncherSystemStore = launcherPrefs

    internal open fun createHeroVideo(onEnded: () -> Unit, onError: (String) -> Unit): fr.sygix.sygixos.ui.hero.HeroVideo =
        fr.sygix.sygixos.ui.hero.HeroPlayer(this, onEnded, onError)

    override fun onTerminate() {
        appScope.cancel()
        if (programArtworkInstance.isInitialized()) programArtwork.close()
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

    override fun newImageLoader(): ImageLoader {
        val memory = MemoryCache.Builder(this).maxSizePercent(0.15).build()
        return ImageLoader.Builder(this)
            .allowRgb565(true)
            .crossfade(false)
            .respectCacheHeaders(false)
            .memoryCache(memory)
            .diskCache(imageDisk)
            .okHttpClient(imageHttp)
            .components {
                add(ProgramArtworkInterceptor(programArtwork))
                add(fr.sygix.sygixos.data.ProgramImageFetcher.Factory(imageDisk, programArtwork))
            }
            .decoderDispatcher(fr.sygix.sygixos.data.ArtworkDecodeDispatcher)
            .fetcherDispatcher(Dispatchers.IO.limitedParallelism(4))
            .build().also { loader -> programArtwork.imageLoader = { loader } }
    }
}
