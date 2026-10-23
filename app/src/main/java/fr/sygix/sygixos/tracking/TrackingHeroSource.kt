/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.HeroRead
import fr.sygix.sygixos.domain.UpNextContentKey
import fr.sygix.sygixos.domain.UpNextMemory
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CancellationException

class TrackingHeroSource(
    private val local: HeroContentProvider,
    private val tracking: StateFlow<TrackingState>,
    private val memory: UpNextMemory,
    private val enrichment: TrackingLocalContents? = null,
    private val disabledSources: Flow<Set<String>> = kotlinx.coroutines.flow.flowOf(emptySet()),
) : HeroContentProvider {
    override suspend fun load(): List<HeroItem> = read().items
    override suspend fun read(): HeroRead {
        val original = local.read()
        val removed = try { memory.removed.first().map { it.key } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { return original.copy(errors = original.errors + error) }
        val tracked = tracking.value.contents.filter { content -> removed.none { it.matches(UpNextContentKey.of(TrackingProjection.card(content))) } }
        val disabled = disabledSources.first()
        val local = enrichment?.items().orEmpty().filter { it.source.packageName !in disabled }
        val merged = TrackingProjection.merge(local, tracked).filter { item ->
            val content = item.tracking
            content?.membership == AccountMembership.PRESENT && content.parent !in tracking.value.pendingRemovals && removed.none { it.matches(UpNextContentKey.of(item)) }
        }
        val identities = merged.mapNotNull { it.tracking?.parent }.toSet()
        val localByKey = local.associateBy { "${it.source.packageName}:${it.id}" }
        val native = original.items.filter { hero ->
            val localKey = "${hero.sourcePackage}:${hero.id.removePrefix("watchnext-")}"
            val content = localByKey[localKey]?.tracking
            content == null || content.parent !in identities || !hero.id.startsWith("watchnext-")
        }
        return original.copy(items = native + merged.map(TrackingProjection::hero), shelfItems = original.shelfItems ?: original.items)
    }

    override fun changes(): Flow<Unit> = merge(local.changes(), tracking.map { Unit }, memory.removed.catch { emit(emptyList()) }.map { Unit },
        enrichment?.contents?.map { Unit } ?: kotlinx.coroutines.flow.emptyFlow(), disabledSources.map { Unit })
}
