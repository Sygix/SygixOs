/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import fr.sygix.sygixos.core.designsystem.FocusTargets
import fr.sygix.sygixos.domain.FocusFallback
import fr.sygix.sygixos.domain.GridSections

@Stable
internal class TileFocus : FocusTargets() {
    var focusedApp by mutableStateOf<String?>(null)
        private set
    private var focusedIndex by mutableIntStateOf(0)
    private var card by mutableStateOf<FocusFallback.CardVisit?>(null)
    var lastCard: String? = null
        private set

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
}
