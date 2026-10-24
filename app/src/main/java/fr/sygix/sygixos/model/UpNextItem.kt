/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

import android.graphics.drawable.Drawable

data class UpNextSourceEntry(
    val packageName: String,
    val label: String = packageName,
    val icon: Drawable? = null,
    val intentUri: String? = null,
    val targetIdentity: fr.sygix.sygixos.tracking.TrackingIdentity? = null,
)

data class UpNextItem(
    val id: Long,
    val source: UpNextSourceEntry,
    val type: UpNextContentType,
    val title: String,
    val seriesTitle: String? = null,
    val season: String? = null,
    val episode: String? = null,
    val episodeTitle: String? = null,
    val titleFromEpisode: Boolean = false,
    val imageUrl: String? = null,
    val portrait: Boolean = false,
    val positionMillis: Long? = null,
    val durationMillis: Long? = null,
    val watchNextType: ProgramKind = ProgramKind.WATCH_NEXT,
    val engagement: Long = 0L,
    val internalProviderId: String? = null,
    val contentId: String? = null,
    val year: Int? = null,
    val externalIds: Map<String, String> = emptyMap(),
    val sources: List<UpNextSourceEntry> = listOf(source),
    val posterUrl: String? = imageUrl,
    val thumbnailUrl: String? = null,
    val posterAspectRatio: Int = -1,
    val releaseDate: String? = null,
    val seriesId: String? = null,
    val tracking: fr.sygix.sygixos.tracking.TrackingContent? = null,
    val providerArtwork: Boolean = false,
) {
    val artwork: ProgramArtwork get() = tracking?.takeIf { providerArtwork }?.let { content -> ProgramArtwork(
        content.identity.provider, listOfNotNull(imageUrl, thumbnailUrl, posterUrl).distinct(),
        listOf(content.identity, title, season, episode, imageUrl, thumbnailUrl, posterUrl).joinToString("\u0000"),
        publicationId = "tracking-card-${content.identity.type}-${content.identity.id}",
        sharedIdentity = "${content.parent.provider}:${content.parent.type}:${content.parent.id}",
    ) } ?: ProgramArtwork(
        source.packageName, listOfNotNull(imageUrl, thumbnailUrl, posterUrl).distinct(),
        listOf(source.packageName, "watchnext", title, source.intentUri, internalProviderId, contentId, releaseDate, season, episode, seriesId, type == UpNextContentType.EPISODE, posterUrl ?: imageUrl, thumbnailUrl).joinToString("\u0000"),
        publicationId = "watchnext-$id",
        sharedIdentity = sharedArtworkIdentity(type == UpNextContentType.EPISODE, seriesId, contentId, internalProviderId, source.intentUri, title, releaseDate),
    )
    val key: String get() = tracking?.identity?.takeIf { providerArtwork }?.let { "${it.provider}:${it.type}:${it.id}" } ?: "${source.packageName}:$id"
    val cardIdentity: String get() = listOf(source.packageName, type, title, source.intentUri, internalProviderId, contentId,
        releaseDate, season, episode, seriesId).joinToString("\u0000")
    val displayTitle: String get() = if (type == UpNextContentType.EPISODE) seriesTitle ?: title else title
    val progress: Float? get() = playbackRatio(positionMillis, durationMillis)
}

enum class UpNextContentType { EPISODE, MOVIE }
enum class UpNextPosition { BEFORE_APPS, AFTER_APPS }
