/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.sygix.sygixos.domain.FocusFallback
import fr.sygix.sygixos.domain.QrCode
import fr.sygix.sygixos.model.UpNextItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Locale

data class TrackingCountry(val code: String, val label: String)

object TrackingCountries {
    fun sorted(locale: Locale): List<TrackingCountry> {
        val collator = java.text.Collator.getInstance(locale)
        return Locale.getISOCountries().map { code -> TrackingCountry(code, Locale.Builder().setRegion(code).build().getDisplayCountry(locale)) }
            .sortedWith(compareBy(collator) { it.label })
    }
}
data class TrackingMenuAction(val tag: String, val label: Int, val mutation: TrackingAction? = null)
data class TrackingEpisodeList(val content: TrackingContent, val loading: Boolean = true, val items: List<TrackingContent> = emptyList(), val failure: TrackingFailure? = null,
    val selected: TrackingIdentity? = null, val focused: TrackingIdentity? = null, val focusedIndex: Int = 0,
    val choosingSeason: Boolean = false, val seasons: List<Int> = emptyList())
data class TrackingUiState(
    val tracking: TrackingState = TrackingState(),
    val device: DeviceAssociationState = DeviceAssociationState.Closed,
    val country: String? = null,
    val countryProposed: String? = null,
    val countries: List<TrackingCountry> = emptyList(),
    val player: PlaybackSelection? = null,
    val menu: UpNextItem? = null,
    val episodes: TrackingEpisodeList? = null,
    val notice: TrackingNotice? = null,
    val qr: QrCode? = null,
    val statusOpen: Boolean = false,
    val menuActions: List<TrackingMenuAction> = emptyList(),
    val playerApps: List<PlaybackApp> = emptyList(),
) {
    val modal: Boolean get() = statusOpen || player != null || menu != null || episodes != null || device != DeviceAssociationState.Closed && device != DeviceAssociationState.Connected
}

