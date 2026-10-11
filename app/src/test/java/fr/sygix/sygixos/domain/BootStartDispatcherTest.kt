/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.BootStartState
import fr.sygix.sygixos.model.LauncherSystemPreferences
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootStartDispatcherTest {
    private class Store(enabled: Boolean) : LauncherSystemStore {
        override val launcherSystem = MutableStateFlow(LauncherSystemPreferences(bootStart = BootStartState(enabled = enabled, enabledAtBoot = 3)))
        override suspend fun markLauncherGuideSeen() = Unit
        override suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?) = Unit
        override suspend fun recordBootStartObserved(currentBoot: Int?) {
            launcherSystem.value = launcherSystem.value.copy(bootStart = launcherSystem.value.bootStart.copy(observedAtBoot = currentBoot))
        }
    }

    private var opens = 0
    private var sinceBoot = 30_000L

    private fun dispatcher(store: LauncherSystemStore, opens: () -> Boolean = { this.opens++; true }) =
        BootStartDispatcher(store, bootCount = { 4 }, sinceBootMs = { sinceBoot }, openHome = opens)

    @Test
    fun `the boot broadcast then the service connection open the launcher only once`() = runTest {
        val dispatcher = dispatcher(Store(true))
        val both = listOf(async { dispatcher.dispatch() }, async { dispatcher.dispatch() }).map { it.await() }
        assertEquals(listOf(true, false), both)
        assertEquals(1, opens)
    }

    @Test
    fun `the service connection alone opens once`() = runTest {
        assertTrue(dispatcher(Store(true)).dispatch())
        assertEquals(1, opens)
    }

    @Test
    fun `an opening already observed for this boot is not repeated by another process`() = runTest {
        val store = Store(true)
        store.recordBootStartObserved(4)
        assertFalse(dispatcher(store).dispatch())
        assertEquals(0, opens)
    }

    @Test
    fun `a connection more than five minutes after the boot opens nothing`() = runTest {
        sinceBoot = BootStartDispatcher.WINDOW_MS
        assertFalse(dispatcher(Store(true)).dispatch())
        assertEquals(0, opens)
    }

    @Test
    fun `a disabled option opens nothing on either path`() = runTest {
        val dispatcher = dispatcher(Store(false))
        assertFalse(dispatcher.dispatch())
        assertFalse(dispatcher.dispatch())
        assertEquals(0, opens)
    }

    @Test
    fun `a refused opening keeps the other path possible and unreadable preferences never crash`() = runTest {
        var attempts = 0
        val refusing = dispatcher(Store(true)) { attempts++; throw SecurityException() }
        assertFalse(refusing.dispatch())
        assertFalse(refusing.dispatch())
        assertEquals(2, attempts)
        val broken = object : LauncherSystemStore by Store(true) {
            override val launcherSystem = flow<LauncherSystemPreferences> { throw IllegalStateException() }
        }
        assertFalse(dispatcher(broken).dispatch())
        assertEquals(0, opens)
    }
}
