/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.LauncherSystemStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class BootStartDispatcher(private val store: LauncherSystemStore, private val openHome: () -> Boolean) {
    suspend fun handle(action: String?): Boolean {
        if (action != "android.intent.action.BOOT_COMPLETED") return false
        val enabled = try {
            store.launcherSystem.first().bootStart.enabled
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }
        if (!enabled) return false
        return runCatching(openHome).getOrDefault(false)
    }
}
