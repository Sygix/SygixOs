/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

enum class TrackingType { SHOW, EPISODE, MOVIE }

data class TrackingIdentity(val provider: String, val type: TrackingType, val id: String)
data class TrackingAccount(val id: String, val name: String)
enum class AccountMembership { PRESENT, ABSENT, UNKNOWN }
enum class TrackingFailure { NETWORK, PROTOCOL, ACCESS, CONFIGURATION, STORAGE, INCOMPLETE }

class TrackingException(val failure: TrackingFailure) : Exception(failure.name)

data class TrackingContent(
    val identity: TrackingIdentity,
    val parent: TrackingIdentity = identity,
    val title: String,
    val episodeTitle: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val year: Int? = null,
    val image: String? = null,
    val poster: String? = null,
    val durationMillis: Long? = null,
    val externalIds: Map<String, String> = emptyMap(),
    val seen: Boolean = false,
    val archived: Boolean = false,
    val membership: AccountMembership = AccountMembership.PRESENT,
    val movieState: Int = 0,
)

data class TrackingSnapshot(val contents: List<TrackingContent>)
data class TrackingState(
    val generation: Long = 0,
    val account: TrackingAccount? = null,
    val loading: Boolean = false,
    val contents: List<TrackingContent> = emptyList(),
    val failure: TrackingFailure? = null,
)

interface WatchTrackingProvider {
    suspend fun account(token: String): TrackingAccount
    suspend fun snapshot(token: String): TrackingSnapshot
    suspend fun revoke(token: String)
}

interface TrackingSessionStore {
    suspend fun read(): String?
    suspend fun write(token: String?)
}
