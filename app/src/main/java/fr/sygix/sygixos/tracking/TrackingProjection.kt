/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.UpNextFeed
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry

object TrackingProjection {
    fun card(content: TrackingContent): UpNextItem = UpNextItem(
        id = content.identity.id.toLongOrNull() ?: content.identity.hashCode().toLong(),
        source = UpNextSourceEntry(content.readerPackage.orEmpty(), content.readerName ?: "BetaSeries"),
        type = if (content.identity.type == TrackingType.MOVIE) UpNextContentType.MOVIE else UpNextContentType.EPISODE,
        title = content.title, seriesTitle = content.title.takeUnless { content.identity.type == TrackingType.MOVIE },
        season = content.season?.toString(), episode = content.episode?.toString(), episodeTitle = content.episodeTitle,
        imageUrl = content.image ?: content.poster, portrait = content.image == null && content.poster != null,
        durationMillis = content.durationMillis, watchNextType = if (content.identity.type == TrackingType.MOVIE) ProgramKind.WATCHLIST else ProgramKind.NEXT,
        year = content.year, externalIds = content.externalIds, sources = emptyList(), posterUrl = content.poster,
        tracking = content, providerArtwork = true,
    )

    fun hero(content: TrackingContent): HeroItem = HeroItem(
        id = "tracking:${content.identity.type}:${content.identity.id}", title = content.title,
        imageUrl = content.image, sourcePackage = content.readerPackage, sourceLabel = content.readerName ?: "BetaSeries", kind = if (content.identity.type == TrackingType.MOVIE) ProgramKind.WATCHLIST else ProgramKind.NEXT,
        season = content.season?.toString(), episode = content.episode?.toString(), durationMillis = content.durationMillis,
        posterUrl = content.poster, tracking = content,
    )

    fun hero(item: UpNextItem): HeroItem {
        val content = requireNotNull(item.tracking)
        return hero(content).copy(imageUrl = item.imageUrl, posterUrl = item.posterUrl, thumbnailUrl = item.thumbnailUrl,
            positionMillis = item.positionMillis, durationMillis = item.durationMillis, kind = item.watchNextType,
            artworkOverride = item.artwork,
            playbackTargets = item.sources.mapNotNull { source -> source.targetIdentity?.let { PlaybackTarget(source.packageName, it, source.intentUri) } })
    }

    fun merge(local: List<UpNextItem>, contents: List<TrackingContent>): List<UpNextItem> {
        val remaining = local.toMutableList()
        val tracked = contents.map { content ->
            val candidates = remaining.filter { exactMatch(it, content) }
            remaining.removeAll(candidates.toSet())
            val resume = candidates.filter { it.watchNextType == ProgramKind.CONTINUE && it.tracking?.seen != true }.maxByOrNull { it.engagement }
            if (resume != null) {
                val sameEpisode = content.identity.type == TrackingType.MOVIE || resume.season?.toIntOrNull() == content.season && resume.episode?.toIntOrNull() == content.episode
                val identity = resume.tracking ?: content.takeIf { sameEpisode }
                resume.copy(tracking = identity?.copy(membership = AccountMembership.PRESENT, readerPackage = content.readerPackage, readerName = content.readerName), sources = candidates.flatMap { it.sources }.distinctBy { it.packageName to it.targetIdentity })
            } else card(content).copy(sources = candidates.flatMap { it.sources }.distinctBy { it.packageName to it.targetIdentity })
        }
        return remaining.filterNot { it.tracking?.membership == AccountMembership.PRESENT && it.tracking.seen } + tracked
    }

    private fun exactMatch(local: UpNextItem, tracked: TrackingContent): Boolean {
        val sameType = (local.type == UpNextContentType.MOVIE) == (tracked.identity.type == TrackingType.MOVIE)
        return sameType && (local.tracking?.parent == tracked.parent || local.externalIds.any { (namespace, id) -> tracked.externalIds[namespace] == id })
    }
}
