/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

enum class TrackingType { SHOW, EPISODE, MOVIE }
enum class TrackingAction { ADD, SEEN, UNSEEN, ARCHIVE, UNARCHIVE, REMOVE, RESTORE_MOVIE_STATE }

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
    val readerPackage: String? = null,
    val readerName: String? = null,
    val sessionGeneration: Long? = null,
)

data class TrackingSnapshot(val contents: List<TrackingContent>, val memberships: Set<TrackingIdentity> = contents.map { it.parent }.toSet())
data class TrackingState(
    val generation: Long = 0,
    val account: TrackingAccount? = null,
    val loading: Boolean = false,
    val contents: List<TrackingContent> = emptyList(),
    val failure: TrackingFailure? = null,
    val memberships: Set<TrackingIdentity>? = null,
    val pendingRemovals: Set<TrackingIdentity> = emptySet(),
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
