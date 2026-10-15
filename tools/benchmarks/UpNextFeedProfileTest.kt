/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class UpNextFeedProfileTest {
    @Test fun profileSyntheticFeeds() {
        for (size in listOf(100, 500, 1_000)) {
            val items = (0 until size).flatMap { index ->
                listOf("app.a", "app.b").mapIndexed { source, pkg ->
                    UpNextItem(index * 2L + source, UpNextSourceEntry(pkg), UpNextContentType.MOVIE, "Synthetic $index")
                }
            }
            repeat(2) { UpNextFeed.build(items) }
            val elapsed = (1..5).map {
                val start = System.nanoTime()
                val result = UpNextFeed.build(items)
                val nanos = System.nanoTime() - start
                assertEquals((0 until 20).map { it * 2L }, result.map { it.id })
                assertEquals(List(20) { listOf("app.a", "app.b") }, result.map { row -> row.sources.map { it.packageName } })
                nanos / 1_000_000.0
            }
            println("rows=${items.size} millis=$elapsed median=${elapsed.sorted()[2]}")
        }
    }
}
