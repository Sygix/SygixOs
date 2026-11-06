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
import fr.sygix.sygixos.domain.PendingChanges
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
    private val validator: fr.sygix.sygixos.data.VisualChecker,
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
    @Volatile private var projected = emptyMap<String, HeroItem>()
    private var fallbackItems: List<HeroItem> = emptyList()
    private val heroVisuals = MutableStateFlow(VisualResults())
    private val shelfVisuals = MutableStateFlow(VisualResults())
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
    private val busyRetries = java.util.concurrent.ConcurrentHashMap<String, Job>()

    private val upNext = upNextSource?.let {
        UpNextController(it, apps.upNextVisible, apps.upNextPosition, apps.disabledSources, viewModelScope, upNextMemory,
            tracking = tracking?.state ?: MutableStateFlow(fr.sygix.sygixos.tracking.TrackingState()), enrichment = tracking?.enrichment?.contents ?: MutableStateFlow(emptyMap()),
            readers = tracking?.readers ?: MutableStateFlow(emptyMap()), projectionDispatcher = projectionDispatcher, clock = programClock)
    }

    val upNextUpdates: Flow<Unit> = upNext?.updates ?: emptyFlow()

    private val heroChanges = PendingChanges(hero.changes(), viewModelScope)
    private var periodicReadAt: Long? = null

    val heroUpdates: Flow<Unit> = merge(
        heroChanges.changes.onEach { refreshHero() },
        merge(artwork.changes.drop(1).map { Unit }, periodicReads()).onEach { refreshHero(); refreshUpNext() },
    )

    private fun periodicReads(): Flow<Unit> = flow {
        while (true) {
            val now = programClock()
            val due = (periodicReadAt ?: now.also { periodicReadAt = it }) + HERO_TTL_MS
            if (now >= due) {
                periodicReadAt = now
                emit(Unit)
            } else {
                delay((due - now).coerceAtMost(HERO_TTL_MS))
            }
        }
    }

    private val visibleFeed = combine(rawFeed, tracking?.state ?: MutableStateFlow(fr.sygix.sygixos.tracking.TrackingState()),
        tracking?.enrichment?.contents ?: MutableStateFlow(emptyMap()), upNextMemory.removed, apps.disabledSources) { feed, session, _, removed, disabled ->
        val visible = kotlinx.coroutines.withContext(projectionDispatcher) {
            val visible = if (tracking == null) feed else {
                val read = fr.sygix.sygixos.tracking.TrackingHeroSource.project(fr.sygix.sygixos.domain.HeroRead(feed.items, shelfItems = feed.shelfItems), session,
                    removed.map { it.key }, tracking.enrichment.items(), disabled)
                val tracked = read.items.filter { it.tracking != null }
                feed.copy(items = if (!feed.fromApps && tracked.isNotEmpty()) tracked else read.items, fromApps = feed.fromApps || tracked.isNotEmpty(), shelfItems = read.shelfItems)
            }
            projected = visible.publications.associateBy { it.artwork.identity }
            visible
        }
        visible.copy(items = order.arrange(visible.items))
    }

    val state: StateFlow<HomeState> = combine(
        apps.catalog,
        visibleFeed,
        apps.disabledSources,
        heroVisuals,
    ) { catalog, feed, disabled, visuals ->
        disabledHeroSources = disabled
        val epoch = generations.toMap()
        val fallback = fallbackItems
        val (hero, shown) = kotlinx.coroutines.withContext(projectionDispatcher) {
            val shown = feed.withSources(disabled, fallback)
            val admitted = shown.items.mapNotNull { visuals.admit(it)?.copy(expiresAt = Long.MAX_VALUE) }
            val ids = admitted.mapTo(HashSet()) { it.id }
            HeroState(if (admitted.isEmpty() && shown.read) fallback else admitted, shown.fromApps && admitted.isNotEmpty(), validated = visuals.ready.values.toSet(),
                checked = visuals.checked, shelfItems = shown.shelfItems ?: shown.items.filter { it.tracking == null }, generations = epoch.filterKeys { it in ids }) to shown
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
    }.combine(shelfVisuals) { ready, shelf ->
        kotlinx.coroutines.withContext(projectionDispatcher) {
            ready.copy(hero = ready.hero.copy(
                shelfItems = ready.hero.shelfItems.orEmpty().map { item -> (shelf.ready[item.id]?.let { item.copy(imageUrl = it) } ?: shelf.retained[item.id] ?: item).copy(expiresAt = Long.MAX_VALUE) },
                shelfValidated = shelf.ready.values.toSet() + shelf.retained.values.mapNotNull { it.imageUrl },
                checked = ready.hero.checked + shelf.checked,
                shelfChecked = shelf.checked,
            ))
        }
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
            if (startup.value != StartupPhase.Done) {
                runtime.controller.holdSync()
                viewModelScope.launch(startupDispatcher) {
                    try { startup.first { it == StartupPhase.Done } }
                    finally { runtime.controller.releaseSync() }
                }
            }
            runtime.controller.restore()
            viewModelScope.launch { runtime.opened.collect { request ->
                if (request.origin == fr.sygix.sygixos.tracking.PlaybackOrigin.UP_NEXT) upNext?.opened(fr.sygix.sygixos.tracking.TrackingProjection.card(request.content))
            } }
        }
        viewModelScope.launch { artwork.changes.collect { apps.refreshApps() } }
        viewModelScope.launch { hero.cached()?.let { cached -> if (!rawFeed.value.read) publish(cached) } }
        viewModelScope.launch(startupDispatcher) {
            startup.first { it == StartupPhase.Done }
            mascot.release()
        }
        viewModelScope.launch(startupDispatcher) {
            val ready = state.first { it is HomeState.Ready } as HomeState.Ready
            kotlinx.coroutines.withContext(artwork.dispatcher) { artwork.preload(ready.catalog.dock + ready.catalog.grid.take(fr.sygix.sygixos.core.designsystem.Dimens.GridColumns)) }
            if (coldStart) startupController.catalogReady()
            startup.first { it == StartupPhase.Done }
            launch(artwork.dispatcher) { artwork.preload(ready.catalog.dock + ready.catalog.grid) }
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
            publish(hero.load())
        }
    }

    private suspend fun publish(feed: HeroFeed) {
        while (true) {
            val previous = rawFeed.value
            val heroBefore = VisualCarry(heroVisuals.value, heroChecks.toMap())
            val shelfBefore = VisualCarry(shelfVisuals.value, shelfChecks.toMap())
            val (heroAfter, shelfAfter) = kotlinx.coroutines.withContext(projectionDispatcher) {
                val heroAfter = HeroFeedTransition.carry(previous.publications, feed.publications, heroBefore)
                heroAfter to HeroFeedTransition.carry(previous.publications, feed.publications, shelfBefore, heroAfter.results.ready.values.toSet())
            }
            if (!feed.read && rawFeed.value.read) return
            if (rawFeed.value !== previous || heroVisuals.value !== heroBefore.results || shelfVisuals.value !== shelfBefore.results) continue
            val ids = feed.publications.mapTo(HashSet()) { it.id }
            generations.keys.retainAll(ids)
            heroAfter.fresh.forEach { generations[it] = ++generationSequence }
            heroChecks.clear()
            heroChecks.putAll(heroAfter.checks)
            shelfChecks.clear()
            shelfChecks.putAll(shelfAfter.checks)
            heroVisuals.value = heroAfter.results
            shelfVisuals.value = shelfAfter.results
            rawFeed.value = feed
            if (heroValidationJob?.isActive != true) validateHero()
            scheduleProgramExpiry()
            return
        }
    }

    private fun expirePrograms() {
        val now = programClock()
        rawFeed.update { it.copy(items = it.items.filter { item -> item.expiresAt > now }, shelfItems = it.shelfItems?.filter { item -> item.expiresAt > now }) }
        val live = rawFeed.value.publications.mapTo(HashSet()) { it.id }
        generations.keys.retainAll(live)
        heroChecks.keys.retainAll(live)
        shelfChecks.keys.retainAll(live)
        heroVisuals.update { it.keep(live) }
        shelfVisuals.update { it.keep(live) }
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
        val results = if (shelf) shelfVisuals else heroVisuals
        val checks = if (shelf) shelfChecks else heroChecks
        val checkedIdentities = checks.values.mapTo(HashSet()) { it.identity }
        val retries = results.value.failed
        val pending = items.filter { item ->
            item.artwork.candidates.isNotEmpty() && (item.artwork.identity !in checkedIdentities || item.id in retries) &&
                (shelf || busyRetries[item.artwork.identity]?.isActive != true) && validating.add(shelf to item.artwork.identity)
        }
        if (pending.isEmpty()) return null
        val visible = if (shelf) emptySet() else items.take(VisualQuality.HERO_VISIBLE).mapTo(HashSet()) { it.artwork.identity }
        val policy = if (shelf) ArtworkPolicy.Shelf(shelfWidth) else ArtworkPolicy.Hero
        val job = viewModelScope.launch(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            try { kotlinx.coroutines.coroutineScope { pending.filter { item -> !recordIndexed(item, policy, results, checks, shelf) }.groupBy { it.sourcePackage }.values.forEach { group -> launch {
                group.forEach { item ->
                    val identity = item.artwork.identity
                    if (identity !in projected) return@forEach
                    val check = checkVisual(item, policy, identity in visible)
                    validating.remove(shelf to identity)
                    if (check.busy) { if (!shelf) retryBusyHero(item, identity in visible); return@forEach }
                    recordCheck(item, check, results, checks)
                    if (holdAfterFirstDuringStartup && check.usable) startup.first { it == StartupPhase.Done }
                }
            } } } } finally { pending.forEach { validating.remove(shelf to it.artwork.identity) } }
        }
        validationBatches[job] = shelf
        job.invokeOnCompletion { validationBatches.remove(job) }
        job.start()
        return job
    }

    private fun recordCheck(item: HeroItem, check: fr.sygix.sygixos.data.VisualCheck, results: MutableStateFlow<VisualResults>,
        checks: MutableMap<String, fr.sygix.sygixos.model.ProgramArtwork>) {
        val current = projected[item.artwork.identity] ?: return
        checks[current.id] = current.artwork
        results.update { it.record(current.id, item.artwork.candidates, check) }
    }

    private fun retryBusyHero(item: HeroItem, visible: Boolean) {
        val identity = item.artwork.identity
        if (busyRetries[identity]?.isActive == true) return
        val job = viewModelScope.launch {
            while (true) {
                delay(BUSY_HERO_RETRY_MS)
                if (lastHeroItems.none { it.artwork.identity == identity } || identity !in projected) return@launch
                val check = validator.validate(item.artwork, ArtworkPolicy.Hero, visible)
                if (check.busy) continue
                recordCheck(item, check, heroVisuals, heroChecks)
                return@launch
            }
        }
        busyRetries[identity] = job
        validationBatches[job] = false
        job.invokeOnCompletion { busyRetries.remove(identity, job); validationBatches.remove(job) }
    }

    private suspend fun recordIndexed(item: HeroItem, policy: ArtworkPolicy, results: MutableStateFlow<VisualResults>,
        checks: MutableMap<String, fr.sygix.sygixos.model.ProgramArtwork>, shelf: Boolean): Boolean {
        val identity = item.artwork.identity
        val uri = validator.indexed(item.artwork, policy) ?: return false
        validating.remove(shelf to identity)
        val current = projected[identity] ?: return true
        checks[current.id] = current.artwork
        results.update { it.record(current.id, item.artwork.candidates, fr.sygix.sygixos.data.VisualCheck(uri)) }
        return true
    }

    private suspend fun checkVisual(item: HeroItem, policy: ArtworkPolicy, visible: Boolean): fr.sygix.sygixos.data.VisualCheck {
        var check = validator.validate(item.artwork, policy, visible)
        var attempt = 0
        while (check.busy && attempt < BUSY_RETRIES) {
            delay(BUSY_RETRY_MS shl attempt++)
            check = validator.validate(item.artwork, policy, visible)
        }
        return check
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
            val publications = rawFeed.value.publications
            val affected = kotlinx.coroutines.withContext(projectionDispatcher) {
                publications.filter { item -> item.artwork.candidates.any { event.uri in fr.sygix.sygixos.data.visualCandidates(it) } }
            }
            val heroAffected = affected.filter { heroVisuals.value.ready[it.id].let { chosen -> chosen == null || chosen == event.uri } }
            val shelfAffected = affected.filter { shelfVisuals.value.ready[it.id].let { chosen -> chosen == null || chosen == event.uri } }
            heroAffected.forEach { heroChecks.remove(it.id) }
            shelfAffected.forEach { shelfChecks.remove(it.id) }
            if (event is fr.sygix.sygixos.data.ArtworkEvent.ImageExpired ||
                event is fr.sygix.sygixos.data.ArtworkEvent.ImageFailed && event.cause == fr.sygix.sygixos.data.ArtworkFailure.QUALITY) {
                heroVisuals.update { it.withdraw(heroAffected.mapTo(HashSet()) { item -> item.id }) }
                shelfVisuals.update { it.withdraw(shelfAffected.mapTo(HashSet()) { item -> item.id }) }
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
                upNextSource = TvProviderUpNextSource(app, onSnapshot = { items -> app.tracking.enrichment.local(items); app.programArtwork.confirm(fr.sygix.sygixos.data.UP_NEXT_ARTWORK_TABLE, items.map { it.artwork to it.toString() }) }),
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
private const val BUSY_RETRIES = 4
private const val BUSY_RETRY_MS = 1_000L
private const val BUSY_HERO_RETRY_MS = 30_000L

private val SplashTimings = StartupTimings(
    minMs = Motion.SPLASH_MIN_MS,
    visualCapMs = Motion.SPLASH_VISUAL_CAP_MS,
    capMs = Motion.SPLASH_CAP_MS,
)
