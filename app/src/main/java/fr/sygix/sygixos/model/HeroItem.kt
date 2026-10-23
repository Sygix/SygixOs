/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

data class HeroItem(
    val id: String,
    val title: String,
    val videoUrl: String? = null,
    val imageUrl: String? = null,
    val sourcePackage: String? = null,
    val sourceLabel: String? = null,
    val launchUri: String? = null,
    val engagement: Long = 0L,
    val kind: ProgramKind = ProgramKind.FEATURED,
    val season: String? = null,
    val episode: String? = null,
    val durationMillis: Long? = null,
    val positionMillis: Long? = null,
    val posterUrl: String? = imageUrl,
    val thumbnailUrl: String? = null,
    val posterAspectRatio: Int = -1,
    val expiresAt: Long = Long.MAX_VALUE,
    val internalProviderId: String? = null,
    val contentId: String? = null,
    val releaseDate: String? = null,
    val episodeArtwork: Boolean = season != null || episode != null,
    val seriesId: String? = null,
    val artworkFailed: Boolean = false,
    val tracking: fr.sygix.sygixos.tracking.TrackingContent? = null,
) {
    val artwork: ProgramArtwork get() = ProgramArtwork(
        sourcePackage.orEmpty(), listOfNotNull(imageUrl, thumbnailUrl, posterUrl).distinct(),
        listOf(sourcePackage, id.substringBefore('-'), title, launchUri, internalProviderId, contentId, releaseDate, season, episode, seriesId, episodeArtwork, posterUrl ?: imageUrl, thumbnailUrl, videoUrl.takeIf { sourcePackage == null }).joinToString("\u0000"),
        publicationId = id,
        sharedIdentity = sharedArtworkIdentity(episodeArtwork, seriesId, contentId, internalProviderId, launchUri, title, releaseDate),
    )
    val fullFingerprint: String get() = listOf(artwork.identity, positionMillis, durationMillis, engagement, kind, sourceLabel).joinToString("\u0000")
    val progress: Float?
        get() = playbackRatio(positionMillis, durationMillis)
}

fun playbackRatio(positionMillis: Long?, durationMillis: Long?): Float? {
    if (positionMillis == null || durationMillis == null) return null
    return if (durationMillis > 0 && positionMillis in 1 until durationMillis) {
        (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
    } else {
        null
    }
}

enum class ProgramKind { FEATURED, CONTINUE, NEXT, NEW, WATCHLIST, WATCH_NEXT }
