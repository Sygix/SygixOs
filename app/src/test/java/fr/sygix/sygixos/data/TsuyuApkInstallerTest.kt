/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Intent
import fr.sygix.sygixos.data.FakeTransport.Reply
import fr.sygix.sygixos.domain.InstallFailure
import fr.sygix.sygixos.domain.TsuyuInstallError
import fr.sygix.sygixos.domain.TsuyuInstallException
import fr.sygix.sygixos.domain.TsuyuInstallStep
import fr.sygix.sygixos.domain.TsuyuPackage
import fr.sygix.sygixos.domain.TsuyuRelease
import fr.sygix.sygixos.domain.TsuyuReleases
import fr.sygix.sygixos.domain.UpdateError
import fr.sygix.sygixos.domain.UpdateException
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TsuyuApkInstallerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val bytes = ByteArray(SIZE) { (it * 7 + 3).toByte() }
    private val releases = FakeReleases(release())
    private val transport = FakeTransport().apply { on(URL, Reply.Body(body = bytes)) }
    private val inspector = FakeInspector(ApkIdentity(SELF, 5, setOf(CERT)), TsuyuPackage, CERT).apply {
        identify = { ApkIdentity(TsuyuPackage, 1, setOf(CERT)) }
    }
    private var archiveCalls = 0
    private val gateway = FakeGateway()
    private val shown = mutableListOf<Intent>()
    private val steps = mutableListOf<TsuyuInstallStep>()
    private val dir: File by lazy { File(folder.root, "tsuyu") }
    private var includePrereleases = false

    init {
        inspector.onArchive = { archiveCalls++ }
    }

    private fun TestScope.installer() = TsuyuApkInstaller(
        releases = releases,
        includePrereleases = { includePrereleases },
        transport = transport,
        inspector = inspector,
        gateway = gateway,
        downloadDir = dir,
        userAgent = { "SygixOs/test" },
        showUserAction = { shown += it },
        io = StandardTestDispatcher(testScheduler),
    )

    private fun TestScope.start(installer: TsuyuApkInstaller): Deferred<Result<Unit>> =
        async { installer.install { steps += it } }.also { runCurrent() }

    private fun error(result: Result<Unit>): TsuyuInstallError? = (result.exceptionOrNull() as? TsuyuInstallException)?.error

    private fun residualFiles(): List<File> = dir.walkTopDown().filter { it.isFile }.toList()

    @Test
    fun `a verified apk is installed through a tsuyu session after the user confirms`() = runTest {
        includePrereleases = true
        var filesAtCommit: List<File>? = null
        gateway.onCommit = { filesAtCommit = residualFiles() }
        val installer = installer()
        val result = start(installer)
        val session = gateway.sessions.single()
        assertTrue(session.committed)
        assertEquals(emptyList<File>(), filesAtCommit)
        assertArrayEquals(bytes, session.written.toByteArray())
        val confirm = Intent("confirm")
        assertTrue(installer.claim(InstallStatus.PendingUserAction(session.id, confirm)))
        runCurrent()
        assertEquals(listOf(confirm), shown)
        assertFalse(result.isCompleted)
        assertTrue(installer.claim(InstallStatus.Success(session.id)))
        runCurrent()
        assertTrue(result.await().isSuccess)
        assertEquals(listOf(true), releases.requests)
        assertEquals(TsuyuInstallStep.Downloading(0), steps.first())
        assertTrue(steps.contains(TsuyuInstallStep.Downloading(100)))
        assertEquals(listOf(TsuyuInstallStep.Verifying, TsuyuInstallStep.Installing), steps.takeLast(2))
        assertEquals(emptyList<Int>(), gateway.abandonedIds)
        assertFalse(session.abandoned)
        assertEquals(emptyList<File>(), residualFiles())
    }

    @Test
    fun `a size different from the published one is refused before any inspection`() = runTest {
        releases.result = Result.success(release(size = SIZE + 1L))
        assertRejected()
    }

    @Test
    fun `a longer body than published is refused`() = runTest {
        releases.result = Result.success(release(size = SIZE - 1L, sha256 = sha256(bytes.copyOf(SIZE - 1))))
        assertRejected()
    }

    @Test
    fun `a digest different from the published one is refused before any inspection`() = runTest {
        releases.result = Result.success(release(sha256 = "00".repeat(32)))
        assertRejected()
    }

    @Test
    fun `an apk of another package is refused`() = runTest {
        inspector.identify = { ApkIdentity("fr.sygix.other", 1, setOf(CERT)) }
        assertRejected(inspected = true)
    }

    @Test
    fun `an unreadable apk is refused`() = runTest {
        inspector.identify = { null }
        assertRejected(inspected = true)
    }

    @Test
    fun `certificates different from those of sygixos are refused`() = runTest {
        inspector.identify = { ApkIdentity(TsuyuPackage, 1, setOf("other-cert")) }
        assertRejected(inspected = true)
    }

    @Test
    fun `empty certificates are refused even when both sides are empty`() = runTest {
        inspector.installed = ApkIdentity(SELF, 5, emptySet())
        inspector.identify = { ApkIdentity(TsuyuPackage, 1, emptySet()) }
        assertRejected(inspected = true)
    }

    @Test
    fun `an unknown sygixos identity is refused`() = runTest {
        inspector.installed = null
        assertRejected(inspected = true)
    }

    private suspend fun TestScope.assertRejected(inspected: Boolean = false) {
        val result = start(installer())
        assertEquals(TsuyuInstallError.INVALID, error(result.await()))
        assertEquals(emptyList<FakeSession>(), gateway.sessions)
        assertEquals(emptyList<File>(), residualFiles())
        assertEquals(if (inspected) 1 else 0, archiveCalls)
        assertTrue(steps.contains(TsuyuInstallStep.Verifying))
        assertFalse(steps.contains(TsuyuInstallStep.Installing))
    }

    @Test
    fun `a user refusal or a system failure is reported as refused`() = runTest {
        listOf<(Int) -> InstallStatus>(
            { InstallStatus.Aborted(it) },
            { InstallStatus.Failed(it, InstallFailure.CONFLICT) },
        ).forEach { status ->
            val installer = installer()
            val result = start(installer)
            val id = gateway.sessions.last().id
            installer.claim(InstallStatus.PendingUserAction(id, Intent("confirm")))
            runCurrent()
            installer.claim(status(id))
            runCurrent()
            assertEquals(TsuyuInstallError.REFUSED, error(result.await()))
            assertEquals(emptyList<File>(), residualFiles())
        }
    }

    @Test
    fun `returning without a status abandons the session after the grace delay`() = runTest {
        val installer = installer()
        val result = start(installer)
        val id = gateway.sessions.single().id
        installer.claim(InstallStatus.PendingUserAction(id, Intent("confirm")))
        runCurrent()
        installer.onForegroundReturn()
        testScheduler.advanceTimeBy(TsuyuApkInstaller.RETURN_GRACE_MS - 1)
        runCurrent()
        assertFalse(result.isCompleted)
        testScheduler.advanceTimeBy(2)
        runCurrent()
        assertTrue(result.isCompleted)
        assertEquals(TsuyuInstallError.REFUSED, error(result.await()))
        assertEquals(listOf(id), gateway.abandonedIds)
    }

    @Test
    fun `returning while the session is still active keeps waiting for its status`() = runTest {
        val installer = installer()
        val result = start(installer)
        val id = gateway.sessions.single().id
        installer.claim(InstallStatus.PendingUserAction(id, Intent("confirm")))
        runCurrent()
        gateway.active = true
        installer.onForegroundReturn()
        testScheduler.advanceTimeBy(TsuyuApkInstaller.RETURN_GRACE_MS * 3)
        runCurrent()
        assertFalse(result.isCompleted)
        assertEquals(emptyList<Int>(), gateway.abandonedIds)
        installer.claim(InstallStatus.Success(id))
        runCurrent()
        assertTrue(result.await().isSuccess)
    }

    @Test
    fun `a status arriving within the grace delay after returning is honoured`() = runTest {
        val installer = installer()
        val result = start(installer)
        val id = gateway.sessions.single().id
        installer.claim(InstallStatus.PendingUserAction(id, Intent("confirm")))
        runCurrent()
        installer.onForegroundReturn()
        testScheduler.advanceTimeBy(TsuyuApkInstaller.RETURN_GRACE_MS / 2)
        installer.claim(InstallStatus.Success(id))
        runCurrent()
        assertTrue(result.await().isSuccess)
        assertEquals(emptyList<Int>(), gateway.abandonedIds)
    }

    @Test
    fun `returning before any user action is requested changes nothing`() = runTest {
        val installer = installer()
        val result = start(installer)
        val id = gateway.sessions.single().id
        installer.onForegroundReturn()
        testScheduler.advanceTimeBy(TsuyuApkInstaller.RETURN_GRACE_MS * 3)
        runCurrent()
        assertFalse(result.isCompleted)
        installer.claim(InstallStatus.Success(id))
        runCurrent()
        assertTrue(result.await().isSuccess)
    }

    @Test
    fun `a user action without a screen is refused and abandoned`() = runTest {
        val installer = installer()
        val result = start(installer)
        val id = gateway.sessions.single().id
        installer.claim(InstallStatus.PendingUserAction(id, null))
        runCurrent()
        assertEquals(TsuyuInstallError.REFUSED, error(result.await()))
        assertEquals(listOf(id), gateway.abandonedIds)
    }

    @Test
    fun `no status at all ends as refused after the global delay`() = runTest {
        val result = start(installer())
        testScheduler.advanceTimeBy(TsuyuApkInstaller.STATUS_TIMEOUT_MS + 1)
        runCurrent()
        assertEquals(TsuyuInstallError.REFUSED, error(result.await()))
        assertEquals(listOf(gateway.sessions.single().id), gateway.abandonedIds)
    }

    @Test
    fun `network failures while reading the release are reported as no network`() = runTest {
        listOf(UpdateError.NoNetwork, UpdateError.Timeout, UpdateError.SecureConnection, UpdateError.RateLimited(0)).forEach { failure ->
            releases.result = Result.failure(UpdateException(failure))
            assertEquals(failure.toString(), TsuyuInstallError.NO_NETWORK, error(start(installer()).await()))
        }
        assertEquals(0, transport.calls.size)
    }

    @Test
    fun `a release that is no longer eligible is invalid`() = runTest {
        releases.result = Result.success(null)
        assertEquals(TsuyuInstallError.INVALID, error(start(installer()).await()))
        assertEquals(0, transport.calls.size)
    }

    @Test
    fun `a download failure is reported as no network and leaves no file`() = runTest {
        listOf(Reply.Fail(IOException("reset")), Reply.Body(body = bytes, failAfter = SIZE / 2), Reply.Body(status = 503)).forEach { reply ->
            transport.on(URL, reply)
            assertEquals(reply.toString(), TsuyuInstallError.NO_NETWORK, error(start(installer()).await()))
            assertEquals(emptyList<File>(), residualFiles())
        }
        assertEquals(emptyList<FakeSession>(), gateway.sessions)
    }

    @Test
    fun `an asset gone from the server is invalid`() = runTest {
        transport.on(URL, Reply.Body(status = 404))
        assertEquals(TsuyuInstallError.INVALID, error(start(installer()).await()))
    }

    @Test
    fun `statuses of other sessions are left to the update and ignored`() = runTest {
        val installer = installer()
        val result = start(installer)
        val id = gateway.sessions.single().id
        val routed = mutableListOf<InstallStatus>()
        val router = InstallStatusRouter(listOf(installer), routed::add)
        router.route(InstallStatus.Aborted(id + 10))
        router.route(InstallStatus.Success(id + 11))
        runCurrent()
        assertFalse(result.isCompleted)
        assertEquals(listOf(InstallStatus.Aborted(id + 10), InstallStatus.Success(id + 11)), routed)
        router.route(InstallStatus.Success(id))
        runCurrent()
        assertTrue(result.await().isSuccess)
        assertEquals(2, routed.size)
    }

    @Test
    fun `late statuses of a finished tsuyu session stay with tsuyu`() = runTest {
        val installer = installer()
        val result = start(installer)
        val id = gateway.sessions.single().id
        installer.claim(InstallStatus.Aborted(id))
        runCurrent()
        result.await()
        val routed = mutableListOf<InstallStatus>()
        InstallStatusRouter(listOf(installer), routed::add).route(InstallStatus.Failed(id, InstallFailure.OTHER))
        assertEquals(emptyList<InstallStatus>(), routed)
    }

    @Test
    fun `cancelling while waiting for android abandons the session and removes files`() = runTest {
        val result = start(installer())
        val id = gateway.sessions.single().id
        result.cancel()
        runCurrent()
        assertTrue(result.isCancelled)
        assertEquals(listOf(id), gateway.abandonedIds)
        assertEquals(emptyList<File>(), residualFiles())
    }

    @Test
    fun `cancelling during the download removes the partial file and opens no session`() = runTest {
        lateinit var result: Deferred<Result<Unit>>
        val installer = installer()
        var cancelled = false
        result = async {
            installer.install { step ->
                if (!cancelled && step is TsuyuInstallStep.Downloading && step.percent >= 50) {
                    cancelled = true
                    result.cancel()
                }
            }
        }
        runCurrent()
        assertTrue(result.isCancelled)
        assertEquals(emptyList<FakeSession>(), gateway.sessions)
        assertEquals(emptyList<File>(), residualFiles())
    }

    private fun release(size: Long = SIZE.toLong(), sha256: String = sha256(bytes)) = TsuyuRelease("v1.0.0", false, URL, size, sha256)

    private fun sha256(data: ByteArray): String = UpdateInstaller.hex(MessageDigest.getInstance("SHA-256").digest(data))

    private class FakeReleases(release: TsuyuRelease?) : TsuyuReleases {
        var result: Result<TsuyuRelease?> = Result.success(release)
        val requests = mutableListOf<Boolean>()
        override suspend fun refresh(includePrereleases: Boolean): Result<TsuyuRelease?> {
            requests += includePrereleases
            return result
        }
        override suspend fun lastKnown(): TsuyuRelease? = result.getOrNull()
    }

    private companion object {
        const val SIZE = 4_096
        const val SELF = "fr.sygix.sygixos"
        const val CERT = "release-cert"
        const val URL = "https://github.com/Sygix/Tsuyu/releases/download/v1.0.0/tsuyu.apk"
    }
}
