/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

enum class SystemControlState { ACTIVE, INACTIVE, UNAVAILABLE, PENDING, BLOCKED }

enum class GuideStep { ACCESSIBILITY, BOOT, OVERLAY, HOME_ROLE }

enum class GuideOrigin { AUTOMATIC, SETTINGS }

enum class StepStatus { TODO, PENDING, DONE, BLOCKED, UNAVAILABLE, OPTIONAL }

enum class StepDetail {
    ACCESSIBILITY,
    ACCESSIBILITY_BLOCKED,
    ACCESSIBILITY_UNAVAILABLE,
    BOOT,
    BOOT_NOT_OBSERVED,
    BOOT_UNAVAILABLE,
    OVERLAY,
    HOME_ROLE,
}

data class GuideStepState(
    val step: GuideStep,
    val status: StepStatus,
    val detail: StepDetail,
    val checked: Boolean? = null,
) {
    val done: Boolean get() = status == StepStatus.DONE
    val mandatory: Boolean get() = step != GuideStep.HOME_ROLE
    val dimmed: Boolean get() = status == StepStatus.UNAVAILABLE
}

data class GuideProgress(val done: Int, val total: Int)

enum class SummaryStatus { DONE, PARTIAL, TODO, BLOCKED, UNAVAILABLE }

enum class SummaryDetail { ALL_DONE, HOME_KEY, BOOT, HOME_KEY_AND_BOOT, BLOCKED, UNAVAILABLE }

data class GuideSummary(val status: SummaryStatus, val detail: SummaryDetail)

data class LauncherSystemState(
    val homeRole: SystemControlState = SystemControlState.UNAVAILABLE,
    val accessibility: SystemControlState = SystemControlState.UNAVAILABLE,
    val overlay: SystemControlState = SystemControlState.UNAVAILABLE,
    val bootEnabled: Boolean = false,
    val bootNotObserved: Boolean = false,
    val guide: GuideOrigin? = null,
    val homeRequest: Long = 0,
    val focusReturn: GuideStep? = null,
    val settingsFocus: Long = 0,
    val preferencesLoaded: Boolean = false,
) {
    val systemPending: Boolean
        get() = homeRole == SystemControlState.PENDING || accessibility == SystemControlState.PENDING ||
            overlay == SystemControlState.PENDING

    val guideShown: Boolean get() = guide != null

    val bootBlocked: Boolean get() = !bootEnabled && overlay == SystemControlState.UNAVAILABLE
}
