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
    fun changes(): Flow<Unit> = flowOf(Unit)
}
