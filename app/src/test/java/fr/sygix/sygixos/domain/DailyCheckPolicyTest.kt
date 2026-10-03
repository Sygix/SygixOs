/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCheckPolicyTest {

    private val hour = 60L * 60 * 1000
    private val now = 1_000L * hour

    @Test
    fun `never checked is due`() {
        assertTrue(DailyCheckPolicy.isDue(now, lastCheckAt = null, retryAt = null))
    }

    @Test
    fun `three hours after the last check is not due`() {
        assertFalse(DailyCheckPolicy.isDue(now, lastCheckAt = now - 3 * hour, retryAt = null))
    }

    @Test
    fun `twenty four hours after the last check is due`() {
        assertTrue(DailyCheckPolicy.isDue(now, lastCheckAt = now - 24 * hour, retryAt = null))
        assertFalse(DailyCheckPolicy.isDue(now, lastCheckAt = now - 24 * hour + 1, retryAt = null))
    }

    @Test
    fun `twenty six hours later on a return to the foreground is due`() {
        assertTrue(DailyCheckPolicy.isDue(now, lastCheckAt = now - 26 * hour, retryAt = null))
    }

    @Test
    fun `a last check in the future is due`() {
        assertTrue(DailyCheckPolicy.isDue(now, lastCheckAt = now + hour, retryAt = null))
    }

    @Test
    fun `nothing is due before the retry time and it is due again after it`() {
        assertFalse(DailyCheckPolicy.isDue(now, lastCheckAt = null, retryAt = now + 1))
        assertFalse(DailyCheckPolicy.canRequest(now, retryAt = now + 1))
        assertTrue(DailyCheckPolicy.isDue(now, lastCheckAt = null, retryAt = now))
        assertTrue(DailyCheckPolicy.canRequest(now, retryAt = now - 1))
    }
}
