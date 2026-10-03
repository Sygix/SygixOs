/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Intent
import fr.sygix.sygixos.data.FakeTransport.Reply
import fr.sygix.sygixos.data.UpdateHarness.Companion.HOUR
import fr.sygix.sygixos.data.UpdateHarness.Companion.LIST_URL
import fr.sygix.sygixos.data.UpdateHarness.Companion.tagUrl
import fr.sygix.sygixos.domain.CheckResult
import fr.sygix.sygixos.domain.InstallDetail
import fr.sygix.sygixos.domain.InstallFailure
import fr.sygix.sygixos.domain.UpdateError
import fr.sygix.sygixos.domain.UpdateStatusText
import fr.sygix.sygixos.domain.UpdateStep
import fr.sygix.sygixos.domain.asset
import fr.sygix.sygixos.domain.release
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UpdateRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.harness(installedCode: Long = 199, store: UpdateStore = MemoryUpdateStore()): UpdateHarness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return UpdateHarness(CoroutineScope(SupervisorJob() + dispatcher), dispatcher, folder.root.resolve("updates"), store, installedCode)
    }

    private fun TestScope.checked(h: UpdateHarness): UpdateHarness {
        h.repository.status
        h.repository.check()
        advanceUntilIdle()
        return h
    }

    private fun TestScope.update(h: UpdateHarness, tag: String) {
        h.repository.startUpdate(tag)
        advanceUntilIdle()
    }

    private fun UpdateHarness.step() = repository.status.value.step

    private fun UpdateHarness.failure() = (step() as? UpdateStep.Failed)?.error

    @Test
    fun `check proposes the new version and the badge appears`() = runTest {
        val h = harness().apply { publish("v0.0.1", "v0.0.2") }
        checked(h)
        val status = h.repository.status.value
        assertEquals("v0.0.2", status.proposed?.tag)
        assertTrue(status.badge)
        assertEquals(CheckResult.Ok, status.lastResult)
        assertEquals(1, h.listCalls())
    }

    @Test
    fun `checking state is exposed and a second press sends no second request`() = runTest {
        val h = harness().apply { publish("v0.0.2") }
        val seen = mutableListOf<Boolean>()
        h.scope.launch { h.repository.status.collect { seen += it.checking } }
        testScheduler.runCurrent()
        h.repository.check()
        h.repository.check()
        advanceUntilIdle()
        assertTrue(seen.toString(), true in seen)
        assertFalse(h.repository.status.value.checking)
        assertEquals(1, h.listCalls())
    }

    @Test
    fun `nominal update reads the release again, downloads, verifies and commits the verified bytes`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        var filesAtCommit: List<java.io.File>? = null
        h.gateway.onCommit = { filesAtCommit = h.residualFiles() }
        update(h, "v0.0.2")
        val session = h.gateway.sessions.single()
        assertTrue(session.committed)
        assertFalse(session.abandoned)
        assertTrue(h.apkBytes("v0.0.2").contentEquals(session.written.toByteArray()))
        assertEquals(emptyList<java.io.File>(), filesAtCommit)
        assertEquals(1, h.transport.calls.count { it.first == tagUrl("v0.0.2") })
        assertEquals(1, h.apkCalls("v0.0.2"))
        assertTrue(h.step() is UpdateStep.Installing)
        assertTrue(h.residualFiles().isEmpty())
        assertEquals(true, (h.store as MemoryUpdateStore).relaunch)
    }

    @Test
    fun `installation in the background asks for no relaunch`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.foreground.isForeground = false
        update(h, "v0.0.2")
        assertTrue(h.gateway.sessions.single().committed)
        assertEquals(false, (h.store as MemoryUpdateStore).relaunch)
    }

    @Test
    fun `steps go from download progress to verification then installation`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        val steps = mutableListOf<UpdateStep>()
        val outcome = h.installer.run(requireNotNull(h.repository.status.value.proposed)) { steps += it }
        assertTrue(outcome is InstallOutcome.Committed)
        val percents = steps.filterIsInstance<UpdateStep.Downloading>().map { it.percent }
        assertEquals(0, percents.first())
        assertEquals(100, percents.last())
        assertEquals(percents.sorted().distinct(), percents)
        assertTrue(percents.size <= 101)
        assertTrue(steps.dropWhile { it is UpdateStep.Downloading }.map { it::class } == listOf(UpdateStep.Verifying::class, UpdateStep.Installing::class))
    }

    @Test
    fun `invalid digest at download never reaches the installer and leaves no file`() = runTest {
        val h = harness()
        val bad = h.releaseOf("v0.0.2", bytes = ByteArray(UpdateHarness.APK_SIZE) { 7 })
        h.publishReleases(bad)
        checked(h)
        update(h, "v0.0.2")
        assertEquals(UpdateError.Corrupt, h.failure())
        assertTrue(h.gateway.sessions.isEmpty())
        assertTrue(h.residualFiles().isEmpty())
        assertEquals("v0.0.2", h.repository.status.value.proposed?.tag)
    }

    @Test
    fun `altered copy into the session is abandoned and never committed`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.inspector.onArchive = { file -> file.writeBytes(ByteArray(UpdateHarness.APK_SIZE) { 3 }) }
        update(h, "v0.0.2")
        val session = h.gateway.sessions.single()
        assertTrue(session.abandoned)
        assertFalse(session.committed)
        assertEquals(UpdateError.Corrupt, h.failure())
        assertTrue(h.residualFiles().isEmpty())
    }

    @Test
    fun `different certificate is refused before the installer`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.archiveAs(certificates = setOf("debug-cert"))
        update(h, "v0.0.2")
        assertEquals(UpdateError.SignatureMismatch, h.failure())
        assertTrue(h.gateway.sessions.isEmpty())
        assertTrue(h.residualFiles().isEmpty())
    }

    @Test
    fun `other package, other version code or unreadable apk is an inconsistent release`() = runTest {
        val cases = listOf<(UpdateHarness) -> Unit>(
            { it.archiveAs(packageName = "com.other") },
            { it.archiveAs(versionCode = 298) },
            { it.archiveAs(versionCode = 300) },
            { it.inspector.installed = ApkIdentity(UpdateHarness.PACKAGE, 299, setOf(UpdateHarness.CERT)) },
            { it.inspector.identify = { null } },
        )
        cases.forEach { arrange ->
            val h = checked(harness().apply { publish("v0.0.2") })
            arrange(h)
            update(h, "v0.0.2")
            assertEquals(UpdateError.Inconsistent, h.failure())
            assertTrue(h.gateway.sessions.isEmpty())
            assertTrue(h.residualFiles().isEmpty())
        }
    }

    @Test
    fun `too many or too few bytes are a corrupt file`() = runTest {
        listOf(UpdateHarness.APK_SIZE + 10, UpdateHarness.APK_SIZE - 10).forEach { served ->
            val h = checked(harness().apply { publish("v0.0.2") })
            h.transport.on(h.apkUrl("v0.0.2"), Reply.Body(body = h.apkBytes("v0.0.2").copyOf(served)))
            update(h, "v0.0.2")
            assertEquals(UpdateError.Corrupt, h.failure())
            assertTrue(h.gateway.sessions.isEmpty())
            assertTrue(h.residualFiles().isEmpty())
        }
    }

    @Test
    fun `missing asset at download keeps the version proposed`() = runTest {
        listOf(404, 410).forEach { code ->
            val h = checked(harness().apply { publish("v0.0.2") })
            h.transport.on(h.apkUrl("v0.0.2"), Reply.Body(status = code))
            update(h, "v0.0.2")
            assertEquals(UpdateError.AssetNotFound, h.failure())
            assertTrue(h.gateway.sessions.isEmpty())
            assertTrue(h.residualFiles().isEmpty())
            assertEquals("v0.0.2", h.repository.status.value.proposed?.tag)
            assertTrue(h.repository.status.value.badge)
        }
    }

    @Test
    fun `redirect to http is refused and nothing is installed`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.transport.on(h.apkUrl("v0.0.2"), Reply.Body(body = h.apkBytes("v0.0.2"), finalUrl = "http://release-assets.githubusercontent.com/app-release.apk"))
        update(h, "v0.0.2")
        assertEquals(UpdateError.Interrupted, h.failure())
        assertTrue(h.gateway.sessions.isEmpty())
        assertTrue(h.residualFiles().isEmpty())
    }

    @Test
    fun `lost connection deletes the partial file and a new press starts again from the beginning`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.transport.on(h.apkUrl("v0.0.2"), Reply.Body(body = h.apkBytes("v0.0.2"), failAfter = 1_024))
        update(h, "v0.0.2")
        assertEquals(UpdateError.Interrupted, h.failure())
        assertTrue(h.residualFiles().isEmpty())
        h.transport.on(h.apkUrl("v0.0.2"), Reply.Body(body = h.apkBytes("v0.0.2")))
        update(h, "v0.0.2")
        assertEquals(2, h.apkCalls("v0.0.2"))
        assertTrue(h.gateway.sessions.single().committed)
    }

    @Test
    fun `not enough space downloads nothing`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.freeSpace = 2L * UpdateHarness.APK_SIZE - 1
        update(h, "v0.0.2")
        assertEquals(UpdateError.NoSpace, h.failure())
        assertEquals(0, h.apkCalls("v0.0.2"))
    }

    @Test
    fun `withdrawn release is removed from the known versions and the badge is recalculated`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.transport.on(tagUrl("v0.0.2"), Reply.Body(status = 404))
        update(h, "v0.0.2")
        val status = h.repository.status.value
        assertNull(status.proposed)
        assertFalse(status.badge)
        assertEquals(0, h.apkCalls("v0.0.2"))
        assertEquals(InstallDetail.Failed(UpdateError.Withdrawn), UpdateStatusText.installLine(status)?.detail)
        assertNull((h.store as MemoryUpdateStore).state.value.known.bestAny)
    }

    @Test
    fun `release no longer eligible on reread is withdrawn`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        val noDigest = release("v0.0.2", assets = listOf(asset(digest = null, url = h.apkUrl("v0.0.2"))))
        h.transport.on(tagUrl("v0.0.2"), Reply.Body(body = releaseJson(noDigest).toString().toByteArray()))
        update(h, "v0.0.2")
        assertEquals(UpdateError.Withdrawn, h.failure())
        assertFalse(h.repository.status.value.badge)
    }

    @Test
    fun `changed release on reread uses the new url and digest`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        val newBytes = ByteArray(UpdateHarness.APK_SIZE + 5) { 9 }
        val moved = "https://github.com/Sygix/SygixOs/releases/download/v0.0.2/moved.apk"
        h.transport.on(tagUrl("v0.0.2"), Reply.Body(body = releaseJson(h.releaseOf("v0.0.2", bytes = newBytes, url = moved)).toString().toByteArray()))
        h.transport.on(moved, Reply.Body(body = newBytes))
        update(h, "v0.0.2")
        assertEquals(0, h.apkCalls("v0.0.2"))
        assertTrue(newBytes.contentEquals(h.gateway.sessions.single().written.toByteArray()))
        assertTrue(h.gateway.sessions.single().committed)
    }

    @Test
    fun `failed reread keeps the version proposed and persists the retry time`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.transport.on(tagUrl("v0.0.2"), Reply.Body(status = 403, headers = mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "${(h.now + HOUR) / 1000}")))
        update(h, "v0.0.2")
        assertEquals(UpdateError.RateLimited(h.now + HOUR), h.failure())
        assertEquals(h.now + HOUR, (h.store as MemoryUpdateStore).state.value.retryAt)
        assertEquals("v0.0.2", h.repository.status.value.proposed?.tag)
        assertEquals(0, h.apkCalls("v0.0.2"))
        update(h, "v0.0.2")
        assertEquals(1, h.transport.calls.count { it.first == tagUrl("v0.0.2") })
    }

    @Test
    fun `press during an operation has no effect`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.repository.startUpdate("v0.0.2")
        h.repository.startUpdate("v0.0.2")
        advanceUntilIdle()
        h.repository.startUpdate("v0.0.2")
        advanceUntilIdle()
        assertEquals(1, h.transport.calls.count { it.first == tagUrl("v0.0.2") })
        assertEquals(1, h.gateway.sessions.size)
    }

    @Test
    fun `update of a version that is not proposed is ignored`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        update(h, "v0.0.9")
        assertNull(h.step())
        assertEquals(0, h.transport.calls.count { it.first == tagUrl("v0.0.9") })
    }

    @Test
    fun `operation goes on when nobody watches the state any more`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        val ui = h.scope.launch { h.repository.status.collect { } }
        h.repository.startUpdate("v0.0.2")
        testScheduler.runCurrent()
        ui.cancel()
        advanceUntilIdle()
        assertTrue(h.gateway.sessions.single().committed)
    }

    @Test
    fun `user action in the foreground shows the system screen at once`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        update(h, "v0.0.2")
        val screen = Intent("android.content.pm.action.CONFIRM_INSTALL")
        h.repository.onInstallStatus(InstallStatus.PendingUserAction(1, screen))
        assertEquals(listOf(screen.action), h.screens.launched.map { it.action })
    }

    @Test
    fun `user action in the background waits for the return to the foreground`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        update(h, "v0.0.2")
        h.foreground.isForeground = false
        h.repository.onInstallStatus(InstallStatus.PendingUserAction(1, Intent("confirm")))
        assertTrue(h.screens.launched.isEmpty())
        h.foreground.isForeground = true
        h.repository.onForeground()
        assertEquals(listOf("confirm"), h.screens.launched.map { it.action })
        h.repository.onForeground()
        assertEquals(1, h.screens.launched.size)
    }

    @Test
    fun `return from the system screen is signalled once`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        update(h, "v0.0.2")
        h.repository.onInstallStatus(InstallStatus.PendingUserAction(1, Intent("confirm")))
        assertEquals(0, h.repository.systemScreenReturns.value)
        h.repository.onForeground()
        assertEquals(1, h.repository.systemScreenReturns.value)
        h.repository.onForeground()
        assertEquals(1, h.repository.systemScreenReturns.value)
    }

    @Test
    fun `unavailable system screen abandons the session`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        update(h, "v0.0.2")
        h.screens.available = false
        h.repository.onInstallStatus(InstallStatus.PendingUserAction(7, Intent("confirm")))
        advanceUntilIdle()
        assertEquals(listOf(7), h.gateway.abandonedIds)
        assertEquals(UpdateError.SystemScreenUnavailable, h.failure())
    }

    @Test
    fun `refused or failed installation keeps the version proposed`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        update(h, "v0.0.2")
        h.repository.onInstallStatus(InstallStatus.Aborted)
        advanceUntilIdle()
        assertEquals(UpdateError.InstallAborted, h.failure())
        assertEquals("v0.0.2", h.repository.status.value.proposed?.tag)
        update(h, "v0.0.2")
        h.repository.onInstallStatus(InstallStatus.Failed(InstallFailure.STORAGE))
        advanceUntilIdle()
        assertEquals(UpdateError.InstallFailed(InstallFailure.STORAGE), h.failure())
        assertTrue(h.repository.status.value.badge)
        assertEquals(2, h.gateway.sessions.size)
    }

    @Test
    fun `switching prereleases during an operation keeps the version being installed`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2", "v0.0.3-rc.1") })
        h.repository.startUpdate("v0.0.2")
        testScheduler.runCurrent()
        h.repository.setIncludePrereleases(true)
        testScheduler.runCurrent()
        assertEquals("v0.0.2", h.repository.status.value.proposed?.tag)
        advanceUntilIdle()
        assertEquals("v0.0.2", h.repository.status.value.proposed?.tag)
        assertTrue(h.apkBytes("v0.0.2").contentEquals(h.gateway.sessions.single().written.toByteArray()))
        h.repository.onInstallStatus(InstallStatus.Aborted)
        advanceUntilIdle()
        assertEquals("v0.0.3-rc.1", h.repository.status.value.proposed?.tag)
    }

    @Test
    fun `cold start removes leftover files and abandons the open sessions`() = runTest {
        val h = harness()
        h.updatesDir.mkdirs()
        h.updatesDir.resolve("v0.0.2.apk.part").writeBytes(ByteArray(10))
        h.repository.coldStart()
        advanceUntilIdle()
        assertTrue(h.residualFiles().isEmpty())
        assertEquals(1, h.gateway.abandonAllCalls)
    }

    @Test
    fun `prerelease switch changes the proposed version without any request`() = runTest {
        val h = checked(harness().apply { publish("v0.0.1", "v0.0.2-rc.1") })
        assertNull(h.repository.status.value.proposed)
        assertFalse(h.repository.status.value.badge)
        val calls = h.transport.calls.size
        h.repository.setIncludePrereleases(true)
        advanceUntilIdle()
        assertEquals("v0.0.2-rc.1", h.repository.status.value.proposed?.tag)
        assertTrue(h.repository.status.value.badge)
        h.repository.setIncludePrereleases(false)
        advanceUntilIdle()
        assertNull(h.repository.status.value.proposed)
        assertEquals(calls, h.transport.calls.size)
    }

    @Test
    fun `unreadable answer keeps the known versions`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.transport.on(LIST_URL, Reply.Body(body = "oops".toByteArray()))
        h.repository.check()
        advanceUntilIdle()
        assertEquals(CheckResult.Error(UpdateError.Unreadable), h.repository.status.value.lastResult)
        assertEquals("v0.0.2", h.repository.status.value.proposed?.tag)
    }

    @Test
    fun `every api answer gives the expected result`() = runTest {
        val cases = listOf(
            Reply.Body(body = "[]".toByteArray()) to CheckResult.Ok,
            Reply.Body(body = releasesJson(release("v0.0.5", assets = emptyList())).toByteArray()) to CheckResult.Ok,
            Reply.Body(body = releasesJson(release("v0.0.5", assets = listOf(asset(digest = null)))).toByteArray()) to CheckResult.Ok,
            Reply.Body(body = "{".toByteArray()) to CheckResult.Error(UpdateError.Unreadable),
            Reply.Body(status = 404) to CheckResult.Error(UpdateError.Unavailable(404)),
            Reply.Fail(java.net.UnknownHostException()) to CheckResult.Error(UpdateError.NoNetwork),
            Reply.Fail(java.net.SocketTimeoutException()) to CheckResult.Error(UpdateError.Timeout),
        )
        cases.forEach { (reply, expected) ->
            val h = harness()
            h.transport.on(LIST_URL, reply)
            checked(h)
            assertEquals(expected, h.repository.status.value.lastResult)
            assertNull(h.repository.status.value.proposed)
            assertEquals(h.now, (h.store as MemoryUpdateStore).state.value.lastCheckAt)
        }
    }

    @Test
    fun `rate limit is persisted and no request is sent before the retry time`() = runTest {
        val h = harness()
        h.transport.on(LIST_URL, Reply.Body(status = 403, headers = mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "${(h.now + HOUR) / 1000}")))
        checked(h)
        val limited = CheckResult.Error(UpdateError.RateLimited(h.now + HOUR))
        assertEquals(limited, h.repository.status.value.lastResult)
        assertEquals(h.now + HOUR, (h.store as MemoryUpdateStore).state.value.retryAt)
        h.now += HOUR / 2
        checked(h)
        assertEquals(1, h.listCalls())
        assertEquals(limited, h.repository.status.value.lastResult)
        h.now += HOUR
        h.publish("v0.0.2")
        checked(h)
        assertEquals(2, h.listCalls())
    }

    @Test
    fun `version installed by other means is no longer proposed after a restart`() = runTest {
        val store = MemoryUpdateStore()
        checked(harness(installedCode = 199, store = store).apply { publish("v0.0.2") })
        val restarted = harness(installedCode = 299, store = store)
        restarted.repository.status
        advanceUntilIdle()
        assertNull(restarted.repository.status.value.proposed)
        assertFalse(restarted.repository.status.value.badge)
        val earlier = harness(installedCode = 199, store = store)
        earlier.repository.status
        advanceUntilIdle()
        assertTrue(earlier.repository.status.value.badge)
    }

    @Test
    fun `automatic check runs after the home is shown when due and online`() = runTest {
        val h = harness().apply { publish("v0.0.2") }
        h.repository.status
        h.repository.onForeground()
        advanceUntilIdle()
        assertEquals(0, h.listCalls())
        h.repository.onHomeShown()
        advanceUntilIdle()
        assertEquals(1, h.listCalls())
        val persisted = (h.store as MemoryUpdateStore).state.value
        assertEquals(h.now, persisted.lastCheckAt)
        assertEquals(CheckResult.Ok, persisted.lastResult)
        assertTrue(h.repository.status.value.badge)
        assertEquals(0, h.apkCalls("v0.0.2"))
        assertTrue(h.gateway.sessions.isEmpty())
    }

    @Test
    fun `automatic check without a validated network sends nothing and records no date`() = runTest {
        val h = harness().apply { publish("v0.0.2") }
        h.network.validated = false
        h.repository.onHomeShown()
        advanceUntilIdle()
        assertEquals(0, h.listCalls())
        assertNull((h.store as MemoryUpdateStore).state.value.lastCheckAt)
        h.network.validated = true
        h.repository.onForeground()
        advanceUntilIdle()
        assertEquals(1, h.listCalls())
    }

    @Test
    fun `automatic check waits a day and then runs on a return to the foreground`() = runTest {
        val h = harness().apply { publish("v0.0.2") }
        h.repository.onHomeShown()
        advanceUntilIdle()
        h.now += 3 * HOUR
        h.repository.onForeground()
        advanceUntilIdle()
        assertEquals(1, h.listCalls())
        h.now += 23 * HOUR
        h.repository.onForeground()
        advanceUntilIdle()
        assertEquals(2, h.listCalls())
    }

    @Test
    fun `automatic check is skipped before the retry time`() = runTest {
        val store = MemoryUpdateStore()
        val h = harness(store = store).apply { publish("v0.0.2") }
        store.state.value = store.state.value.copy(retryAt = h.now + HOUR)
        h.repository.onHomeShown()
        advanceUntilIdle()
        assertEquals(0, h.listCalls())
    }

    @Test
    fun `failed automatic check is silent and persisted with its date`() = runTest {
        val h = checked(harness().apply { publish("v0.0.2") })
        h.now += 25 * HOUR
        h.transport.on(LIST_URL, Reply.Fail(java.net.SocketTimeoutException()))
        h.repository.onHomeShown()
        advanceUntilIdle()
        val persisted = (h.store as MemoryUpdateStore).state.value
        assertEquals(CheckResult.Error(UpdateError.Timeout), persisted.lastResult)
        assertEquals(h.now, persisted.lastCheckAt)
        assertEquals("v0.0.2", persisted.known.bestFinal?.tag)
        assertTrue(h.screens.launched.isEmpty())
    }
}
