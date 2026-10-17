/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Intent
import fr.sygix.sygixos.domain.TsuyuPermission
import fr.sygix.sygixos.domain.TsuyuProbe
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShizukuTsuyuLinkTest {
    private val api = FakeShizukuApi()
    private var installed = true
    private val launched = mutableListOf<Intent>()
    private val link = ShizukuTsuyuLink(api, { installed }, { launched += it; true })

    @Test fun probeReadsEveryFact() {
        assertEquals(TsuyuProbe(installed = true, binder = true, apiVersion = 13, authorized = true), link.probe())
        assertTrue(link.active())
        api.permission = false
        api.denied = true
        assertEquals(TsuyuProbe(installed = true, binder = true, apiVersion = 13, authorized = false, permanentlyDenied = true), link.probe())
        assertFalse(link.active())
    }

    @Test fun probeWithoutBinderAsksNothingElse() {
        installed = false
        api.binder = false
        assertEquals(TsuyuProbe(), link.probe())
    }

    @Test fun outdatedApiIsNotActive() {
        api.apiVersion = 12
        assertEquals(12, link.probe().apiVersion)
        assertFalse(link.active())
    }

    @Test fun binderAlreadyReceivedIsReturnedAtOnce() = runTest {
        assertTrue(link.awaitBinder(3_000))
        assertTrue(api.received.isEmpty())
    }

    @Test fun binderReceivedWhileWaiting() = runTest {
        api.binder = false
        val waiting = async { link.awaitBinder(3_000) }
        runCurrent()
        api.receive()
        assertTrue(waiting.await())
        assertTrue(api.received.isEmpty())
    }

    @Test fun binderNeverReceivedTimesOut() = runTest {
        api.binder = false
        assertFalse(link.awaitBinder(3_000))
        assertTrue(api.received.isEmpty())
    }

    @Test fun permanentRefusalIsReportedWithoutAsking() = runTest {
        api.permission = false
        api.denied = true
        assertEquals(TsuyuPermission.NO_MORE_REQUESTS, link.requestPermission())
        assertTrue(api.requests.isEmpty())
    }

    @Test fun grantedPermissionIsNotRequested() = runTest {
        assertEquals(TsuyuPermission.GRANTED, link.requestPermission())
        assertTrue(api.requests.isEmpty())
    }

    @Test fun requestWaitsForItsOwnResult() = runTest {
        api.permission = false
        val asked = async { link.requestPermission() }
        runCurrent()
        assertEquals(listOf(ShizukuTsuyuLink.RequestCode), api.requests)
        api.answer(ShizukuTsuyuLink.RequestCode + 1, true)
        runCurrent()
        assertFalse(asked.isCompleted)
        api.answer(ShizukuTsuyuLink.RequestCode, true)
        assertEquals(TsuyuPermission.GRANTED, asked.await())
        assertTrue(api.results.isEmpty())
    }

    @Test fun refusalDistinguishesFinalRefusal() = runTest {
        api.permission = false
        val asked = async { link.requestPermission() }
        runCurrent()
        api.answer(ShizukuTsuyuLink.RequestCode, false)
        assertEquals(TsuyuPermission.DENIED, asked.await())
        val again = async { link.requestPermission() }
        runCurrent()
        api.denied = true
        api.answer(ShizukuTsuyuLink.RequestCode, false)
        assertEquals(TsuyuPermission.NO_MORE_REQUESTS, again.await())
    }

    @Test fun deadBinderEndsTheRequest() = runTest {
        api.permission = false
        val asked = async { link.requestPermission() }
        runCurrent()
        api.die()
        assertEquals(TsuyuPermission.DENIED, asked.await())
    }

    @Test fun openUsesAnExplicitIntentInTheCallerTask() {
        assertTrue(link.open())
        val intent = launched.single()
        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertEquals("fr.sygix.tsuyu", intent.component?.packageName)
        assertEquals("fr.sygix.tsuyu.MainActivity", intent.component?.className)
        assertEquals(0, intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test fun openFailureIsReported() {
        assertFalse(ShizukuTsuyuLink(api, { true }, { throw IllegalStateException("No activity") }).open())
        assertFalse(ShizukuTsuyuLink(api, { true }, { false }).open())
    }
}
