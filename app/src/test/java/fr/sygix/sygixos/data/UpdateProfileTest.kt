/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.data.FakeTransport.Reply
import fr.sygix.sygixos.data.UpdateHarness.Companion.sha256
import fr.sygix.sygixos.domain.DexMetadata
import fr.sygix.sygixos.domain.InstallFailure
import fr.sygix.sygixos.domain.UpdateError
import fr.sygix.sygixos.domain.Release
import fr.sygix.sygixos.domain.UpdateSelector
import fr.sygix.sygixos.domain.UpdateStep
import fr.sygix.sygixos.domain.asset
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
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
class UpdateProfileTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val tag = "v0.0.2"
    private val profileUrl = "https://github.com/Sygix/SygixOs/releases/download/$tag/app-release.dm"
    private val profile = dexMetadata(UpdateSelector.PROFILE_NAME.length)

    private fun dexMetadata(seed: Int, entries: List<String> = listOf(DexMetadata.PROFILE_ENTRY, DexMetadata.METADATA_ENTRY)): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            entries.forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(ByteArray(64) { (it + seed).toByte() })
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun TestScope.harness(): UpdateHarness {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return UpdateHarness(CoroutineScope(SupervisorJob() + dispatcher), dispatcher, folder.root.resolve("updates"))
    }

    private fun UpdateHarness.publishWithProfile(
        served: ByteArray = profile,
        digest: String? = "sha256:" + sha256(profile),
        size: Long = profile.size.toLong(),
        reply: Reply = Reply.Body(body = served),
        url: String = profileUrl,
        state: String = "uploaded",
    ) {
        val apk = releaseOf(tag)
        val release = Release(
            tag = apk.tag,
            draft = false,
            prerelease = false,
            htmlUrl = apk.htmlUrl,
            assets = apk.assets + asset(name = UpdateSelector.PROFILE_NAME, state = state, size = size, digest = digest, url = url),
        )
        publishReleases(release)
        transport.on(url, reply)
    }

    private fun TestScope.install(h: UpdateHarness) {
        h.repository.status
        h.repository.check()
        advanceUntilIdle()
        h.repository.startUpdate(tag)
        advanceUntilIdle()
    }

    private fun UpdateHarness.profileCalls() = transport.calls.count { it.first == profileUrl }

    @Test
    fun `verified profile is written in the same session as the apk`() = runTest {
        val h = harness().apply { publishWithProfile() }
        install(h)
        val session = h.gateway.sessions.single()
        assertTrue(session.committed)
        assertTrue(profile.contentEquals(session.profile))
        assertTrue(h.apkBytes(tag).contentEquals(session.written.toByteArray()))
        assertEquals(1, h.profileCalls())
        assertTrue(h.residualFiles().isEmpty())
        assertTrue(h.repository.status.value.step is UpdateStep.Installing)
    }

    @Test
    fun `release without profile installs the apk alone`() = runTest {
        val h = harness().apply { publish(tag) }
        install(h)
        val session = h.gateway.sessions.single()
        assertTrue(session.committed)
        assertNull(session.profile)
    }

    @Test
    fun `profile without a valid digest is never downloaded nor installed`() = runTest {
        listOf(null, "sha256:short", "md5:" + "a".repeat(32)).forEach { digest ->
            val h = harness().apply { publishWithProfile(digest = digest) }
            install(h)
            val session = h.gateway.sessions.single()
            assertTrue(session.committed)
            assertNull(session.profile)
            assertEquals(0, h.profileCalls())
        }
    }

    @Test
    fun `profile whose bytes differ from the digest is dropped and the apk still installs`() = runTest {
        val h = harness().apply { publishWithProfile(served = dexMetadata(7)) }
        install(h)
        val session = h.gateway.sessions.single()
        assertTrue(session.committed)
        assertNull(session.profile)
    }

    @Test
    fun `missing, cut or oversized profile is dropped and the apk still installs`() = runTest {
        val replies = listOf(
            Reply.Body(status = 404),
            Reply.Fail(IOException("connection reset")),
            Reply.Body(body = profile, failAfter = 10),
            Reply.Body(body = profile + ByteArray(8)),
        )
        replies.forEach { reply ->
            val h = harness().apply { publishWithProfile(reply = reply) }
            install(h)
            val session = h.gateway.sessions.single()
            assertTrue(session.committed)
            assertNull(session.profile)
        }
    }

    @Test
    fun `profile that is not a dex metadata archive is dropped`() = runTest {
        listOf(
            dexMetadata(3, entries = listOf(DexMetadata.PROFILE_ENTRY, "classes.dex")),
            dexMetadata(4, entries = listOf(DexMetadata.PROFILE_ENTRY)),
            "not a zip".toByteArray(),
        ).forEach { bad ->
            val h = harness().apply { publishWithProfile(served = bad, digest = "sha256:" + sha256(bad), size = bad.size.toLong()) }
            install(h)
            assertNull(h.gateway.sessions.single().profile)
            assertTrue(h.gateway.sessions.single().committed)
            assertTrue(h.residualFiles().isEmpty())
        }
    }

    @Test
    fun `profile over 16 MiB or not completely uploaded is never downloaded`() = runTest {
        listOf(
            { h: UpdateHarness -> h.publishWithProfile(size = UpdateSelector.MAX_PROFILE_BYTES + 1) },
            { h: UpdateHarness -> h.publishWithProfile(state = "starter") },
        ).forEach { publish ->
            val h = harness().apply(publish)
            install(h)
            assertEquals(0, h.profileCalls())
            assertNull(h.gateway.sessions.single().profile)
            assertTrue(h.gateway.sessions.single().committed)
        }
    }

    @Test
    fun `session failing with the profile is installed again once without it`() = runTest {
        val h = harness().apply { publishWithProfile() }
        install(h)
        val first = h.gateway.sessions.single()
        assertTrue(profile.contentEquals(first.profile))
        h.repository.onInstallStatus(InstallStatus.Failed(first.id, InstallFailure.INVALID))
        advanceUntilIdle()
        val second = h.gateway.sessions.last()
        assertEquals(2, h.gateway.sessions.size)
        assertTrue(second.committed)
        assertNull(second.profile)
        assertTrue(h.apkBytes(tag).contentEquals(second.written.toByteArray()))
        assertEquals(2, h.apkCalls(tag))
        assertEquals(1, h.profileCalls())
        assertTrue(h.repository.status.value.step is UpdateStep.Installing)
        assertTrue(h.residualFiles().isEmpty())
        h.repository.onInstallStatus(InstallStatus.Success(second.id))
        advanceUntilIdle()
        assertTrue(h.repository.status.value.step is UpdateStep.Installing)
    }

    @Test
    fun `session failing again without the profile ends in the usual error without a loop`() = runTest {
        val h = harness().apply { publishWithProfile() }
        install(h)
        h.repository.onInstallStatus(InstallStatus.Failed(1, InstallFailure.INVALID))
        advanceUntilIdle()
        h.repository.onInstallStatus(InstallStatus.Failed(2, InstallFailure.INVALID))
        advanceUntilIdle()
        assertEquals(2, h.gateway.sessions.size)
        assertEquals(UpdateError.InstallFailed(InstallFailure.INVALID), (h.repository.status.value.step as? UpdateStep.Failed)?.error)
        assertNull((h.store as MemoryUpdateStore).relaunch)
        assertEquals(tag, h.repository.status.value.proposed?.tag)
    }

    @Test
    fun `failed session without profile is not installed again`() = runTest {
        val h = harness().apply { publish(tag) }
        install(h)
        h.repository.onInstallStatus(InstallStatus.Failed(1, InstallFailure.STORAGE))
        advanceUntilIdle()
        assertEquals(1, h.gateway.sessions.size)
        assertEquals(UpdateError.InstallFailed(InstallFailure.STORAGE), (h.repository.status.value.step as? UpdateStep.Failed)?.error)
    }

    @Test
    fun `profile over http is never downloaded`() = runTest {
        val url = "http://github.com/Sygix/SygixOs/releases/download/$tag/app-release.dm"
        val h = harness().apply { publishWithProfile(url = url) }
        install(h)
        assertEquals(0, h.transport.calls.count { it.first == url })
        assertNull(h.gateway.sessions.single().profile)
        assertTrue(h.gateway.sessions.single().committed)
    }

    @Test
    fun `session refusing the profile is abandoned and the apk is installed in a new session`() = runTest {
        val h = harness().apply { publishWithProfile() }
        h.gateway.onWriteProfile = { _, _ -> throw IOException("refused") }
        install(h)
        val (first, second) = h.gateway.sessions
        assertTrue(first.abandoned)
        assertFalse(first.committed)
        assertTrue(second.committed)
        assertNull(second.profile)
        assertTrue(h.apkBytes(tag).contentEquals(second.written.toByteArray()))
        assertTrue(h.residualFiles().isEmpty())
    }
}
