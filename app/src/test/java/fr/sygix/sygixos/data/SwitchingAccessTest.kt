/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.TsuyuLink
import fr.sygix.sygixos.domain.TsuyuPermission
import fr.sygix.sygixos.domain.TsuyuProbe
import fr.sygix.sygixos.model.AdbAccessState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwitchingAccessTest {
    private class Link(var probe: TsuyuProbe) : TsuyuLink {
        override fun probe() = probe
        override suspend fun awaitBinder(timeoutMs: Long) = probe.binder
        override suspend fun requestPermission() = TsuyuPermission.DENIED
        override fun open() = false
    }

    private class Means(var debug: Boolean = false, var authorized: Boolean = false) : AdbAccess {
        override val state = MutableStateFlow(AdbAccessState.DISABLED)
        val executed = mutableListOf<String>()
        var connects = 0
        var closes = 0
        override fun debugEnabled() = debug
        override fun previouslyAuthorized() = authorized
        override suspend fun connect(): Result<Unit> { connects++; state.value = AdbAccessState.READY; return Result.success(Unit) }
        override suspend fun execute(command: String): Result<String> { executed += command; return Result.success("") }
        override fun close() { closes++; state.value = AdbAccessState.LOST }
    }

    private val active = TsuyuProbe(installed = true, binder = true, apiVersion = 13, authorized = true)
    private val link = Link(active)
    private val tsuyu = Means()
    private val adb = Means()
    private val access = SwitchingAccess(link, tsuyu, adb)

    @Test fun activeTsuyuIsChosenAndNoAdbConnectionIsOpened() = runTest {
        assertTrue(access.tsuyuActive())
        assertTrue(access.debugEnabled() && access.previouslyAuthorized())
        assertTrue(access.connect().isSuccess)
        assertTrue(access.usingTsuyu)
        access.execute("cmd appops write-settings")
        assertEquals(listOf("cmd appops write-settings"), tsuyu.executed)
        assertEquals(0, adb.connects)
        assertTrue(adb.executed.isEmpty())
        assertEquals(AdbAccessState.READY, access.state.value)
    }

    @Test fun inactiveTsuyuFallsBackToBuiltInAdb() = runTest {
        for (probe in listOf(active.copy(binder = false, apiVersion = null), active.copy(apiVersion = 12), active.copy(authorized = false))) {
            link.probe = probe
            assertFalse(access.tsuyuActive())
            assertTrue(access.connect().isSuccess)
            assertFalse(access.usingTsuyu)
        }
        assertEquals(3, adb.connects)
        assertEquals(0, tsuyu.connects)
    }

    @Test fun debugAndAuthorizationComeFromAdbWithoutTsuyu() {
        link.probe = active.copy(authorized = false)
        assertFalse(access.debugEnabled() || access.previouslyAuthorized())
        adb.debug = true
        adb.authorized = true
        assertTrue(access.debugEnabled() && access.previouslyAuthorized())
    }

    @Test fun meansIsChosenAgainAtEachConnection() = runTest {
        access.connect()
        link.probe = active.copy(binder = false)
        access.connect()
        assertFalse(access.usingTsuyu)
        assertEquals(1, tsuyu.closes)
        val adbCloses = adb.closes
        access.close()
        assertEquals(adbCloses + 1, adb.closes)
        assertEquals(1, tsuyu.closes)
        assertEquals(AdbAccessState.LOST, access.state.value)
    }

    @Test fun stateFollowsTheCurrentMeans() = runTest {
        val seen = mutableListOf<AdbAccessState>()
        val collecting = launch(UnconfinedTestDispatcher(testScheduler)) { access.state.toList(seen) }
        adb.state.value = AdbAccessState.WAITING
        tsuyu.state.value = AdbAccessState.REFUSED
        assertEquals(listOf(AdbAccessState.DISABLED, AdbAccessState.WAITING), seen)
        access.connect()
        assertEquals(AdbAccessState.LOST, adb.state.value)
        assertEquals(AdbAccessState.READY, seen.last())
        assertEquals(AdbAccessState.READY, access.state.value)
        tsuyu.state.value = AdbAccessState.LOST
        collecting.cancel()
        assertEquals(AdbAccessState.LOST, seen.last())
    }
}
