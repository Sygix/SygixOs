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
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
    private val catalogue: TrackingIdentityCache? = null,
    private val onIdentities: suspend (List<fr.sygix.sygixos.domain.UpNextIdentityLink>) -> Unit = {}) : TrackingLocalContents {
    private val mutable = MutableStateFlow<Map<String, TrackingContent>>(emptyMap())
    override val contents = mutable.asStateFlow()
    @Volatile private var local: List<UpNextItem> = emptyList()
    val sourcePackages: Set<String> get() = local.map { it.source.packageName }.toSet()
    override fun items(): List<UpNextItem> = local.map { item ->
        val current = controller.state.value
        val content = mutable.value[item.key]?.takeIf { current.account != null && current.failure != TrackingFailure.ACCESS && it.sessionGeneration == current.generation }?.let { content ->
            current.contents.firstOrNull { it.identity == content.identity } ?: content
        }
        if (content == null) item else item.copy(tracking = content, externalIds = content.externalIds,
            source = item.source.copy(targetIdentity = content.identity), sources = item.sources.map { it.copy(targetIdentity = content.identity) })
    }
    fun publishers(parent: TrackingIdentity): Set<String> = items().filter { it.tracking?.parent == parent }.flatMap { item -> item.sources.map { it.packageName } }.toSet()
    private var job: Job? = null
    private var generation = -1L
    private val identified = mutableMapOf<String, Pair<String, Long>>()

    init {
        scope.launch { controller.state.collect { state ->
            if (generation != state.generation || state.account == null) {
                job?.cancel(); mutable.value = emptyMap(); identified.clear()
            }
            val connected = generation != state.generation || mutable.value.isEmpty()
            mutable.value = mutable.value.mapValues { (_, content) -> state.contents.firstOrNull { it.identity == content.identity } ?: content }
            generation = state.generation
            if (connected && state.account != null && !state.loading && state.failure != TrackingFailure.ACCESS && job?.isActive != true) refresh()
        } }
    }

    fun local(items: List<UpNextItem>) {
        if (items == local) return
        local = items
        refresh()
    }

    fun refresh() {
        job?.cancel()
        val current = controller.state.value
        if (current.account == null || current.failure == TrackingFailure.ACCESS) return
        val generation = current.generation
        val items = local
        job = scope.launch {
            val updated = mutable.value.toMutableMap()
            withTimeoutOrNull(30_000) {
                for (item in items) {
                    val cached = identified[item.key]
                    if (cached?.first == item.cardIdentity && clock() - cached.second < 15 * 60_000) continue
                    try {
                        val known = catalogue?.get(item.key, item.cardIdentity)
                            ?: updated[item.key]?.takeIf { cached?.first == item.cardIdentity }?.let { LocalTrackingIdentity(it.identity, it.parent) }
                        val content = controller.authenticated { token -> provider.identify(token, item, known) }
                        if (content != null) {
                            catalogue?.put(item.key, item.cardIdentity, content)
                            updated[item.key] = content.copy(sessionGeneration = generation)
                        }
                        identified[item.key] = item.cardIdentity to clock()
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (error: Exception) { updated[item.key]?.let { updated[item.key] = it.copy(failure = error.trackingFailure()) } }
                    if (generation != controller.state.value.generation) return@withTimeoutOrNull
                }
            }
            if (generation == controller.state.value.generation && controller.state.value.failure != TrackingFailure.ACCESS) {
                val links = items.mapNotNull { item -> updated[item.key]?.let { content ->
                    fr.sygix.sygixos.domain.UpNextIdentityLink(fr.sygix.sygixos.domain.UpNextContentKey.of(item.copy(tracking = null)), fr.sygix.sygixos.domain.UpNextContentKey.of(item.copy(tracking = content)))
                } }
                try { onIdentities(links) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { return@launch }
                if (generation != controller.state.value.generation) return@launch
                mutable.value = updated.toMap()
            }
        }
    }
}
