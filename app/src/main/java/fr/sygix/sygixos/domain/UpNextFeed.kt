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
    private val Marks = Regex("\\p{M}+")
    private val Year = Regex("\\(\\d{4}\\)")
    private val Separators = Regex("[^\\p{L}\\p{N}]+")
    private val Spaces = Regex("\\s+")

    fun normalize(title: String): String = Normalizer.normalize(title, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
        .replace(Marks, "")
        .replace(Year, "")
        .replace(Separators, " ")
        .trim()
        .replace(Spaces, " ")

    const val LIMIT = 20

    fun build(
        items: List<UpNextItem>,
        disabledSources: Set<String> = emptySet(),
        opened: Map<UpNextContentKey, Long> = emptyMap(),
        removed: Collection<UpNextContentKey> = emptyList(),
    ): List<UpNextItem> {
        val normalized = mutableMapOf<String, String>()
        fun title(value: String): String = normalized.getOrPut(value) { normalize(value) }
        val enabled = items.filter { it.tracking?.membership == fr.sygix.sygixos.tracking.AccountMembership.PRESENT || it.source.packageName !in disabledSources }
        val remade = enabled.filter { it.type == UpNextContentType.MOVIE }.groupBy { title(it.title) }
            .filterValues { movies -> movies.mapNotNull { it.year }.distinct().size > 1 }.keys
        val exact = enabled.groupBy { listOf(it.source.packageName, it.type, it.tracking?.identity ?: exactIdentity(it)) }.values.map(::merge)
        val series = exact.groupBy {
            if (it.type == UpNextContentType.EPISODE) listOf(it.source.packageName, "series", it.tracking?.parent ?: title(it.displayTitle))
            else listOf(it.source.packageName, "row", it.id.toString())
        }.values.map(::merge)
        val episodes = mergeAcrossApps(series) { item ->
            if (item.type == UpNextContentType.EPISODE && item.season != null && item.episode != null) {
                EpisodeKey(item.tracking?.parent ?: title(item.displayTitle), number(item.season), number(item.episode))
            } else null
        }
        val movies = mergeAcrossApps(episodes) { item ->
            if (item.type == UpNextContentType.MOVIE) {
                val name = title(item.title)
                item.tracking?.parent ?: MovieKey(name, item.year.takeIf { name in remade })
            } else null
        }
        val keys = movies.associateWith(UpNextContentKey::of)
        val kept = movies.filter { item -> removed.none { it.matches(keys.getValue(item)) } }
        val openedAt = kept.associateWith { item ->
            val key = keys.getValue(item)
            opened.entries.filter { it.key.matches(key) }.maxOfOrNull { it.value }
        }
        return kept.sortedWith(compareByDescending<UpNextItem> { openedAt[it] }
            .thenBy { group(it.watchNextType) }
            .thenByDescending { it.engagement }.thenBy { it.id }).take(LIMIT)
    }

    fun stillPublished(key: UpNextContentKey, items: List<UpNextItem>): Boolean = items.any { key.matches(it) }

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

    private data class EpisodeKey(val identity: Any, val season: String, val episode: String)
    private data class MovieKey(val title: String, val year: Int?)

    private fun mergeAcrossApps(items: List<UpNextItem>, identity: (UpNextItem) -> Any?): List<UpNextItem> {
        val keyed = items.map { it to identity(it) }
        val groups = keyed.filter { it.second != null }.groupBy({ it.second }, { it.first })
            .filterValues { group -> group.any { it.source.packageName != group.first().source.packageName } }
        val merged = mutableSetOf<Any?>()
        return keyed.mapNotNull { (item, key) ->
            val group = groups[key] ?: return@mapNotNull item
            if (!merged.add(key)) return@mapNotNull null
            val (sameApp, otherApps) = group.drop(1).partition { it.source.packageName == group.first().source.packageName }
            merge(listOf(group.first()) + otherApps + sameApp)
        }
    }
}
