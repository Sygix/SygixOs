/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

data class ProgramArtwork(val packageName: String, val candidates: List<String>, val identity: String) {
    fun withCandidates(values: List<String>) = copy(candidates = values.distinct())
}
