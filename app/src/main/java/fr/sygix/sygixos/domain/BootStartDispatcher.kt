/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.LauncherSystemPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BootStartDispatcher(
    private val store: LauncherSystemStore,
    private val bootCount: () -> Int?,
    private val sinceBootMs: () -> Long,
    private val foreground: () -> Boolean,
    private val openHome: () -> Boolean,
) {
    private val mutex = Mutex()

    suspend fun onBootCompleted(): Boolean = mutex.withLock {
        val preferences = read() ?: return@withLock false
        open(preferences, runCatching(bootCount).getOrNull())
    }

    suspend fun onServiceConnected(): Boolean = mutex.withLock {
        val preferences = read() ?: return@withLock false
        val current = runCatching(bootCount).getOrNull() ?: return@withLock false
        val previous = preferences.accessibilityConnectedAtBoot
        if (previous != current) record(current)
        val enabledBeforeBoot = previous != null && previous < current
        enabledBeforeBoot && !runCatching(foreground).getOrDefault(true) && open(preferences, current)
    }

    private suspend fun read(): LauncherSystemPreferences? = try {
        store.launcherSystem.first()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }

    private suspend fun record(current: Int): Boolean = try {
        store.recordAccessibilityConnected(current)
        true
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        false
    }

    private fun open(preferences: LauncherSystemPreferences, current: Int?): Boolean {
        if (runCatching(sinceBootMs).getOrDefault(Long.MAX_VALUE) >= WINDOW_MS) return false
        val boot = preferences.bootStart
        if (!boot.enabled) return false
        if (current != null && boot.observedAtBoot == current) return false
        return runCatching(openHome).getOrDefault(false)
    }

    companion object {
        const val WINDOW_MS = 5 * 60 * 1_000L
    }
}
