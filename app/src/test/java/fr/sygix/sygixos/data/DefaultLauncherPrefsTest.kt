/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.SetupStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultLauncherPrefsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @Before fun reset() = runBlocking { context.dataStore.edit { it.clear() }; Unit }

    @Test fun firstPreviousHomeSurvivesNewInstancesWithoutChangingGuideDismissal() = runBlocking {
        val legacy = LauncherPrefs(context)
        legacy.markLauncherGuideSeen()
        legacy.setBootStartEnabled(true, 3)
        val store = DefaultLauncherPrefs(context)
        store.rememberPrevious("first.home/.Home")
        store.rememberPrevious("second.home/.Home")
        store.record(SetupStep.HOME)
        store.record(SetupStep.HOME)
        assertEquals("first.home/.Home", DefaultLauncherPrefs(context).loadMemory().previousHome)
        assertEquals(setOf(SetupStep.HOME), DefaultLauncherPrefs(context).loadMemory().modifications)
        assertTrue(legacy.launcherSystem.first().guideSeen)
        assertTrue(legacy.launcherSystem.first().bootStart.enabled)
    }
}
