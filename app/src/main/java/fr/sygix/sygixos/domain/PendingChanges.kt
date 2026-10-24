/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class PendingChanges(private val source: Flow<Unit>, private val scope: CoroutineScope) {
    private val pending = MutableStateFlow(false)
    private val observing = AtomicBoolean()

    val changes: Flow<Unit> = flow {
        if (observing.compareAndSet(false, true)) scope.launch { source.collect { pending.value = true } }
        pending.collect { if (it && pending.compareAndSet(true, false)) emit(Unit) }
    }

    fun mark() {
        pending.value = true
    }
}
