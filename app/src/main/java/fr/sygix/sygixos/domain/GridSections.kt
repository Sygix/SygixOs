/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextPosition

data class GridSections(
    val upNextKeys: List<String>,
    val apps: List<String>,
    val upNextShown: Boolean,
    val upNextFirst: Boolean,
) {
    val focusOrder: List<String> get() = if (upNextFirst) upNextKeys + apps else apps + upNextKeys
    val firstAppRow: Int get() = if (upNextShown && upNextFirst) 1 else 0
    val appIndexOffset: Int get() = if (upNextFirst) upNextKeys.size else 0

    fun upNextRow(appRows: Int): Int = if (upNextFirst) 0 else appRows

    fun cardOrderIndex(index: Int): Int = if (upNextFirst) index else apps.size + index

    fun rowOf(key: String?, columns: Int): Int {
        val appRows = (apps.size + columns - 1) / columns
        if (key in upNextKeys) return upNextRow(appRows)
        val index = apps.indexOf(key)
        return if (index < 0) -1 else index / columns + firstAppRow
    }

    sealed interface Up {
        data object Exit : Up
        data object Default : Up
        data class Card(val key: String) : Up
    }

    fun upFrom(focused: String?, columns: Int, lastCard: String?): Up {
        if (focused == null) return Up.Exit
        if (focused in upNextKeys) return if (upNextFirst) Up.Exit else Up.Default
        val index = apps.indexOf(focused)
        if (index < 0 || index >= columns) return Up.Default
        if (!upNextFirst || upNextKeys.isEmpty()) return Up.Exit
        return Up.Card(lastCard?.takeIf { it in upNextKeys } ?: upNextKeys.first())
    }

    fun emptyAppsHeight(viewport: Float, margin: Float, upNextHeight: Float, titleHeight: Float, rowSpacing: Float): Float =
        (viewport - 2 * margin - if (upNextShown) upNextHeight + titleHeight + rowSpacing else 0f).coerceAtLeast(0f)

    fun rows(appRows: Int, upNextHeight: Float, appRowHeight: Float, titleHeight: Float): List<GridScroll.RowGeometry> = buildList {
        if (upNextShown && upNextFirst) add(GridScroll.RowGeometry(upNextHeight, titleHeight))
        repeat(appRows) { add(GridScroll.RowGeometry(appRowHeight, if (it == 0) titleHeight else 0f)) }
        if (upNextShown && !upNextFirst) add(GridScroll.RowGeometry(upNextHeight, titleHeight))
    }

    companion object {
        const val ERROR_KEY = "upnext-error"

        fun of(upNext: UpNextState?, apps: List<String>): GridSections {
            val shown = upNext?.takeUnless { it.content == UpNextContent.Pending }
            val keys = when (val content = shown?.content) {
                is UpNextContent.Items -> content.items.map { it.key }
                UpNextContent.Error -> listOf(ERROR_KEY)
                else -> emptyList()
            }
            return GridSections(
                upNextKeys = keys,
                apps = apps,
                upNextShown = shown != null,
                upNextFirst = shown?.position != UpNextPosition.AFTER_APPS || apps.isEmpty(),
            )
        }

        fun hasFocusTarget(appCount: Int, upNext: UpNextState?): Boolean =
            appCount > 0 || of(upNext, emptyList()).upNextKeys.isNotEmpty()
    }
}
