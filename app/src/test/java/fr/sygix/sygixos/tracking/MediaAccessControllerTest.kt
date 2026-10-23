/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MediaAccessControllerTest {
    @Test fun privilegedActivationRequiresEffectiveAccessAndDoesNotLoopAfterRevocation() = runTest {
        val gate = CompletableDeferred<Unit>()
        var allowed = false
        var activations = 0
        val gateway = object : MediaAccessGateway {
            override suspend fun granted() = allowed
            override fun privilegedAvailable() = true
            override suspend fun activate() { ++activations; gate.await(); allowed = true }
            override fun manualAvailable() = true
            override fun openManual(): Boolean { fail("Automatic path must not open settings"); return false }
        }
        val controller = MediaAccessController(gateway, backgroundScope)
        controller.refresh(1, connected = true, enabled = true); runCurrent()
        assertEquals(MediaAccessState.ACTIVATING, controller.state.value)
        gate.complete(Unit); runCurrent()
        assertEquals(MediaAccessState.GRANTED, controller.state.value)
        allowed = false
        controller.refresh(1, connected = true, enabled = true); runCurrent()
        assertEquals(MediaAccessState.MANUAL, controller.state.value)
        assertEquals(1, activations)
        controller.refresh(1, connected = true, enabled = false); runCurrent()
        assertEquals(MediaAccessState.DISABLED, controller.state.value)
    }

    @Test fun ambiguousEvidenceCannotMarkWatchedAndLateAccountsAreRejected() {
        val content = TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "5"), title = "Fixture")
        val gate = PlaybackEvidenceGate(setOf("verified-fixture"))
        gate.start(content.identity, 2, 9)
        val proof = PlaybackEvidence(content, 2, 9, "verified-fixture", exactIdentity = true, completed = true)
        assertFalse(gate.accept(proof.copy(completed = false)))
        assertFalse(gate.accept(proof.copy(exactIdentity = false)))
        assertFalse(gate.accept(proof.copy(generation = 1)))
        assertFalse(gate.accept(proof.copy(adapter = "unknown")))
        assertTrue(gate.accept(proof))
        assertFalse(gate.accept(proof))
        gate.stop()
        assertFalse(gate.accept(proof))
    }
}
