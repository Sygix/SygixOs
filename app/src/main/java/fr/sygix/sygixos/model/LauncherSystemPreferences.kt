/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

data class LauncherSystemPreferences(
    val onboardingDismissed: Boolean = false,
    val bootStart: BootStartState = BootStartState(),
)

data class BootStartState(
    val enabled: Boolean = false,
    val enabledAtBoot: Int? = null,
    val observedAtBoot: Int? = null,
) {
    fun notObserved(currentBoot: Int?): Boolean =
        enabled && currentBoot != null && enabledAtBoot != null &&
            currentBoot > enabledAtBoot && observedAtBoot != currentBoot
}
