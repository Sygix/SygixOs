/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.data.ClockSource
import fr.sygix.sygixos.data.HeroRepository
import fr.sygix.sygixos.data.MascotAnimationSource
import fr.sygix.sygixos.data.SettingsMotionSource
import fr.sygix.sygixos.data.NatureFallbackProvider
import fr.sygix.sygixos.data.RawMascotAnimationSource
import fr.sygix.sygixos.data.SystemClockSource
import fr.sygix.sygixos.data.UpdateController
import fr.sygix.sygixos.data.SystemMotionSource
import fr.sygix.sygixos.data.TvProviderUpNextSource
import fr.sygix.sygixos.data.VisualValidator
import fr.sygix.sygixos.domain.HeroFeed
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.domain.ShelfPosters
import fr.sygix.sygixos.domain.StartupGate
import fr.sygix.sygixos.domain.StartupPhase
import fr.sygix.sygixos.domain.StartupSession
import fr.sygix.sygixos.domain.StartupTimings
import fr.sygix.sygixos.domain.InMemoryUpNextMemory
import fr.sygix.sygixos.domain.UpNextController
import fr.sygix.sygixos.domain.UpNextMemory
import fr.sygix.sygixos.domain.UpNextOpener
import fr.sygix.sygixos.domain.UpNextSource
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.domain.VisualQuality
import fr.sygix.sygixos.domain.withSources
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HeroState(
    val items: List<HeroItem>,
    val fromApps: Boolean,
    val validated: Set<String> = emptySet(),
    val checked: Set<String> = emptySet(),
)

sealed interface HomeState {
    data object Loading : HomeState
    data class Ready(val catalog: Catalog, val hero: HeroState, val upNext: UpNextState? = null) : HomeState
}

