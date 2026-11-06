/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

object PlaybackDirectApp {
    fun resolve(installed: List<PlaybackApp>, preferred: String?, targets: List<PlaybackTarget>, publishers: Set<String>): PlaybackApp? {
        fun installedApp(packageName: String?): PlaybackApp? = installed.firstOrNull { it.packageName == packageName }
        return installedApp(preferred)
            ?: targets.firstNotNullOfOrNull { installedApp(it.packageName) }
            ?: publishers.singleOrNull()?.let(::installedApp)
    }
}
