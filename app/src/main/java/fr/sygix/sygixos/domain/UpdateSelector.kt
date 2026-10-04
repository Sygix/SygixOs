/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object UpdateSelector {

    const val APK_NAME = "app-release.apk"
    const val MAX_APK_BYTES = 200L * 1024 * 1024
    const val PROFILE_NAME = "app-release.dm"
    const val MAX_PROFILE_BYTES = 16L * 1024 * 1024
    private const val UPLOADED = "uploaded"
    private val Sha256Digest = Regex("sha256:([0-9a-fA-F]{64})")

    fun eligible(release: Release): UpdateCandidate? {
        if (release.draft) return null
        val version = ReleaseVersion.parse(release.tag) ?: return null
        val asset = release.assets.firstOrNull { it.name == APK_NAME } ?: return null
        if (asset.state != UPLOADED || asset.size <= 0 || asset.size > MAX_APK_BYTES) return null
        val sha256 = sha256Of(asset) ?: return null
        if (!UpdateUrlPolicy.isAllowed(asset.downloadUrl)) return null
        return UpdateCandidate(
            tag = release.tag,
            versionName = version.name,
            versionCode = version.versionCode,
            prerelease = version.suffixed || release.prerelease,
            apkUrl = asset.downloadUrl,
            size = asset.size,
            sha256 = sha256,
            htmlUrl = release.htmlUrl,
            profile = profile(release),
        )
    }

    fun profile(release: Release): ProfileAsset? {
        val asset = release.assets.firstOrNull { it.name == PROFILE_NAME } ?: return null
        if (asset.state != UPLOADED || asset.size <= 0 || asset.size > MAX_PROFILE_BYTES) return null
        val sha256 = sha256Of(asset) ?: return null
        if (!UpdateUrlPolicy.isAllowed(asset.downloadUrl)) return null
        return ProfileAsset(url = asset.downloadUrl, size = asset.size, sha256 = sha256)
    }

    private fun sha256Of(asset: ReleaseAsset): String? =
        asset.digest?.let { Sha256Digest.matchEntire(it) }?.groupValues?.get(1)?.lowercase()

    fun known(releases: List<Release>): KnownUpdates {
        val candidates = releases.mapNotNull(::eligible)
        return KnownUpdates(
            bestFinal = candidates.filterNot { it.prerelease }.maxByOrNull { it.versionCode },
            bestAny = candidates.maxByOrNull { it.versionCode },
        )
    }

    fun proposed(known: KnownUpdates, includePrereleases: Boolean, installedVersionCode: Long): UpdateCandidate? {
        val best = if (includePrereleases) known.bestAny else known.bestFinal
        return best?.takeIf { it.versionCode > installedVersionCode }
    }
}
