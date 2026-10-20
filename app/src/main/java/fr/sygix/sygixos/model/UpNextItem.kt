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
) {
    val artwork: ProgramArtwork get() = ProgramArtwork(
        source.packageName, listOfNotNull(imageUrl, thumbnailUrl, posterUrl).distinct(),
        listOf("watchnext-$id", title, source.intentUri, internalProviderId, contentId, releaseDate, season, episode, seriesId, type == UpNextContentType.EPISODE).joinToString("\u0000"),
        publicationId = "watchnext-$id",
        sharedIdentity = sharedArtworkIdentity(type == UpNextContentType.EPISODE, seriesId, contentId, internalProviderId, source.intentUri, title, releaseDate),
    )
    val key: String get() = "${source.packageName}:$id"
    val displayTitle: String get() = if (type == UpNextContentType.EPISODE) seriesTitle ?: title else title
    val progress: Float? get() = playbackRatio(positionMillis, durationMillis)
}

enum class UpNextContentType { EPISODE, MOVIE }
enum class UpNextPosition { BEFORE_APPS, AFTER_APPS }
