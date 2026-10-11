/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.annotation.StringRes
import fr.sygix.sygixos.R
import fr.sygix.sygixos.model.GuideStep
import fr.sygix.sygixos.model.StepDetail
import fr.sygix.sygixos.model.StepStatus
import fr.sygix.sygixos.model.SummaryDetail
import fr.sygix.sygixos.model.SummaryStatus

@StringRes
internal fun GuideStep.titleRes(): Int = when (this) {
    GuideStep.ACCESSIBILITY -> R.string.launcher_step_accessibility
    GuideStep.BOOT -> R.string.launcher_step_boot
    GuideStep.OVERLAY -> R.string.launcher_step_overlay
    GuideStep.HOME_ROLE -> R.string.launcher_step_home_role
}

@StringRes
internal fun StepDetail.textRes(): Int = when (this) {
    StepDetail.ACCESSIBILITY -> R.string.launcher_step_accessibility_detail
    StepDetail.ACCESSIBILITY_BLOCKED -> R.string.launcher_step_accessibility_blocked
    StepDetail.ACCESSIBILITY_UNAVAILABLE -> R.string.launcher_step_accessibility_unavailable
    StepDetail.BOOT -> R.string.launcher_step_boot_detail
    StepDetail.BOOT_NOT_OBSERVED -> R.string.launcher_step_boot_not_observed
    StepDetail.BOOT_UNAVAILABLE -> R.string.launcher_step_boot_unavailable
    StepDetail.OVERLAY -> R.string.launcher_step_overlay_detail
    StepDetail.HOME_ROLE -> R.string.launcher_step_home_role_detail
}

@StringRes
internal fun StepStatus.labelRes(): Int = when (this) {
    StepStatus.TODO -> R.string.launcher_state_todo
    StepStatus.PENDING -> R.string.launcher_state_pending
    StepStatus.DONE -> R.string.launcher_state_done
    StepStatus.BLOCKED -> R.string.launcher_state_blocked
    StepStatus.UNAVAILABLE -> R.string.launcher_state_unavailable
    StepStatus.OPTIONAL -> R.string.launcher_state_optional
}

@StringRes
internal fun SummaryStatus.labelRes(): Int = when (this) {
    SummaryStatus.DONE -> R.string.launcher_summary_done
    SummaryStatus.PARTIAL -> R.string.launcher_summary_partial
    SummaryStatus.TODO -> R.string.launcher_state_todo
    SummaryStatus.BLOCKED -> R.string.launcher_state_blocked
    SummaryStatus.UNAVAILABLE -> R.string.launcher_state_unavailable
}

@StringRes
internal fun SummaryDetail.textRes(): Int = when (this) {
    SummaryDetail.ALL_DONE -> R.string.launcher_summary_all_done
    SummaryDetail.HOME_KEY -> R.string.launcher_summary_home_key
    SummaryDetail.BOOT -> R.string.launcher_summary_boot
    SummaryDetail.HOME_KEY_AND_BOOT -> R.string.launcher_summary_home_key_and_boot
    SummaryDetail.BLOCKED -> R.string.launcher_summary_blocked
    SummaryDetail.UNAVAILABLE -> R.string.launcher_summary_unavailable
}

internal fun GuideStep.tag(): String = when (this) {
    GuideStep.ACCESSIBILITY -> "onboarding-step-accessibility"
    GuideStep.BOOT -> "onboarding-step-boot"
    GuideStep.OVERLAY -> "onboarding-step-overlay"
    GuideStep.HOME_ROLE -> "onboarding-step-home-role"
}
