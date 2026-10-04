/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.ProgramKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeroCaptionTest {

    private fun item(
        kind: ProgramKind = ProgramKind.FEATURED,
        app: String? = "Appli",
        season: String? = null,
        episode: String? = null,
        duration: Long? = null,
        position: Long? = null,
        progress: Float? = null,
    ) = HeroItem(
        id = "i",
        title = "Titre fictif",
        sourceLabel = app,
        kind = kind,
        season = season,
        episode = episode,
        durationMillis = duration,
        positionMillis = position,
        progress = progress,
    )

    @Test
    fun `header keeps the program type of a watch next program with the app name`() {
        listOf(ProgramKind.CONTINUE, ProgramKind.NEXT, ProgramKind.NEW, ProgramKind.WATCHLIST).forEach { kind ->
            assertEquals(HeroHeader(kind, "Appli"), HeroCaption.header(item(kind)))
        }
    }

    @Test
    fun `featured program and watch next program without a known type show the app name only`() {
        assertEquals(HeroHeader(ProgramKind.FEATURED, "Appli"), HeroCaption.header(item(ProgramKind.FEATURED)))
        assertEquals(HeroHeader(ProgramKind.FEATURED, "Appli"), HeroCaption.header(item(ProgramKind.WATCH_NEXT)))
    }

    @Test
    fun `missing app name keeps the type alone, and nothing is left without type or name`() {
        assertEquals(HeroHeader(ProgramKind.CONTINUE, null), HeroCaption.header(item(ProgramKind.CONTINUE, app = null)))
        assertEquals(HeroHeader(ProgramKind.CONTINUE, null), HeroCaption.header(item(ProgramKind.CONTINUE, app = " ")))
        assertNull(HeroCaption.header(item(ProgramKind.FEATURED, app = null)))
        assertNull(HeroCaption.header(item(ProgramKind.WATCH_NEXT, app = null)))
    }

    @Test
    fun `details keep only the provided season, episode and duration`() {
        assertEquals(HeroDetails("2", "5", 42), HeroCaption.details(item(season = "2", episode = "5", duration = 42 * 60_000L)))
        assertEquals(HeroDetails(null, "5", null), HeroCaption.details(item(episode = "5")))
        assertEquals(HeroDetails("2", null, null), HeroCaption.details(item(season = "2", episode = " ")))
        assertEquals(HeroDetails(null, null, 95), HeroCaption.details(item(duration = 95 * 60_000L)))
        assertNull(HeroCaption.details(item()))
        assertNull(HeroCaption.details(item(duration = 0)))
    }

    @Test
    fun `duration is rounded to the minute and never shown as zero`() {
        assertEquals(42, HeroCaption.durationMinutes(42 * 60_000L + 20_000))
        assertEquals(43, HeroCaption.durationMinutes(42 * 60_000L + 30_000))
        assertEquals(1, HeroCaption.durationMinutes(10_000))
        assertEquals(HoursMinutes(1, 35), HeroCaption.split(95))
        assertEquals(HoursMinutes(2, 0), HeroCaption.split(120))
        assertEquals(HoursMinutes(0, 42), HeroCaption.split(42))
    }

    @Test
    fun `remaining time needs the progress, the position and the duration`() {
        val duration = 42 * 60_000L
        assertEquals(25, HeroCaption.remainingMinutes(item(duration = duration, position = 17 * 60_000L, progress = 0.4f)))
        assertEquals(1, HeroCaption.remainingMinutes(item(duration = duration, position = duration - 5_000, progress = 0.99f)))
        assertEquals(25, HeroCaption.remainingMinutes(item(duration = duration, position = 17 * 60_000L + 30_000, progress = 0.4f)))
        assertNull(HeroCaption.remainingMinutes(item(duration = duration, position = 17 * 60_000L, progress = null)))
        assertNull(HeroCaption.remainingMinutes(item(duration = null, position = 17 * 60_000L, progress = 0.4f)))
        assertNull(HeroCaption.remainingMinutes(item(duration = duration, position = null, progress = 0.4f)))
    }
}
