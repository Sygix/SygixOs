/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UpNextContent {
    data object Pending : UpNextContent
    data object Skeleton : UpNextContent
    data object Error : UpNextContent
    data class Items(val items: List<UpNextItem>) : UpNextContent
}

data class UpNextState(val content: UpNextContent, val position: UpNextPosition)

class UpNextController(
    private val source: UpNextSource,
    visible: Flow<Boolean>,
    position: Flow<UpNextPosition>,
    disabledSources: Flow<Set<String>>,
    private val scope: CoroutineScope,
) {
    private val raw = MutableStateFlow<UpNextContent>(UpNextContent.Pending)
    private var reload: Job? = null
    val state: StateFlow<UpNextState?> = combine(raw, visible, position, disabledSources) { content, enabled, placement, disabled ->
        val shown = if (content is UpNextContent.Items) UpNextContent.Items(UpNextFeed.build(content.items, disabled)) else content
        if (!enabled || shown is UpNextContent.Items && shown.items.isEmpty()) null else UpNextState(shown, placement)
    }.stateIn(scope, SharingStarted.Eagerly, null)

    val updates: Flow<Unit> = source.changes().onEach { refresh() }

    fun refresh() {
        reload?.cancel()
        reload = scope.launch {
            val skeleton = if (raw.value == UpNextContent.Pending) launch {
                delay(300)
                raw.value = UpNextContent.Skeleton
            } else null
            try {
                val result = source.load()
                result.fold(
                    onSuccess = { raw.value = UpNextContent.Items(it) },
                    onFailure = { error ->
                        if (error is SecurityException) raw.value = UpNextContent.Items(emptyList())
                        else if (raw.value !is UpNextContent.Items) raw.value = UpNextContent.Error
                    },
                )
            } finally { skeleton?.cancel() }
        }
    }
}
