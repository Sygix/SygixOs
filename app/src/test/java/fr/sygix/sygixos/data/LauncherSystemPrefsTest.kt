/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherSystemPrefsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = LauncherPrefs(context)

    @Before
    fun clearPreferences(): Unit = runBlocking {
        context.dataStore.edit { it.clear() }
    }

    @Test
    fun `existing installation sees onboarding without enabling boot start or writing on read`() = runBlocking {
        prefs.setPinned(setOf("com.example.tv"))
        val before = context.dataStore.data.first()
        val state = prefs.launcherSystem.first()
        assertFalse(state.onboardingDismissed)
        assertFalse(state.bootStart.enabled)
        assertNull(state.bootStart.enabledAtBoot)
        assertNull(state.bootStart.observedAtBoot)
        assertFalse(state.bootStart.notObserved(7))
        assertEquals(before, context.dataStore.data.first())
    }

    @Test
    fun `onboarding dismissal survives a new preferences instance without changing user choices`() = runBlocking {
        prefs.setPinned(setOf("com.example.tv"))
        prefs.setBootStartEnabled(true, 7)
        prefs.dismissLauncherOnboarding()
        val state = LauncherPrefs(context).launcherSystem.first()
        assertTrue(state.onboardingDismissed)
        assertTrue(state.bootStart.enabled)
        assertEquals(7, state.bootStart.enabledAtBoot)
        assertEquals(setOf("com.example.tv"), prefs.pinned.first())
    }

    @Test
    fun `boot start activation persists its boot number and does not fail the current boot`() = runBlocking {
        prefs.setBootStartEnabled(true, 7)
        val boot = LauncherPrefs(context).launcherSystem.first().bootStart
        assertTrue(boot.enabled)
        assertEquals(7, boot.enabledAtBoot)
        assertFalse(boot.notObserved(7))
        assertTrue(boot.notObserved(8))
        assertFalse(prefs.launcherSystem.first().onboardingDismissed)
    }

    @Test
    fun `repeating enable does not postpone an already unobserved boot`() = runBlocking {
        prefs.setBootStartEnabled(true, 7)
        prefs.setBootStartEnabled(true, 8)
        val boot = prefs.launcherSystem.first().bootStart
        assertEquals(7, boot.enabledAtBoot)
        assertTrue(boot.notObserved(8))
    }

    @Test
    fun `late boot observation clears failure for that boot only`() = runBlocking {
        prefs.setBootStartEnabled(true, 7)
        assertTrue(prefs.launcherSystem.first().bootStart.notObserved(8))
        prefs.recordBootStartObserved(8)
        val boot = LauncherPrefs(context).launcherSystem.first().bootStart
        assertEquals(8, boot.observedAtBoot)
        assertFalse(boot.notObserved(8))
        assertTrue(boot.notObserved(9))
    }

    @Test
    fun `disabling clears failure and re-enabling starts evaluation at the next boot`() = runBlocking {
        prefs.setBootStartEnabled(true, 7)
        prefs.recordBootStartObserved(8)
        prefs.setBootStartEnabled(false, 9)
        val disabled = prefs.launcherSystem.first().bootStart
        assertFalse(disabled.enabled)
        assertFalse(disabled.notObserved(9))
        assertNull(disabled.enabledAtBoot)
        assertNull(disabled.observedAtBoot)
        prefs.setBootStartEnabled(true, 9)
        val enabled = prefs.launcherSystem.first().bootStart
        assertEquals(9, enabled.enabledAtBoot)
        assertFalse(enabled.notObserved(9))
        assertTrue(enabled.notObserved(10))
    }

    @Test
    fun `boot observation does not opt in or dismiss onboarding`() = runBlocking {
        prefs.recordBootStartObserved(7)
        val state = prefs.launcherSystem.first()
        assertFalse(state.bootStart.enabled)
        assertFalse(state.bootStart.notObserved(8))
        assertFalse(state.onboardingDismissed)
    }
}
