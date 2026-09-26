/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.domain.FocusFallback

@Stable
internal class TileFocus(initialApp: String?) {
    var focusedApp by mutableStateOf(initialApp)
        private set
    private var focusedIndex by mutableIntStateOf(0)
    private val requesters = mutableMapOf<String, FocusRequester>()

    fun requesterFor(packageName: String): FocusRequester = requesters.getOrPut(packageName) { FocusRequester() }

    fun onFocused(packageName: String, index: Int) {
        focusedApp = packageName
        focusedIndex = index
    }

    fun entry(packages: List<String>): String? = FocusFallback.entry(packages, focusedApp, focusedIndex)

    fun refocus(packages: List<String>, active: Boolean): String? =
        FocusFallback.refocus(packages, focusedApp, focusedIndex, active)
}

@Composable
internal fun rememberTileFocus(packages: List<String>, focusEnabled: Boolean, initialApp: String? = null): TileFocus {
    val focus = remember { TileFocus(initialApp) }
    val target = focus.refocus(packages, focusEnabled)
    LaunchedEffect(packages, focusEnabled) {
        if (target == null) return@LaunchedEffect
        withFrameNanos { }
        focus.requesterFor(target).tryRequestFocus()
    }
    return focus
}
