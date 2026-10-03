/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.Release
import fr.sygix.sygixos.domain.ReleaseAsset
import fr.sygix.sygixos.domain.UpdateError
import fr.sygix.sygixos.domain.UpdateException
import fr.sygix.sygixos.domain.UpdateUrlPolicy
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.URLEncoder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

interface ReleaseSource {
    suspend fun list(): Result<List<Release>>
    suspend fun byTag(tag: String): Result<Release?>
}

class GitHubReleaseSource(
    private val transport: HttpTransport,
    private val userAgent: String,
    private val clock: () -> Long = System::currentTimeMillis,
    private val apiBase: String = API_BASE,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ReleaseSource {

    override suspend fun list(): Result<List<Release>> =
        request("$apiBase/releases?per_page=100") { body -> parseList(body) }
            .mapCatching { it ?: throw UpdateException(UpdateError.Unavailable(NOT_FOUND)) }

    override suspend fun byTag(tag: String): Result<Release?> =
        request("$apiBase/releases/tags/${URLEncoder.encode(tag, Charsets.UTF_8.name())}") { body -> parseRelease(JSONObject(body)) }

    private suspend fun <T> request(url: String, parse: (String) -> T): Result<T?> = withContext(io) { fetch(url, parse) }

    private fun <T> fetch(url: String, parse: (String) -> T): Result<T?> = runCatching {
        if (!UpdateUrlPolicy.isAllowed(url)) throw UpdateException(UpdateError.Unavailable(0))
        val body = ByteArrayOutputStream()
        val response = try {
            transport.get(url, apiHeaders(userAgent), MAX_BODY_BYTES, ApiTimeouts, body)
        } catch (e: UnknownHostException) {
            throw UpdateException(UpdateError.NoNetwork)
        } catch (e: SocketTimeoutException) {
            throw UpdateException(UpdateError.Timeout)
        } catch (e: InterruptedIOException) {
            throw UpdateException(UpdateError.Timeout)
        } catch (e: IOException) {
            throw UpdateException(UpdateError.NoNetwork)
        }
        rateLimit(response)?.let { throw UpdateException(it) }
        when {
            response.status == NOT_FOUND -> null
            response.status !in 200..299 -> throw UpdateException(UpdateError.Unavailable(response.status))
            !UpdateUrlPolicy.isAllowed(response.finalUrl) -> throw UpdateException(UpdateError.Unavailable(response.status))
            response.exceeded -> throw UpdateException(UpdateError.Unreadable)
            else -> try {
                parse(body.toString(Charsets.UTF_8.name()))
            } catch (e: JSONException) {
                throw UpdateException(UpdateError.Unreadable)
            }
        }
    }.recoverCatching { throw if (it is UpdateException) it else UpdateException(UpdateError.Unreadable) }

    private fun rateLimit(response: HttpResponse): UpdateError.RateLimited? {
        if (response.status != FORBIDDEN && response.status != TOO_MANY) return null
        val retryAfter = response.headers["retry-after"]?.trim()?.toLongOrNull()
        val exhausted = response.headers["x-ratelimit-remaining"]?.trim() == "0"
        if (retryAfter == null && !exhausted) return null
        val reset = response.headers["x-ratelimit-reset"]?.trim()?.toLongOrNull()
        val retryAt = when {
            retryAfter != null -> clock() + retryAfter * 1000
            reset != null -> reset * 1000
            else -> clock() + FALLBACK_WAIT_MS
        }
        return UpdateError.RateLimited(retryAt)
    }

    private fun parseList(body: String): List<Release> {
        val array = JSONArray(body)
        return (0 until array.length()).map { parseRelease(array.getJSONObject(it)) }
    }

    private fun parseRelease(json: JSONObject): Release {
        val assets = json.optJSONArray("assets") ?: JSONArray()
        return Release(
            tag = json.optString("tag_name", ""),
            draft = json.optBoolean("draft", false),
            prerelease = json.optBoolean("prerelease", false),
            htmlUrl = json.optString("html_url", ""),
            assets = (0 until assets.length()).map { index ->
                val asset = assets.getJSONObject(index)
                ReleaseAsset(
                    name = asset.optString("name", ""),
                    state = asset.optString("state", ""),
                    size = asset.optLong("size", 0),
                    digest = if (asset.isNull("digest")) null else asset.optString("digest"),
                    downloadUrl = asset.optString("browser_download_url", ""),
                )
            },
        )
    }

    companion object {
        const val API_BASE = "https://api.github.com/repos/Sygix/SygixOs"
        const val MAX_BODY_BYTES = 2L * 1024 * 1024
        val ApiTimeouts = HttpTimeouts(connectMs = 10_000, readMs = 15_000)
        private const val NOT_FOUND = 404
        private const val FORBIDDEN = 403
        private const val TOO_MANY = 429
        private const val FALLBACK_WAIT_MS = 60L * 60 * 1000

        fun apiHeaders(userAgent: String): Map<String, String> = mapOf(
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
            "User-Agent" to userAgent,
        )
    }
}
