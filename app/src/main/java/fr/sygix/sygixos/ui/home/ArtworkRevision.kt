/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import fr.sygix.sygixos.SygixOsApp
import kotlinx.coroutines.flow.StateFlow

internal val LocalArtworkChanges = staticCompositionLocalOf<StateFlow<Long>?> { null }
internal val LocalArtworkRetries = staticCompositionLocalOf<StateFlow<Long>?> { null }

@Composable
internal fun artworkRevision(): Long {
    val app = LocalContext.current.applicationContext as? SygixOsApp
    val changes = LocalArtworkChanges.current ?: app?.programArtwork?.changes ?: return 0
    return changes.collectAsStateWithLifecycle().value
}

@Composable
internal fun artworkRetryRevision(): Long {
    val app = LocalContext.current.applicationContext as? SygixOsApp
    val changes = LocalArtworkRetries.current ?: app?.programArtwork?.retryChanges ?: return 0
    return changes.collectAsStateWithLifecycle().value
}
