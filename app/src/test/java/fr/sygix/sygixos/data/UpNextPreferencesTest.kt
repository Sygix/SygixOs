/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import fr.sygix.sygixos.model.UpNextPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpNextPreferencesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = LauncherPrefs(context)

    @Before
    fun clear() = runBlocking { context.dataStore.edit { it.clear() }; Unit }

    @Test
    fun `visibility and position defaults persist across repository instances`() = runBlocking {
        assertTrue(prefs.upNextVisible.first())
        assertEquals(UpNextPosition.BEFORE_APPS, prefs.upNextPosition.first())
        prefs.setUpNextVisible(false)
        prefs.setUpNextPosition(UpNextPosition.AFTER_APPS)
        val restarted = LauncherPrefs(context)
        assertFalse(restarted.upNextVisible.first())
        assertEquals(UpNextPosition.AFTER_APPS, restarted.upNextPosition.first())
        restarted.setUpNextVisible(true)
        assertEquals(UpNextPosition.AFTER_APPS, prefs.upNextPosition.first())
    }

    @Test
    fun `unknown stored position uses default`() = runBlocking {
        context.dataStore.edit { it[stringPreferencesKey("up_next_position")] = "FUTURE_VALUE" }
        assertEquals(UpNextPosition.BEFORE_APPS, prefs.upNextPosition.first())
    }
}
