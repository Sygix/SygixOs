/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupGateTest {

    private val timings = StartupTimings(minMs = 600, visualCapMs = 2_000, capMs = 5_000)
    private var time = 10_000L

    private fun gate(animations: Boolean = true) = StartupGate(timings, animations) { time }

    private fun StartupGate.at(elapsed: Long): StartupPhase {
        time = 10_000L + elapsed
        return phase()
    }

    private fun StartupGate.shownNow(): StartupGate = apply {
        time = 10_000L
        splashShown()
        mascotShown()
    }

    @Test
    fun `stays on the splash until it is shown`() {
        val gate = gate()
        gate.catalogReady()
        gate.heroVisualReady()
        time += 60_000
        assertEquals(StartupPhase.Splash, gate.phase())
        assertNull(gate.millisUntilChange())
    }

    @Test
    fun `home ready at 100 ms keeps the splash until 600 ms then fades out`() {
        val gate = gate().shownNow()
        gate.at(100)
        gate.catalogReady()
        gate.heroVisualReady()
        assertEquals(StartupPhase.Splash, gate.at(100))
        assertEquals(500L, gate.millisUntilChange())
        assertEquals(StartupPhase.Splash, gate.at(599))
        assertEquals(StartupPhase.FadingOut, gate.at(600))
        assertNull(gate.millisUntilChange())
        assertEquals(StartupPhase.FadingOut, gate.at(60_000))
        gate.fadeFinished()
        assertEquals(StartupPhase.Done, gate.at(60_000))
    }

    @Test
    fun `fade reported finished before it starts is ignored`() {
        val gate = gate().shownNow()
        gate.at(100)
        gate.catalogReady()
        gate.heroVisualReady()
        gate.fadeFinished()
        assertEquals(StartupPhase.Splash, gate.at(599))
        assertEquals(StartupPhase.FadingOut, gate.at(600))
    }

    @Test
    fun `waits for the first hero visual`() {
        val gate = gate().shownNow()
        gate.at(200)
        gate.catalogReady()
        assertEquals(1_800L, gate.millisUntilChange())
        assertEquals(StartupPhase.Splash, gate.at(1_499))
        gate.at(1_500)
        gate.heroVisualReady()
        assertEquals(StartupPhase.FadingOut, gate.at(1_500))
    }

    @Test
    fun `stops waiting for the hero visual at 2 s`() {
        val gate = gate().shownNow()
        gate.at(200)
        gate.catalogReady()
        assertEquals(StartupPhase.Splash, gate.at(1_999))
        assertEquals(StartupPhase.FadingOut, gate.at(2_000))
    }

    @Test
    fun `catalog loaded after the visual cap fades out at once`() {
        val gate = gate().shownNow()
        gate.at(3_000)
        gate.catalogReady()
        assertEquals(StartupPhase.FadingOut, gate.at(3_000))
    }

    @Test
    fun `never loaded catalog fades out at 5 s`() {
        val gate = gate().shownNow()
        gate.at(100)
        gate.heroVisualReady()
        assertEquals(StartupPhase.Splash, gate.at(4_999))
        assertEquals(StartupPhase.FadingOut, gate.at(5_000))
    }

    @Test
    fun `readiness reported before the splash counts from its first frame`() {
        val gate = gate()
        time = 9_000L
        gate.catalogReady()
        gate.heroVisualReady()
        gate.shownNow()
        assertEquals(StartupPhase.Splash, gate.at(599))
        assertEquals(StartupPhase.FadingOut, gate.at(600))
    }

    @Test
    fun `disabled animations go from splash to done without fading out`() {
        val gate = gate(animations = false).shownNow()
        gate.at(100)
        gate.catalogReady()
        gate.heroVisualReady()
        assertEquals(StartupPhase.Splash, gate.at(599))
        assertEquals(StartupPhase.Done, gate.at(600))
        assertNull(gate.millisUntilChange())
    }

    @Test
    fun `first frame and readiness are recorded once`() {
        val gate = gate().shownNow()
        gate.at(100)
        gate.catalogReady()
        gate.heroVisualReady()
        gate.at(400)
        gate.splashShown()
        assertEquals(StartupPhase.FadingOut, gate.at(600))
    }

    @Test
    fun `minimum duration counts from the first frame showing the mascot`() {
        val gate = gate()
        time = 10_000L
        gate.splashShown()
        gate.catalogReady()
        gate.heroVisualReady()
        assertEquals(StartupPhase.Splash, gate.at(900))
        gate.at(450)
        gate.mascotShown()
        assertEquals(600L, gate.millisUntilChange())
        assertEquals(StartupPhase.Splash, gate.at(1_049))
        assertEquals(StartupPhase.FadingOut, gate.at(1_050))
    }

    @Test
    fun `mascot not shown yet waits until the global cap`() {
        val gate = gate()
        time = 10_000L
        gate.splashShown()
        gate.catalogReady()
        gate.heroVisualReady()
        assertEquals(5_000L, gate.millisUntilChange())
        assertEquals(StartupPhase.Splash, gate.at(4_999))
        assertEquals(StartupPhase.FadingOut, gate.at(5_000))
    }

    @Test
    fun `unreadable mascot counts the minimum from the first frame of the splash`() {
        val gate = gate()
        time = 10_000L
        gate.splashShown()
        gate.at(300)
        gate.mascotUnavailable()
        gate.catalogReady()
        gate.heroVisualReady()
        assertEquals(StartupPhase.Splash, gate.at(599))
        assertEquals(StartupPhase.FadingOut, gate.at(600))
    }

    @Test
    fun `late mascot keeps the caps counted from the first frame of the splash`() {
        val gate = gate()
        time = 10_000L
        gate.splashShown()
        gate.at(4_700)
        gate.mascotShown()
        gate.catalogReady()
        gate.heroVisualReady()
        assertEquals(StartupPhase.Splash, gate.at(4_999))
        assertEquals(StartupPhase.FadingOut, gate.at(5_000))
    }
}
