/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.GuideProgress
import fr.sygix.sygixos.model.GuideStep
import fr.sygix.sygixos.model.GuideStepState
import fr.sygix.sygixos.model.GuideSummary
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.model.StepDetail
import fr.sygix.sygixos.model.StepStatus
import fr.sygix.sygixos.model.SummaryDetail
import fr.sygix.sygixos.model.SummaryStatus
import fr.sygix.sygixos.model.SystemControlState

object LauncherGuide {
    fun steps(state: LauncherSystemState): List<GuideStepState> = buildList {
        add(accessibility(state.accessibility))
        val overlay = overlay(state)
        add(boot(state, overlay))
        overlay?.let(::add)
        homeRole(state.homeRole)?.let(::add)
    }

    fun progress(steps: List<GuideStepState>): GuideProgress {
        val counted = steps.filter { it.mandatory && it.status != StepStatus.UNAVAILABLE }
        return GuideProgress(counted.count { it.done }, counted.size)
    }

    fun progressQuantity(done: Int): Int = done.coerceAtLeast(1)

    fun initialFocus(steps: List<GuideStepState>): GuideStep? = steps.firstOrNull { it.mandatory && !it.done }?.step

    fun summary(steps: List<GuideStepState>): GuideSummary {
        val counted = steps.filter { it.mandatory && it.status != StepStatus.UNAVAILABLE }
        if (counted.isEmpty()) return GuideSummary(SummaryStatus.UNAVAILABLE, SummaryDetail.UNAVAILABLE)
        if (counted.any { it.status == StepStatus.BLOCKED }) return GuideSummary(SummaryStatus.BLOCKED, SummaryDetail.BLOCKED)
        if (counted.all { it.done }) return GuideSummary(SummaryStatus.DONE, SummaryDetail.ALL_DONE)
        val homeKey = counted.any { it.step == GuideStep.ACCESSIBILITY && !it.done }
        val boot = counted.any { (it.step == GuideStep.BOOT || it.step == GuideStep.OVERLAY) && !it.done }
        val detail = when {
            homeKey && boot -> SummaryDetail.HOME_KEY_AND_BOOT
            homeKey -> SummaryDetail.HOME_KEY
            else -> SummaryDetail.BOOT
        }
        return GuideSummary(if (counted.any { it.done }) SummaryStatus.PARTIAL else SummaryStatus.TODO, detail)
    }

    private fun accessibility(state: SystemControlState) = when (state) {
        SystemControlState.ACTIVE -> GuideStepState(GuideStep.ACCESSIBILITY, StepStatus.DONE, StepDetail.ACCESSIBILITY)
        SystemControlState.INACTIVE -> GuideStepState(GuideStep.ACCESSIBILITY, StepStatus.TODO, StepDetail.ACCESSIBILITY)
        SystemControlState.PENDING -> GuideStepState(GuideStep.ACCESSIBILITY, StepStatus.PENDING, StepDetail.ACCESSIBILITY)
        SystemControlState.BLOCKED -> GuideStepState(GuideStep.ACCESSIBILITY, StepStatus.BLOCKED, StepDetail.ACCESSIBILITY_BLOCKED)
        SystemControlState.UNAVAILABLE -> GuideStepState(GuideStep.ACCESSIBILITY, StepStatus.UNAVAILABLE, StepDetail.ACCESSIBILITY_UNAVAILABLE)
    }

    private fun overlay(state: LauncherSystemState): GuideStepState? {
        if (!state.bootEnabled) return null
        val status = when (state.overlay) {
            SystemControlState.ACTIVE -> StepStatus.DONE
            SystemControlState.PENDING -> StepStatus.PENDING
            SystemControlState.INACTIVE, SystemControlState.BLOCKED -> StepStatus.TODO
            SystemControlState.UNAVAILABLE -> return null
        }
        return GuideStepState(GuideStep.OVERLAY, status, StepDetail.OVERLAY)
    }

    private fun boot(state: LauncherSystemState, overlay: GuideStepState?): GuideStepState {
        if (state.bootBlocked) return GuideStepState(GuideStep.BOOT, StepStatus.UNAVAILABLE, StepDetail.BOOT_UNAVAILABLE)
        val done = state.bootEnabled && (overlay == null || overlay.done)
        val detail = if (state.bootEnabled && state.bootNotObserved) StepDetail.BOOT_NOT_OBSERVED else StepDetail.BOOT
        return GuideStepState(GuideStep.BOOT, if (done) StepStatus.DONE else StepStatus.TODO, detail, checked = state.bootEnabled)
    }

    private fun homeRole(state: SystemControlState): GuideStepState? = when (state) {
        SystemControlState.ACTIVE -> GuideStepState(GuideStep.HOME_ROLE, StepStatus.DONE, StepDetail.HOME_ROLE)
        SystemControlState.PENDING -> GuideStepState(GuideStep.HOME_ROLE, StepStatus.PENDING, StepDetail.HOME_ROLE)
        SystemControlState.INACTIVE, SystemControlState.BLOCKED -> GuideStepState(GuideStep.HOME_ROLE, StepStatus.OPTIONAL, StepDetail.HOME_ROLE)
        SystemControlState.UNAVAILABLE -> null
    }
}
