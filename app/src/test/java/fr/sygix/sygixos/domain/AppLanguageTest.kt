/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `the first TV language that is translated wins, whatever its region`() {
        assertEquals(AppLanguage.FRENCH, AppLanguage.resolve(listOf(Locale.forLanguageTag("ja-JP"), Locale.forLanguageTag("fr-BE"), Locale.forLanguageTag("de-DE"))))
        assertEquals(AppLanguage.SPANISH, AppLanguage.resolve(listOf(Locale.forLanguageTag("es-419"))))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve(listOf(Locale.forLanguageTag("en-GB"))))
    }

    @Test
    fun `an untranslated TV language falls back to English`() {
        val state = LanguageState(system = AppLanguage.resolve(listOf(Locale.forLanguageTag("ja-JP"))))
        assertNull(state.system)
        assertEquals(AppLanguage.ENGLISH, state.effective)
        assertNull(AppLanguage.resolve(emptyList()))
    }

    @Test
    fun `a chosen language overrides the TV language`() {
        assertEquals(AppLanguage.ITALIAN, LanguageState(selected = AppLanguage.ITALIAN, system = AppLanguage.FRENCH).effective)
        assertEquals(AppLanguage.FRENCH, LanguageState(system = AppLanguage.FRENCH).effective)
    }

    @Test
    fun `languages are listed alphabetically by their own name with unique tags`() {
        val names = AppLanguage.entries.map { it.nativeName }
        assertEquals(names.sortedBy { it.lowercase(Locale.ROOT) }, names)
        assertEquals(AppLanguage.entries.size, AppLanguage.entries.map { it.tag }.toSet().size)
    }
}
