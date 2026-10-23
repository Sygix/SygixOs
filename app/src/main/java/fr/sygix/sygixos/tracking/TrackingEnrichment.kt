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

interface TrackingLocalContents {
    val contents: kotlinx.coroutines.flow.StateFlow<Map<String, TrackingContent>>
    fun items(): List<UpNextItem>
}

class TrackingEnrichment(private val controller: TrackingController, private val provider: BetaSeriesProvider, private val scope: CoroutineScope,
    private val onIdentities: suspend (List<fr.sygix.sygixos.domain.UpNextIdentityLink>) -> Unit = {}) : TrackingLocalContents {
    private val mutable = MutableStateFlow<Map<String, TrackingContent>>(emptyMap())
    override val contents = mutable.asStateFlow()
    private val failureMutable = MutableStateFlow<Map<String, TrackingFailure>>(emptyMap())
    val failures = failureMutable.asStateFlow()
    @Volatile private var local: List<UpNextItem> = emptyList()
    val sourcePackages: Set<String> get() = local.map { it.source.packageName }.toSet()
    override fun items(): List<UpNextItem> = local.map { item ->
        val current = controller.state.value
        val content = mutable.value[item.key]?.takeIf { current.account != null && current.failure == null && it.sessionGeneration == current.generation }?.let { content ->
            content.copy(membership = current.memberships?.let { if (content.parent in it) AccountMembership.PRESENT else AccountMembership.ABSENT } ?: AccountMembership.UNKNOWN)
        }
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
                val links = items.mapNotNull { item -> updated[item.key]?.let { content ->
                    fr.sygix.sygixos.domain.UpNextIdentityLink(fr.sygix.sygixos.domain.UpNextContentKey.of(item.copy(tracking = null)), fr.sygix.sygixos.domain.UpNextContentKey.of(item.copy(tracking = content)))
                } }
                try { onIdentities(links) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { failureMutable.value = items.associate { it.key to TrackingFailure.STORAGE }; return@launch }
                if (generation != controller.state.value.generation) return@launch
                mutable.value = updated.toMap()
                failureMutable.value = failures.toMap()
            }
        }
    }
}
