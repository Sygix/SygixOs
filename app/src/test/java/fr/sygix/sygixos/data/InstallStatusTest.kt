/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.pm.PackageInstaller
import fr.sygix.sygixos.domain.InstallFailure
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallStatusTest {

    private fun of(status: Int) = InstallStatus.of(status, sessionId = 3, intent = null)

    @Test
    fun `success, user action and refusal are told apart`() {
        assertEquals(InstallStatus.Success, of(PackageInstaller.STATUS_SUCCESS))
        assertEquals(InstallStatus.PendingUserAction(3, null), of(PackageInstaller.STATUS_PENDING_USER_ACTION))
        assertEquals(InstallStatus.Aborted, of(PackageInstaller.STATUS_FAILURE_ABORTED))
    }

    @Test
    fun `each failure keeps its family`() {
        mapOf(
            PackageInstaller.STATUS_FAILURE_BLOCKED to InstallFailure.BLOCKED,
            PackageInstaller.STATUS_FAILURE_CONFLICT to InstallFailure.CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE to InstallFailure.INCOMPATIBLE,
            PackageInstaller.STATUS_FAILURE_INVALID to InstallFailure.INVALID,
            PackageInstaller.STATUS_FAILURE_STORAGE to InstallFailure.STORAGE,
            PackageInstaller.STATUS_FAILURE_TIMEOUT to InstallFailure.TIMEOUT,
            PackageInstaller.STATUS_FAILURE to InstallFailure.OTHER,
            12345 to InstallFailure.OTHER,
        ).forEach { (status, family) -> assertEquals(InstallStatus.Failed(family), of(status)) }
    }
}
