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
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Loaded(
        val content: UpNextContent,
        val opened: Map<UpNextContentKey, Long> = emptyMap(),
        val returnKey: UpNextContentKey? = null,
        val returnId: Long = 0,
    )

    private val raw = MutableStateFlow(Loaded(UpNextContent.Pending))
    private var reload: Job? = null
    private var pendingReturn: Pair<UpNextContentKey, Long>? = null
    private var returns = 0L

    private val removedKeys: Flow<List<UpNextContentKey>> = memory.removed.keys()

    val state: StateFlow<UpNextState?> = combine(raw, visible, position, disabledSources, removedKeys) { loaded, enabled, placement, disabled, removed ->
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

    fun refresh() {
        reload?.cancel()
        reload = scope.launch {
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
                            pendingReturn = null
                            raw.value = Loaded(UpNextContent.Items(emptyList()))
                        } else if (raw.value.content !is UpNextContent.Items) {
                            raw.update { it.copy(content = UpNextContent.Error) }
                        }
                    },
                )
            } finally { skeleton?.cancel() }
        }
    }

    private suspend fun loaded(items: List<UpNextItem>) {
        val stored = readOpened()
        val pending = pendingReturn
        pendingReturn = null
        val opened = if (pending == null) stored else stored.filterKeys { it != pending.first } + pending
        raw.value = Loaded(UpNextContent.Items(items), opened, pending?.first, if (pending == null) 0 else ++returns)
        guard { memory.pruneOpened { key -> UpNextFeed.stillPublished(key, items) } }
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
        } catch (_: Exception) {
            Unit
        }
    }

    private companion object {
        const val SKELETON_DELAY_MS = 300L
    }
}

private fun Flow<List<RemovedUpNextContent>>.keys(): Flow<List<UpNextContentKey>> =
    map { list -> list.map { it.key } }.catch { emit(emptyList()) }
