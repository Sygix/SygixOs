/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import kotlin.math.roundToInt

object HomePage {

    enum class Transition { SNAP, ZONE, SAME_ZONE }

    fun target(gridActive: Boolean, anchor: GridScroll.Anchor?, origin: Float): Float =
        if (gridActive) maxOf(anchor?.scroll ?: origin, origin) else 0f

    fun transition(previousGridActive: Boolean?, gridActive: Boolean): Transition = when (previousGridActive) {
        null -> Transition.SNAP
        gridActive -> Transition.SAME_ZONE
        else -> Transition.ZONE
    }

    fun heroOnScreen(scroll: Int, heroHeight: Float): Boolean = scroll < heroHeight.roundToInt()
}
