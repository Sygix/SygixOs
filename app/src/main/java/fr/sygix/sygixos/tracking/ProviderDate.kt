/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.time.LocalDate
import java.time.format.DateTimeParseException

object ProviderDate {
    fun day(value: String?): LocalDate? = value?.take(10)?.let {
        try { LocalDate.parse(it) } catch (_: DateTimeParseException) { null }
    }
}
