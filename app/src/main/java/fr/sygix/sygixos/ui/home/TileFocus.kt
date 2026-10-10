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
import fr.sygix.sygixos.domain.GridSections

@Stable
internal class TileFocus(initialApp: String?) {
    var focusedApp by mutableStateOf(initialApp)
        private set
    private var focusedIndex by mutableIntStateOf(0)
    private var card by mutableStateOf<FocusFallback.CardVisit?>(null)
    var lastCard: String? = null
        private set
    private val requesters = mutableMapOf<String, FocusRequester>()

    fun requesterFor(packageName: String): FocusRequester = requesters.getOrPut(packageName) { FocusRequester() }

    fun onFocused(packageName: String, index: Int, card: FocusFallback.CardVisit? = null) {
        focusedApp = packageName
        focusedIndex = index
        this.card = card
        if (card != null) lastCard = packageName
    }

    fun entry(packages: List<String>): String? = FocusFallback.entry(packages, focusedApp, focusedIndex)

    fun gridEntry(sections: GridSections): String? = FocusFallback.gridEntry(sections, focusedApp, focusedIndex, card)

    fun track(sections: GridSections) {
        val tracked = FocusFallback.track(sections.upNextKeys, focusedApp, card)
        if (tracked == card || tracked == null) return
        card = tracked
        focusedIndex = sections.cardOrderIndex(tracked.index)
    }

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
