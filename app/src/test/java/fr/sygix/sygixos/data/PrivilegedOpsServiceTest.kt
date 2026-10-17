/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivilegedOpsServiceTest {
    private val received = mutableListOf<String>()
    private val runner = CommandRunner { received += it; CommandOutcome(0, "done") }

    @Test fun commandOutsideTheListIsRefusedWithoutRunning() {
        val reply = PrivilegedReply.decode(PrivilegedReply.handle("reboot", runner))
        assertEquals(PrivilegedReply.Refused, reply?.code)
        val injected = PrivilegedReply.decode(PrivilegedReply.handle("cmd appops write-settings; reboot", runner))
        assertEquals(PrivilegedReply.Refused, injected?.code)
        assertTrue(received.isEmpty())
    }

    @Test fun allowedCommandRunsAndReturnsCodeThenOutput() {
        assertEquals("0\ndone", PrivilegedReply.handle("cmd appops write-settings", runner))
        assertEquals(listOf("cmd appops write-settings"), received)
    }

    @Test fun replyKeepsMultilineOutputAndRejectsGarbage() {
        assertEquals(CommandOutcome(2, "a\nb"), PrivilegedReply.decode(PrivilegedReply.encode(CommandOutcome(2, "a\nb"))))
        assertEquals(CommandOutcome(0, ""), PrivilegedReply.decode("0"))
        assertNull(PrivilegedReply.decode("oops\nx"))
        assertNull(PrivilegedReply.decode(null))
    }

    @Test fun processRunnerReturnsExitCodeAndCombinedOutput() {
        val outcome = ProcessCommandRunner().run("echo out; echo err >&2; exit 3")
        assertEquals(3, outcome.code)
        assertTrue(outcome.output.contains("out") && outcome.output.contains("err"))
    }

    @Test fun processRunnerKillsCommandsPastTheDeadline() {
        val started = System.nanoTime()
        val outcome = ProcessCommandRunner(timeoutMs = 200).run("exec sleep 30")
        assertEquals(PrivilegedReply.Timeout, outcome.code)
        assertTrue(System.nanoTime() - started < 10_000_000_000)
    }
}
