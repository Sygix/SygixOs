/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

enum class SystemControlState { ACTIVE, INACTIVE, UNAVAILABLE, PENDING, BLOCKED }

enum class GuideStep { ACCESSIBILITY, BOOT, OVERLAY }

enum class GuideOrigin { AUTOMATIC, SETTINGS }

data class LauncherSystemState(
    val accessibility: SystemControlState = SystemControlState.UNAVAILABLE,
    val overlay: SystemControlState = SystemControlState.UNAVAILABLE,
    val bootEnabled: Boolean = false,
    val bootNotObserved: Boolean = false,
    val guide: GuideOrigin? = null,
    val homeRequest: Long = 0,
    val focusReturn: GuideStep? = null,
    val settingsFocus: Long = 0,
    val preferencesLoaded: Boolean = false,
    val defaultLauncher: DefaultLauncherState = DefaultLauncherState(),
) {
    val systemPending: Boolean
        get() = accessibility == SystemControlState.PENDING || overlay == SystemControlState.PENDING

    val guideShown: Boolean get() = guide != null

    val bootBlocked: Boolean get() = !bootEnabled && overlay == SystemControlState.UNAVAILABLE
}
