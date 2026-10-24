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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TrackingRuntime(context: Context, catalog: AppCatalogRepository, scope: CoroutineScope,
    private val onArtwork: suspend (List<Pair<fr.sygix.sygixos.model.ProgramArtwork, String>>) -> Unit = {},
    private val onIdentities: suspend (List<fr.sygix.sygixos.domain.UpNextIdentityLink>) -> Unit = {},
    transport: TrackingTransport = BoundedTrackingTransport(TrackingHttpsTransport()),
    sessionStore: TrackingSessionStore = KeystoreTrackingSession(context),
    val preferences: TrackingSettingsStore = TrackingPreferences(context),
    playbackApps: PlaybackAppResolver? = null,
    playbackLauncher: PlaybackLauncher = AndroidPlaybackLauncher(context),
    clock: () -> Long = { System.nanoTime() / 1_000_000 },
    nanoTime: () -> Long = System::nanoTime,
    wallClock: () -> Long = System::currentTimeMillis,
    val parserDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default,
    configurationFile: java.io.File? = context.noBackupFilesDir.resolve("tracking-configuration"),
    identityFile: java.io.File? = context.noBackupFilesDir.resolve("tracking-identities"),
) {
    private val api = BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport, configurationFile, wallClock, parserDispatcher), parserDispatcher)
    val provider = BetaSeriesProvider(api) { java.time.Instant.ofEpochMilli(wallClock()).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    val region = preferences.country.transformLatest { country ->
        emit(null)
        if (country != null) emit(try { provider.platforms(country) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { null })
    }.stateIn(scope, SharingStarted.Eagerly, null)
    val readers = combine(preferences.players, catalog.allApps) { preferences, installed ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val selected = preferences.values.toSet()
            installed.filter { it.packageName in selected }.associate { app ->
                app.packageName to fr.sygix.sygixos.model.UpNextSourceEntry(app.packageName, app.label,
                    runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull())
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())
    val controller = TrackingController(provider, sessionStore, scope, clock)
    val enrichment = TrackingEnrichment(controller, provider, scope, clock, TrackingIdentityCache(identityFile, parserDispatcher), onIdentities = { links ->
        try { onIdentities(links) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
    })
    val device = BetaSeriesDevice(api, controller, scope, clock)
    val actions = TrackingActions(controller, { token, content, action -> provider.mutate(token, content, action).forEach(controller::patch) }, scope,
        reconcile = { token, content, action -> try { provider.verify(token, content, action).forEach(controller::patch); true }
            catch (error: TrackingException) { if (error.failure == TrackingFailure.INCOMPLETE) false else throw error } }, clock = nanoTime)
    val opened = MutableSharedFlow<PlaybackRequest>(extraBufferCapacity = 8)
    val playback = PlaybackCoordinator(playbackApps ?: AndroidPlaybackApps(context, catalog, { enrichment.sourcePackages }), playbackLauncher, preferences, controller, actions, scope, onOpened = { opened.tryEmit(it) })
    val state = combine(controller.state, actions.excluded, preferences.players, readers, region) { current, excluded, players, readers, region ->
        current.copy(contents = TrackingProjection.candidates(current.contents).filter { it.parent !in excluded }.map { content ->
            val reader = readers[players[TrackingPreferences.playerKey(content.parent)]]
            content.copy(readerPackage = reader?.packageName, readerName = reader?.label, offers = BetaSeriesOffers.confirm(content.offers, region))
        }, pendingRemovals = excluded)
    }.mapLatest { value ->
        try {
            onArtwork(value.contents.flatMap { content ->
                val card = TrackingProjection.card(content)
                val hero = TrackingProjection.hero(content)
                listOf(card.artwork to card.cardIdentity, hero.artwork to hero.fullFingerprint)
            })
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { return@mapLatest value.copy(contents = emptyList(), failure = TrackingFailure.STORAGE) }
        value
    }.stateIn(scope, SharingStarted.Eagerly, TrackingState())

    fun onForeground() {
        device.setVisible(true)
        actions.setForeground(true)
        playback.onForeground()
        controller.setForeground(true)
        enrichment.refresh()
    }

    fun onBackground() {
        controller.setForeground(false)
        device.setVisible(false)
        actions.setForeground(false)
        playback.onBackground()
    }

    fun disconnect() { device.close(); playback.close(); controller.disconnect() }
}
