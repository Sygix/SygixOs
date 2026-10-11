/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

enum class AccessibilityCheckStatus { IDLE, CHECKING, BLOCKED }

class AccessibilityCheck(
    private val enabled: () -> Boolean,
    private val connected: StateFlow<Boolean>,
    private val scope: CoroutineScope,
    private val waitMs: Long = WAIT_MS,
) {
    private val mutableStatus = MutableStateFlow(AccessibilityCheckStatus.IDLE)
    val status: StateFlow<AccessibilityCheckStatus> = mutableStatus.asStateFlow()

    fun check() {
        if (mutableStatus.value == AccessibilityCheckStatus.CHECKING) return
        if (!runCatching(enabled).getOrDefault(false) || connected.value) {
            mutableStatus.value = AccessibilityCheckStatus.IDLE
            return
        }
        if (mutableStatus.value == AccessibilityCheckStatus.BLOCKED) return
        mutableStatus.value = AccessibilityCheckStatus.CHECKING
        scope.launch {
            val bound = withTimeoutOrNull(waitMs) { connected.first { it } } != null
            mutableStatus.value = if (bound || connected.value) AccessibilityCheckStatus.IDLE else AccessibilityCheckStatus.BLOCKED
        }
    }

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
