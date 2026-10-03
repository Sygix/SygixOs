/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.data.FakeTransport.Reply
import fr.sygix.sygixos.domain.UpdateError
import fr.sygix.sygixos.domain.UpdateException
import fr.sygix.sygixos.domain.UpdateSelector
import fr.sygix.sygixos.domain.asset
import fr.sygix.sygixos.domain.release
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GitHubReleaseSourceTest {

    private val transport = FakeTransport()
    private val now = 1_000_000L
    private val source = GitHubReleaseSource(transport, "SygixOs/0.0.1", clock = { now })
    private val listUrl = "${GitHubReleaseSource.API_BASE}/releases?per_page=100"
    private fun tagUrl(tag: String) = "${GitHubReleaseSource.API_BASE}/releases/tags/$tag"

    private fun errorOf(result: Result<*>): UpdateError? = (result.exceptionOrNull() as? UpdateException)?.error

    private fun list(reply: Reply) = runBlocking {
        transport.on(listUrl, reply)
        source.list()
    }

    @Test
    fun `one https request with the technical headers`() {
        list(Reply.Body(body = "[]".toByteArray()))
        assertEquals(1, transport.calls.size)
        val (url, headers) = transport.calls.single()
        assertTrue(url.startsWith("https://api.github.com/repos/Sygix/SygixOs/"))
        assertEquals(setOf("Accept", "X-GitHub-Api-Version", "User-Agent"), headers.keys)
        assertEquals("SygixOs/0.0.1", headers["User-Agent"])
    }

    @Test
    fun `empty list is a success`() {
        assertEquals(emptyList<Any>(), list(Reply.Body(body = "[]".toByteArray())).getOrThrow())
    }

    @Test
    fun `valid list is parsed with every field used`() {
        val published = release("v0.0.2-rc.1")
        val releases = list(Reply.Body(body = releasesJson(published, release("v0.0.1", assets = listOf(asset(digest = null)))).toByteArray())).getOrThrow()
        assertEquals(listOf(published, release("v0.0.1", assets = listOf(asset(digest = null)))), releases)
        assertEquals("v0.0.2-rc.1", UpdateSelector.known(releases).bestAny?.tag)
        assertNull(UpdateSelector.known(releases).bestFinal)
    }

    @Test
    fun `unreadable json is an unreadable error`() {
        assertEquals(UpdateError.Unreadable, errorOf(list(Reply.Body(body = "<html>".toByteArray()))))
        assertEquals(UpdateError.Unreadable, errorOf(list(Reply.Body(body = "{\"message\":\"x\"}".toByteArray()))))
        assertEquals(UpdateError.Unreadable, errorOf(list(Reply.Body(body = "[1, 2]".toByteArray()))))
    }

    @Test
    fun `404 means unreachable releases`() {
        assertEquals(UpdateError.Unavailable(404), errorOf(list(Reply.Body(status = 404))))
        assertEquals(UpdateError.Unavailable(500), errorOf(list(Reply.Body(status = 500))))
    }

    @Test
    fun `403 with no remaining request gives the reset time`() {
        val reply = Reply.Body(status = 403, headers = mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "1700000000"))
        assertEquals(UpdateError.RateLimited(1_700_000_000_000L), errorOf(list(reply)))
    }

    @Test
    fun `403 without the limit headers is unreachable releases`() {
        val reply = Reply.Body(status = 403, headers = mapOf("x-ratelimit-remaining" to "12"))
        assertEquals(UpdateError.Unavailable(403), errorOf(list(reply)))
    }

    @Test
    fun `429 with retry after gives the retry time`() {
        val reply = Reply.Body(status = 429, headers = mapOf("retry-after" to "120"))
        assertEquals(UpdateError.RateLimited(now + 120_000L), errorOf(list(reply)))
    }

    @Test
    fun `no network and timeouts are told apart`() {
        assertEquals(UpdateError.NoNetwork, errorOf(list(Reply.Fail(UnknownHostException("api.github.com")))))
        assertEquals(UpdateError.NoNetwork, errorOf(list(Reply.Fail(IOException("unreachable")))))
        assertEquals(UpdateError.Timeout, errorOf(list(Reply.Fail(SocketTimeoutException("read")))))
    }

    @Test
    fun `redirect to http is refused`() {
        val reply = Reply.Body(body = "[]".toByteArray(), finalUrl = "http://api.github.com/repos/Sygix/SygixOs/releases")
        assertTrue(errorOf(list(reply)) is UpdateError.Unavailable)
    }

    @Test
    fun `body over the size limit is unreadable`() {
        val huge = ByteArray((GitHubReleaseSource.MAX_BODY_BYTES + 1).toInt()) { ' '.code.toByte() }
        assertEquals(UpdateError.Unreadable, errorOf(list(Reply.Body(body = huge))))
    }

    @Test
    fun `reread returns the changed release`() = runBlocking {
        val changed = release("v0.0.2", assets = listOf(asset(digest = "sha256:" + "c".repeat(64), url = "https://github.com/Sygix/SygixOs/releases/download/v0.0.2/other.apk", name = "app-release.apk")))
        transport.on(tagUrl("v0.0.2"), Reply.Body(body = releaseJson(changed).toString().toByteArray()))
        assertEquals(changed, source.byTag("v0.0.2").getOrThrow())
    }

    @Test
    fun `reread of a missing release returns nothing`() = runBlocking {
        transport.on(tagUrl("v0.0.2"), Reply.Body(status = 404))
        assertNull(source.byTag("v0.0.2").getOrThrow())
    }
}
