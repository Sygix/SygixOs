/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.model.AdbAccessState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdbAccessGatewayTest {
    private class Transport : LocalAdbTransport {
        var reachable = true
        var probes = 0
        var closed = 0
        var identity = "uid=2000(shell) gid=2000(shell)"
        var authorization: CompletableDeferred<Unit>? = null
        var failure: Exception? = null
        var commandWait: CompletableDeferred<Unit>? = null
        override suspend fun reachable(): Boolean { probes++; return reachable }
        override suspend fun authenticate(): String { authorization?.await(); failure?.let { throw it }; return identity }
        override suspend fun command(command: String): String { commandWait?.await(); failure?.let { throw it }; return "ok" }
        override fun close() { closed++ }
    }

    @Test fun disabledDoesNotProbeOrAuthenticate() = runTest {
        val transport = Transport()
        val gateway = AdbAccessGateway({ false }, transport)
        assertTrue(gateway.connect().isFailure)
        assertEquals(AdbAccessState.DISABLED, gateway.state.value)
        assertEquals(0, transport.probes)
    }

    @Test fun closedPortIsUnavailable() = runTest {
        val gateway = AdbAccessGateway({ true }, Transport().apply { reachable = false })
        assertTrue(gateway.connect().isFailure)
        assertEquals(AdbAccessState.UNREACHABLE, gateway.state.value)
    }

    @Test fun authorizationWaitThenShellIdentityIsReady() = runTest {
        val authorization = CompletableDeferred<Unit>()
        val gateway = AdbAccessGateway({ true }, Transport().apply { this.authorization = authorization })
        val connect = async { gateway.connect() }
        runCurrent()
        assertEquals(AdbAccessState.WAITING, gateway.state.value)
        authorization.complete(Unit)
        assertTrue(connect.await().isSuccess)
        assertEquals(AdbAccessState.READY, gateway.state.value)
        gateway.close()
        assertNotEquals(AdbAccessState.READY, gateway.state.value)
    }

    @Test fun refusedAuthorizationIsNotReady() = runTest {
        val gateway = AdbAccessGateway({ true }, Transport().apply { failure = IllegalStateException() })
        assertTrue(gateway.connect().isFailure)
        assertEquals(AdbAccessState.REFUSED, gateway.state.value)
    }

    @Test fun expiredAuthorizationClosesAndFails() = runTest {
        val transport = Transport().apply { authorization = CompletableDeferred() }
        val gateway = AdbAccessGateway({ true }, transport, authorizationMs = 60)
        val connect = async { gateway.connect() }
        advanceUntilIdle()
        assertTrue(connect.await().isFailure)
        assertEquals(AdbAccessState.REFUSED, gateway.state.value)
        assertTrue(transport.closed >= 2)
    }

    @Test fun rootIdentityIsRejected() = runTest {
        val gateway = AdbAccessGateway({ true }, Transport().apply { identity = "uid=0(root)" })
        assertTrue(gateway.connect().isFailure)
        assertEquals(AdbAccessState.REFUSED, gateway.state.value)
    }

    @Test fun debuggingLostBeforeCommandFailsAndCloses() = runTest {
        var enabled = true
        val transport = Transport()
        val gateway = AdbAccessGateway({ enabled }, transport)
        gateway.connect().getOrThrow()
        enabled = false
        assertTrue(gateway.execute("id").isFailure)
        assertEquals(AdbAccessState.LOST, gateway.state.value)
    }

    @Test fun commandDeadlineDoesNotSucceed() = runTest {
        val transport = Transport()
        val gateway = AdbAccessGateway({ true }, transport, commandMs = 10)
        gateway.connect().getOrThrow()
        transport.commandWait = CompletableDeferred()
        assertTrue(gateway.execute("id").isFailure)
        assertEquals(AdbAccessState.LOST, gateway.state.value)
    }

    @Test fun operationRejectionKeepsTransportForIndependentSteps() = runTest {
        val transport = Transport()
        val gateway = AdbAccessGateway({ true }, transport)
        gateway.connect().getOrThrow()
        transport.failure = ShellOperationFailure("Unknown operation")
        assertTrue(gateway.execute("appops").isFailure)
        assertEquals(AdbAccessState.READY, gateway.state.value)
    }
}
