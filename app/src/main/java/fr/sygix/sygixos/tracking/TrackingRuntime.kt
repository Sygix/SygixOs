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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

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
    durable: TrackingSyncServices? = if (sessionStore is KeystoreTrackingSession) TrackingSyncServices.get(context) else null,
    repository: TrackingRepository? = durable?.repository,
    scheduler: TrackingWorkScheduler? = durable?.scheduler,
) {
    private val actualProvider = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    private val chosenProvider = preferences.provider.stateIn(scope, SharingStarted.Eagerly, "betaseries")
    private val session = durable?.session ?: (sessionStore as? CoordinatedTrackingSessionStore ?: CoordinatedTrackingSessionStore(sessionStore))
    private val observedSession = TrackingObservedSessionStore(session) { token -> actualProvider.value = token?.let(TraktSessionEnvelope::provider) }
    private val api = durable?.api ?: BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport, configurationFile, wallClock, parserDispatcher), parserDispatcher)
    private val betaProvider = durable?.betaProvider ?: BetaSeriesProvider(api) { java.time.Instant.ofEpochMilli(wallClock()).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
    private val traktApi = durable?.traktApi ?: TraktApi(transport, TraktConfigurationSource(transport,
        configurationFile?.let { java.io.File(it.parentFile, "${it.name}-trakt") }, wallClock, parserDispatcher), session, parserDispatcher, wallClock, clock)
    private val traktProvider = durable?.traktProvider ?: TraktProvider(traktApi, wallClock)
    val provider = TrackingProviderRegistry(betaProvider, traktProvider) { actualProvider.value ?: chosenProvider.value }
    val readers = combine(preferences.players, catalog.allApps) { preferences, installed ->
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val selected = preferences.values.toSet()
            installed.filter { it.packageName in selected }.associate { app ->
                app.packageName to fr.sygix.sygixos.model.UpNextSourceEntry(app.packageName, app.label,
                    runCatching { context.packageManager.getApplicationIcon(app.packageName) }.getOrNull())
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())
    val controller = TrackingController(provider, observedSession, scope, clock, repository, scheduler,
        beforeRestore = { durable?.recoverPurge() ?: false }, invalidate = { durable?.invalidate() })
    val selectedProvider = combine(chosenProvider, actualProvider, controller.state.map { it.hasSession }.distinctUntilChanged()) { chosen, actual, saved ->
        actual?.takeIf { saved } ?: chosen
    }.stateIn(scope, SharingStarted.Eagerly, chosenProvider.value)
    val region = combine(preferences.country, selectedProvider, controller.state.map {
        it.account?.id?.takeIf { _ -> it.hasSession && it.failure != TrackingFailure.ACCESS }
    }.distinctUntilChanged()) { country, provider, account -> Triple(country, provider, account) }.transformLatest { (country, providerId, account) ->
        emit(null)
        if (country != null && account != null) emit(try { provider.forId(providerId).platforms(country) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { null })
    }.flowOn(parserDispatcher).stateIn(scope, SharingStarted.Eagerly, null)
    val enrichment = TrackingEnrichment(controller, provider, scope, clock, TrackingIdentityCache(identityFile, parserDispatcher), onIdentities = { links ->
        try { onIdentities(links) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
    })
    val device = TrackingDeviceFlow({ object : TrackingDeviceProtocol {
        private var source: TrackingDeviceProtocol? = null
        override suspend fun prepare(): DeviceAssociation {
            val selected = actualProvider.value?.takeIf { controller.state.value.hasSession } ?: preferences.provider.first()
            val protocol = when (selected) {
                "betaseries" -> BetaSeriesDeviceProtocol(api, clock)
                "trakt" -> TraktDeviceProtocol(traktApi, clock, wallClock)
                else -> throw TrackingException(TrackingFailure.CONFIGURATION)
            }
            source = protocol
            return protocol.prepare()
        }
        override suspend fun poll(association: DeviceAssociation) = (source ?: throw TrackingException(TrackingFailure.PROTOCOL)).poll(association)
    } }, controller, scope, clock)
    val actions = TrackingActions(controller, { token, content, action -> provider.mutate(token, content, action).forEach(controller::patch) }, scope,
        reconcile = { token, content, action -> try { provider.verify(token, content, action).forEach(controller::patch); true }
            catch (error: TrackingException) { if (error.failure == TrackingFailure.INCOMPLETE) false else throw error } }, clock = nanoTime, initialForeground = false)
    val opened = MutableSharedFlow<PlaybackRequest>(extraBufferCapacity = 8)
    val playback = PlaybackCoordinator(playbackApps ?: AndroidPlaybackApps(context, catalog, { enrichment.sourcePackages }), playbackLauncher, preferences, controller, actions, scope,
        loadOffers = { content ->
            val country = preferences.country.first()
            if (country == null) TrackingOfferResult(content.offers.map { it.copy(confirmedCountry = null) })
            else controller.authenticated { token -> provider.offers(token, content, country) }
        }, onOpened = { opened.tryEmit(it) })
    private var confirmedArtwork = emptyList<Pair<fr.sygix.sygixos.model.ProgramArtwork, String>>()
    val state = combine(controller.state, actions.excluded, preferences.players, readers, region) { current, excluded, players, readers, region ->
        current.copy(contents = TrackingProjection.candidates(current.contents).filter { it.parent !in excluded }.map { content ->
            val reader = readers[players[TrackingPreferences.playerKey(content.parent)]]
            content.copy(readerPackage = reader?.packageName, readerName = reader?.label, offers = confirmOffers(content, region))
        }, pendingRemovals = excluded)
    }.mapLatest { value ->
        try {
            val pairs = value.contents.flatMap { content ->
                val card = TrackingProjection.card(content)
                val hero = TrackingProjection.hero(content)
                listOf(card.artwork to card.cardIdentity, hero.artwork to hero.fullFingerprint)
            }
            if (pairs != confirmedArtwork) { onArtwork(pairs); confirmedArtwork = pairs }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { return@mapLatest value.copy(contents = emptyList(), failure = TrackingFailure.STORAGE) }
        value
    }.flowOn(parserDispatcher).stateIn(scope, SharingStarted.Eagerly, TrackingState())

    fun confirmOffers(content: TrackingContent, region: RegionalPlatforms?): List<PlaybackOffer> = provider.forId(content.parent.provider).confirmOffers(content.offers, region)

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
