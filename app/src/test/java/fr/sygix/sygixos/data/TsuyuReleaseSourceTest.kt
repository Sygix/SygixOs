/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.data.FakeTransport.Reply
import fr.sygix.sygixos.domain.Release
import fr.sygix.sygixos.domain.ReleaseAsset
import fr.sygix.sygixos.domain.TsuyuRelease
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TsuyuReleaseSourceTest {

    private val transport = FakeTransport()
    private val store = MemoryTsuyuStore()

    private fun TestScope.source() = TsuyuReleaseSource(
        GitHubReleaseSource(transport, { "SygixOs/test" }, apiBase = TsuyuReleaseSource.API_BASE, io = StandardTestDispatcher(testScheduler)),
        store,
    )

    @Test
    fun `one request to the tsuyu repository finds and keeps the release`() = runTest {
        publish(release("v1.1.0"), release("v1.0.0"))
        val result = source().refresh(includePrereleases = false)
        assertEquals("v1.1.0", result.getOrNull()?.tag)
        assertEquals(listOf(LIST_URL), transport.calls.map { it.first })
        assertEquals(result.getOrNull(), source().lastKnown())
    }

    @Test
    fun `no eligible release is kept as nothing`() = runTest {
        store.value = known
        publish(release("v2.0.0-rc.1", prerelease = true))
        val result = source().refresh(includePrereleases = false)
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
        assertNull(source().lastKnown())
    }

    @Test
    fun `the prerelease setting is applied`() = runTest {
        publish(release("v2.0.0-rc.1", prerelease = true), release("v1.0.0"))
        assertEquals("v2.0.0-rc.1", source().refresh(includePrereleases = true).getOrNull()?.tag)
        assertEquals("v1.0.0", source().refresh(includePrereleases = false).getOrNull()?.tag)
    }

    @Test
    fun `a failed refresh keeps the last known release`() = runTest {
        listOf(
            Reply.Fail(IOException("offline")),
            Reply.Fail(SocketTimeoutException("slow")),
            Reply.Body(status = 500),
            Reply.Body(status = 403, headers = mapOf("x-ratelimit-remaining" to "0")),
            Reply.Body(body = "not json".toByteArray()),
        ).forEach { reply ->
            store.value = known
            store.saves = 0
            transport.on(LIST_URL, reply)
            val result = source().refresh(includePrereleases = true)
            assertTrue(reply.toString(), result.isFailure)
            assertEquals(reply.toString(), known, source().lastKnown())
            assertEquals(reply.toString(), 0, store.saves)
        }
    }

    @Test
    fun `a failed save is reported`() = runTest {
        publish(release("v1.0.0"))
        store.failSave = true
        assertTrue(source().refresh(includePrereleases = false).isFailure)
    }

    private fun publish(vararg releases: Release) {
        transport.on(LIST_URL, Reply.Body(body = releasesJson(*releases).toByteArray()))
    }

    private fun release(tag: String, prerelease: Boolean = false) = Release(
        tag,
        false,
        prerelease,
        "https://github.com/Sygix/Tsuyu/releases/tag/$tag",
        listOf(ReleaseAsset("tsuyu.apk", "uploaded", 2_048, "sha256:" + "cd".repeat(32), "https://github.com/Sygix/Tsuyu/releases/download/$tag/tsuyu.apk")),
    )

    private val known = TsuyuRelease("v0.9.0", false, "https://github.com/Sygix/Tsuyu/releases/download/v0.9.0/tsuyu.apk", 1_024, "ef".repeat(32))

    private companion object {
        const val LIST_URL = "https://api.github.com/repos/Sygix/Tsuyu/releases?per_page=100"
    }
}

internal class MemoryTsuyuStore(var value: TsuyuRelease? = null) : TsuyuReleaseStore {
    var saves = 0
    var failSave = false
    override suspend fun load(): TsuyuRelease? = value
    override suspend fun save(release: TsuyuRelease?) {
        if (failSave) throw IOException("disk full")
        saves++
        value = release
    }
}
