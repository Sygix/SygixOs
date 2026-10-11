/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

data class UpNextContentKey(val type: UpNextContentType, val title: String, val year: Int? = null) {
    val serialized: String
        get() = when (type) {
            UpNextContentType.EPISODE -> "${type.name}$SEPARATOR$title"
            UpNextContentType.MOVIE -> "${type.name}$SEPARATOR$title$SEPARATOR${year ?: ""}"
        }

    fun matches(other: UpNextContentKey): Boolean =
        type == other.type && title == other.title &&
            (type == UpNextContentType.EPISODE || year == null || other.year == null || year == other.year)

    fun matches(item: UpNextItem): Boolean = matches(of(item))

    companion object {
        private const val SEPARATOR = "|"

        fun of(item: UpNextItem): UpNextContentKey = when (item.type) {
            UpNextContentType.EPISODE -> UpNextContentKey(UpNextContentType.EPISODE, UpNextFeed.normalize(item.displayTitle))
            UpNextContentType.MOVIE -> UpNextContentKey(UpNextContentType.MOVIE, UpNextFeed.normalize(item.title), item.year)
        }

        fun parse(value: String): UpNextContentKey? {
            val parts = value.split(SEPARATOR)
            val type = UpNextContentType.entries.firstOrNull { it.name == parts.firstOrNull() } ?: return null
            val title = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
            return when (type) {
                UpNextContentType.EPISODE -> if (parts.size == 2) UpNextContentKey(type, title) else null
                UpNextContentType.MOVIE -> {
                    if (parts.size != 3) return null
                    val year = parts[2].takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: return null }
                    UpNextContentKey(type, title, year)
                }
            }
        }
    }
}

data class RemovedUpNextContent(
    val key: UpNextContentKey,
    val title: String,
    val packages: List<String>,
    val imageUrl: String?,
    val removedAt: Long,
)

interface UpNextMemory {
    val opened: Flow<Map<UpNextContentKey, Long>>
    val removed: Flow<List<RemovedUpNextContent>>
    suspend fun recordOpened(key: UpNextContentKey, at: Long)
    suspend fun pruneOpened(keep: (UpNextContentKey) -> Boolean)
    suspend fun addRemoved(content: RemovedUpNextContent)
    suspend fun restoreRemoved(key: UpNextContentKey)
}

class InMemoryUpNextMemory : UpNextMemory {
    override val opened = MutableStateFlow<Map<UpNextContentKey, Long>>(emptyMap())
    override val removed = MutableStateFlow<List<RemovedUpNextContent>>(emptyList())
    override suspend fun recordOpened(key: UpNextContentKey, at: Long) { opened.value = opened.value + (key to at) }
    override suspend fun pruneOpened(keep: (UpNextContentKey) -> Boolean) { opened.value = opened.value.filterKeys(keep) }
    override suspend fun addRemoved(content: RemovedUpNextContent) {
        removed.value = removed.value.filterNot { it.key == content.key } + content
    }
    override suspend fun restoreRemoved(key: UpNextContentKey) { removed.value = removed.value.filterNot { it.key == key } }
}
