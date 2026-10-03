/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateStatusTextTest {

    private val v2 = candidate("v0.0.2")
    private val rc = candidate("v0.0.3-rc.1")

    @Test
    fun `never checked when nothing is persisted`() {
        assertEquals(CheckLine.NeverChecked, UpdateStatusText.checkLine(UpdateStatus()))
        assertNull(UpdateStatusText.installLine(UpdateStatus()))
    }

    @Test
    fun `checking wins over everything`() {
        assertEquals(CheckLine.Checking, UpdateStatusText.checkLine(UpdateStatus(checking = true, proposed = v2, lastResult = CheckResult.Ok)))
    }

    @Test
    fun `proposed version wins over the last result even after an error`() {
        val status = UpdateStatus(proposed = rc, lastResult = CheckResult.Error(UpdateError.Timeout))
        assertEquals(CheckLine.Available("0.0.3-rc.1", prerelease = true), UpdateStatusText.checkLine(status))
        assertEquals(InstallLine(rc, null), UpdateStatusText.installLine(status))
    }

    @Test
    fun `last result is shown without a proposed version`() {
        assertEquals(CheckLine.UpToDate, UpdateStatusText.checkLine(UpdateStatus(lastResult = CheckResult.Ok)))
        assertEquals(
            CheckLine.Failed(UpdateError.Unavailable(404)),
            UpdateStatusText.checkLine(UpdateStatus(lastResult = CheckResult.Error(UpdateError.Unavailable(404)))),
        )
    }

    @Test
    fun `after a restart the persisted automatic failure is shown and not never checked`() {
        val restarted = UpdateStatus(lastResult = CheckResult.Error(UpdateError.RateLimited(42L)))
        assertEquals(CheckLine.Failed(UpdateError.RateLimited(42L)), UpdateStatusText.checkLine(restarted))
    }

    @Test
    fun `install line shows the running step`() {
        assertEquals(
            InstallLine(v2, InstallDetail.Downloading(42)),
            UpdateStatusText.installLine(UpdateStatus(proposed = v2, step = UpdateStep.Downloading(v2, 42))),
        )
        assertEquals(InstallDetail.Verifying, UpdateStatusText.installLine(UpdateStatus(proposed = v2, step = UpdateStep.Verifying(v2)))?.detail)
        assertEquals(InstallDetail.Installing, UpdateStatusText.installLine(UpdateStatus(proposed = v2, step = UpdateStep.Installing(v2)))?.detail)
    }

    @Test
    fun `failure of the proposed version is shown on its line`() {
        val status = UpdateStatus(proposed = v2, step = UpdateStep.Failed(v2, UpdateError.Corrupt))
        assertEquals(InstallLine(v2, InstallDetail.Failed(UpdateError.Corrupt)), UpdateStatusText.installLine(status))
    }

    @Test
    fun `failure of another version is not shown on the new proposed line`() {
        val status = UpdateStatus(proposed = rc, step = UpdateStep.Failed(v2, UpdateError.Corrupt))
        assertEquals(InstallLine(rc, null), UpdateStatusText.installLine(status))
    }

    @Test
    fun `withdrawn version keeps its line with the reason until nothing else is proposed`() {
        val status = UpdateStatus(lastResult = CheckResult.Ok, step = UpdateStep.Failed(v2, UpdateError.Withdrawn))
        assertEquals(InstallLine(v2, InstallDetail.Failed(UpdateError.Withdrawn)), UpdateStatusText.installLine(status))
        assertEquals(CheckLine.UpToDate, UpdateStatusText.checkLine(status))
    }
}