class HomeViewModel(
    private val apps: AppCatalogRepository,
    private val hero: HeroRepository,
    val artwork: AppArtworkSource,
    private val validator: VisualValidator,
    clockSource: ClockSource,
    private val updates: UpdateController,
    session: StartupSession,
    motion: SystemMotionSource,
    val mascot: MascotAnimationSource,
    startupClock: () -> Long = SystemClock::uptimeMillis,
    startupDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    upNextSource: UpNextSource? = null,
    upNextMemory: UpNextMemory = InMemoryUpNextMemory(),
    private val upNextOpener: UpNextOpener = UpNextOpener { _, _ -> false },
) : ViewModel() {

    val clock: StateFlow<String> = clockSource.time()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), clockSource.current())

    val updateBadge: StateFlow<Boolean> = updates.status
        .map { it.badge }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), updates.status.value.badge)

    private val rawFeed = MutableStateFlow(HeroFeed.Empty)
    private var fallbackItems: List<HeroItem> = emptyList()
    private val validated = MutableStateFlow<Set<String>>(emptySet())
    private val checked = MutableStateFlow<Set<String>>(emptySet())
    private val retryable = mutableSetOf<String>()
    private var heroValidationJob: Job? = null
    private var heroRefreshJob: Job? = null
    private var shelfValidationJob: Job? = null
    private var shelfValidationUris: List<String> = emptyList()
    private var lastHeroUris: List<String> = emptyList()
    private var lastDisabled: Set<String>? = null
    private var lastShelfPackage: String? = null

    private val upNext = upNextSource?.let {
        UpNextController(it, apps.upNextVisible, apps.upNextPosition, apps.disabledSources, viewModelScope, upNextMemory)
    }

    val upNextUpdates: Flow<Unit> = upNext?.updates ?: emptyFlow()

    val state: StateFlow<HomeState> = combine(
        apps.catalog,
        rawFeed,
        apps.disabledSources,
        validated,
        checked,
    ) { catalog, feed, disabled, v, c ->
        val shown = feed.withSources(disabled, fallbackItems)
        val hero = HeroState(shown.items, shown.fromApps, validated = v, checked = c)
        val heroUris = shown.items.asSequence()
            .mapNotNull { it.imageUrl }
            .distinct()
            .take(VisualQuality.HERO_VALIDATED)
            .toList()
        launchHeroValidationIfNeeded(heroUris)
        if (disabled != lastDisabled) onSourcesChanged(disabled, shown.items)
        HomeState.Ready(catalog, hero)
    }.combine(upNext?.state ?: MutableStateFlow(null)) { ready, row ->
        ready.copy(upNext = row) as HomeState
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState.Loading)

    private val coldStart = session.claimColdStart()

    val startupAnimated: Boolean = coldStart && motion.animationsEnabled()

    private val startupController = StartupController(
        coldStart = coldStart,
        gate = StartupGate(SplashTimings, startupAnimated, startupClock),
        scope = viewModelScope,
        dispatcher = startupDispatcher,
    )

    val startup: StateFlow<StartupPhase> = startupController.phase

    init {
        viewModelScope.launch(startupDispatcher) {
            startup.first { it == StartupPhase.Done }
            mascot.release()
        }
        viewModelScope.launch(startupDispatcher) {
            val ready = state.first { it is HomeState.Ready } as HomeState.Ready
            launch(artwork.dispatcher) { artwork.preload(ready.catalog.dock + ready.catalog.grid) }
            if (coldStart) startupController.catalogReady()
        }
    }

    fun onSplashShown() = startupController.splashShown()

    fun onMascotShown() = startupController.mascotShown()

    fun onMascotUnavailable() = startupController.mascotUnavailable()

    fun onHeroVisualReady() = startupController.heroVisualReady()

    fun onSplashFadeFinished() = startupController.fadeFinished()

    private fun launchHeroValidationIfNeeded(uris: List<String>) {
        if (uris == lastHeroUris) return
        lastHeroUris = uris
        heroValidationJob?.cancel()
        validateHero()
    }

    private fun validateHero() {
        heroValidationJob = launchValidation(lastHeroUris, VisualQuality.HERO_IN_MEMORY, holdAfterFirstDuringStartup = true)
    }

    private fun onSourcesChanged(disabled: Set<String>, items: List<HeroItem>) {
        lastDisabled = disabled
        lastShelfPackage?.let { requestShelf(it, items) }
    }

    fun refresh() {
        refreshApps()
        refreshHero()
        refreshUpNext()
    }

    fun refreshUpNext() { upNext?.refresh() }

    fun openUpNext(item: UpNextItem, source: UpNextSourceEntry? = null) {
        if (upNextOpener.open(item, source)) upNext?.opened(item)
    }

    fun removeUpNext(item: UpNextItem) { upNext?.remove(item) }

    fun onUpNextReturnHandled(id: Long) { upNext?.returnHandled(id) }

    fun onForeground() = updates.onForeground()

    fun onHomeShown() = updates.onHomeShown()

    fun refreshApps() {
        viewModelScope.launch { apps.refreshApps() }
    }

    fun refreshHero() {
        heroRefreshJob?.cancel()
        heroRefreshJob = viewModelScope.launch {
            fallbackItems = hero.fallbackItems()
            rawFeed.value = hero.load()
            if (heroValidationJob?.isActive != true) validateHero()
        }
    }

    fun prepareShelf(packageName: String) {
        lastShelfPackage = packageName
        requestShelf(packageName, (state.value as? HomeState.Ready)?.hero?.items ?: emptyList())
    }

    private fun requestShelf(packageName: String, items: List<HeroItem>) {
        val uris = ShelfPosters.candidates(items, packageName, VisualQuality.SHELF_VALIDATED_PER_APP)
        if (uris == shelfValidationUris && shelfValidationJob?.isActive == true) return
        shelfValidationJob?.cancel()
        shelfValidationUris = uris
        shelfValidationJob = launchValidation(uris, keepInMemory = 0)
    }

    private fun launchValidation(uris: List<String>, keepInMemory: Int, holdAfterFirstDuringStartup: Boolean = false): Job? {
        val pending = uris.filter { it !in checked.value || it in retryable }
        if (pending.isEmpty()) return null
        return viewModelScope.launch {
            pending.forEachIndexed { index, uri ->
                var usable = false
                validator.validate(listOf(uri), if (index < keepInMemory) 1 else 0).collect { check ->
                    checked.update { it + check.uri }
                    if (check.usable) validated.update { it + check.uri }
                    if (check.retryable) retryable += check.uri else retryable -= check.uri
                    usable = check.usable
                }
                if (holdAfterFirstDuringStartup && usable) startup.first { it == StartupPhase.Done }
            }
        }
    }

    fun togglePin(app: TvApp) {
        viewModelScope.launch { apps.togglePin(app.packageName) }
    }

    fun moveInGrid(app: TvApp, delta: Int) {
        viewModelScope.launch { apps.moveInGrid(app.packageName, delta) }
    }

    fun setGridOrder(order: List<String>) {
        viewModelScope.launch { apps.setGridOrder(order) }
    }

    fun hideApp(app: TvApp) {
        viewModelScope.launch { apps.hideApp(app.packageName) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as SygixOsApp
            return HomeViewModel(
                apps = app.appCatalogRepository,
                hero = HeroRepository(app.tvProviderHeroSource, NatureFallbackProvider()),
                artwork = app.appArtwork,
                validator = VisualValidator(app),
                clockSource = SystemClockSource(app),
                updates = app.updateRepository,
                session = app.startupSession,
                motion = SettingsMotionSource(app.contentResolver),
                mascot = app.mascotAnimation,
                upNextSource = TvProviderUpNextSource(app),
                upNextMemory = app.launcherPrefs,
                upNextOpener = UpNextOpener { item, source ->
                    if (source == null) AppLauncher.open(app, item) else AppLauncher.openWith(app, item, source)
                },
            ) as T
        }
    }
}

private val SplashTimings = StartupTimings(
    minMs = Motion.SPLASH_MIN_MS,
    visualCapMs = Motion.SPLASH_VISUAL_CAP_MS,
    capMs = Motion.SPLASH_CAP_MS,
)
