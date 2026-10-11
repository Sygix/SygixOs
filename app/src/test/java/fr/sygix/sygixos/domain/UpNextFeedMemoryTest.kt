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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpNextFeedMemoryTest {
    private fun movie(id: Long, title: String, kind: ProgramKind = ProgramKind.NEXT, engagement: Long = 0, year: Int? = null, pkg: String = "app.$id") =
        UpNextItem(id, UpNextSourceEntry(pkg, intentUri = "intent://$id"), UpNextContentType.MOVIE, title, year = year, watchNextType = kind, engagement = engagement)

    private fun episode(id: Long, pkg: String, series: String, season: String, number: String) =
        UpNextItem(id, UpNextSourceEntry(pkg, intentUri = "intent://$id"), UpNextContentType.EPISODE, "Ep", seriesTitle = series, season = season, episode = number)

    private fun key(item: UpNextItem) = UpNextContentKey.of(item)

    @Test
    fun `contents opened from the launcher come first by most recent opening then the groups`() {
        val watchlist = movie(1, "Liste", ProgramKind.WATCHLIST)
        val next = movie(2, "Suivant", ProgramKind.NEXT)
        val running = movie(3, "En cours", ProgramKind.CONTINUE, engagement = 50)
        val other = movie(4, "Autre", ProgramKind.CONTINUE, engagement = 10)
        val opened = mapOf(key(watchlist) to 1_000L, key(next) to 5_000L)
        assertEquals(listOf(2L, 1L, 3L, 4L), UpNextFeed.build(listOf(watchlist, next, running, other), opened = opened).map { it.id })
        assertEquals(listOf(3L, 4L, 2L, 1L), UpNextFeed.build(listOf(watchlist, next, running, other)).map { it.id })
    }

    @Test
    fun `an opened series stays first whatever the episode and app now published`() {
        val opened = mapOf(UpNextContentKey(UpNextContentType.EPISODE, "serie") to 9L)
        val other = movie(1, "Film", ProgramKind.CONTINUE, engagement = 99)
        val nextEpisode = episode(7, "app.b", "Série", "1", "4")
        assertEquals(listOf(7L, 1L), UpNextFeed.build(listOf(other, nextEpisode), opened = opened).map { it.id })
    }

    @Test
    fun `a removed series disappears for every app and episode while another year of a film stays`() {
        val removed = listOf(UpNextContentKey(UpNextContentType.EPISODE, "serie"), UpNextContentKey(UpNextContentType.MOVIE, "film", 2001))
        val items = listOf(
            episode(1, "app.a", "Série", "1", "1"),
            episode(2, "app.b", "Serie", "3", "5"),
            movie(3, "Film", year = 2001),
            movie(4, "Film", year = 2019),
            movie(5, "Film", year = null, pkg = "app.unknown"),
        )
        val shown = UpNextFeed.build(items, removed = removed).map { it.id }
        assertEquals(listOf(4L), shown)
    }

    @Test
    fun `a removed content frees its place within the limit of twenty`() {
        val items = (1L..21L).map { movie(it, "Titre $it", engagement = 100 - it) }
        assertEquals((1L..20L).toList(), UpNextFeed.build(items).map { it.id })
        val removed = listOf(key(items.first()))
        assertEquals((2L..21L).toList(), UpNextFeed.build(items, removed = removed).map { it.id })
    }

    @Test
    fun `a disabled source neither wins nor hides a remembered content`() {
        val first = movie(1, "Titre", pkg = "app.off")
        val second = movie(2, "Titre", pkg = "app.on")
        val built = UpNextFeed.build(listOf(first, second), disabledSources = setOf("app.off"))
        assertEquals(listOf(2L), built.map { it.id })
        assertEquals(listOf("app.on"), built.single().sources.map { it.packageName })
        assertTrue(UpNextFeed.stillPublished(key(first), listOf(first)))
        assertFalse(UpNextFeed.stillPublished(UpNextContentKey(UpNextContentType.MOVIE, "absent"), listOf(first)))
    }
}
