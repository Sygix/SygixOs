/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.time.LocalDate

object ReleasedContent {
    fun released(content: TrackingContent, today: LocalDate): Boolean {
        val date = ProviderDate.day(content.airDate)
        return if (date != null) !date.isAfter(today) else content.year?.let { it <= today.year } ?: true
    }
}
