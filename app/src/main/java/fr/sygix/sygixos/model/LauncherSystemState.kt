/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

enum class SystemControlState { ACTIVE, INACTIVE, UNAVAILABLE, PENDING }

enum class SystemControl { HOME_ROLE, BOOT_START, ACCESSIBILITY }

enum class SystemRowDetail { HOME_ROLE, HOME_ROLE_UNAVAILABLE, BOOT_START, BOOT_NOT_OBSERVED, BOOT_UNAVAILABLE, ACCESSIBILITY, ACCESSIBILITY_UNAVAILABLE }

data class SystemRow(
    val control: SystemControl,
    val detail: SystemRowDetail,
    val state: SystemControlState? = null,
    val checked: Boolean? = null,
) {
    val dimmed: Boolean get() = state == SystemControlState.UNAVAILABLE
}

data class LauncherSystemState(
    val homeRole: SystemControlState = SystemControlState.UNAVAILABLE,
    val accessibility: SystemControlState = SystemControlState.UNAVAILABLE,
    val overlay: SystemControlState = SystemControlState.UNAVAILABLE,
    val bootEnabled: Boolean = false,
    val bootNotObserved: Boolean = false,
    val showOnboarding: Boolean = false,
    val showOverlayConfirmation: Boolean = false,
    val homeRequest: Long = 0,
    val focusReturn: SystemControl? = null,
    val preferencesLoaded: Boolean = false,
) {
    val systemPending: Boolean
        get() = homeRole == SystemControlState.PENDING || accessibility == SystemControlState.PENDING ||
            overlay == SystemControlState.PENDING

    val overlayShown: Boolean get() = showOnboarding || showOverlayConfirmation

    val bootBlocked: Boolean get() = !bootEnabled && overlay == SystemControlState.UNAVAILABLE

    val rows: List<SystemRow>
        get() = listOf(
            SystemRow(
                SystemControl.HOME_ROLE,
                if (homeRole == SystemControlState.UNAVAILABLE) SystemRowDetail.HOME_ROLE_UNAVAILABLE else SystemRowDetail.HOME_ROLE,
                state = homeRole,
            ),
            if (bootBlocked) {
                SystemRow(SystemControl.BOOT_START, SystemRowDetail.BOOT_UNAVAILABLE, state = SystemControlState.UNAVAILABLE)
            } else {
                SystemRow(
                    SystemControl.BOOT_START,
                    if (bootEnabled && bootNotObserved) SystemRowDetail.BOOT_NOT_OBSERVED else SystemRowDetail.BOOT_START,
                    checked = bootEnabled,
                )
            },
            SystemRow(
                SystemControl.ACCESSIBILITY,
                if (accessibility == SystemControlState.UNAVAILABLE) SystemRowDetail.ACCESSIBILITY_UNAVAILABLE else SystemRowDetail.ACCESSIBILITY,
                state = accessibility,
            ),
        )
}
