/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface HeroContentProvider {
    suspend fun load(): List<HeroItem>
    suspend fun read(): HeroRead = try {
        HeroRead(load())
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        HeroRead(emptyList(), listOf(error))
    }
    fun changes(): Flow<Unit> = flowOf(Unit)
}

data class HeroRead(val items: List<HeroItem>, val errors: List<Throwable> = emptyList(), val shelfItems: List<HeroItem>? = null)
