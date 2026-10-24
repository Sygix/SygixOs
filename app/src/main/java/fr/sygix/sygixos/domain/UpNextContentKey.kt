/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

data class UpNextContentKey(val type: UpNextContentType, val title: String, val year: Int? = null, val tracking: fr.sygix.sygixos.tracking.TrackingIdentity? = null) {
    val serialized: String
        get() = when (type) {
            UpNextContentType.EPISODE -> "${type.name}$SEPARATOR$title"
            UpNextContentType.MOVIE -> "${type.name}$SEPARATOR$title$SEPARATOR${year ?: ""}"
        } + (tracking?.let { "$SEPARATOR@${SEPARATOR}${it.provider}$SEPARATOR${it.type}$SEPARATOR${it.id}" } ?: "")

    fun matches(other: UpNextContentKey): Boolean =
        type == other.type && if (tracking != null && other.tracking != null) tracking == other.tracking
        else title == other.title && (type == UpNextContentType.EPISODE || year == null || other.year == null || year == other.year)

    fun matches(item: UpNextItem): Boolean = matches(of(item))

    companion object {
        private const val SEPARATOR = "|"
        private val ProviderPattern = Regex("[a-z][a-z0-9_-]{0,63}")
        private val IdPattern = Regex("[A-Za-z0-9_-]{1,128}")

        fun of(item: UpNextItem): UpNextContentKey = when (item.type) {
            UpNextContentType.EPISODE -> UpNextContentKey(UpNextContentType.EPISODE, UpNextFeed.normalize(item.displayTitle), tracking = item.tracking?.parent)
            UpNextContentType.MOVIE -> UpNextContentKey(UpNextContentType.MOVIE, UpNextFeed.normalize(item.title), item.year, item.tracking?.parent)
        }

        fun parse(value: String): UpNextContentKey? {
            val parts = value.split(SEPARATOR)
            val type = UpNextContentType.entries.firstOrNull { it.name == parts.firstOrNull() } ?: return null
            val title = parts.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return null
            val baseSize = if (type == UpNextContentType.EPISODE) 2 else 3
            val tracking = if (parts.size == baseSize) null else {
                if (parts.size != baseSize + 4 || parts[baseSize] != "@") return null
                val provider = parts[baseSize + 1].takeIf { it.matches(ProviderPattern) } ?: return null
                val trackingType = fr.sygix.sygixos.tracking.TrackingType.entries.firstOrNull { it.name == parts[baseSize + 2] } ?: return null
                val id = parts[baseSize + 3].takeIf { it.matches(IdPattern) } ?: return null
                fr.sygix.sygixos.tracking.TrackingIdentity(provider, trackingType, id)
            }
            return when (type) {
                UpNextContentType.EPISODE -> UpNextContentKey(type, title, tracking = tracking)
                UpNextContentType.MOVIE -> {
                    val year = parts[2].takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: return null }
                    UpNextContentKey(type, title, year, tracking)
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

data class UpNextIdentityLink(val legacy: UpNextContentKey, val enriched: UpNextContentKey)

interface UpNextMemory {
    val opened: Flow<Map<UpNextContentKey, Long>>
    val removed: Flow<List<RemovedUpNextContent>>
    suspend fun recordOpened(key: UpNextContentKey, at: Long)
    suspend fun pruneOpened(keep: (UpNextContentKey) -> Boolean)
    suspend fun addRemoved(content: RemovedUpNextContent)
    suspend fun restoreRemoved(key: UpNextContentKey)
    suspend fun enrich(links: List<UpNextIdentityLink>) {}
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
    override suspend fun enrich(links: List<UpNextIdentityLink>) {
        opened.value = UpNextIdentityMigration.opened(opened.value, links)
        removed.value = UpNextIdentityMigration.removed(removed.value, links)
    }
}
