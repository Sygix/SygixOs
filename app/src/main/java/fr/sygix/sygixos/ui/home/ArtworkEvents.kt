/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.filter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.data.ArtworkEvent
import fr.sygix.sygixos.data.ArtworkRepository
import fr.sygix.sygixos.model.ProgramArtwork

internal val LocalProgramArtwork = androidx.compose.runtime.staticCompositionLocalOf<ArtworkRepository?> { null }

@Composable
internal fun artworkEvent(source: ProgramArtwork?, uri: String? = null): ArtworkEvent? {
    val app = LocalContext.current.applicationContext as? SygixOsApp
    val repository = LocalProgramArtwork.current ?: app?.programArtwork ?: return null
    DisposableEffect(repository, source?.identity) {
        val watch = source?.let { repository.watch(it, fr.sygix.sygixos.domain.ArtworkPolicy.Card(1)) }
        onDispose { watch?.close() }
    }
    return source?.let {
        val signal = repository.signal(it.identity)
        val events = remember(repository, it.identity, uri) { signal.filter { event -> event == null || uri == null || event.uri == uri } }
        events.collectAsStateWithLifecycle(initialValue = signal.value?.takeIf { event -> uri == null || event.uri == uri }).value
    }
}
