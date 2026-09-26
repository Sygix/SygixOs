/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ShelfPostersTest {

    private val programs = listOf(
        HeroItem("1", "A", imageUrl = "https://a/1.jpg", sourcePackage = "com.a"),
        HeroItem("2", "A2", imageUrl = "https://a/1.jpg", sourcePackage = "com.a"),
        HeroItem("3", "A3", imageUrl = "https://a/3.jpg", sourcePackage = "com.a"),
        HeroItem("4", "A4", videoUrl = "https://a/v.mp4", sourcePackage = "com.a"),
        HeroItem("5", "B", imageUrl = "https://b/1.jpg", sourcePackage = "com.b"),
    )

    private val validated = setOf("https://a/1.jpg", "https://a/3.jpg", "https://b/1.jpg")

    @Test
    fun `filters by package and deduplicates posters`() {
        assertEquals(listOf("https://a/1.jpg", "https://a/3.jpg"), ShelfPosters.forPackage(programs, validated, "com.a"))
    }

    @Test
    fun `only validated visuals are proposed`() {
        assertEquals(listOf("https://a/3.jpg"), ShelfPosters.forPackage(programs, setOf("https://a/3.jpg"), "com.a"))
        assertEquals(emptyList<String>(), ShelfPosters.forPackage(programs, emptySet(), "com.a"))
    }

    @Test
    fun `candidates are limited and independent of validation`() {
        assertEquals(listOf("https://a/1.jpg", "https://a/3.jpg"), ShelfPosters.candidates(programs, "com.a", 8))
        assertEquals(listOf("https://a/1.jpg"), ShelfPosters.candidates(programs, "com.a", 1))
        assertEquals(emptyList<String>(), ShelfPosters.candidates(programs, "com.zzz", 8))
    }

    @Test
    fun `unknown package yields nothing`() {
        assertEquals(emptyList<String>(), ShelfPosters.forPackage(programs, validated, "com.zzz"))
    }
}
