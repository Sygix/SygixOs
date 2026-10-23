/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UpNextContent {
    data object Pending : UpNextContent
    data object Skeleton : UpNextContent
    data object Error : UpNextContent
    data class Items(val items: List<UpNextItem>) : UpNextContent
}

data class UpNextReturn(val key: String, val id: Long)

data class UpNextState(val content: UpNextContent, val position: UpNextPosition, val returnFocus: UpNextReturn? = null)

class UpNextController(
    private val source: UpNextSource,
    visible: Flow<Boolean>,
    position: Flow<UpNextPosition>,
    disabledSources: Flow<Set<String>>,
    private val scope: CoroutineScope,
    private val memory: UpNextMemory = InMemoryUpNextMemory(),
    private val tracking: StateFlow<fr.sygix.sygixos.tracking.TrackingState> = MutableStateFlow(fr.sygix.sygixos.tracking.TrackingState()),
    enrichment: StateFlow<Map<String, fr.sygix.sygixos.tracking.TrackingContent>> = MutableStateFlow(emptyMap()),
    readers: StateFlow<Map<String, fr.sygix.sygixos.model.UpNextSourceEntry>> = MutableStateFlow(emptyMap()),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Loaded(
        val content: UpNextContent,
        val opened: Map<UpNextContentKey, Long> = emptyMap(),
        val returnKey: UpNextContentKey? = null,
        val returnId: Long = 0,
        val memoryLoaded: Boolean = false,
    )

    private val raw = MutableStateFlow(Loaded(UpNextContent.Pending))
    private var reload: Job? = null
    private var expiry: Job? = null
    private var confirmedAt: Long? = null
    private var pendingReturn: Pair<UpNextContentKey, Long>? = null
    private var returns = 0L

    private val removedKeys: Flow<List<UpNextContentKey>> = memory.removed.keys()

    private val combinedRaw = combine(raw, tracking, disabledSources, enrichment, readers) { loaded, tracked, disabled, identities, readers ->
        if (!loaded.memoryLoaded) return@combine loaded.copy(content = if (loaded.content == UpNextContent.Skeleton) UpNextContent.Skeleton else UpNextContent.Pending)
        val local = (loaded.content as? UpNextContent.Items)?.items.orEmpty().filter { it.source.packageName !in disabled }.map { item ->
            val content = identities[item.key]?.takeIf { tracked.account != null && tracked.failure == null && it.sessionGeneration == tracked.generation }?.let { content ->
                content.copy(membership = tracked.memberships?.let { if (content.parent in it) fr.sygix.sygixos.tracking.AccountMembership.PRESENT else fr.sygix.sygixos.tracking.AccountMembership.ABSENT } ?: fr.sygix.sygixos.tracking.AccountMembership.UNKNOWN)
            }
            if (content == null) item else item.copy(tracking = content, externalIds = content.externalIds,
                source = item.source.copy(targetIdentity = content.identity), sources = item.sources.map { it.copy(targetIdentity = content.identity) })
        }.filter { it.tracking?.parent !in tracked.pendingRemovals }
        if (tracked.contents.isEmpty() && identities.isEmpty()) loaded else loaded.copy(content = UpNextContent.Items(fr.sygix.sygixos.tracking.TrackingProjection.merge(local, tracked.contents).map { item ->
            val reader = item.tracking?.readerPackage?.let(readers::get)
            if (reader == null) item else item.copy(source = reader)
        }))
    }

    val state: StateFlow<UpNextState?> = combine(combinedRaw, visible, position, disabledSources, removedKeys) { loaded, enabled, placement, disabled, removed ->
        val content = loaded.content
        val shown = if (content is UpNextContent.Items) {
            UpNextContent.Items(UpNextFeed.build(content.items, disabled, loaded.opened, removed))
        } else {
            content
        }
        if (!enabled || shown is UpNextContent.Items && shown.items.isEmpty()) {
            null
        } else {
            val target = loaded.returnKey?.let { key -> (shown as? UpNextContent.Items)?.items?.firstOrNull { key.matches(it) } }
            UpNextState(shown, placement, target?.let { UpNextReturn(it.key, loaded.returnId) })
        }
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val updates: Flow<Unit> = source.changes().onEach { refresh() }

    init {
        scope.launch {
            memory.opened.catch { emit(emptyMap()) }.collect { stored ->
                fun identified(key: UpNextContentKey?): UpNextContentKey? {
                    if (key == null || key.tracking != null) return key
                    val identities = stored.keys.filter { it.tracking != null && it.matches(key) }.mapNotNull { it.tracking }.distinct()
                    return identities.singleOrNull()?.let { key.copy(tracking = it) } ?: key
                }
                pendingReturn = pendingReturn?.let { requireNotNull(identified(it.first)) to it.second }
                raw.update {
                    it.copy(opened = if (!it.memoryLoaded || pendingReturn == null) stored else it.opened,
                        memoryLoaded = true, returnKey = identified(it.returnKey))
                }
            }
        }
    }

    fun refresh() {
        expireIfNeeded()
        reload?.cancel()
        reload = scope.launch {
            (raw.value.content as? UpNextContent.Items)?.let { previous ->
                val accessible = source.accessible(previous.items)
                if (accessible != previous.items) raw.update { it.copy(content = UpNextContent.Items(accessible)) }
            }
            val skeleton = if (raw.value.content == UpNextContent.Pending) launch {
                delay(SKELETON_DELAY_MS)
                raw.update { it.copy(content = UpNextContent.Skeleton) }
            } else null
            try {
                val result = source.load()
                result.fold(
                    onSuccess = { items -> loaded(items) },
                    onFailure = { error ->
                        if (error is SecurityException) {
                            confirmedAt = null
                            expiry?.cancel()
                            publish(UpNextContent.Items(emptyList()))
                        } else publish(raw.value.content.takeIf { it is UpNextContent.Items } ?: UpNextContent.Error)
                    },
                )
            } finally { skeleton?.cancel() }
        }
    }

    private suspend fun loaded(items: List<UpNextItem>) {
        publish(UpNextContent.Items(items))
        confirmedAt = clock()
        expiry?.cancel()
        expiry = scope.launch { delay(ArtworkAge.FRESH_MS); expireIfNeeded() }
        val tracked = tracking.value
        if (!tracked.loading && tracked.failure == null) {
            val published = items + tracked.contents.map(fr.sygix.sygixos.tracking.TrackingProjection::card)
            guard { memory.pruneOpened { key -> key.tracking != null && tracked.account == null || UpNextFeed.stillPublished(key, published) } }
        }
    }

    private suspend fun publish(content: UpNextContent) {
        val stored = readOpened()
        val pending = pendingReturn
        pendingReturn = null
        val opened = if (pending == null) stored else stored.filterKeys { it != pending.first } + pending
        raw.value = Loaded(content, opened, pending?.first, if (pending == null) 0 else ++returns, memoryLoaded = true)
    }

    private fun expireIfNeeded() {
        val at = confirmedAt ?: return
        if (clock() < at || clock() - at >= ArtworkAge.FRESH_MS) {
            raw.update { it.copy(content = UpNextContent.Error, returnKey = null) }
            confirmedAt = null
        }
    }

    fun opened(item: UpNextItem) {
        val key = UpNextContentKey.of(item)
        val at = clock()
        pendingReturn = key to at
        scope.launch { guard { memory.recordOpened(key, at) } }
    }

    fun remove(item: UpNextItem) {
        val content = RemovedUpNextContent(
            key = UpNextContentKey.of(item),
            title = item.displayTitle,
            packages = item.sources.map { it.packageName }.distinct(),
            imageUrl = item.imageUrl,
            removedAt = clock(),
        )
        scope.launch { guard { memory.addRemoved(content) } }
    }

    fun returnHandled(id: Long) {
        raw.update { if (it.returnId == id) it.copy(returnKey = null) else it }
    }

    private suspend fun readOpened(): Map<UpNextContentKey, Long> = try {
        memory.opened.first()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyMap()
    }

    private suspend fun guard(block: suspend () -> Unit) {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {}
    }

    private companion object {
        const val SKELETON_DELAY_MS = 300L
    }
}

private fun Flow<List<RemovedUpNextContent>>.keys(): Flow<List<UpNextContentKey>> =
    map { list -> list.map { it.key } }.catch { emit(emptyList()) }
