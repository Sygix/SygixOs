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
    private val motion: SystemMotionSource,
    val mascot: MascotAnimationSource,
    startupClock: () -> Long = SystemClock::uptimeMillis,
    startupDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    upNextSource: UpNextSource? = null,
    upNextMemory: UpNextMemory = InMemoryUpNextMemory(),
    private val upNextOpener: UpNextOpener = UpNextOpener { _, _ -> false },
    private val onArtworkForeground: () -> Unit = {},
    private val onArtworkBackground: () -> Unit = {},
    private val order: fr.sygix.sygixos.domain.HeroOrder = HeroSessionOrder(),
    private val programClock: () -> Long = System::currentTimeMillis,
    val tracking: fr.sygix.sygixos.tracking.TrackingRuntime? = null,
    private val projectionDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    val clock: StateFlow<String> = clockSource.time()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), clockSource.current())

    val updateBadge: StateFlow<Boolean> = updates.status
        .map { it.badge }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), updates.status.value.badge)

    private val systemMotion = MutableStateFlow(true)

    val visualMotion: StateFlow<Boolean> = combine(apps.visualMotion, systemMotion) { enabled, system -> enabled && system }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private val rawFeed = MutableStateFlow(HeroFeed.Empty)
    private var projectedPublications = emptyList<HeroItem>()
    private var fallbackItems: List<HeroItem> = emptyList()
    private val validated = MutableStateFlow<Map<String, String>>(emptyMap())
    private val retainedHero = MutableStateFlow<Map<String, HeroItem>>(emptyMap())
    private val retryable = MutableStateFlow<Set<String>>(emptySet())
    private val shelfRetryable = MutableStateFlow<Set<String>>(emptySet())
    private val heroVisuals = combine(validated, retainedHero, retryable) { ready, retained, failed -> Triple(ready, retained, failed) }
    private val checked = MutableStateFlow<Set<String>>(emptySet())
    private val shelfResults = MutableStateFlow<Map<String, String>>(emptyMap())
    private val retainedShelf = MutableStateFlow<Map<String, HeroItem>>(emptyMap())
    private val shelfVisuals = combine(shelfResults, retainedShelf) { ready, retained -> ready to retained }
    private val shelfChecked = MutableStateFlow<Set<String>>(emptySet())
    private var programExpiry: Job? = null
    private var generationSequence = 0L
    private var foreground = true
    private val generations = mutableMapOf<String, Long>()
    private var lastHeroItems: List<HeroItem> = emptyList()
    private val heroChecks = mutableMapOf<String, fr.sygix.sygixos.model.ProgramArtwork>()
    private val shelfChecks = mutableMapOf<String, fr.sygix.sygixos.model.ProgramArtwork>()
    private var heroValidationJob: Job? = null
    private var heroRefreshJob: Job? = null
    private var shelfValidationJob: Job? = null
    private var shelfValidationUris: List<String> = emptyList()
    private var lastHeroUris: List<String> = emptyList()
    private var lastItems: List<HeroItem> = emptyList()
    private var lastShelfPackage: String? = null
    private var shelfValidationPackage: String? = null
    private var disabledHeroSources = emptySet<String>()
    private val validating = java.util.concurrent.ConcurrentHashMap.newKeySet<Pair<Boolean, String>>()
    private val validationBatches = java.util.concurrent.ConcurrentHashMap<Job, Boolean>()

    private val upNext = upNextSource?.let {
        UpNextController(it, apps.upNextVisible, apps.upNextPosition, apps.disabledSources, viewModelScope, upNextMemory,
            tracking = tracking?.state ?: MutableStateFlow(fr.sygix.sygixos.tracking.TrackingState()), enrichment = tracking?.enrichment?.contents ?: MutableStateFlow(emptyMap()),
            readers = tracking?.readers ?: MutableStateFlow(emptyMap()), projectionDispatcher = projectionDispatcher, clock = programClock)
    }

    val upNextUpdates: Flow<Unit> = upNext?.updates ?: emptyFlow()

    val heroUpdates: Flow<Unit> = merge(hero.changes(), artwork.changes.drop(1).map { Unit }, flow { while (true) { delay(HERO_TTL_MS); emit(Unit) } })
        .onEach { refreshHero(); refreshUpNext() }

    private val visibleFeed = combine(rawFeed, tracking?.state ?: MutableStateFlow(fr.sygix.sygixos.tracking.TrackingState()),
        tracking?.enrichment?.contents ?: MutableStateFlow(emptyMap()), upNextMemory.removed, apps.disabledSources) { feed, session, _, removed, disabled ->
        val visible = if (tracking == null) feed else kotlinx.coroutines.withContext(projectionDispatcher) {
            val projected = fr.sygix.sygixos.tracking.TrackingHeroSource.project(fr.sygix.sygixos.domain.HeroRead(feed.items, shelfItems = feed.shelfItems), session,
                removed.map { it.key }, tracking.enrichment.items(), disabled)
            feed.copy(items = projected.items, fromApps = feed.fromApps || projected.items.any { it.tracking != null }, shelfItems = projected.shelfItems)
        }
        projectedPublications = visible.publications
        visible.copy(items = order.arrange(visible.items))
    }

    val state: StateFlow<HomeState> = combine(
        apps.catalog,
        visibleFeed,
        apps.disabledSources,
        heroVisuals,
        checked,
    ) { catalog, feed, disabled, visualState, c ->
        val (v, retained, failed) = visualState
        disabledHeroSources = disabled
        val epoch = generations.toMap()
        val fallback = fallbackItems
        val (hero, shown) = kotlinx.coroutines.withContext(projectionDispatcher) {
            val shown = feed.withSources(disabled, fallback)
            val items = shown.items.filter { it.videoUrl != null || it.id in v || it.id in failed || it.id in retained }.map { item ->
                (v[item.id]?.let { item.copy(imageUrl = it) } ?: retained[item.id] ?: item.copy(artworkFailed = item.id in failed)).copy(expiresAt = Long.MAX_VALUE)
            }
            HeroState(items.ifEmpty { fallback }, shown.fromApps && items.isNotEmpty(), validated = v.values.toSet(), checked = c,
                shelfItems = shown.shelfItems ?: shown.items.filter { it.tracking == null }, generations = epoch.filterKeys { id -> items.any { it.id == id } }) to shown
        }
        val heroItems = shown.items.asSequence()
            .take(VisualQuality.HERO_VALIDATED)
            .toList()
        launchHeroValidationIfNeeded(heroItems)
        if (shown.publications != lastItems) {
            lastItems = shown.publications
            lastShelfPackage?.let { requestShelf(it, hero.shelfItems.orEmpty()) }
        }
        HomeState.Ready(catalog, hero)
    }.combine(shelfVisuals) { ready, visualState ->
        val (results, retained) = visualState
        val original = ready.hero.shelfItems.orEmpty()
        ready.copy(hero = ready.hero.copy(
            shelfItems = original.map { item -> (results[item.id]?.let { item.copy(imageUrl = it) } ?: retained[item.id] ?: item).copy(expiresAt = Long.MAX_VALUE) },
            shelfValidated = results.values.toSet() + retained.values.mapNotNull { it.imageUrl },
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
        tracking?.let { runtime ->
            runtime.controller.restore()
            viewModelScope.launch { runtime.opened.collect { request ->
                if (request.origin == fr.sygix.sygixos.tracking.PlaybackOrigin.UP_NEXT) upNext?.opened(fr.sygix.sygixos.tracking.TrackingProjection.card(request.content))
            } }
        }
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
        val identities = items.map { it.artwork.identity }
        lastHeroItems = items
        if (identities == lastHeroUris) return
        lastHeroUris = identities
        validateHero()
    }

    private fun validateHero() {
        heroValidationJob = launchValidation(lastHeroItems.filter { it.tracking != null || it.sourcePackage !in disabledHeroSources }.take(VisualQuality.HERO_VALIDATED), holdAfterFirstDuringStartup = true)
    }

    fun refreshUpNext() { upNext?.refresh() }

    fun openUpNext(item: UpNextItem, source: UpNextSourceEntry? = null) {
        if (item.tracking?.membership == fr.sygix.sygixos.tracking.AccountMembership.PRESENT && tracking != null) {
            tracking.playback.open(fr.sygix.sygixos.tracking.PlaybackRequest(item.tracking, fr.sygix.sygixos.tracking.PlaybackOrigin.UP_NEXT,
                item.sources.mapNotNull { entry -> entry.targetIdentity?.let { target -> fr.sygix.sygixos.tracking.PlaybackTarget(entry.packageName, target, entry.intentUri) } }))
            return
        }
        if (upNextOpener.open(item, source)) upNext?.opened(item)
    }

    fun openHero(item: HeroItem, local: () -> Unit) {
        if (item.tracking != null && tracking != null) tracking.playback.open(fr.sygix.sygixos.tracking.PlaybackRequest(item.tracking, fr.sygix.sygixos.tracking.PlaybackOrigin.HERO, item.playbackTargets)) else local()
    }

    fun removeUpNext(item: UpNextItem) { upNext?.remove(item) }

    fun onUpNextReturnHandled(id: Long) { upNext?.returnHandled(id) }

    fun onForeground() {
        foreground = true
        tracking?.onForeground()
        expirePrograms()
        onArtworkForeground()
        updates.onForeground()
        viewModelScope.launch(io) { systemMotion.value = motion.animationsEnabled() }
    }

    fun onDeparture() { tracking?.playback?.onDeparture() }

    fun onBackground() {
        foreground = false
        tracking?.onBackground()
        onArtworkBackground()
        validationBatches.keys.forEach { it.cancel() }
    }

    fun onHeroDisplayed(id: String?) { order.displayedId = id }

    fun onHomeShown() = updates.onHomeShown()

    fun refreshHero() {
        expirePrograms()
        heroRefreshJob?.cancel()
        heroRefreshJob = viewModelScope.launch {
            fallbackItems = hero.fallbackItems()
            val feed = hero.load()
            val previous = rawFeed.value.publications.associateBy { it.artwork.identity }
            val checkedHero = heroChecks.values.map { it.identity }.toSet()
            val checkedShelf = shelfChecks.values.map { it.identity }.toSet()
            val retryHero = rawFeed.value.publications.filter { it.id in retryable.value }.map { it.artwork.identity }.toSet()
            val retryShelf = rawFeed.value.publications.filter { it.id in shelfRetryable.value }.map { it.artwork.identity }.toSet()
            val previousByContent = rawFeed.value.publications.associateBy(::contentIdentity)
            fun retained(previousImages: Map<String, String>, alreadyRetained: Map<String, HeroItem>): Map<String, HeroItem> = feed.publications.mapNotNull { item ->
                val old = previousByContent[contentIdentity(item)]
                val held = alreadyRetained.values.firstOrNull { contentIdentity(it) == contentIdentity(item) }
                val retained = held ?: old?.takeIf { it.artwork.identity != item.artwork.identity }?.let { oldItem -> previousImages[oldItem.id]?.let { oldItem.copy(imageUrl = it) } }
                retained?.copy(id = item.id)?.let { item.id to it }
            }.toMap()
            retainedHero.value = retained(validated.value, retainedHero.value)
            retainedShelf.value = retained(shelfResults.value, retainedShelf.value)
            val previousValidated = rawFeed.value.publications.mapNotNull { old -> validated.value[old.id]?.let { old.artwork.identity to it } }.toMap()
            val previousShelf = rawFeed.value.publications.mapNotNull { old -> shelfResults.value[old.id]?.let { old.artwork.identity to it } }.toMap()
            val ids = feed.publications.map { it.id }.toSet()
            generations.keys.retainAll(ids)
            heroChecks.keys.retainAll(ids)
            shelfChecks.keys.retainAll(ids)
            retryable.update { it intersect ids }
            shelfRetryable.update { it intersect ids }
            feed.publications.forEach { item ->
                if (item.artwork.identity in checkedHero) heroChecks[item.id] = item.artwork
                if (item.artwork.identity in checkedShelf) shelfChecks[item.id] = item.artwork
                if (item.artwork.identity in retryHero) retryable.update { it + item.id }
                if (item.artwork.identity in retryShelf) shelfRetryable.update { it + item.id }
                if (previous[item.artwork.identity] == null) {
                    generations[item.id] = ++generationSequence
                    heroChecks.remove(item.id)
                    shelfChecks.remove(item.id)
                }
            }
            validated.value = feed.publications.mapNotNull { item -> previousValidated[item.artwork.identity]?.let { item.id to it } }.toMap()
            shelfResults.value = feed.publications.mapNotNull { item -> previousShelf[item.artwork.identity]?.let { item.id to it } }.toMap()
            rawFeed.value = feed
            val kept = feed.publications.flatMap { it.artwork.candidates }.toSet() + validated.value.values
            checked.update { it intersect kept }
            shelfChecked.update { it intersect (kept + shelfResults.value.values) }
            if (heroValidationJob?.isActive != true) validateHero()
            scheduleProgramExpiry()
        }
    }

    private fun expirePrograms() {
        val now = programClock()
        rawFeed.update { it.copy(items = it.items.filter { item -> item.expiresAt > now }, shelfItems = it.shelfItems?.filter { item -> item.expiresAt > now }) }
        val live = rawFeed.value.publications.map { it.id }.toSet()
        generations.keys.retainAll(live)
        heroChecks.keys.retainAll(live)
        shelfChecks.keys.retainAll(live)
        retryable.update { it intersect live }
        shelfRetryable.update { it intersect live }
        validated.update { it.filterKeys { id -> id in live } }
        shelfResults.update { it.filterKeys { id -> id in live } }
        retainedHero.update { it.filterKeys { id -> id in live } }
        retainedShelf.update { it.filterKeys { id -> id in live } }
    }

    private fun scheduleProgramExpiry() {
        programExpiry?.cancel()
        val deadline = rawFeed.value.publications.minOfOrNull { it.expiresAt } ?: return
        if (deadline == Long.MAX_VALUE) return
        programExpiry = viewModelScope.launch {
            delay((deadline - programClock()).coerceAtLeast(1))
            expirePrograms()
            scheduleProgramExpiry()
        }
    }

    fun prepareShelf(packageName: String) {
        lastShelfPackage = packageName
        requestShelf(packageName, (state.value as? HomeState.Ready)?.hero?.shelfItems ?: emptyList())
    }

    private fun requestShelf(packageName: String, items: List<HeroItem>) {
        val candidates = (rawFeed.value.shelfItems ?: rawFeed.value.items).filter { it.tracking == null && it.sourcePackage == packageName && items.any { visible -> visible.sourcePackage == packageName } }.take(VisualQuality.SHELF_VALIDATED_PER_APP)
        val uris = candidates.map { it.id + it.artwork.toString() + generations[it.id] }
        if (uris == shelfValidationUris && shelfValidationJob?.isActive == true) return
        if (shelfValidationPackage != packageName) validationBatches.filterValues { it }.keys.forEach { it.cancel() }
        shelfValidationPackage = packageName
        shelfValidationUris = uris
        shelfValidationJob = launchValidation(candidates, shelf = true)
    }

    private fun launchValidation(items: List<HeroItem>, holdAfterFirstDuringStartup: Boolean = false, shelf: Boolean = false): Job? {
        if (!foreground) return null
        val results = if (shelf) shelfResults else validated
        val checks = if (shelf) shelfChecks else heroChecks
        val retries = if (shelf) shelfRetryable else retryable
        val pending = items.filter { item ->
            (checks.values.none { it.identity == item.artwork.identity } || item.id in retries.value) && validating.add(shelf to item.artwork.identity)
        }
        if (pending.isEmpty()) return null
        val job = viewModelScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try { kotlinx.coroutines.coroutineScope { pending.groupBy { it.sourcePackage }.values.forEach { group -> launch {
                group.forEach { item ->
                    if (projectedPublications.none { it.artwork.identity == item.artwork.identity }) return@forEach
                    val policy = if (shelf) ArtworkPolicy.Shelf(shelfWidth) else ArtworkPolicy.Hero
                    val check = if (shelf) validator.prepare(item.artwork, shelfWidth) else validator.validate(item.artwork, policy)
                    val current = projectedPublications.firstOrNull { it.artwork.identity == item.artwork.identity } ?: return@forEach
                    checks[current.id] = current.artwork
                    retries.update { if (check.retryable) it + current.id else it - current.id }
                    checked.update { it + item.artwork.candidates + listOfNotNull(check.visual) }
                    if (shelf) shelfChecked.update { it + item.artwork.candidates + listOfNotNull(check.visual) }
                    results.update { values -> check.visual?.let { values + (current.id to it) } ?: (values - current.id) }
                    if (shelf) retainedShelf.update { it - current.id } else retainedHero.update { it - current.id }
                    validating.remove(shelf to item.artwork.identity)
                    if (holdAfterFirstDuringStartup && check.usable) startup.first { it == StartupPhase.Done }
                }
            } } } } finally { pending.forEach { validating.remove(shelf to it.artwork.identity) } }
        }
        validationBatches[job] = shelf
        job.invokeOnCompletion { validationBatches.remove(job) }
        job.start()
        return job
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

    internal fun observeArtwork(events: Flow<fr.sygix.sygixos.data.ArtworkEvent>) = viewModelScope.launch {
        events.collect { event ->
            if (event is fr.sygix.sygixos.data.ArtworkEvent.ProgramsChanged) return@collect
            val affected = rawFeed.value.publications.filter { item -> event.uri in item.artwork.candidates.flatMap { fr.sygix.sygixos.data.visualCandidates(it) } }
            val heroAffected = affected.filter { validated.value[it.id].let { chosen -> chosen == null || chosen == event.uri } }
            val shelfAffected = affected.filter { shelfResults.value[it.id].let { chosen -> chosen == null || chosen == event.uri } }
            heroAffected.forEach { item ->
                heroChecks.remove(item.id)
                if (event is fr.sygix.sygixos.data.ArtworkEvent.ImageExpired ||
                    event is fr.sygix.sygixos.data.ArtworkEvent.ImageFailed && event.cause == fr.sygix.sygixos.data.ArtworkFailure.QUALITY) {
                    retryable.update { it - item.id }
                    validated.update { it - item.id }
                }
            }
            shelfAffected.forEach { item ->
                shelfChecks.remove(item.id)
                if (event is fr.sygix.sygixos.data.ArtworkEvent.ImageExpired ||
                    event is fr.sygix.sygixos.data.ArtworkEvent.ImageFailed && event.cause == fr.sygix.sygixos.data.ArtworkFailure.QUALITY) {
                    shelfRetryable.update { it - item.id }
                    shelfResults.update { it - item.id }
                }
            }
            launchValidation(heroAffected)
            lastShelfPackage?.let { pkg -> launchValidation(shelfAffected.filter { it.sourcePackage == pkg }, shelf = true) }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as SygixOsApp
            return HomeViewModel(
                apps = app.appCatalogRepository,
                hero = HeroRepository(fr.sygix.sygixos.tracking.TrackingHeroSource(app.tvProviderHeroSource), NatureFallbackProvider()),
                artwork = app.appArtwork,
                validator = VisualValidator(app).also { it.repository = app.programArtwork },
                clockSource = SystemClockSource(app),
                updates = app.updateRepository,
                session = app.startupSession,
                motion = SettingsMotionSource(app.contentResolver),
                mascot = app.mascotAnimation,
                upNextSource = TvProviderUpNextSource(app, onSnapshot = { items -> app.tracking.enrichment.local(items); app.programArtwork.confirm("watchnext", items.map { it.artwork to it.toString() }) }),
                upNextMemory = app.launcherPrefs,
                onArtworkForeground = app.programArtwork::onForeground,
                onArtworkBackground = app.programArtwork::onBackground,
                order = app.heroOrder,
                tracking = app.tracking,
                projectionDispatcher = Dispatchers.Default,
                upNextOpener = UpNextOpener { item, source ->
                    if (source == null) AppLauncher.open(app, item) else AppLauncher.openWith(app, item, source)
                },
            ).also { vm ->
                vm.setShelfWidth(((app.resources.configuration.screenWidthDp - 2 * fr.sygix.sygixos.core.designsystem.Dimens.ScreenMarginH.value) * app.resources.displayMetrics.density).toInt())
                vm.observeArtwork(app.programArtwork.events)
            } as T
        }
    }
}

internal const val HERO_TTL_MS = 15 * 60_000L

private fun contentIdentity(item: HeroItem) = listOf(item.sourcePackage, item.id.substringBefore('-'), item.title, item.launchUri,
    item.internalProviderId, item.contentId, item.season, item.episode, item.seriesId, item.releaseDate)

private val SplashTimings = StartupTimings(
    minMs = Motion.SPLASH_MIN_MS,
    visualCapMs = Motion.SPLASH_VISUAL_CAP_MS,
    capMs = Motion.SPLASH_CAP_MS,
)
