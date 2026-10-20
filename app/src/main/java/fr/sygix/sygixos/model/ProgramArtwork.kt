/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

data class ProgramArtwork(
    val packageName: String,
    val candidates: List<String>,
    val identity: String,
    val publicationId: String = identity,
    val sharedIdentity: String? = null,
) {
    fun withCandidates(values: List<String>) = copy(candidates = values.distinct())
}

internal fun sharedArtworkIdentity(episode: Boolean, seriesId: String?, contentId: String?, internalId: String?, intent: String?, title: String, releaseDate: String?): String? =
    if (episode) seriesId?.let { "series:$it" } else {
        val published = contentId?.let { "content:$it" } ?: internalId?.let { "internal:$it" } ?: intent?.let { "intent:$it" }
        published?.let { listOf("movie", it, title, releaseDate).joinToString("\u0000") }
    }
