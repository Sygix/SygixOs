/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.model

data class TvApp(
    val packageName: String,
    val label: String,
    val activityName: String? = null,
)
