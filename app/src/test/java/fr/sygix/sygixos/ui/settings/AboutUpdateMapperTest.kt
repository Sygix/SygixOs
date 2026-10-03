/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import fr.sygix.sygixos.domain.CheckLine
import fr.sygix.sygixos.domain.InstallLine
import fr.sygix.sygixos.domain.QrCode
import fr.sygix.sygixos.domain.UpdateStatus
import fr.sygix.sygixos.domain.candidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutUpdateMapperTest {

    private val encoded = mutableListOf<String>()
    private val mapper = AboutUpdateMapper { url -> encoded += url; QrCode.encode(url) }
    private val v2 = candidate("v0.0.2")

    @Test
    fun `proposed version on a github release page gets a release notes code`() {
        val state = mapper.map(UpdateStatus(proposed = v2), systemScreenReturns = 3)
        assertNotNull(state.releaseNotesQr)
        assertEquals(CheckLine.Available("0.0.2", prerelease = false), state.checkLine)
        assertEquals(InstallLine(v2, null), state.installLine)
        assertTrue(state.badge)
        assertEquals(3, state.systemScreenReturns)
    }

    @Test
    fun `release page outside github or not in https gets no code`() {
        listOf("https://example.org/Sygix/SygixOs/releases/tag/v0.0.2", "http://github.com/Sygix/SygixOs/releases/tag/v0.0.2", "").forEach { url ->
            val state = mapper.map(UpdateStatus(proposed = v2.copy(htmlUrl = url)), 0)
            assertNull(url, state.releaseNotesQr)
            assertEquals(InstallLine(v2.copy(htmlUrl = url), null), state.installLine)
        }
        assertTrue(encoded.isEmpty())
    }

    @Test
    fun `no proposed version gets no code and no badge`() {
        val state = mapper.map(UpdateStatus(), 0)
        assertNull(state.releaseNotesQr)
        assertNull(state.installLine)
        assertEquals(false, state.badge)
    }

    @Test
    fun `the code is encoded once per release page`() {
        repeat(5) { mapper.map(UpdateStatus(proposed = v2), 0) }
        assertEquals(1, encoded.size)
    }
}
