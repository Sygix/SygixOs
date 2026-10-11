/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class RebindStatus { IDLE, CHECKING, BLOCKED }

class AccessibilityRebind(
    private val enabled: () -> Boolean,
    private val connected: StateFlow<Boolean>,
    private val toggle: suspend () -> Unit,
    private val scope: CoroutineScope,
    private val waitMs: Long = WAIT_MS,
) {
    private val mutableStatus = MutableStateFlow(RebindStatus.IDLE)
    val status: StateFlow<RebindStatus> = mutableStatus.asStateFlow()
    private var toggled = false

    fun check() {
        if (mutableStatus.value == RebindStatus.CHECKING) return
        if (!runCatching(enabled).getOrDefault(false) || connected.value) {
            mutableStatus.value = RebindStatus.IDLE
            return
        }
        if (mutableStatus.value == RebindStatus.BLOCKED && toggled) return
        mutableStatus.value = RebindStatus.CHECKING
        scope.launch {
            val bound = awaitConnection() || !toggled && toggleOnce() && awaitConnection()
            mutableStatus.value = if (bound || connected.value) RebindStatus.IDLE else RebindStatus.BLOCKED
        }
    }

    private suspend fun toggleOnce(): Boolean {
        toggled = true
        try {
            toggle()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Unit
        }
        return true
    }

    private suspend fun awaitConnection(): Boolean = withTimeoutOrNull(waitMs) { connected.first { it } } != null

    companion object {
        const val WAIT_MS = 2_000L

        fun isEnabled(setting: String?, packageName: String, className: String): Boolean =
            setting.orEmpty().split(':').any { entry ->
                val parts = entry.trim().split('/')
                if (parts.size != 2 || parts[0] != packageName) return@any false
                val name = parts[1].let { if (it.startsWith('.')) packageName + it else it }
                name == className
            }
    }
}
