/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HeroRepositoryTest {

    private fun provider(vararg items: HeroItem) = object : HeroContentProvider {
        override suspend fun load(): List<HeroItem> = items.toList()
    }

    private val failing = object : HeroContentProvider {
        override suspend fun load(): List<HeroItem> = error("provider down")
    }

    @Test
    fun `published programs win over fallback`() = runBlocking {
        val repo = HeroRepository(provider(HeroItem("p", "Film")), provider(HeroItem("n", "", videoUrl = "https://x/clip.mp4")))
        val feed = repo.load()
        assertTrue(feed.fromApps)
        assertEquals(listOf("p"), feed.items.map { it.id })
    }

    @Test
    fun `fallback used when nothing is published`() = runBlocking {
        val repo = HeroRepository(provider(), provider(HeroItem("n", "", videoUrl = "https://x/clip.mp4")))
        val feed = repo.load()
        assertFalse(feed.fromApps)
        assertEquals(listOf("n"), feed.items.map { it.id })
    }

    @Test
    fun `provider failures never crash and degrade to empty`() = runBlocking {
        val feed = HeroRepository(failing, failing).load()
        assertFalse(feed.fromApps)
        assertTrue(feed.items.isEmpty())
    }

    @Test
    fun `nature fallback exposes only https landscape clips`() = runBlocking {
        val clips = NatureFallbackProvider().load()
        assertTrue(clips.isNotEmpty())
        assertTrue(clips.all { it.videoUrl!!.startsWith("https://") && it.videoUrl!!.contains("2560_1440") })
        assertTrue(clips.all { it.title.isEmpty() && it.imageUrl == null })
    }
}
