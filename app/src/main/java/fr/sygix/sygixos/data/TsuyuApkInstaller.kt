/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Intent
import fr.sygix.sygixos.domain.TsuyuInstallError
import fr.sygix.sygixos.domain.TsuyuInstallException
import fr.sygix.sygixos.domain.TsuyuInstallStep
import fr.sygix.sygixos.domain.TsuyuInstaller
import fr.sygix.sygixos.domain.TsuyuPackage
import fr.sygix.sygixos.domain.TsuyuRelease
import fr.sygix.sygixos.domain.TsuyuReleases
import fr.sygix.sygixos.domain.UpdateUrlPolicy
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class TsuyuApkInstaller(
    private val releases: TsuyuReleases,
    private val includePrereleases: suspend () -> Boolean,
    private val transport: HttpTransport,
    private val inspector: ApkInspector,
    private val gateway: PackageInstallerGateway,
    private val downloadDir: File,
    private val userAgent: () -> String,
    private val showUserAction: (Intent) -> Unit,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : TsuyuInstaller, InstallStatusClaim {

    private sealed interface Event {
        data class Status(val status: InstallStatus) : Event
        data object ForegroundReturn : Event
    }

    private class Download(val file: File, val response: HttpResponse, val sha256: String)

    private val mutex = Mutex()
    private val lock = Any()
    private val owned = mutableSetOf<Int>()
    private var current: Int? = null
    private var events: Channel<Event>? = null

    override suspend fun install(onStep: (TsuyuInstallStep) -> Unit): Result<Unit> = mutex.withLock {
        try {
            withContext(io) { attempt(onStep) }
            Result.success(Unit)
        } catch (e: TsuyuInstallException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(TsuyuInstallException(TsuyuInstallError.REFUSED))
        } finally {
            finish()
        }
    }

    override fun claim(status: InstallStatus): Boolean = synchronized(lock) {
        if (status.sessionId !in owned) return false
        if (status.sessionId == current) events?.trySend(Event.Status(status))
        true
    }

    fun onForegroundReturn() {
        synchronized(lock) { events?.trySend(Event.ForegroundReturn) }
    }

    private suspend fun attempt(onStep: (TsuyuInstallStep) -> Unit) {
        val release = releases.refresh(includePrereleases()).getOrElse { fail(TsuyuInstallError.NO_NETWORK) }
            ?: fail(TsuyuInstallError.INVALID)
        onStep(TsuyuInstallStep.Downloading(0))
        val download = download(release) { percent -> onStep(TsuyuInstallStep.Downloading(percent)) }
        onStep(TsuyuInstallStep.Verifying)
        verify(download, release)
        onStep(TsuyuInstallStep.Installing)
        await(commit(download.file, release))
    }

    private suspend fun download(release: TsuyuRelease, onPercent: (Int) -> Unit): Download {
        if (!UpdateUrlPolicy.isAllowed(release.apkUrl)) fail(TsuyuInstallError.INVALID)
        downloadDir.deleteRecursively()
        if (!downloadDir.mkdirs() && !downloadDir.isDirectory) fail(TsuyuInstallError.INVALID)
        val file = File(downloadDir, APK_FILE)
        val digest = MessageDigest.getInstance("SHA-256")
        val job = currentCoroutineContext().job
        var lastPercent = 0
        val response = try {
            DigestOutputStream(file.outputStream().buffered(), digest).use { out ->
                transport.get(release.apkUrl, mapOf("User-Agent" to userAgent()), release.size, UpdateInstaller.DownloadTimeouts, out) { bytes ->
                    job.ensureActive()
                    val percent = (bytes * 100 / release.size).toInt()
                    if (percent > lastPercent) {
                        lastPercent = percent
                        onPercent(percent)
                    }
                }
            }
        } catch (e: IOException) {
            fail(TsuyuInstallError.NO_NETWORK)
        }
        when {
            response.status == NOT_FOUND || response.status == GONE -> fail(TsuyuInstallError.INVALID)
            response.status !in 200..299 -> fail(TsuyuInstallError.NO_NETWORK)
            !UpdateUrlPolicy.isAllowed(response.finalUrl) -> fail(TsuyuInstallError.INVALID)
        }
        return Download(file, response, UpdateInstaller.hex(digest.digest()))
    }

    private fun verify(download: Download, release: TsuyuRelease) {
        val response = download.response
        if (response.exceeded || response.bodyBytes != release.size || download.file.length() != release.size) fail(TsuyuInstallError.INVALID)
        if (download.sha256 != release.sha256) fail(TsuyuInstallError.INVALID)
        val archive = inspector.archive(download.file) ?: fail(TsuyuInstallError.INVALID)
        if (archive.packageName != TsuyuPackage) fail(TsuyuInstallError.INVALID)
        val self = inspector.installed() ?: fail(TsuyuInstallError.INVALID)
        if (archive.certificates.isEmpty() || archive.certificates != self.certificates) fail(TsuyuInstallError.INVALID)
    }

    private fun commit(apk: File, release: TsuyuRelease): Channel<Event> {
        val session = try {
            gateway.create(release.size)
        } catch (e: Exception) {
            fail(TsuyuInstallError.REFUSED)
        }
        val channel = Channel<Event>(Channel.UNLIMITED)
        synchronized(lock) {
            owned += session.id
            current = session.id
            events = channel
        }
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            session.write(release.size) { out -> copy(apk, DigestOutputStream(out, digest)) }
            if (UpdateInstaller.hex(digest.digest()) != release.sha256) {
                session.abandon()
                fail(TsuyuInstallError.INVALID)
            }
            downloadDir.deleteRecursively()
            session.commit()
        } catch (e: TsuyuInstallException) {
            throw e
        } catch (e: Exception) {
            session.abandon()
            fail(TsuyuInstallError.REFUSED)
        }
        return channel
    }

    private suspend fun await(channel: Channel<Event>) {
        withTimeoutOrNull(STATUS_TIMEOUT_MS) { outcome(channel) } ?: fail(TsuyuInstallError.REFUSED)
    }

    private suspend fun outcome(channel: Channel<Event>) {
        var awaitingUser = false
        while (true) {
            val status = when (val event = channel.receive()) {
                is Event.Status -> event.status
                Event.ForegroundReturn -> {
                    if (!awaitingUser) continue
                    withTimeoutOrNull(RETURN_GRACE_MS) { nextStatus(channel) } ?: if (sessionStillActive()) continue else fail(TsuyuInstallError.REFUSED)
                }
            }
            when (status) {
                is InstallStatus.Success -> {
                    settle()
                    return
                }
                is InstallStatus.PendingUserAction -> {
                    showUserAction(status.intent ?: fail(TsuyuInstallError.REFUSED))
                    awaitingUser = true
                }
                is InstallStatus.Aborted, is InstallStatus.Failed -> {
                    settle()
                    fail(TsuyuInstallError.REFUSED)
                }
            }
        }
    }

    private suspend fun sessionStillActive(): Boolean {
        val id = synchronized(lock) { current } ?: return false
        return withContext(io) { gateway.isActive(id) }
    }

        private suspend fun nextStatus(channel: Channel<Event>): InstallStatus {
        while (true) {
            val event = channel.receive()
            if (event is Event.Status) return event.status
        }
    }

    private fun settle() {
        synchronized(lock) { current = null }
    }

    private fun finish() {
        val pending = synchronized(lock) {
            events?.close()
            events = null
            current.also { current = null }
        }
        if (pending != null) gateway.abandon(pending)
        downloadDir.deleteRecursively()
    }

    private fun fail(error: TsuyuInstallError): Nothing = throw TsuyuInstallException(error)

    private fun copy(apk: File, out: OutputStream) {
        apk.inputStream().use { input -> input.copyTo(out) }
        out.flush()
    }

    companion object {
        const val RETURN_GRACE_MS = 2_000L
        const val STATUS_TIMEOUT_MS = 10L * 60 * 1000
        private const val APK_FILE = "tsuyu.apk"
        private const val NOT_FOUND = 404
        private const val GONE = 410
    }
}
