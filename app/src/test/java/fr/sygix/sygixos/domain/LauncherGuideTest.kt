/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.GuideProgress
import fr.sygix.sygixos.model.GuideStep
import fr.sygix.sygixos.model.GuideSummary
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.model.StepDetail
import fr.sygix.sygixos.model.StepStatus
import fr.sygix.sygixos.model.SummaryDetail
import fr.sygix.sygixos.model.SummaryStatus
import fr.sygix.sygixos.model.SystemControlState.ACTIVE
import fr.sygix.sygixos.model.SystemControlState.BLOCKED
import fr.sygix.sygixos.model.SystemControlState.INACTIVE
import fr.sygix.sygixos.model.SystemControlState.PENDING
import fr.sygix.sygixos.model.SystemControlState.UNAVAILABLE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LauncherGuideTest {
    private val fresh = LauncherSystemState(homeRole = INACTIVE, accessibility = INACTIVE, overlay = INACTIVE)

    private fun steps(state: LauncherSystemState) = LauncherGuide.steps(state)

    private fun shown(state: LauncherSystemState) = steps(state).map { it.step }

    private fun status(state: LauncherSystemState, step: GuideStep) = steps(state).single { it.step == step }.status

    @Test
    fun `nothing done shows steps 1 2 and 3 with two mandatory steps and the focus on step 1`() {
        assertEquals(listOf(GuideStep.ACCESSIBILITY, GuideStep.BOOT, GuideStep.HOME_ROLE), shown(fresh))
        assertEquals(StepStatus.TODO, status(fresh, GuideStep.ACCESSIBILITY))
        assertEquals(false, steps(fresh).single { it.step == GuideStep.BOOT }.checked)
        assertEquals(StepStatus.OPTIONAL, status(fresh, GuideStep.HOME_ROLE))
        assertEquals(GuideProgress(0, 2), LauncherGuide.progress(steps(fresh)))
        assertEquals(GuideStep.ACCESSIBILITY, LauncherGuide.initialFocus(steps(fresh)))
        assertEquals(GuideSummary(SummaryStatus.TODO, SummaryDetail.HOME_KEY_AND_BOOT), LauncherGuide.summary(steps(fresh)))
    }

    @Test
    fun `the sub step appears when the option is on and its permission screen exists`() {
        val booting = fresh.copy(accessibility = ACTIVE, bootEnabled = true)
        assertEquals(listOf(GuideStep.ACCESSIBILITY, GuideStep.BOOT, GuideStep.OVERLAY, GuideStep.HOME_ROLE), shown(booting))
        assertEquals(StepStatus.TODO, status(booting, GuideStep.BOOT))
        assertEquals(GuideProgress(1, 3), LauncherGuide.progress(steps(booting)))
        assertEquals(GuideStep.BOOT, LauncherGuide.initialFocus(steps(booting)))
        assertEquals(GuideSummary(SummaryStatus.PARTIAL, SummaryDetail.BOOT), LauncherGuide.summary(steps(booting)))
        val granted = booting.copy(overlay = ACTIVE)
        assertEquals(StepStatus.DONE, status(granted, GuideStep.OVERLAY))
        assertEquals(StepStatus.DONE, status(granted, GuideStep.BOOT))
        assertEquals(GuideProgress(3, 3), LauncherGuide.progress(steps(granted)))
        assertNull(LauncherGuide.initialFocus(steps(granted)))
        assertEquals(GuideSummary(SummaryStatus.DONE, SummaryDetail.ALL_DONE), LauncherGuide.summary(steps(granted)))
        assertEquals(StepStatus.PENDING, status(booting.copy(overlay = PENDING), GuideStep.OVERLAY))
        assertEquals(listOf(GuideStep.ACCESSIBILITY, GuideStep.BOOT, GuideStep.HOME_ROLE), shown(booting.copy(overlay = UNAVAILABLE)))
        assertEquals(StepStatus.DONE, status(booting.copy(overlay = UNAVAILABLE), GuideStep.BOOT))
    }

    @Test
    fun `a missing permission screen makes the disabled option unavailable without a switch`() {
        val blocked = fresh.copy(overlay = UNAVAILABLE)
        val boot = steps(blocked).single { it.step == GuideStep.BOOT }
        assertEquals(StepStatus.UNAVAILABLE, boot.status)
        assertEquals(StepDetail.BOOT_UNAVAILABLE, boot.detail)
        assertNull(boot.checked)
        assertEquals(GuideProgress(0, 1), LauncherGuide.progress(steps(blocked)))
        assertEquals(GuideSummary(SummaryStatus.TODO, SummaryDetail.HOME_KEY), LauncherGuide.summary(steps(blocked)))
    }

    @Test
    fun `the role step is hidden without role nor replacement screen and never counted`() {
        val noRole = fresh.copy(homeRole = UNAVAILABLE)
        assertEquals(listOf(GuideStep.ACCESSIBILITY, GuideStep.BOOT), shown(noRole))
        assertEquals(StepStatus.DONE, status(fresh.copy(homeRole = ACTIVE), GuideStep.HOME_ROLE))
        assertEquals(StepStatus.PENDING, status(fresh.copy(homeRole = PENDING), GuideStep.HOME_ROLE))
        assertEquals(GuideProgress(0, 2), LauncherGuide.progress(steps(fresh.copy(homeRole = ACTIVE))))
    }

    @Test
    fun `accessibility states map to their labels and details`() {
        assertEquals(StepStatus.PENDING, status(fresh.copy(accessibility = PENDING), GuideStep.ACCESSIBILITY))
        val blocked = steps(fresh.copy(accessibility = BLOCKED)).first()
        assertEquals(StepStatus.BLOCKED, blocked.status)
        assertEquals(StepDetail.ACCESSIBILITY_BLOCKED, blocked.detail)
        val unavailable = steps(fresh.copy(accessibility = UNAVAILABLE)).first()
        assertEquals(StepDetail.ACCESSIBILITY_UNAVAILABLE, unavailable.detail)
        assertEquals(GuideStep.ACCESSIBILITY, LauncherGuide.initialFocus(steps(fresh.copy(accessibility = UNAVAILABLE))))
        assertEquals(GuideProgress(0, 1), LauncherGuide.progress(steps(fresh.copy(accessibility = UNAVAILABLE))))
    }

    @Test
    fun `the settings summary covers blocked partial and unavailable cases`() {
        assertEquals(
            GuideSummary(SummaryStatus.BLOCKED, SummaryDetail.BLOCKED),
            LauncherGuide.summary(steps(fresh.copy(accessibility = BLOCKED, bootEnabled = true, overlay = ACTIVE))),
        )
        assertEquals(
            GuideSummary(SummaryStatus.PARTIAL, SummaryDetail.HOME_KEY),
            LauncherGuide.summary(steps(fresh.copy(bootEnabled = true, overlay = ACTIVE))),
        )
        assertEquals(
            GuideSummary(SummaryStatus.TODO, SummaryDetail.HOME_KEY_AND_BOOT),
            LauncherGuide.summary(steps(fresh.copy(bootEnabled = true))),
        )
        assertEquals(
            GuideSummary(SummaryStatus.UNAVAILABLE, SummaryDetail.UNAVAILABLE),
            LauncherGuide.summary(steps(fresh.copy(accessibility = UNAVAILABLE, overlay = UNAVAILABLE))),
        )
    }

    @Test
    fun `the boot detail reports a start that was not observed`() {
        val missed = fresh.copy(bootEnabled = true, bootNotObserved = true)
        assertEquals(StepDetail.BOOT_NOT_OBSERVED, steps(missed).single { it.step == GuideStep.BOOT }.detail)
        assertEquals(StepDetail.BOOT, steps(missed.copy(bootEnabled = false)).single { it.step == GuideStep.BOOT }.detail)
    }
}
