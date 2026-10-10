/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootStartStateTest {
    @Test
    fun `unobserved subsequent boot is a failure regardless of why opening did not happen`() {
        assertTrue(BootStartState(enabled = true, enabledAtBoot = 7).notObserved(8))
    }

    @Test
    fun `current boot activation and observation never report failure`() {
        assertFalse(BootStartState(enabled = true, enabledAtBoot = 7).notObserved(7))
        assertFalse(BootStartState(enabled = true, enabledAtBoot = 7, observedAtBoot = 8).notObserved(8))
    }

    @Test
    fun `observation from a previous boot cannot hide a later failure`() {
        assertTrue(BootStartState(enabled = true, enabledAtBoot = 7, observedAtBoot = 8).notObserved(9))
    }

    @Test
    fun `disabled boot start cannot report failure`() {
        assertFalse(BootStartState(enabled = false, enabledAtBoot = 7).notObserved(8))
    }

    @Test
    fun `unknown boot numbers cannot establish an unobserved boot`() {
        assertFalse(BootStartState(enabled = true, enabledAtBoot = 7).notObserved(null))
        assertFalse(BootStartState(enabled = true).notObserved(8))
        assertFalse(BootStartState(enabled = true, enabledAtBoot = 8).notObserved(7))
    }
}
