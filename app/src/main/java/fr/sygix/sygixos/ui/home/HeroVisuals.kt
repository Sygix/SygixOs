/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import fr.sygix.sygixos.data.VisualCheck
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.ProgramArtwork

internal data class VisualResults(
    val ready: Map<String, String> = emptyMap(),
    val retained: Map<String, HeroItem> = emptyMap(),
    val failed: Set<String> = emptySet(),
    val rejected: Set<String> = emptySet(),
    val checked: Set<String> = emptySet(),
) {
    fun record(id: String, candidates: List<String>, check: VisualCheck) = copy(
        ready = check.visual?.let { ready + (id to it) } ?: (ready - id),
        retained = retained - id,
        failed = if (check.retryable) failed + id else failed - id,
        rejected = if (check.visual == null && !check.retryable) rejected + id else rejected - id,
        checked = checked + candidates + listOfNotNull(check.visual),
    )

    fun withdraw(ids: Set<String>) = copy(ready = ready - ids, failed = failed - ids, rejected = rejected - ids)

    fun keep(live: Set<String>) = copy(
        ready = ready.filterKeys { it in live },
        retained = retained.filterKeys { it in live },
        failed = failed intersect live,
        rejected = rejected intersect live,
    )

    fun admit(item: HeroItem): HeroItem? {
        ready[item.id]?.let { return item.copy(imageUrl = it) }
        retained[item.id]?.let { return it }
        return when {
            item.videoUrl != null || item.artwork.candidates.isEmpty() -> item
            item.id in failed -> item.copy(artworkFailed = true)
            item.id in rejected -> null
            else -> item.copy(artworkPending = true)
        }
    }
}

internal class VisualCarry(val results: VisualResults, val checks: Map<String, ProgramArtwork>, val fresh: Set<String> = emptySet())

internal object HeroFeedTransition {

    fun carry(previous: List<HeroItem>, current: List<HeroItem>, before: VisualCarry, kept: Set<String> = emptySet()): VisualCarry {
        val results = before.results
        val previousIdentities = previous.mapTo(HashSet()) { it.artwork.identity }
        val checkedIdentities = before.checks.values.mapTo(HashSet()) { it.identity }
        val failedIdentities = previous.filter { it.id in results.failed }.mapTo(HashSet()) { it.artwork.identity }
        val rejectedIdentities = previous.filter { it.id in results.rejected }.mapTo(HashSet()) { it.artwork.identity }
        val readyByIdentity = previous.mapNotNull { old -> results.ready[old.id]?.let { old.artwork.identity to it } }.toMap()
        val previousByContent = previous.associateBy(::contentIdentity)
        val heldByContent = results.retained.values.associateBy(::contentIdentity)
        val ids = current.mapTo(HashSet()) { it.id }
        val checks = before.checks.filterKeys { it in ids }.toMutableMap()
        val failed = (results.failed intersect ids).toMutableSet()
        val rejected = (results.rejected intersect ids).toMutableSet()
        val retained = mutableMapOf<String, HeroItem>()
        val ready = mutableMapOf<String, String>()
        val fresh = mutableSetOf<String>()
        current.forEach { item ->
            val identity = item.artwork.identity
            val content = contentIdentity(item)
            val held = heldByContent[content] ?: previousByContent[content]?.takeIf { it.artwork.identity != identity }
                ?.let { old -> results.ready[old.id]?.let { old.copy(imageUrl = it) } }
            held?.let { retained[item.id] = it.copy(id = item.id) }
            if (identity in checkedIdentities) checks[item.id] = item.artwork
            if (identity in failedIdentities) failed += item.id
            if (identity in rejectedIdentities) rejected += item.id
            readyByIdentity[identity]?.let { ready[item.id] = it }
            if (identity !in previousIdentities) {
                fresh += item.id
                checks -= item.id
                failed -= item.id
                rejected -= item.id
            }
        }
        val visible = current.flatMapTo(HashSet()) { it.artwork.candidates } + ready.values + kept
        return VisualCarry(VisualResults(ready, retained, failed, rejected, results.checked intersect visible), checks, fresh)
    }

    private fun contentIdentity(item: HeroItem) = listOf(item.sourcePackage, item.id.substringBefore('-'), item.title, item.launchUri,
        item.internalProviderId, item.contentId, item.season, item.episode, item.seriesId, item.releaseDate)
}
