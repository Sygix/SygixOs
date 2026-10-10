/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import java.text.Normalizer
import java.util.Locale

object UpNextFeed {
    fun normalize(title: String): String = Normalizer.normalize(title, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .replace(Regex("\\p{M}+"), "")
        .replace(Regex("\\(\\d{4}\\)"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    fun build(items: List<UpNextItem>, disabledSources: Set<String> = emptySet()): List<UpNextItem> {
        val enabled = items.filter { it.source.packageName !in disabledSources }
        val years = enabled.filter { it.type == UpNextContentType.MOVIE }
            .groupBy { normalize(it.title) }.mapValues { (_, movies) -> movies.mapNotNull { it.year }.toSet() }
        val exact = enabled.groupBy { listOf(it.source.packageName, exactIdentity(it)) }.values.map(::merge)
        val series = exact.groupBy {
            if (it.type == UpNextContentType.EPISODE) listOf(it.source.packageName, "series", normalize(it.displayTitle))
            else listOf(it.source.packageName, "row", it.id.toString())
        }.values.map(::merge)
        val episodes = mergeAcrossApps(series) { a, b ->
            a.type == UpNextContentType.EPISODE && b.type == UpNextContentType.EPISODE &&
                a.season != null && a.episode != null && b.season != null && b.episode != null &&
                normalize(a.displayTitle) == normalize(b.displayTitle) &&
                number(a.season) == number(b.season) && number(a.episode) == number(b.episode)
        }
        val movies = mergeAcrossApps(episodes) { a, b ->
            val title = normalize(a.title)
            a.type == UpNextContentType.MOVIE && b.type == UpNextContentType.MOVIE &&
                title == normalize(b.title) &&
                (a.year == b.year || ((a.year == null || b.year == null) && years.getValue(title).size <= 1))
        }
        return movies.sortedWith(compareBy<UpNextItem> { group(it.watchNextType) }
            .thenByDescending { it.engagement }.thenBy { it.id }).take(20)
    }

    private fun exactIdentity(item: UpNextItem): String = when {
        item.internalProviderId != null -> "provider:${item.internalProviderId}"
        item.contentId != null -> "content:${item.contentId}"
        item.source.intentUri != null -> "intent:${item.source.intentUri}"
        else -> "row:${item.id}"
    }

    private fun number(value: String): String = value.toIntOrNull()?.toString() ?: value

    private fun group(kind: ProgramKind): Int = when (kind) {
        ProgramKind.CONTINUE -> 0
        ProgramKind.WATCHLIST -> 2
        else -> 1
    }

    private val preference = listOf("org.jellyfin.androidtv")
    private val winner = compareBy<UpNextItem> { group(it.watchNextType) }
        .thenByDescending { it.engagement }
        .thenBy { preference.indexOf(it.source.packageName).takeIf { index -> index >= 0 } ?: preference.size }

    private fun merge(items: List<UpNextItem>): UpNextItem {
        val ranked = items.sortedWith(winner)
        return ranked.first().copy(sources = ranked.flatMap { it.sources }.distinctBy { it.packageName })
    }

    private fun mergeAcrossApps(items: List<UpNextItem>, matches: (UpNextItem, UpNextItem) -> Boolean): List<UpNextItem> {
        val remaining = items.toMutableList()
        val result = mutableListOf<UpNextItem>()
        while (remaining.isNotEmpty()) {
            val members = mutableListOf(remaining.removeAt(0))
            var changed: Boolean
            do {
                val added = remaining.filter { candidate -> members.any { member ->
                    member.source.packageName != candidate.source.packageName && matches(member, candidate)
                } }
                changed = added.isNotEmpty()
                members.addAll(added)
                remaining.removeAll(added.toSet())
            } while (changed)
            result.add(merge(members))
        }
        return result
    }
}
