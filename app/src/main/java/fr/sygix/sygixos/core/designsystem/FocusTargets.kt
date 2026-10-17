/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester

@Stable
open class FocusTargets {
    private val targets = mutableMapOf<String, FocusRequester>()

    @Composable
    fun bind(key: String, requester: FocusRequester? = null): FocusRequester {
        val bound = remember(key, requester) { requester ?: FocusRequester() }
        DisposableEffect(key, bound) {
            targets[key] = bound
            onDispose { if (targets[key] === bound) targets.remove(key) }
        }
        return bound
    }

    fun focus(key: String): Boolean = targets[key]?.tryRequestFocus() == true

    suspend fun reveal(key: String, show: suspend () -> Unit): Boolean {
        show()
        return focus(key)
    }
}
