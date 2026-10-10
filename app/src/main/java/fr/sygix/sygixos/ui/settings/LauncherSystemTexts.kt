/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.annotation.StringRes
import fr.sygix.sygixos.R
import fr.sygix.sygixos.model.SystemControl
import fr.sygix.sygixos.model.SystemControlState
import fr.sygix.sygixos.model.SystemRowDetail

@StringRes
internal fun SystemControl.titleRes(): Int = when (this) {
    SystemControl.HOME_ROLE -> R.string.launcher_home_role
    SystemControl.BOOT_START -> R.string.launcher_boot
    SystemControl.ACCESSIBILITY -> R.string.launcher_accessibility
}

@StringRes
internal fun SystemRowDetail.textRes(): Int = when (this) {
    SystemRowDetail.HOME_ROLE -> R.string.launcher_home_detail
    SystemRowDetail.HOME_ROLE_UNAVAILABLE -> R.string.launcher_home_unavailable
    SystemRowDetail.BOOT_START -> R.string.launcher_boot_detail
    SystemRowDetail.BOOT_NOT_OBSERVED -> R.string.launcher_boot_not_observed
    SystemRowDetail.BOOT_UNAVAILABLE -> R.string.launcher_boot_unavailable
    SystemRowDetail.ACCESSIBILITY -> R.string.launcher_accessibility_detail
    SystemRowDetail.ACCESSIBILITY_UNAVAILABLE -> R.string.launcher_accessibility_unavailable
}

@StringRes
internal fun SystemControlState.labelRes(): Int = when (this) {
    SystemControlState.ACTIVE -> R.string.launcher_state_active
    SystemControlState.INACTIVE -> R.string.launcher_state_inactive
    SystemControlState.UNAVAILABLE -> R.string.launcher_state_unavailable
    SystemControlState.PENDING -> R.string.launcher_state_pending
}
