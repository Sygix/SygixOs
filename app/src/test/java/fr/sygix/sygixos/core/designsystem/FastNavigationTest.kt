/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastNavigationTest {

    private val navigation = FastNavigation(window = 300)

    private fun down(time: Long, repeat: Int = 0, direction: Long? = DOWN) =
        navigation.record(direction = direction, down = true, repeatCount = repeat, eventTime = time)

    private fun up(time: Long, direction: Long? = DOWN) =
        navigation.record(direction = direction, down = false, repeatCount = 0, eventTime = time)

    @Test
    fun `an isolated press is not fast`() {
        down(1_000)
        assertFalse(navigation.active)
        down(1_300)
        assertFalse(navigation.active)
    }

    @Test
    fun `a held key repeating is fast`() {
        down(1_000)
        down(1_500, repeat = 1)
        assertTrue(navigation.active)
    }

    @Test
    fun `presses closer than the focus animation are fast`() {
        down(1_000)
        down(1_299)
        assertTrue(navigation.active)
    }

    @Test
    fun `a pause after a burst ends fast navigation`() {
        down(1_000)
        down(1_050, repeat = 1)
        down(2_000)
        assertFalse(navigation.active)
    }

    @Test
    fun `releasing the repeated direction ends fast navigation`() {
        down(1_000)
        down(1_050, repeat = 1)
        up(1_060, direction = RIGHT)
        assertTrue(navigation.active)
        up(1_070)
        assertFalse(navigation.active)
    }

    @Test
    fun `quick taps released between presses are still fast`() {
        down(1_000)
        up(1_040)
        down(1_200)
        assertTrue(navigation.active)
    }

    @Test
    fun `fast navigation expires when no direction is pressed within the focus animation`() = runTest {
        backgroundScope.launch { navigation.expireAfterPauses() }
        runCurrent()
        down(1_000)
        down(1_050, repeat = 1)
        runCurrent()
        advanceTimeBy(299)
        assertTrue(navigation.active)
        down(1_100, repeat = 2)
        runCurrent()
        advanceTimeBy(299)
        assertTrue(navigation.active)
        advanceTimeBy(2)
        assertFalse(navigation.active)
    }

    @Test
    fun `another key ends fast navigation and the next press is isolated`() {
        down(1_000)
        down(1_050, repeat = 1)
        down(1_100, direction = null)
        assertFalse(navigation.active)
        down(1_150)
        assertFalse(navigation.active)
    }

    private companion object {
        const val DOWN = 20L
        const val RIGHT = 22L
    }
}
