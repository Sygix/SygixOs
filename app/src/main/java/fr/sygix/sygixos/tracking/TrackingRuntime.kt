/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.content.Context
import fr.sygix.sygixos.data.AppCatalogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class TrackingRuntime(context: Context, catalog: AppCatalogRepository, scope: CoroutineScope, privilegedAccess: fr.sygix.sygixos.data.AdbAccess? = null,
    private val onArtwork: suspend (List<Pair<fr.sygix.sygixos.model.ProgramArtwork, String>>) -> Unit = {},
    transport: TrackingTransport = BoundedTrackingTransport(TrackingHttpsTransport()),
    sessionStore: TrackingSessionStore = KeystoreTrackingSession(context),
    val preferences: TrackingSettingsStore = TrackingPreferences(context),
    playbackApps: PlaybackAppResolver? = null,
    playbackLauncher: PlaybackLauncher = AndroidPlaybackLauncher(context),
    mediaGateway: MediaAccessGateway = AndroidMediaAccess(context, privilegedAccess),
    clock: () -> Long = { System.nanoTime() / 1_000_000 },
    nanoTime: () -> Long = System::nanoTime,
) {
    private val api = BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport))
    val provider = BetaSeriesProvider(api)
    val readers = combine(preferences.players, catalog.allApps) { preferences, installed ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val selected = preferences.values.toSet()
            installed.filter { it.packageName in selected }.associate { app ->
                app.packageName to fr.sygix.sygixos.model.UpNextSourceEntry(app.packageName, app.label,
                    runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull())
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())
    val controller = TrackingController(provider, sessionStore, scope)
    val enrichment = TrackingEnrichment(controller, provider, scope)
    val device = BetaSeriesDevice(api, controller, scope, clock)
    val actions = TrackingActions(controller, provider::mutate, scope, reconcile = provider::statusMatches, clock = nanoTime)
    val media = MediaAccessController(mediaGateway, scope)
    val opened = MutableSharedFlow<PlaybackRequest>(extraBufferCapacity = 8)
    val playback = PlaybackCoordinator(playbackApps ?: AndroidPlaybackApps(context, catalog, { enrichment.sourcePackages }), playbackLauncher, preferences, controller, actions, scope, onOpened = { opened.tryEmit(it) })
    val state = combine(controller.state, actions.excluded, preferences.players, readers) { current, excluded, players, readers ->
        val value = current.copy(contents = current.contents.filterNot { it.parent in excluded }.map { content ->
            val reader = readers[players[TrackingPreferences.playerKey(content.parent)]]
            content.copy(readerPackage = reader?.packageName, readerName = reader?.label)
        }, pendingRemovals = excluded)
        try {
            onArtwork(value.contents.flatMap { content ->
                val card = TrackingProjection.card(content)
                val hero = TrackingProjection.hero(content)
                listOf(card.artwork to card.cardIdentity, hero.artwork to hero.fullFingerprint)
            })
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { return@combine value.copy(contents = emptyList(), failure = TrackingFailure.STORAGE) }
        if (value.generation == controller.state.value.generation) value else controller.state.value.copy(contents = emptyList())
    }.stateIn(scope, SharingStarted.Eagerly, TrackingState())

    init {
        scope.launch {
            combine(controller.state, preferences.observation, media.state) { state, enabled, media -> state.account != null && enabled && media == MediaAccessState.GRANTED }
                .distinctUntilChanged().collect(playback::setObservationEnabled)
        }
        scope.launch {
            combine(controller.state, preferences.observation) { state, enabled -> Triple(state.generation, state.account != null, enabled) }
                .distinctUntilChanged().collect { (generation, connected, enabled) -> media.refresh(generation, connected, enabled) }
        }
    }

    fun onForeground() {
        device.setVisible(true)
        actions.setForeground(true)
        playback.onForeground()
        controller.refresh()
        media.refresh(controller.state.value.generation, controller.state.value.account != null, media.state.value != MediaAccessState.DISABLED)
    }

    fun onBackground() {
        device.setVisible(false)
        actions.setForeground(false)
        playback.onBackground()
    }

    fun disconnect() { device.close(); playback.close(); controller.disconnect() }
}