class TrackingViewModel(val runtime: TrackingRuntime, private val remove: (UpNextItem) -> Unit) : TrackingFocusViewModel() {
    private val menu = MutableStateFlow<UpNextItem?>(null)
    private val episodes = MutableStateFlow<TrackingEpisodeList?>(null)
    private val status = MutableStateFlow(false)
    private var episodeJob: Job? = null
    private var episodeRequest = 0L
    private val countryProposed = android.content.res.Resources.getSystem().configuration.locales[0].country.takeIf { it in Locale.getISOCountries() }
    private val displayLocale = MutableStateFlow(Locale.getDefault())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val countries = displayLocale.mapLatest { locale -> kotlinx.coroutines.withContext(runtime.parserDispatcher) { TrackingCountries.sorted(locale) } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val base = combine(runtime.state, runtime.device.state, runtime.preferences.country, runtime.playback.selection, runtime.actions.notice) { tracking, device, country, player, notice ->
        TrackingUiState(tracking, device, country, countryProposed, player = player, notice = notice,
            qr = (device as? DeviceAssociationState.Waiting)?.association?.verificationUrl?.let(QrCode::encode))
    }
    val state = combine(base, menu, episodes, countries, status) { value, menu, episodes, countries, status -> value.copy(menu = menu, episodes = episodes, countries = countries, statusOpen = status,
        menuActions = menu?.tracking?.let(::menuActions).orEmpty()) }
        .combine(runtime.region) { value, region ->
            val player = value.player?.let { player ->
                val content = player.request.content
                player.copy(request = player.request.copy(content = content.copy(offers = BetaSeriesOffers.confirm(content.offers, region?.takeIf { it.country == value.country }))))
            }
            value.copy(player = player, playerApps = player?.let { selection ->
                PlaybackAppOrder.sort(selection.apps,
                    selection.request.targets.map { it.packageName }.toSet() + runtime.enrichment.publishers(selection.request.content.parent), selection.request.content.offers,
                    includeAll = selection.otherApps)
            }.orEmpty())
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TrackingUiState(countryProposed = countryProposed))

    init {
        viewModelScope.launch { runtime.controller.state.map { it.generation }.distinctUntilChanged().collect {
            if (menu.value?.tracking?.sessionGeneration?.let { generation -> generation != it } == true) menu.value = null
            if (episodes.value?.content?.sessionGeneration?.let { generation -> generation != it } == true) endEpisodes()
        } }
    }

    fun displayLocale(locale: Locale) {
        displayLocale.value = locale
    }

    fun menu(item: UpNextItem): Boolean {
        if (item.tracking?.sessionGeneration?.let { it != runtime.controller.state.value.generation } == true) return false
        if (item.tracking?.membership != AccountMembership.PRESENT) return false
        menu.value = item
        return true
    }

    fun openWith() {
        val item = menu.value ?: return
        val content = item.tracking ?: return
        menu.value = null
        runtime.playback.open(PlaybackRequest(content, PlaybackOrigin.UP_NEXT, item.sources.mapNotNull { source ->
            source.targetIdentity?.let { PlaybackTarget(source.packageName, it, source.intentUri) }
        }), choose = true)
    }

    fun action(action: TrackingAction) {
        val content = menu.value?.tracking ?: return
        menu.value = null
        runtime.actions.perform(content, action)
    }

    fun menuAction(tag: String) {
        when (tag) {
            "tracking-action-player" -> openWith()
            "tracking-action-episodes" -> showEpisodes()
            "upnext-menu-remove" -> removeLocal()
            else -> state.value.menuActions.firstOrNull { it.tag == tag }?.mutation?.let(::action)
        }
    }

    private fun menuActions(content: TrackingContent): List<TrackingMenuAction> = buildList {
        add(TrackingMenuAction("tracking-action-player", fr.sygix.sygixos.R.string.upnext_open_with))
        if (content.identity.type != TrackingType.SHOW) add(TrackingMenuAction("tracking-action-seen",
            if (content.seen) fr.sygix.sygixos.R.string.tracking_unseen else fr.sygix.sygixos.R.string.tracking_seen,
            if (content.seen) TrackingAction.UNSEEN else TrackingAction.SEEN))
        if (content.identity.type == TrackingType.EPISODE) add(TrackingMenuAction("tracking-action-episodes", fr.sygix.sygixos.R.string.tracking_episodes))
        if (content.parent.type == TrackingType.SHOW && content.archived != null) add(TrackingMenuAction("tracking-action-archive",
            if (content.archived) fr.sygix.sygixos.R.string.tracking_unarchive else fr.sygix.sygixos.R.string.tracking_archive,
            if (content.archived) TrackingAction.UNARCHIVE else TrackingAction.ARCHIVE))
        add(TrackingMenuAction("tracking-action-remove", fr.sygix.sygixos.R.string.tracking_remove, TrackingAction.REMOVE))
        add(TrackingMenuAction("upnext-menu-remove", fr.sygix.sygixos.R.string.upnext_remove))
    }

    fun removeLocal() { val item = menu.value ?: return; menu.value = null; remove(item) }
    fun addLocal(item: UpNextItem) { item.tracking?.takeIf { it.membership == AccountMembership.ABSENT }?.let { runtime.actions.perform(it, TrackingAction.ADD) } }
    fun confirmCountry(code: String) { viewModelScope.launch { runtime.preferences.confirmCountry(code) } }
    fun connect() { runtime.device.open() }
    fun disconnect() { runtime.disconnect() }
    fun synchronize() { if (state.value.tracking.failure == TrackingFailure.ACCESS) { runtime.disconnect(); runtime.device.open() } else runtime.controller.retry() }
    fun choosePlayer(packageName: String) { runtime.playback.choose(packageName) }
    fun otherApps() { runtime.playback.otherApps() }
    fun noticeFocused(value: Boolean) { runtime.actions.setFocused(value) }
    fun settingsOpen(value: Boolean) { runtime.actions.setSettingsOpen(value) }
    fun dismissNotice() { runtime.actions.dismiss() }
    fun noticeAction() {
        val notice = state.value.notice ?: return
        when {
            notice.returnPrompt -> runtime.actions.confirmReturn()
            notice.failure == TrackingFailure.ACCESS -> { runtime.actions.dismiss(); synchronize() }
            notice.failure != null -> runtime.actions.retry()
            !notice.busy -> runtime.actions.undo()
        }
    }
    fun showEpisodes() {
        val content = menu.value?.tracking ?: return
        menu.value = null
        loadEpisodes(content)
    }

    fun retryEpisodes() { episodes.value?.content?.let(::loadEpisodes) }
    fun chooseSeason(season: Int) { episodes.value?.content?.let { loadEpisodes(it.copy(season = season)) } }
    fun showSeasons() {
        val current = episodes.value ?: return
        episodeJob?.cancel()
        val request = ++episodeRequest
        episodes.value = current.copy(choosingSeason = true, loading = true, failure = null)
        episodeJob = viewModelScope.launch {
            try {
                val seasons = runtime.controller.authenticated { runtime.provider.seasons(it, current.content) }
                if (request == episodeRequest) episodes.value = episodes.value?.copy(seasons = seasons, loading = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (request == episodeRequest) episodes.value = episodes.value?.copy(loading = false, failure = error.trackingFailure()) }
        }
    }
    fun reconnectEpisodes() { endEpisodes(); runtime.disconnect(); runtime.device.open() }
    fun onForeground() { episodes.value?.content?.let(::loadEpisodes) }
    fun episodeFocused(key: String) {
        val current = episodes.value ?: return
        val index = current.items.indexOfFirst { "tracking-episode-${it.identity.id}" == key }
        if (index >= 0 && (current.focused != current.items[index].identity || current.focusedIndex != index)) episodes.value = current.copy(focused = current.items[index].identity, focusedIndex = index)
    }
    fun showStatus() { status.value = true }
    fun retryStatus() {
        status.value = false
        if (state.value.tracking.failure == TrackingFailure.ACCESS) { runtime.disconnect(); runtime.device.open() } else runtime.controller.retry()
    }
    private fun loadEpisodes(content: TrackingContent) {
        episodeJob?.cancel()
        val request = ++episodeRequest
        val previous = episodes.value?.takeIf { it.content.parent == content.parent && it.content.season == content.season }
        episodes.value = (previous ?: TrackingEpisodeList(content)).copy(choosingSeason = false, loading = true, failure = null)
        episodeJob = viewModelScope.launch {
            try {
                val generation = runtime.controller.state.value.generation
                if (content.sessionGeneration?.let { it != generation } == true) { endEpisodes(); return@launch }
                val items = runtime.controller.authenticated { token -> runtime.provider.episodes(token, content) }.map { it.copy(sessionGeneration = generation) }
                val current = episodes.value?.takeIf { request == episodeRequest && it.content.parent == content.parent } ?: return@launch
                val focused = fr.sygix.sygixos.domain.FocusFallback.entry(items.map { it.identity.id }, (current.focused ?: content.identity).id, current.focusedIndex)
                episodes.value = TrackingEpisodeList(content, loading = false, items = items,
                    selected = current.selected?.takeIf { selected -> items.any { it.identity == selected } },
                    focused = items.firstOrNull { it.identity.id == focused }?.identity,
                    focusedIndex = items.indexOfFirst { it.identity.id == focused }.coerceAtLeast(0))
                if (episodes.value?.selected == null) panelActions("tracking-episodes", items.map { "tracking-episode-${it.identity.id}" } + listOf("tracking-seasons", "tracking-episodes-close"),
                    items.indexOfFirst { it.identity.id == focused }.coerceAtLeast(0), focused?.let { "tracking-episode-$it" }, false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (request == episodeRequest && episodes.value?.content?.parent == content.parent) episodes.value = TrackingEpisodeList(content, loading = false, failure = error.trackingFailure())
            }
        }
    }

    fun selectEpisode(content: TrackingContent) { episodes.value = episodes.value?.copy(selected = content.identity, focused = content.identity) }
    private fun selectedEpisode(): TrackingContent? = episodes.value?.let { list -> list.items.firstOrNull { it.identity == list.selected } }
    fun markSelectedEpisode() {
        val content = selectedEpisode() ?: return
        endEpisodes()
        runtime.actions.perform(content, if (content.seen) TrackingAction.UNSEEN else TrackingAction.SEEN)
    }
    fun openSelectedEpisode() {
        val content = selectedEpisode() ?: return
        endEpisodes()
        runtime.playback.open(PlaybackRequest(content, PlaybackOrigin.UP_NEXT))
    }

    fun close() {
        when {
            status.value -> status.value = false
            state.value.player != null -> runtime.playback.close()
            episodes.value?.selected != null -> episodes.value = episodes.value?.copy(selected = null)
            episodes.value?.choosingSeason == true -> { ++episodeRequest; episodeJob?.cancel(); episodes.value = episodes.value?.copy(choosingSeason = false, loading = false, failure = null) }
            episodes.value != null -> endEpisodes()
            menu.value != null -> menu.value = null
            else -> runtime.device.close()
        }
    }

    private fun endEpisodes() { ++episodeRequest; episodeJob?.cancel(); episodes.value = null }
    override fun onCleared() { endEpisodes(); runtime.device.close(); runtime.playback.close() }
}

open class TrackingFocusViewModel : ViewModel() {
    private val mutableFocus = MutableStateFlow<String?>(null)
    val panelFocus = mutableFocus.asStateFlow()
    private var panel: String? = null
    private var keys: List<String> = emptyList()
    private var index = 0
    private var loading = false
    private var restoring = false

    fun panelActions(tag: String, actions: List<String>, initial: Int, fallback: String?, isLoading: Boolean) {
        val entry = when {
            panel != tag || loading && !isLoading -> actions.getOrNull(initial)
            mutableFocus.value !in actions && fallback in actions -> fallback
            else -> FocusFallback.entry(actions, mutableFocus.value, index)
        }
        restoring = restoring || panel != tag || keys != actions || mutableFocus.value != entry
        panel = tag
        loading = isLoading
        keys = actions
        mutableFocus.value = entry
        index = actions.indexOf(entry).coerceAtLeast(0)
    }

    fun panelFocused(key: String, displayedKeys: List<String>): Boolean {
        if (displayedKeys != keys || key !in keys || restoring && key != mutableFocus.value) return false
        index = keys.indexOf(key); mutableFocus.value = key
        return true
    }

    fun panelRestored(key: String?) { if (key == mutableFocus.value) restoring = false }
}
