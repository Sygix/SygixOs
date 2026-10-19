/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import fr.sygix.sygixos.SygixOsApp

@Composable
internal fun artworkRevision(): Long {
    val app = LocalContext.current.applicationContext as? SygixOsApp ?: return 0
    return app.programArtwork.changes.collectAsStateWithLifecycle().value
}
