/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

internal val Sha = "a".repeat(64)

internal fun asset(
    name: String = UpdateSelector.APK_NAME,
    state: String = "uploaded",
    size: Long = 1_000,
    digest: String? = "sha256:$Sha",
    url: String = "https://github.com/Sygix/SygixOs/releases/download/v/app-release.apk",
) = ReleaseAsset(name = name, state = state, size = size, digest = digest, downloadUrl = url)

internal fun release(
    tag: String,
    prerelease: Boolean = tag.contains('-'),
    draft: Boolean = false,
    htmlUrl: String = "https://github.com/Sygix/SygixOs/releases/tag/$tag",
    assets: List<ReleaseAsset> = listOf(asset(url = "https://github.com/Sygix/SygixOs/releases/download/$tag/app-release.apk")),
) = Release(tag = tag, draft = draft, prerelease = prerelease, htmlUrl = htmlUrl, assets = assets)

internal fun candidate(tag: String, size: Long = 1_000, sha256: String = Sha): UpdateCandidate =
    requireNotNull(UpdateSelector.eligible(release(tag, assets = listOf(asset(size = size, digest = "sha256:$sha256", url = "https://github.com/Sygix/SygixOs/releases/download/$tag/app-release.apk")))))
