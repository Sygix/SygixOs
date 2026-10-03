/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

data class ReleaseAsset(
    val name: String,
    val state: String,
    val size: Long,
    val digest: String?,
    val downloadUrl: String,
)

data class Release(
    val tag: String,
    val draft: Boolean,
    val prerelease: Boolean,
    val htmlUrl: String,
    val assets: List<ReleaseAsset>,
)

data class UpdateCandidate(
    val tag: String,
    val versionName: String,
    val versionCode: Long,
    val prerelease: Boolean,
    val apkUrl: String,
    val size: Long,
    val sha256: String,
    val htmlUrl: String,
)

data class KnownUpdates(val bestFinal: UpdateCandidate? = null, val bestAny: UpdateCandidate? = null) {

    fun without(tag: String): KnownUpdates = KnownUpdates(
        bestFinal = bestFinal?.takeIf { it.tag != tag },
        bestAny = bestAny?.takeIf { it.tag != tag } ?: bestFinal?.takeIf { it.tag != tag },
    )

    companion object {
        val None = KnownUpdates()
    }
}

sealed interface UpdateError {
    data object NoNetwork : UpdateError
    data object Timeout : UpdateError
    data object SecureConnection : UpdateError
    data class RateLimited(val retryAt: Long) : UpdateError
    data class Unavailable(val code: Int) : UpdateError
    data object Unreadable : UpdateError
    data object Withdrawn : UpdateError
    data object AssetNotFound : UpdateError
    data object Corrupt : UpdateError
    data object SignatureMismatch : UpdateError
    data object Inconsistent : UpdateError
    data object Interrupted : UpdateError
    data object NoSpace : UpdateError
    data object InstallAborted : UpdateError
    data class InstallFailed(val family: InstallFailure) : UpdateError
    data object SystemScreenUnavailable : UpdateError
}

enum class InstallFailure { BLOCKED, CONFLICT, INCOMPATIBLE, INVALID, STORAGE, TIMEOUT, OTHER }

class UpdateException(val error: UpdateError) : Exception(error.toString())

sealed interface CheckResult {
    data object Ok : CheckResult
    data class Error(val error: UpdateError) : CheckResult
}
