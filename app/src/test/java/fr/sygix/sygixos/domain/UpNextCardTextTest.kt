/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpNextCardTextTest {
    private val episode = UpNextItem(1, UpNextSourceEntry("app"), UpNextContentType.EPISODE, "Série", seriesTitle = "Série", season = "1", episode = "12", episodeTitle = "Pilote")

    @Test
    fun `episode numbers are padded to two digits and absent without both numbers`() {
        assertEquals("01" to "12", UpNextCardText.episodeNumbers(episode))
        assertEquals("10" to "123", UpNextCardText.episodeNumbers(episode.copy(season = "10", episode = "123")))
        assertNull(UpNextCardText.episodeNumbers(episode.copy(season = null)))
        assertNull(UpNextCardText.episodeNumbers(episode.copy(episode = null)))
        assertNull(UpNextCardText.episodeNumbers(episode.copy(type = UpNextContentType.MOVIE)))
    }

    @Test
    fun `episode line joins number and title with the separator and is absent for movies`() {
        assertEquals("S01E12 · Pilote", UpNextCardText.episodeLine(episode, "S01E12", " · "))
        assertEquals("Pilote", UpNextCardText.episodeLine(episode, null, " · "))
        assertEquals("S01E12", UpNextCardText.episodeLine(episode.copy(episodeTitle = null), "S01E12", " · "))
        assertNull(UpNextCardText.episodeLine(episode.copy(episodeTitle = null), null, " · "))
        assertNull(UpNextCardText.episodeLine(episode.copy(type = UpNextContentType.MOVIE), "S01E12", " · "))
    }

    @Test
    fun `progress bar only for continue items with progress`() {
        val playing = episode.copy(watchNextType = ProgramKind.CONTINUE, positionMillis = 25, durationMillis = 100)
        assertEquals(0.25f, UpNextCardText.progress(playing))
        assertNull(UpNextCardText.progress(playing.copy(watchNextType = ProgramKind.NEXT)))
        assertNull(UpNextCardText.progress(playing.copy(durationMillis = null)))
    }

    @Test
    fun `episode titled by its episode title has no episode line`() {
        val fallback = episode.copy(title = "Pilote", seriesTitle = null, titleFromEpisode = true)
        assertEquals("Pilote", fallback.displayTitle)
        assertNull(UpNextCardText.episodeNumbers(fallback))
        assertNull(UpNextCardText.episodeLine(fallback, "S01E12", " · "))
    }
}
