/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FastNavigationTest {

    private val navigation = FastNavigation(window = 300)

    private fun down(time: Long, repeat: Int = 0, direction: Boolean = true) =
        navigation.record(direction = direction, down = true, repeatCount = repeat, eventTime = time)

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
    fun `a key release keeps the state of the last press`() {
        down(1_000)
        down(1_050, repeat = 1)
        navigation.record(direction = true, down = false, repeatCount = 0, eventTime = 1_060)
        assertTrue(navigation.active)
    }

    @Test
    fun `another key ends fast navigation and the next press is isolated`() {
        down(1_000)
        down(1_050, repeat = 1)
        down(1_100, direction = false)
        assertFalse(navigation.active)
        down(1_150)
        assertFalse(navigation.active)
    }
}
