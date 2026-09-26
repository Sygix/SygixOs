/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

data class TvApp(
    val packageName: String,
    val label: String,
    val activityName: String? = null,
)
