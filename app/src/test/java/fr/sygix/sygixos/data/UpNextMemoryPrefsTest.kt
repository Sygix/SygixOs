/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.domain.RemovedUpNextContent
import fr.sygix.sygixos.domain.UpNextContentKey
import fr.sygix.sygixos.model.UpNextContentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpNextMemoryPrefsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val series = UpNextContentKey(UpNextContentType.EPISODE, "serie")
    private val film = UpNextContentKey(UpNextContentType.MOVIE, "film", 2001)

    @Before
    fun clear() = runBlocking { context.dataStore.edit { it.clear() }; Unit }

    @Test
    fun `opened contents persist across restarts, keep the latest opening and prune`() = runBlocking {
        val prefs = LauncherPrefs(context)
        prefs.recordOpened(series, 10)
        prefs.recordOpened(film, 20)
        prefs.recordOpened(series, 30)
        assertEquals(mapOf(series to 30L, film to 20L), LauncherPrefs(context).opened.first())
        prefs.pruneOpened { it == film }
        assertEquals(mapOf(film to 20L), LauncherPrefs(context).opened.first())
    }

    @Test
    fun `removed contents persist with their title sources image and instant until restored`() = runBlocking {
        val prefs = LauncherPrefs(context)
        val content = RemovedUpNextContent(series, "La\tSérie", listOf("app.a", "app.b"), "content://image/1", 42)
        prefs.addRemoved(content)
        prefs.addRemoved(RemovedUpNextContent(film, "Film", emptyList(), null, 50))
        val restored = LauncherPrefs(context).removed.first()
        assertEquals(listOf(content.copy(title = "La Série"), RemovedUpNextContent(film, "Film", emptyList(), null, 50)), restored)
        prefs.restoreRemoved(series)
        assertEquals(listOf(film), prefs.removed.first().map { it.key })
    }

    @Test
    fun `unreadable stored values read as empty`() = runBlocking {
        context.dataStore.edit {
            it[stringPreferencesKey("up_next_opened")] = "garbage\nEPISODE|x\tnot-a-number"
            it[stringPreferencesKey("up_next_removed")] = "broken\tline"
        }
        val prefs = LauncherPrefs(context)
        assertTrue(prefs.opened.first().isEmpty())
        assertTrue(prefs.removed.first().isEmpty())
    }
}
