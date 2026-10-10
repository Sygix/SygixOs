/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.model.LauncherSystemPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class BootStartDispatcherTest {
    private class Store : LauncherSystemStore {
        var prefs = LauncherSystemPreferences()
        var reads = 0
        var observations = 0
        override val launcherSystem: Flow<LauncherSystemPreferences> = flow { reads++; emit(prefs) }
        override suspend fun dismissLauncherOnboarding() = Unit
        override suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?) = Unit
        override suspend fun recordBootStartObserved(currentBoot: Int?) { observations++ }
    }

    @Test
    fun `other broadcasts and disabled boot start cannot launch`() = runTest {
        val store = Store()
        var opens = 0
        val dispatcher = BootStartDispatcher(store) { opens++; true }
        assertFalse(dispatcher.handle("other"))
        assertEquals(0, store.reads)
        assertFalse(dispatcher.handle("android.intent.action.BOOT_COMPLETED"))
        assertEquals(0, opens)
    }

    @Test
    fun `enabled boot starts but only observed activity can record success`() = runTest {
        val store = Store().apply { prefs = prefs.copy(bootStart = prefs.bootStart.copy(enabled = true)) }
        var opens = 0
        val dispatcher = BootStartDispatcher(store) { opens++; true }
        assertTrue(dispatcher.handle("android.intent.action.BOOT_COMPLETED"))
        assertEquals(1, opens)
        assertEquals(0, store.observations)
    }

    @Test
    fun `blocked activity does not mark boot observed or discard opt in`() = runTest {
        val store = Store().apply { prefs = prefs.copy(bootStart = prefs.bootStart.copy(enabled = true)) }
        val dispatcher = BootStartDispatcher(store) { throw SecurityException() }
        assertFalse(dispatcher.handle("android.intent.action.BOOT_COMPLETED"))
        assertTrue(store.prefs.bootStart.enabled)
        assertEquals(0, store.observations)
    }
}
