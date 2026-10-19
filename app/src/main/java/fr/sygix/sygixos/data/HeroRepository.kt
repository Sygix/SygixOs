/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.HeroFeed
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

class HeroRepository(
    private val programs: HeroContentProvider,
    private val fallback: HeroContentProvider,
) {
    suspend fun load(): HeroFeed {
        val published = programs.read()
        if (published.items.isNotEmpty()) return HeroFeed(published.items, fromApps = true, published.errors)
        return HeroFeed(fallbackItems(), fromApps = false, published.errors)
    }

    suspend fun fallbackItems(): List<HeroItem> = items(fallback)

    fun changes(): Flow<Unit> = programs.changes()

    private suspend fun items(provider: HeroContentProvider): List<HeroItem> = try {
        provider.load()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        emptyList()
    }
}
