/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.model.AdbAccessState
import java.util.concurrent.CountDownLatch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TsuyuAccessTest {
    private val api = FakeShizukuApi()
    private fun TestScope.access() = TsuyuAccess(api, io = StandardTestDispatcher(testScheduler))

    @Test fun activeTsuyuConnectsAndRunsAllowedCommands() = runTest {
        val access = access()
        assertTrue(access.debugEnabled() && access.previouslyAuthorized())
        assertTrue(access.connect().isSuccess)
        assertEquals(AdbAccessState.READY, access.state.value)
        api.reply = { "0\nAUTO_START: allow\n" }
        assertEquals("AUTO_START: allow", access.execute("appops get --user 0 fr.sygix.sygixos AUTO_START").getOrThrow())
        assertEquals(listOf("appops get --user 0 fr.sygix.sygixos AUTO_START"), api.sent)
    }

    @Test fun stoppedTsuyuIsDisabledWithoutBinding() = runTest {
        api.binder = false
        val access = access()
        assertFalse(access.debugEnabled())
        assertTrue(access.connect().isFailure)
        assertEquals(AdbAccessState.DISABLED, access.state.value)
        assertEquals(0, api.binds)
    }

    @Test fun outdatedTsuyuIsNeverUsed() = runTest {
        api.apiVersion = 12
        val access = access()
        assertTrue(access.connect().isFailure)
        assertEquals(AdbAccessState.UNREACHABLE, access.state.value)
        assertEquals(0, api.binds)
        assertTrue(access.execute("cmd appops write-settings").isFailure)
        assertTrue(api.sent.isEmpty())
    }

    @Test fun missingPermissionIsRefused() = runTest {
        api.permission = false
        val access = access()
        assertFalse(access.previouslyAuthorized())
        assertTrue(access.connect().isFailure)
        assertEquals(AdbAccessState.REFUSED, access.state.value)
        assertEquals(0, api.binds)
    }

    @Test fun serviceNotConnectedInTimeIsUnreachableAndUnbound() = runTest {
        api.connectOnBind = false
        val access = access()
        assertTrue(access.connect().isFailure)
        assertEquals(AdbAccessState.UNREACHABLE, access.state.value)
        assertEquals(1, api.unbinds)
    }

    @Test fun commandOutsideTheListIsNotSent() = runTest {
        val access = access()
        access.connect()
        assertTrue(access.execute("reboot").isFailure)
        assertTrue(api.sent.isEmpty())
        assertEquals(AdbAccessState.READY, access.state.value)
    }

    @Test fun nonZeroExitIsAnOperationFailure() = runTest {
        val access = access()
        access.connect()
        api.reply = { "1\nUnknown operation" }
        val failure = access.execute("appops get --user 0 fr.sygix.sygixos AUTO_START").exceptionOrNull()
        assertTrue(failure is ShellOperationFailure && failure.output == "Unknown operation")
        assertEquals(AdbAccessState.READY, access.state.value)
    }

    @Test fun deadBinderMakesAccessLost() = runTest {
        val access = access()
        access.connect()
        api.die()
        assertEquals(AdbAccessState.LOST, access.state.value)
        assertEquals(1, api.unbinds)
        assertTrue(access.execute("cmd appops write-settings").isFailure)
        assertTrue(api.sent.isEmpty())
    }

    @Test fun lostServiceMakesAccessLost() = runTest {
        val access = access()
        access.connect()
        api.lost?.invoke()
        assertEquals(AdbAccessState.LOST, access.state.value)
        assertTrue(access.execute("cmd appops write-settings").isFailure)
    }

    @Test fun remoteFailureMakesAccessLost() = runTest {
        val access = access()
        access.connect()
        api.reply = { throw IllegalStateException("DeadObject") }
        assertTrue(access.execute("cmd appops write-settings").isFailure)
        assertEquals(AdbAccessState.LOST, access.state.value)
    }

    @Test fun commandTimeoutReportedByTheServiceMakesAccessLost() = runTest {
        val access = access()
        access.connect()
        api.reply = { "${PrivilegedReply.Timeout}\n" }
        assertTrue(access.execute("cmd appops write-settings").isFailure)
        assertEquals(AdbAccessState.LOST, access.state.value)
    }

    @Test fun unansweredCommandMakesAccessLost() = runTest {
        val access = TsuyuAccess(api, commandMs = 12_000)
        val release = CountDownLatch(1)
        access.connect()
        api.reply = { release.await(); "0\n" }
        try {
            assertTrue(access.execute("cmd appops write-settings").isFailure)
            assertEquals(AdbAccessState.LOST, access.state.value)
        } finally {
            release.countDown()
        }
    }

    @Test fun revokedPermissionMakesAccessLost() = runTest {
        val access = access()
        access.connect()
        api.permission = false
        assertTrue(access.execute("cmd appops write-settings").isFailure)
        assertEquals(AdbAccessState.LOST, access.state.value)
        assertTrue(api.sent.isEmpty())
    }

    @Test fun closeUnbindsTheService() = runTest {
        val access = access()
        access.connect()
        access.close()
        assertEquals(1, api.unbinds)
        assertEquals(AdbAccessState.LOST, access.state.value)
        assertTrue(api.dead.isEmpty())
        assertTrue(access.execute("cmd appops write-settings").isFailure)
    }
}
