/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlaybackOrigin { HERO, UP_NEXT }
enum class PlaybackPrecision { CONTENT, APPLICATION }
data class PlaybackApp(val packageName: String, val label: String, val video: Boolean)
data class PlaybackTarget(val packageName: String, val identity: TrackingIdentity, val intentUri: String? = null)
data class PlaybackRequest(
    val content: TrackingContent,
    val origin: PlaybackOrigin,
    val targets: List<PlaybackTarget> = emptyList(),
)
data class PlaybackSelection(
    val request: PlaybackRequest,
    val apps: List<PlaybackApp> = emptyList(),
    val loading: Boolean = true,
    val otherApps: Boolean = false,
    val failure: TrackingFailure? = null,
    val preferredPackage: String? = null,
    val missingPreferred: Boolean = false,
)

interface PlaybackAppResolver {
    suspend fun installed(): List<PlaybackApp>
    fun changes(): kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.emptyFlow()
}

interface PlaybackLauncher {
    fun launch(app: PlaybackApp, request: PlaybackRequest): PlaybackPrecision?
}

interface PlaybackPreferences {
    suspend fun preferred(content: TrackingIdentity): String?
    suspend fun remember(content: TrackingIdentity, packageName: String)
}

class PlaybackCoordinator(
    private val apps: PlaybackAppResolver,
    private val launcher: PlaybackLauncher,
    private val preferences: PlaybackPreferences,
    private val tracking: TrackingController,
    private val actions: TrackingActions,
    private val scope: CoroutineScope,
    private val onOpened: (PlaybackRequest) -> Unit = {},
) {
    private val mutable = MutableStateFlow<PlaybackSelection?>(null)
    val selection = mutable.asStateFlow()
    private var job: Job? = null
    private var catalogJob: Job? = null
    private var pending: Pair<PlaybackRequest, Long>? = null
    private var departed = false
    private var requestId = 0L
    private var accountGeneration = tracking.state.value.generation
    private var departure: Job? = null

    init {
        scope.launch { apps.changes().collect { refreshSelection() } }
        scope.launch { tracking.state.collect { state ->
            if (state.account == null || accountGeneration != state.generation) {
                close(); departure?.cancel(); pending = null
            }
            accountGeneration = state.generation
        } }
    }

    fun open(request: PlaybackRequest, choose: Boolean = false) {
        close()
        val id = requestId
        val generation = tracking.state.value.generation
        if (request.content.membership != AccountMembership.PRESENT || tracking.state.value.account == null || request.content.sessionGeneration?.let { it != generation } == true) return
        mutable.value = PlaybackSelection(request)
        job = scope.launch {
            try {
                val installed = apps.installed()
                val preferred = preferences.preferred(request.content.parent)
                if (id != requestId || generation != tracking.state.value.generation) return@launch
                val app = installed.firstOrNull { it.packageName == preferred }
                if (!choose && app != null) launch(request, app, id, generation)
                else mutable.value = PlaybackSelection(request, installed, loading = false, preferredPackage = preferred, missingPreferred = preferred != null && app == null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (id == requestId) mutable.value = PlaybackSelection(request, loading = false, failure = error.trackingFailure()) }
        }
    }

    fun otherApps() { mutable.value = mutable.value?.copy(otherApps = true) }

    fun choose(packageName: String) {
        val current = mutable.value ?: return
        if (current.loading) return
        if (current.request.content.sessionGeneration?.let { it != tracking.state.value.generation } == true) { close(); return }
        catalogJob?.cancel()
        val id = requestId
        val generation = tracking.state.value.generation
        mutable.value = current.copy(loading = true, failure = null)
        job = scope.launch {
            try {
                val installed = apps.installed()
                if (id != requestId || generation != tracking.state.value.generation) return@launch
                val app = installed.firstOrNull { it.packageName == packageName }
                if (app == null) mutable.value = current.copy(apps = installed, missingPreferred = true)
                else launch(current.request, app, id, generation)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (id == requestId) mutable.value = current.copy(loading = false, failure = error.trackingFailure()) }
        }
    }

    private fun refreshSelection() {
        val current = mutable.value?.takeUnless { it.loading } ?: return
        val id = requestId
        val generation = tracking.state.value.generation
        catalogJob?.cancel()
        catalogJob = scope.launch {
            try {
                val installed = apps.installed()
                val latest = mutable.value
                if (id == requestId && generation == tracking.state.value.generation && latest != null && !latest.loading) mutable.value = latest.copy(apps = installed,
                    missingPreferred = latest.preferredPackage != null && installed.none { it.packageName == latest.preferredPackage })
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (id == requestId) mutable.value = current.copy(failure = error.trackingFailure()) }
        }
    }

    private suspend fun launch(request: PlaybackRequest, app: PlaybackApp, id: Long, generation: Long) {
        if (generation != tracking.state.value.generation || id != requestId || tracking.state.value.account == null || request.content.sessionGeneration?.let { it != generation } == true) return
        val result = launcher.launch(app, request)
        if (result == null) {
            departure?.cancel(); pending = null; departed = false
            mutable.value = PlaybackSelection(request, apps.installed(), loading = false, failure = TrackingFailure.PROTOCOL)
            return
        }
        pending = request to generation
        departed = false
        actions.dismissReturn()
        departure?.cancel()
        departure = scope.launch { kotlinx.coroutines.delay(15_000); if (!departed) pending = null }
        onOpened(request)
        try { preferences.remember(request.content.parent, app.packageName) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (generation == tracking.state.value.generation) mutable.value = PlaybackSelection(request, loading = false, failure = TrackingFailure.STORAGE)
            return
        }
        if (id == requestId) mutable.value = null
    }

    fun onDeparture() { departure?.cancel(); if (pending != null) departed = true }
    fun onBackground() = onDeparture()
    fun onForeground() {
        if (!departed) return
        val returned = pending
        pending = null
        departed = false
        if (returned != null) actions.returned(returned.first.content, returned.second)
    }

    fun close() { ++requestId; job?.cancel(); catalogJob?.cancel(); mutable.value = null }
}
