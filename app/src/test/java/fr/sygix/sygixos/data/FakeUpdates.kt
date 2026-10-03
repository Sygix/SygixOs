/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Intent
import fr.sygix.sygixos.domain.CheckResult
import fr.sygix.sygixos.domain.KnownUpdates
import fr.sygix.sygixos.domain.Release
import fr.sygix.sygixos.domain.UpdateStatus
import fr.sygix.sygixos.domain.ReleaseVersion
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

internal class FakeTransport : HttpTransport {

    sealed interface Reply {
        data class Body(
            val status: Int = 200,
            val body: ByteArray = ByteArray(0),
            val headers: Map<String, String> = emptyMap(),
            val finalUrl: String? = null,
            val failAfter: Int? = null,
        ) : Reply
        data class Fail(val error: IOException) : Reply
    }

    val calls = mutableListOf<Pair<String, Map<String, String>>>()
    private val replies = mutableMapOf<String, Reply>()

    fun on(url: String, reply: Reply) {
        replies[url] = reply
    }

    override fun get(
        url: String,
        headers: Map<String, String>,
        maxBytes: Long,
        timeouts: HttpTimeouts,
        sink: OutputStream,
        onBytes: (Long) -> Unit,
    ): HttpResponse {
        calls += url to headers
        val reply = replies[url] ?: Reply.Body(status = 404)
        when (reply) {
            is Reply.Fail -> throw reply.error
            is Reply.Body -> {
                if (reply.status !in 200..299) return HttpResponse(reply.status, reply.headers, reply.finalUrl ?: url, 0, false)
                val limit = minOf(reply.body.size.toLong(), maxBytes).toInt()
                var written = 0
                while (written < limit) {
                    if (reply.failAfter != null && written >= reply.failAfter) throw IOException("connection reset")
                    val chunk = minOf(CHUNK, limit - written)
                    sink.write(reply.body, written, chunk)
                    written += chunk
                    onBytes(written.toLong())
                }
                return HttpResponse(reply.status, reply.headers, reply.finalUrl ?: url, written.toLong(), reply.body.size > maxBytes)
            }
        }
    }

    private companion object {
        const val CHUNK = 64
    }
}

internal fun releasesJson(vararg releases: Release): String = JSONArray(releases.map { releaseJson(it) }).toString()

internal fun releaseJson(release: Release): JSONObject = JSONObject()
    .put("tag_name", release.tag)
    .put("draft", release.draft)
    .put("prerelease", release.prerelease)
    .put("html_url", release.htmlUrl)
    .put(
        "assets",
        JSONArray(
            release.assets.map { asset ->
                JSONObject()
                    .put("name", asset.name)
                    .put("state", asset.state)
                    .put("size", asset.size)
                    .put("digest", asset.digest ?: JSONObject.NULL)
                    .put("browser_download_url", asset.downloadUrl)
            },
        ),
    )

internal class MemoryUpdateStore(initial: UpdatePersisted = UpdatePersisted()) : UpdateStore {
    val state = MutableStateFlow(initial)
    var relaunch: Long? = null
    override val data: Flow<UpdatePersisted> = state
    override suspend fun togglePrereleases() = state.update { it.copy(includePrereleases = !it.includePrereleases) }
    override suspend fun setLastCheckAt(at: Long) = state.update { it.copy(lastCheckAt = at) }
    override suspend fun setLastResult(result: CheckResult) = state.update { it.copy(lastResult = result) }
    override suspend fun setRetryAt(at: Long) = state.update { it.copy(retryAt = at) }
    override suspend fun setKnown(known: KnownUpdates) = state.update { it.copy(known = known) }
    override suspend fun setRelaunch(versionCode: Long?) {
        relaunch = versionCode
    }
    override suspend fun takeRelaunch(): Long? = relaunch.also { relaunch = null }
}

internal class FakeInspector(var installed: ApkIdentity?, private val packageName: String, private val certificate: String) : ApkInspector {
    var identify: (File) -> ApkIdentity? = { file ->
        ReleaseVersion.parse(file.name.removeSuffix(".apk"))?.let { ApkIdentity(packageName, it.versionCode, setOf(certificate)) }
    }
    var onArchive: (File) -> Unit = {}
    override fun archive(file: File): ApkIdentity? {
        onArchive(file)
        return identify(file)
    }
    override fun installed(): ApkIdentity? = installed
}

internal class FakeSession(override val id: Int, private val gateway: FakeGateway) : InstallSession {
    val written = ByteArrayOutputStream()
    var committed = false
    var abandoned = false
    override fun write(size: Long, block: (OutputStream) -> Unit) {
        block(written)
    }
    override fun commit() {
        gateway.onCommit(this)
        committed = true
    }
    override fun abandon() {
        abandoned = true
    }
}

internal class FakeGateway : PackageInstallerGateway {
    val sessions = mutableListOf<FakeSession>()
    val abandonedIds = mutableListOf<Int>()
    var abandonAllCalls = 0
    var openSessions = 0
    var active = false
    var onCommit: (FakeSession) -> Unit = {}
    override fun create(size: Long): InstallSession = FakeSession(sessions.size + 1, this).also { sessions += it }
    override fun abandon(sessionId: Int) {
        abandonedIds += sessionId
    }
    override fun abandonAll(): Int {
        abandonAllCalls++
        return openSessions.also { openSessions = 0 }
    }
    override fun isActive(sessionId: Int): Boolean = active
}

internal class FakeForeground(override var isForeground: Boolean = true) : ForegroundState

internal class FakeNetwork(var validated: Boolean = true) : NetworkStatus {
    override fun hasValidatedNetwork(): Boolean = validated
}

internal class FakeScreens(var available: Boolean = true) : SystemScreenLauncher {
    val launched = mutableListOf<Intent>()
    override fun launch(intent: Intent): Boolean {
        if (!available) return false
        launched += intent
        return true
    }
}

internal class FakeUpdateController(initial: UpdateStatus = UpdateStatus()) : UpdateController {
    override val status = MutableStateFlow(initial)
    override val systemScreenReturns = MutableStateFlow(0)
    val checks = mutableListOf<Unit>()
    val installs = mutableListOf<String>()
    var homeShown = 0
    var foregrounds = 0
    override fun check() {
        checks += Unit
    }
    override fun startUpdate(tag: String) {
        installs += tag
    }
    override fun togglePrereleases() {
        status.update { it.copy(includePrereleases = !it.includePrereleases) }
    }
    override fun onHomeShown() {
        homeShown++
    }
    override fun onForeground() {
        foregrounds++
    }
}
