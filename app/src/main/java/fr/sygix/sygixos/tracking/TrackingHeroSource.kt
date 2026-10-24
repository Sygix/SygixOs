/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.HeroRead
import fr.sygix.sygixos.domain.UpNextContentKey

class TrackingHeroSource(local: HeroContentProvider) : HeroContentProvider by local {
    companion object {
    fun project(original: HeroRead, tracking: TrackingState, removed: List<UpNextContentKey>, localItems: List<fr.sygix.sygixos.model.UpNextItem>, disabled: Set<String>): HeroRead {
        val tracked = TrackingProjection.candidates(tracking.contents).filter { content -> removed.none { it.matches(UpNextContentKey.of(TrackingProjection.card(content))) } }
        val local = localItems.filter { it.source.packageName !in disabled }
        val merged = TrackingProjection.merge(local, tracked).filter { item ->
            val content = item.tracking
            content?.membership == AccountMembership.PRESENT && !content.seen && content.failure == null && content.archived != true && content.parent !in tracking.pendingRemovals && removed.none { it.matches(UpNextContentKey.of(item)) }
        }
        val localByKey = local.associateBy { "${it.source.packageName}:${it.id}" }
        val replacements = mutableSetOf<TrackingIdentity>()
        val native = original.items.map { hero ->
            val localKey = "${hero.sourcePackage}:${hero.id.removePrefix("watchnext-")}"
            val content = localByKey[localKey]?.tracking
            val replacement = merged.firstOrNull { it.tracking?.parent == content?.parent }
            if (content == null || replacement == null || !hero.id.startsWith("watchnext-")) hero
            else { replacements += content.parent; TrackingProjection.hero(replacement).copy(id = hero.id, artworkOverride = hero.artwork) }
        }
        return original.copy(items = native + merged.filterNot { it.tracking?.parent in replacements }.map(TrackingProjection::hero), shelfItems = original.shelfItems ?: original.items)
    }
    }
}
