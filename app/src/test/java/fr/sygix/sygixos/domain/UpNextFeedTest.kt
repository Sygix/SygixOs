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

class UpNextFeedTest {
    private fun movie(id: Long, pkg: String = "app.$id", year: Int? = null, title: String = "Film", kind: ProgramKind = ProgramKind.NEXT, engagement: Long = 0) =
        UpNextItem(id, UpNextSourceEntry(pkg, intentUri = "intent://$id"), UpNextContentType.MOVIE, title, year = year, watchNextType = kind, engagement = engagement)

    private fun episode(id: Long, pkg: String, season: String? = "1", number: String? = "2", kind: ProgramKind = ProgramKind.NEXT) =
        movie(id, pkg, kind = kind).copy(type = UpNextContentType.EPISODE, seriesTitle = "Série", season = season, episode = number)

    @Test
    fun `normalization is strict case accent punctuation year and whitespace insensitive`() {
        assertEquals("ete d hiver", UpNextFeed.normalize("  ÉTÉ : d'hiver (2020)  "))
        assertFalse(UpNextFeed.normalize("Money Heist") == UpNextFeed.normalize("La Casa de Papel"))
    }

    @Test
    fun `exact duplicates use provider id then content id then intent then row id`() {
        for (pair in listOf(
            movie(1, "app").copy(internalProviderId = "same") to movie(2, "app").copy(internalProviderId = "same"),
            movie(1, "app").copy(contentId = "same") to movie(2, "app").copy(contentId = "same"),
            movie(1, "app") to movie(1, "app").copy(id = 2),
            movie(1, "app").copy(source = UpNextSourceEntry("app")) to movie(1, "app").copy(source = UpNextSourceEntry("app")),
        )) {
            assertEquals(1, UpNextFeed.build(listOf(pair.first, pair.second)).size)
        }
        assertEquals(2, UpNextFeed.build(listOf(movie(1, "app"), movie(2, "app"))).size)
    }

    @Test
    fun `one series per app retains continue rather than next episode`() {
        val next = episode(2, "app", number = "3")
        val current = episode(1, "app", kind = ProgramKind.CONTINUE)
        assertEquals(listOf(1L), UpNextFeed.build(listOf(next, current)).map { it.id })
    }

    @Test
    fun `same episode across apps merges with all source intents intact`() {
        val a = episode(1, "app.a")
        val b = episode(2, "app.b", season = "01", number = "02", kind = ProgramKind.CONTINUE)
        val merged = UpNextFeed.build(listOf(a, b)).single()
        assertEquals(2L, merged.id)
        assertEquals(listOf("app.b", "app.a"), merged.sources.map { it.packageName })
        assertEquals(setOf("intent://1", "intent://2"), merged.sources.map { it.intentUri }.toSet())
        assertEquals(2, UpNextFeed.build(listOf(a, b.copy(episode = null))).size)
    }

    @Test
    fun `same movie merges by normalized title and year with unknown year unambiguous`() {
        val a = movie(1, year = 1982, title = "Été (1982)")
        val b = movie(2, title = "ete")
        assertEquals(1, UpNextFeed.build(listOf(a, b)).size)
        assertEquals(1, UpNextFeed.build(listOf(a, b.copy(year = 1982))).size)
        assertEquals(2, UpNextFeed.build(listOf(a, b.copy(year = 2011))).size)
        assertEquals(2, UpNextFeed.build(listOf(a, b.copy(type = UpNextContentType.EPISODE))).size)
    }

    @Test
    fun `ambiguous unknown year never bridges remakes in any permutation`() {
        val items = listOf(movie(1, year = 1982), movie(2), movie(3, year = 2011))
        for (permutation in permutations(items)) {
            val actual = UpNextFeed.build(permutation)
            assertEquals(listOf(1L, 2L, 3L), actual.map { it.id })
            assertTrue(actual.all { it.sources.size == 1 })
        }
    }

    @Test
    fun `unknown type loses to continue and beats watchlist without using progress`() {
        val unknown = movie(2, kind = ProgramKind.WATCH_NEXT, engagement = 1000)
        val watching = movie(1, kind = ProgramKind.CONTINUE)
        val watchlist = movie(3, kind = ProgramKind.WATCHLIST, engagement = 2000)
        assertEquals(1L, UpNextFeed.build(listOf(unknown, watching, watchlist)).single().id)
        assertEquals(2L, UpNextFeed.build(listOf(watchlist, unknown)).single().id)
    }

    @Test
    fun `winner uses newest engagement then Jellyfin priority`() {
        val a = movie(1, "other", engagement = 10)
        val b = movie(2, "org.jellyfin.androidtv", engagement = 10)
        assertEquals(2L, UpNextFeed.build(listOf(a, b)).single().id)
        assertEquals(1L, UpNextFeed.build(listOf(a.copy(engagement = 11), b)).single().id)
    }

    @Test
    fun `filter applies before dedup and source list but hidden apps do not filter`() {
        val disabled = movie(1, "disabled", kind = ProgramKind.CONTINUE)
        val visible = movie(2, "hidden")
        val actual = UpNextFeed.build(listOf(disabled, visible), setOf("disabled")).single()
        assertEquals(2L, actual.id)
        assertEquals(listOf("hidden"), actual.sources.map { it.packageName })
    }

    @Test
    fun `ordering groups engagement then ascending row id is stable and limited to twenty`() {
        val items = listOf(
            movie(4, title = "d", kind = ProgramKind.WATCHLIST, engagement = 1000),
            movie(3, title = "c", kind = ProgramKind.WATCH_NEXT),
            movie(2, title = "b", kind = ProgramKind.CONTINUE),
            movie(1, title = "a", kind = ProgramKind.NEW, engagement = 100),
        )
        assertEquals(listOf(2L, 1L, 3L, 4L), UpNextFeed.build(items).map { it.id })
        val same = listOf(movie(1, "b", title = "b"), movie(1, "a", title = "a"))
        assertEquals(listOf("b", "a"), UpNextFeed.build(same).map { it.source.packageName })
        assertEquals((1L..20L).toList(), UpNextFeed.build((30L downTo 1L).map { movie(it, title = "Movie $it") }).map { it.id })
    }

    @Test
    fun `external identifiers stay inactive in P6`() {
        val a = movie(1, title = "first").copy(externalIds = mapOf("imdb" to "same"))
        val b = movie(2, title = "second").copy(externalIds = a.externalIds)
        assertEquals(2, UpNextFeed.build(listOf(a, b)).size)
    }

    private fun <T> permutations(items: List<T>): List<List<T>> =
        if (items.isEmpty()) listOf(emptyList()) else items.indices.flatMap { index ->
            permutations(items.filterIndexed { i, _ -> i != index }).map { listOf(items[index]) + it }
        }
}
