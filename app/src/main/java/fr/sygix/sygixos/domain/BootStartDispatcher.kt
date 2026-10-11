/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BootStartDispatcher(
    private val store: LauncherSystemStore,
    private val bootCount: () -> Int?,
    private val sinceBootMs: () -> Long,
    private val openHome: () -> Boolean,
) {
    private val mutex = Mutex()
    private var opened = false

    suspend fun dispatch(): Boolean = mutex.withLock {
        if (opened || runCatching(sinceBootMs).getOrDefault(Long.MAX_VALUE) >= WINDOW_MS) return@withLock false
        val boot = try {
            store.launcherSystem.first().bootStart
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            return@withLock false
        }
        if (!boot.enabled) return@withLock false
        val current = runCatching(bootCount).getOrNull()
        if (current != null && boot.observedAtBoot == current) return@withLock false
        opened = runCatching(openHome).getOrDefault(false)
        opened
    }

    companion object {
        const val WINDOW_MS = 5 * 60 * 1_000L
    }
}
