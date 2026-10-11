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
    private class Store(enabled: Boolean, connectedAtBoot: Int? = 3) : LauncherSystemStore {
        override val launcherSystem = MutableStateFlow(
            LauncherSystemPreferences(
                bootStart = BootStartState(enabled = enabled, enabledAtBoot = 3),
                accessibilityConnectedAtBoot = connectedAtBoot,
            ),
        )
        override suspend fun markLauncherGuideSeen() = Unit
        override suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?) = Unit
        override suspend fun recordBootStartObserved(currentBoot: Int?) {
            launcherSystem.value = launcherSystem.value.copy(bootStart = launcherSystem.value.bootStart.copy(observedAtBoot = currentBoot))
        }
        override suspend fun recordAccessibilityConnected(currentBoot: Int) {
            launcherSystem.value = launcherSystem.value.copy(accessibilityConnectedAtBoot = currentBoot)
        }
    }

    private var opens = 0
    private var sinceBoot = 30_000L
    private var foreground = false

    private fun dispatcher(store: LauncherSystemStore, opens: () -> Boolean = { this.opens++; true }) =
        BootStartDispatcher(store, bootCount = { 4 }, sinceBootMs = { sinceBoot }, foreground = { foreground }, openHome = opens)

    @Test
    fun `the boot broadcast then the service connection open the launcher once the opening is observed`() = runTest {
        val store = Store(true)
        val dispatcher = dispatcher(store)
        assertTrue(dispatcher.onBootCompleted())
        store.recordBootStartObserved(4)
        assertFalse(dispatcher.onServiceConnected())
        assertFalse(dispatcher.onBootCompleted())
        assertEquals(1, opens)
    }

    @Test
    fun `a receiver start silently refused still lets the service path open the launcher`() = runTest {
        val dispatcher = dispatcher(Store(true))
        val both = listOf(async { dispatcher.onBootCompleted() }, async { dispatcher.onServiceConnected() }).map { it.await() }
        assertEquals(listOf(true, true), both)
        assertEquals(2, opens)
    }

    @Test
    fun `the service connection alone opens once`() = runTest {
        assertTrue(dispatcher(Store(true)).onServiceConnected())
        assertEquals(1, opens)
    }

    @Test
    fun `an opening already observed for this boot is not repeated by another process`() = runTest {
        val store = Store(true)
        store.recordBootStartObserved(4)
        assertFalse(dispatcher(store).onServiceConnected())
        assertFalse(dispatcher(store).onBootCompleted())
        assertEquals(0, opens)
    }

    @Test
    fun `a connection more than five minutes after the boot opens nothing`() = runTest {
        sinceBoot = BootStartDispatcher.WINDOW_MS
        assertFalse(dispatcher(Store(true)).onServiceConnected())
        assertFalse(dispatcher(Store(true)).onBootCompleted())
        assertEquals(0, opens)
    }

    @Test
    fun `a service enabled by the user during this boot opens nothing and keeps the boot unobserved`() = runTest {
        val first = Store(true, connectedAtBoot = null)
        assertFalse(dispatcher(first).onServiceConnected())
        assertEquals(4, first.launcherSystem.value.accessibilityConnectedAtBoot)
        assertFalse(dispatcher(first).onServiceConnected())
        val again = Store(true, connectedAtBoot = 4)
        assertFalse(dispatcher(again).onServiceConnected())
        assertEquals(0, opens)
        assertEquals(null, first.launcherSystem.value.bootStart.observedAtBoot)
        assertTrue(first.launcherSystem.value.bootStart.notObserved(4))
    }

    @Test
    fun `a service connection with SygixOs already in the foreground opens nothing`() = runTest {
        foreground = true
        val store = Store(true)
        assertFalse(dispatcher(store).onServiceConnected())
        assertEquals(0, opens)
        assertEquals(4, store.launcherSystem.value.accessibilityConnectedAtBoot)
    }

    @Test
    fun `a service enabled before the boot is remembered even when the option is disabled`() = runTest {
        val store = Store(false, connectedAtBoot = null)
        sinceBoot = BootStartDispatcher.WINDOW_MS
        assertFalse(dispatcher(store).onServiceConnected())
        assertEquals(4, store.launcherSystem.value.accessibilityConnectedAtBoot)
    }

    @Test
    fun `a disabled option opens nothing on either path`() = runTest {
        val dispatcher = dispatcher(Store(false))
        assertFalse(dispatcher.onBootCompleted())
        assertFalse(dispatcher.onServiceConnected())
        assertEquals(0, opens)
    }

    @Test
    fun `a refused opening keeps the other path possible and unreadable preferences never crash`() = runTest {
        var attempts = 0
        val refusing = dispatcher(Store(true)) { attempts++; throw SecurityException() }
        assertFalse(refusing.onBootCompleted())
        assertFalse(refusing.onServiceConnected())
        assertEquals(2, attempts)
        val broken = object : LauncherSystemStore by Store(true) {
            override val launcherSystem = flow<LauncherSystemPreferences> { throw IllegalStateException() }
        }
        assertFalse(dispatcher(broken).onBootCompleted())
        assertFalse(dispatcher(broken).onServiceConnected())
        assertEquals(0, opens)
    }
}
