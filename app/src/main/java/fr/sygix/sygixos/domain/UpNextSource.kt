/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextItem
import kotlinx.coroutines.flow.Flow

interface UpNextSource {
    suspend fun load(): Result<List<UpNextItem>>
    fun changes(): Flow<Unit>
}
