/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object UpNextIdentityMigration {
    private fun key(key: UpNextContentKey, links: List<UpNextIdentityLink>): UpNextContentKey {
        if (key.tracking != null) return key
        val identities = links.filter { link ->
            link.legacy.tracking == null && link.enriched.tracking != null && link.legacy.type == link.enriched.type &&
                link.legacy.title == link.enriched.title && link.legacy.year == link.enriched.year && link.legacy.matches(key)
        }.mapNotNull { it.enriched.tracking }.distinct()
        return identities.singleOrNull()?.let { key.copy(tracking = it) } ?: key
    }

    fun opened(current: Map<UpNextContentKey, Long>, links: List<UpNextIdentityLink>): Map<UpNextContentKey, Long> =
        current.entries.map { key(it.key, links) to it.value }.groupBy { it.first.tracking ?: it.first }.values
            .map { group -> group.maxBy { it.second } }.toMap()

    fun removed(current: List<RemovedUpNextContent>, links: List<UpNextIdentityLink>): List<RemovedUpNextContent> =
        current.map { it.copy(key = key(it.key, links)) }.groupBy { it.key.tracking ?: it.key }.values.map { group ->
            val latest = group.maxBy { it.removedAt }
            latest.copy(packages = group.flatMap { it.packages }.distinct(), imageUrl = latest.imageUrl ?: group.firstNotNullOfOrNull { it.imageUrl })
        }
}
