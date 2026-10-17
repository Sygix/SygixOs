/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.domain.TsuyuRelease
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TsuyuReleasePrefsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val release = TsuyuRelease("v1.0.0-rc.1", true, "https://github.com/Sygix/Tsuyu/releases/download/v1.0.0-rc.1/tsuyu.apk", 4_096, "12".repeat(32))

    @Before
    fun reset() = runBlocking {
        context.dataStore.edit { it.clear() }
        Unit
    }

    @Test
    fun `the last release survives a new instance and nothing replaces it`() = runBlocking {
        assertNull(TsuyuReleasePrefs(context).load())
        TsuyuReleasePrefs(context).save(release)
        assertEquals(release, TsuyuReleasePrefs(context).load())
        TsuyuReleasePrefs(context).save(null)
        assertNull(TsuyuReleasePrefs(context).load())
    }

    @Test
    fun `an unreadable value is no release`() = runBlocking {
        context.dataStore.edit { it[stringPreferencesKey("tsuyu_last_release")] = "{broken" }
        assertNull(TsuyuReleasePrefs(context).load())
    }
}
