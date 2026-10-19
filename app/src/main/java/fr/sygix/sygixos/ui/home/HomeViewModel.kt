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
import fr.sygix.sygixos.data.SystemClockSource
import fr.sygix.sygixos.data.UpdateController
import fr.sygix.sygixos.data.SystemMotionSource
import fr.sygix.sygixos.data.TvProviderUpNextSource
import fr.sygix.sygixos.data.VisualValidator
import fr.sygix.sygixos.domain.HeroFeed
import fr.sygix.sygixos.domain.HeroSessionOrder
import fr.sygix.sygixos.domain.ArtworkPolicy
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HeroState(
    val items: List<HeroItem>,
    val fromApps: Boolean,
    val validated: Set<String> = emptySet(),
    val checked: Set<String> = emptySet(),
    val shelfItems: List<HeroItem>? = null,
    val shelfValidated: Set<String>? = null,
    val generations: Map<String, Long> = emptyMap(),
    val shelfChecked: Set<String>? = null,
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
    private val onArtworkForeground: () -> Unit = {},
    private val order: fr.sygix.sygixos.domain.HeroOrder = HeroSessionOrder(),
) : ViewModel() {

    val clock: StateFlow<String> = clockSource.time()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), clockSource.current())

    val updateBadge: StateFlow<Boolean> = updates.status
        .map { it.badge }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), updates.status.value.badge)

    private val rawFeed = MutableStateFlow(HeroFeed.Empty)
    private var fallbackItems: List<HeroItem> = emptyList()
    private val validated = MutableStateFlow<Map<String, String>>(emptyMap())
    private val checked = MutableStateFlow<Set<String>>(emptySet())
    private val shelfResults = MutableStateFlow<Map<String, String>>(emptyMap())
    private val shelfChecked = MutableStateFlow<Set<String>>(emptySet())
    private var programExpiry: Job? = null
    private var generationSequence = 0L
    private var foreground = true
    private val generations = mutableMapOf<String, Long>()
    private var lastHeroItems: List<HeroItem> = emptyList()
    private val retryable = mutableSetOf<String>()
    private val heroChecks = mutableMapOf<String, fr.sygix.sygixos.model.ProgramArtwork>()
    private val shelfChecks = mutableMapOf<String, fr.sygix.sygixos.model.ProgramArtwork>()
    private var heroValidationJob: Job? = null
    private var heroRefreshJob: Job? = null
    private var shelfValidationJob: Job? = null
    private var shelfValidationUris: List<String> = emptyList()
    private var lastHeroUris: List<String> = emptyList()
    private var lastItems: List<HeroItem> = emptyList()
    private var lastShelfPackage: String? = null

    private val upNext = upNextSource?.let {
        UpNextController(it, apps.upNextVisible, apps.upNextPosition, apps.disabledSources, viewModelScope, upNextMemory)
    }

    val upNextUpdates: Flow<Unit> = upNext?.updates ?: emptyFlow()

    val heroUpdates: Flow<Unit> = merge(hero.changes(), artwork.changes.drop(1).map { Unit }, flow { while (true) { delay(HERO_TTL_MS); emit(Unit) } })
        .onEach { refreshHero(); refreshUpNext() }

    val state: StateFlow<HomeState> = combine(
        apps.catalog,
        rawFeed,
        apps.disabledSources,
        validated,
        checked,
    ) { catalog, feed, disabled, v, c ->
        val shown = feed.withSources(disabled, fallbackItems)
        val items = shown.items.map { item -> (v[item.id]?.let { item.copy(imageUrl = it) } ?: item).copy(expiresAt = Long.MAX_VALUE) }
        val hero = HeroState(items, shown.fromApps, validated = v.values.toSet(), checked = c, generations = generations.filterKeys { id -> items.any { it.id == id } })
        val heroItems = shown.items.asSequence()
            .take(VisualQuality.HERO_VALIDATED)
            .toList()
        launchHeroValidationIfNeeded(heroItems)
        if (shown.items != lastItems) {
            lastItems = shown.items
            lastShelfPackage?.let { requestShelf(it, shown.items) }
        }
        HomeState.Ready(catalog, hero)
    }.combine(shelfResults) { ready, results ->
        val original = rawFeed.value.items
        ready.copy(hero = ready.hero.copy(
            shelfItems = original.filter { item -> ready.hero.items.any { it.id == item.id } }.map { item -> (results[item.id]?.let { item.copy(imageUrl = it) } ?: item).copy(expiresAt = Long.MAX_VALUE) },
            shelfValidated = results.values.toSet(),
        ))
    }.combine(shelfChecked) { ready, checked ->
        ready.copy(hero = ready.hero.copy(shelfChecked = checked))
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
        viewModelScope.launch { artwork.changes.collect { apps.refreshApps() } }
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

    private fun launchHeroValidationIfNeeded(items: List<HeroItem>) {
        val identities = items.map { it.id + it.artwork.toString() + generations[it.id] }
        if (identities == lastHeroUris) return
        lastHeroUris = identities
        lastHeroItems = items
        heroValidationJob?.cancel()
        validateHero()
    }

    private fun validateHero() {
        heroValidationJob = launchValidation(lastHeroItems, holdAfterFirstDuringStartup = true)
    }

    fun refreshUpNext() { upNext?.refresh() }

    fun openUpNext(item: UpNextItem, source: UpNextSourceEntry? = null) {
        if (upNextOpener.open(item, source)) upNext?.opened(item)
    }

    fun removeUpNext(item: UpNextItem) { upNext?.remove(item) }

    fun onUpNextReturnHandled(id: Long) { upNext?.returnHandled(id) }

    fun onForeground() { foreground = true; expirePrograms(); onArtworkForeground(); updates.onForeground() }

    fun onBackground() {
        foreground = false
        heroValidationJob?.cancel()
        shelfValidationJob?.cancel()
    }

    fun onHeroDisplayed(id: String?) { order.displayedId = id }

    fun onHomeShown() = updates.onHomeShown()

    fun refreshHero() {
        expirePrograms()
        heroRefreshJob?.cancel()
        heroRefreshJob = viewModelScope.launch {
            fallbackItems = hero.fallbackItems()
            val feed = hero.load()
            val previous = rawFeed.value.items.associateBy { it.id }
            val ids = feed.items.map { it.id }.toSet()
            generations.keys.retainAll(ids)
            heroChecks.keys.retainAll(ids)
            shelfChecks.keys.retainAll(ids)
            retryable.retainAll(ids)
            feed.items.forEach { item ->
                if (previous[item.id]?.copy(expiresAt = Long.MAX_VALUE) != item.copy(expiresAt = Long.MAX_VALUE)) {
                    generations[item.id] = ++generationSequence
                    heroChecks.remove(item.id)
                    shelfChecks.remove(item.id)
                }
            }
            val live = feed.items.filter { previous[it.id]?.artwork == it.artwork }.map { it.id }.toSet()
            validated.update { it.filterKeys { id -> id in live } }
            shelfResults.update { it.filterKeys { id -> id in live } }
            rawFeed.value = feed.copy(items = order.arrange(feed.items))
            val kept = feed.items.flatMap { it.artwork.candidates }.toSet() + validated.value.values
            checked.update { it intersect kept }
            shelfChecked.update { it intersect (kept + shelfResults.value.values) }
            if (heroValidationJob?.isActive != true) validateHero()
            scheduleProgramExpiry()
        }
    }

    private fun expirePrograms() {
        val now = System.currentTimeMillis()
        rawFeed.update { it.copy(items = order.arrange(it.items.filter { item -> item.expiresAt > now })) }
        val live = rawFeed.value.items.map { it.id }.toSet()
        generations.keys.retainAll(live)
        heroChecks.keys.retainAll(live)
        shelfChecks.keys.retainAll(live)
        retryable.retainAll(live)
        validated.update { it.filterKeys { id -> id in live } }
        shelfResults.update { it.filterKeys { id -> id in live } }
    }

    private fun scheduleProgramExpiry() {
        programExpiry?.cancel()
        val deadline = rawFeed.value.items.minOfOrNull { it.expiresAt } ?: return
        if (deadline == Long.MAX_VALUE) return
        programExpiry = viewModelScope.launch {
            delay((deadline - System.currentTimeMillis()).coerceAtLeast(1))
            expirePrograms()
            scheduleProgramExpiry()
        }
    }

    fun prepareShelf(packageName: String) {
        lastShelfPackage = packageName
        requestShelf(packageName, (state.value as? HomeState.Ready)?.hero?.items ?: emptyList())
    }

    private fun requestShelf(packageName: String, items: List<HeroItem>) {
        val candidates = items.filter { it.sourcePackage == packageName }.take(VisualQuality.SHELF_VALIDATED_PER_APP)
        val uris = candidates.map { it.id + it.artwork.toString() + generations[it.id] }
        if (uris == shelfValidationUris && shelfValidationJob?.isActive == true) return
        shelfValidationJob?.cancel()
        shelfValidationUris = uris
        shelfValidationJob = launchValidation(candidates, shelf = true)
    }

    private fun launchValidation(items: List<HeroItem>, holdAfterFirstDuringStartup: Boolean = false, shelf: Boolean = false): Job? {
        if (!foreground) return null
        val results = if (shelf) shelfResults else validated
        val checks = if (shelf) shelfChecks else heroChecks
        val pending = items.filter { checks[it.id] != it.artwork || it.id in retryable }
        if (pending.isEmpty()) return null
        return viewModelScope.launch {
            pending.groupBy { it.sourcePackage }.values.forEach { group -> launch {
                group.forEach { item ->
                    val generation = generations[item.id]
                    val policy = if (shelf) ArtworkPolicy.Shelf(shelfWidth) else ArtworkPolicy.Hero
                    val check = if (shelf) validator.prepare(item.artwork, shelfWidth) else validator.validate(item.artwork, policy)
                    val current = rawFeed.value.items.firstOrNull { it.id == item.id }
                    if (current?.artwork != item.artwork || generation != generations[item.id]) return@forEach
                    checks[item.id] = item.artwork
                    checked.update { it + item.artwork.candidates + listOfNotNull(check.visual) }
                    if (shelf) shelfChecked.update { it + item.artwork.candidates + listOfNotNull(check.visual) }
                    check.visual?.let { visual -> results.update { it + (item.id to visual) } }
                    if (check.retryable) retryable += item.id else retryable -= item.id
                    if (holdAfterFirstDuringStartup && check.usable) startup.first { it == StartupPhase.Done }
                }
            } }
        }
    }

    private var shelfWidth = 640
    fun setShelfWidth(width: Int) { shelfWidth = width.coerceAtLeast(1) }

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
                onArtworkForeground = app.programArtwork::onForeground,
                order = app.heroOrder,
                upNextOpener = UpNextOpener { item, source ->
                    if (source == null) AppLauncher.open(app, item) else AppLauncher.openWith(app, item, source)
                },
            ).also { vm ->
                vm.setShelfWidth(((app.resources.configuration.screenWidthDp - 2 * fr.sygix.sygixos.core.designsystem.Dimens.ScreenMarginH.value) * app.resources.displayMetrics.density).toInt())
                vm.viewModelScope.launch { app.programArtwork.changes.collect {
                    vm.validated.value = emptyMap()
                    vm.shelfResults.value = emptyMap()
                    vm.checked.value = emptySet()
                    vm.shelfChecked.value = emptySet()
                    vm.heroChecks.clear()
                    vm.shelfChecks.clear()
                    vm.validateHero()
                    vm.lastShelfPackage?.let { pkg -> vm.prepareShelf(pkg) }
                } }
            } as T
        }
    }
}

internal const val HERO_TTL_MS = 15 * 60_000L

private val SplashTimings = StartupTimings(
    minMs = Motion.SPLASH_MIN_MS,
    visualCapMs = Motion.SPLASH_VISUAL_CAP_MS,
    capMs = Motion.SPLASH_CAP_MS,
)
