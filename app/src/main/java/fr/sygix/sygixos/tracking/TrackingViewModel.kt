/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.sygix.sygixos.domain.QrCode
import fr.sygix.sygixos.model.UpNextItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class TrackingCountry(val code: String, val label: String)
data class TrackingEpisodeList(val content: TrackingContent, val loading: Boolean = true, val items: List<TrackingContent> = emptyList(), val failure: TrackingFailure? = null)
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
    val mediaAccess: MediaAccessState = MediaAccessState.DISABLED,
    val statusOpen: Boolean = false,
) {
    val modal: Boolean get() = statusOpen || player != null || menu != null || episodes != null || device != DeviceAssociationState.Closed && device != DeviceAssociationState.Connected
}

class TrackingViewModel(val runtime: TrackingRuntime, private val remove: (UpNextItem) -> Unit) : ViewModel() {
    private val menu = MutableStateFlow<UpNextItem?>(null)
    private val episodes = MutableStateFlow<TrackingEpisodeList?>(null)
    private val status = MutableStateFlow(false)
    private var episodeJob: Job? = null
    private val countryProposed = Locale.getDefault().country.takeIf { it in Locale.getISOCountries() }
    private val countries = Locale.getISOCountries().map { code -> TrackingCountry(code, Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.FRENCH)) }.sortedBy { it.label }
    private val base = combine(runtime.state, runtime.device.state, runtime.preferences.country, runtime.playback.selection, runtime.actions.notice) { tracking, device, country, player, notice ->
        TrackingUiState(tracking, device, country, countryProposed, countries, player, notice = notice,
            qr = (device as? DeviceAssociationState.Waiting)?.association?.verificationUrl?.let(QrCode::encode))
    }
    val state = combine(base, menu, episodes, runtime.media.state, status) { value, menu, episodes, media, status -> value.copy(menu = menu, episodes = episodes, mediaAccess = media, statusOpen = status) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, TrackingUiState(countryProposed = countryProposed, countries = countries))

    fun menu(item: UpNextItem): Boolean {
        if (item.tracking?.sessionGeneration?.let { it != runtime.controller.state.value.generation } == true) return true
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
        if (runtime.actions.notice.value?.busy == true) return
        val content = menu.value?.tracking ?: return
        menu.value = null
        runtime.actions.perform(content, action)
    }

    fun removeLocal() { val item = menu.value ?: return; menu.value = null; remove(item) }
    fun addLocal(item: UpNextItem) { item.tracking?.takeIf { it.membership == AccountMembership.ABSENT }?.let { runtime.actions.perform(it, TrackingAction.ADD) } }
    fun confirmCountry(code: String) { viewModelScope.launch { runtime.preferences.confirmCountry(code); runtime.controller.refresh() } }
    fun mediaAccess() {
        when (state.value.mediaAccess) {
            MediaAccessState.GRANTED -> viewModelScope.launch { runtime.preferences.observation(false) }
            MediaAccessState.DISABLED -> viewModelScope.launch { runtime.preferences.observation(true) }
            MediaAccessState.ACTIVATING -> Unit
            else -> { if (!runtime.media.openManual()) runtime.media.refresh(state.value.tracking.generation, state.value.tracking.account != null, true, retry = true) }
        }
    }
    fun showEpisodes() {
        val content = menu.value?.tracking ?: return
        menu.value = null
        loadEpisodes(content)
    }

    fun retryEpisodes() { episodes.value?.content?.let(::loadEpisodes) }
    fun showStatus() { status.value = true }
    fun retryStatus() {
        status.value = false
        if (state.value.tracking.failure == TrackingFailure.ACCESS) { runtime.disconnect(); runtime.device.open() } else runtime.controller.refresh()
    }
    private fun loadEpisodes(content: TrackingContent) {
        episodeJob?.cancel()
        episodes.value = TrackingEpisodeList(content)
        episodeJob = viewModelScope.launch {
            try {
                val items = runtime.controller.authenticated { token -> runtime.provider.episodes(token, content) }
                episodes.value = TrackingEpisodeList(content, loading = false, items = items)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { episodes.value = TrackingEpisodeList(content, loading = false, failure = error.trackingFailure()) }
        }
    }

    fun openEpisode(content: TrackingContent) {
        episodes.value = null
        runtime.playback.open(PlaybackRequest(content, PlaybackOrigin.UP_NEXT))
    }

    fun close() {
        when {
            status.value -> status.value = false
            state.value.player != null -> runtime.playback.close()
            episodes.value != null -> { episodeJob?.cancel(); episodes.value = null }
            menu.value != null -> menu.value = null
            else -> runtime.device.close()
        }
    }

    override fun onCleared() { episodeJob?.cancel(); runtime.device.close(); runtime.playback.close() }
}
