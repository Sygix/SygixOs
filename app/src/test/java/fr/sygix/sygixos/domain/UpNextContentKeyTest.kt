/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpNextContentKeyTest {
    private fun episode(id: Long, pkg: String, series: String, season: String, number: String) =
        UpNextItem(id, UpNextSourceEntry(pkg), UpNextContentType.EPISODE, "Episode $number", seriesTitle = series, season = season, episode = number)

    private fun movie(id: Long, pkg: String, title: String, year: Int?) =
        UpNextItem(id, UpNextSourceEntry(pkg), UpNextContentType.MOVIE, title, year = year)

    @Test
    fun `episodes of the same series share one key across seasons and apps`() {
        val first = UpNextContentKey.of(episode(1, "app.a", "La Série", "1", "3"))
        val second = UpNextContentKey.of(episode(2, "app.b", "la serie", "2", "1"))
        assertEquals(first, second)
        assertEquals("EPISODE|la serie", first.serialized)
    }

    @Test
    fun `movie with an unknown year on one side matches and different years do not`() {
        val known = UpNextContentKey.of(movie(1, "a", "Le Film", 2001))
        assertTrue(known.matches(UpNextContentKey.of(movie(2, "b", "Le Film", null))))
        assertTrue(UpNextContentKey.of(movie(2, "b", "Le Film", null)).matches(known))
        assertFalse(known.matches(UpNextContentKey.of(movie(3, "b", "Le Film", 2019))))
        assertTrue(known.matches(movie(4, "c", "le film", 2001)))
    }

    @Test
    fun `different types never match`() {
        val series = UpNextContentKey.of(episode(1, "a", "Titre", "1", "1"))
        val film = UpNextContentKey.of(movie(2, "a", "Titre", null))
        assertFalse(series.matches(film))
        assertFalse(film.matches(series))
    }

    @Test
    fun `serialized keys parse back and unreadable values are rejected`() {
        val keys = listOf(
            UpNextContentKey(UpNextContentType.EPISODE, "serie"),
            UpNextContentKey(UpNextContentType.MOVIE, "film", 1999),
            UpNextContentKey(UpNextContentType.MOVIE, "film"),
        )
        keys.forEach { assertEquals(it, UpNextContentKey.parse(it.serialized)) }
        assertNull(UpNextContentKey.parse(""))
        assertNull(UpNextContentKey.parse("SHOW|x"))
        assertNull(UpNextContentKey.parse("MOVIE|film|year"))
        assertNull(UpNextContentKey.parse("EPISODE|"))
        assertNull(UpNextContentKey.parse("EPISODE|a|b"))
    }
}
