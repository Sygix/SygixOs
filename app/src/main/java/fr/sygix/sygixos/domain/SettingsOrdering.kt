/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.TvApp

object SettingsOrdering {

    private val byLabel = compareBy<TvApp> { it.label.lowercase() }

    fun sources(apps: List<TvApp>, counts: Map<String, Int>): List<TvApp> {
        val (publishing, silent) = apps.partition { (counts[it.packageName] ?: 0) > 0 }
        return publishing.sortedWith(compareByDescending<TvApp> { counts.getValue(it.packageName) }.then(byLabel)) +
            silent.sortedWith(byLabel)
    }

    fun hidden(apps: List<TvApp>, dates: Map<String, Long?>): List<TvApp> {
        val (dated, undated) = apps.partition { dates[it.packageName] != null }
        return dated.sortedByDescending { dates.getValue(it.packageName) } + undated.sortedWith(byLabel)
    }
}
