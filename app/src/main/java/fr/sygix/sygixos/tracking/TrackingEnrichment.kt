/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.model.UpNextItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class TrackingEnrichment(private val controller: TrackingController, private val provider: BetaSeriesProvider, private val scope: CoroutineScope) {
    private val mutable = MutableStateFlow<Map<String, TrackingContent>>(emptyMap())
    val contents = mutable.asStateFlow()
    private val failureMutable = MutableStateFlow<Map<String, TrackingFailure>>(emptyMap())
    val failures = failureMutable.asStateFlow()
    @Volatile private var local: List<UpNextItem> = emptyList()
    val sourcePackages: Set<String> get() = local.map { it.source.packageName }.toSet()
    fun items(): List<UpNextItem> = local.map { item ->
        val current = controller.state.value
        val content = mutable.value[item.key]?.takeIf { current.account != null && current.failure == null && it.sessionGeneration == current.generation }
        if (content == null) item else item.copy(tracking = content, externalIds = content.externalIds,
            source = item.source.copy(targetIdentity = content.identity), sources = item.sources.map { it.copy(targetIdentity = content.identity) })
    }
    private var job: Job? = null
    private var generation = -1L
    private var memberships: Set<TrackingIdentity>? = null
    private var loading = false

    init {
        scope.launch { controller.state.collect { state ->
            if (generation != state.generation || state.account == null || state.failure != null) {
                job?.cancel(); mutable.value = emptyMap()
            }
            val changed = memberships != state.memberships || generation != state.generation || loading && !state.loading && state.failure == null
            if (memberships != state.memberships) mutable.value = emptyMap()
            memberships = state.memberships
            generation = state.generation
            loading = state.loading
            if (changed && state.memberships != null) refresh()
        } }
    }

    fun local(items: List<UpNextItem>) {
        if (items == local) return
        local = items
        refresh()
    }

    private fun refresh() {
        job?.cancel()
        val current = controller.state.value
        val known = current.memberships ?: return
        val generation = current.generation
        val items = local
        job = scope.launch {
            val updated = mutableMapOf<String, TrackingContent>()
            val failures = mutableMapOf<String, TrackingFailure>()
            withTimeoutOrNull(30_000) {
                for (item in items) {
                    try {
                        val content = controller.authenticated { token -> provider.identify(token, item, known) }
                        if (content != null) updated[item.key] = content.copy(sessionGeneration = generation)
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { failures[item.key] = error.trackingFailure() }
                    if (generation != controller.state.value.generation) return@withTimeoutOrNull
                }
            }
            if (generation == controller.state.value.generation && controller.state.value.failure == null) {
                mutable.value = updated.toMap()
                failureMutable.value = failures.toMap()
            }
        }
    }
}
