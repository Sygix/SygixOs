/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

const val TsuyuPackage = "fr.sygix.tsuyu"
const val TsuyuMinimumApi = 13

data class TsuyuRelease(val tag: String, val prerelease: Boolean, val apkUrl: String, val size: Long, val sha256: String)

enum class TsuyuInstallError { NO_NETWORK, INVALID, REFUSED }

class TsuyuInstallException(val error: TsuyuInstallError) : Exception(error.name)

sealed interface TsuyuInstallStep {
    data class Downloading(val percent: Int) : TsuyuInstallStep
    data object Verifying : TsuyuInstallStep
    data object Installing : TsuyuInstallStep
}

interface TsuyuReleases {
    suspend fun refresh(includePrereleases: Boolean): Result<TsuyuRelease?>
    suspend fun lastKnown(): TsuyuRelease?
}

interface TsuyuInstaller {
    suspend fun install(onStep: (TsuyuInstallStep) -> Unit): Result<Unit>
}

data class TsuyuProbe(
    val installed: Boolean = false,
    val binder: Boolean = false,
    val apiVersion: Int? = null,
    val authorized: Boolean = false,
    val permanentlyDenied: Boolean = false,
)

enum class TsuyuPermission { GRANTED, DENIED, NO_MORE_REQUESTS }

interface TsuyuLink {
    fun probe(): TsuyuProbe
    suspend fun awaitBinder(timeoutMs: Long): Boolean
    suspend fun requestPermission(): TsuyuPermission
    fun open(): Boolean
}
