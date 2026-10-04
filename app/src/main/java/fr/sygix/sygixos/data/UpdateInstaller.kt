/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.DexMetadata
import fr.sygix.sygixos.domain.InstallFailure
import fr.sygix.sygixos.domain.ProfileAsset
import fr.sygix.sygixos.domain.UpdateCandidate
import fr.sygix.sygixos.domain.UpdateError
import fr.sygix.sygixos.domain.UpdateException
import fr.sygix.sygixos.domain.UpdateSelector
import fr.sygix.sygixos.domain.UpdateStep
import fr.sygix.sygixos.domain.UpdateUrlPolicy
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface InstallOutcome {
    data class Committed(val candidate: UpdateCandidate, val sessionId: Int) : InstallOutcome
    data class Failure(val candidate: UpdateCandidate, val error: UpdateError) : InstallOutcome
}

class UpdateInstaller(
    private val source: ReleaseSource,
    private val transport: HttpTransport,
    private val inspector: ApkInspector,
    private val gateway: PackageInstallerGateway,
    private val store: UpdateStore,
    private val foreground: ForegroundState,
    private val updatesDir: File,
    private val freeSpace: () -> Long,
    private val selfPackage: String,
    private val userAgent: () -> String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun run(known: UpdateCandidate, onStep: (UpdateStep) -> Unit): InstallOutcome = withContext(io) {
        val candidate = try {
            reread(known)
        } catch (e: UpdateException) {
            return@withContext InstallOutcome.Failure(known, e.error)
        }
        try {
            if (freeSpace() / 2 < candidate.size) throw UpdateException(UpdateError.NoSpace)
            onStep(UpdateStep.Downloading(candidate, 0))
            val apk = download(candidate) { percent -> onStep(UpdateStep.Downloading(candidate, percent)) }
            onStep(UpdateStep.Verifying(candidate))
            verify(apk, candidate)
            val profile = candidate.profile?.let(::downloadProfile)
            onStep(UpdateStep.Installing(candidate))
            InstallOutcome.Committed(candidate, install(apk, candidate, profile))
        } catch (e: UpdateException) {
            clean()
            InstallOutcome.Failure(candidate, e.error)
        } catch (e: CancellationException) {
            clean()
            throw e
        } catch (e: Exception) {
            clean()
            InstallOutcome.Failure(candidate, UpdateError.InstallFailed(InstallFailure.OTHER))
        }
    }

    fun clean() {
        updatesDir.deleteRecursively()
    }

    private suspend fun reread(known: UpdateCandidate): UpdateCandidate {
        val release = source.byTag(known.tag).getOrElse { throw it as? UpdateException ?: UpdateException(UpdateError.Unreadable) }
        val fresh = release?.let(UpdateSelector::eligible)
        if (fresh == null || fresh.tag != known.tag || fresh.versionCode != known.versionCode) {
            throw UpdateException(UpdateError.Withdrawn)
        }
        return fresh
    }

    private fun download(candidate: UpdateCandidate, onPercent: (Int) -> Unit): File {
        if (!UpdateUrlPolicy.isAllowed(candidate.apkUrl)) throw UpdateException(UpdateError.Interrupted)
        clean()
        if (!updatesDir.mkdirs() && !updatesDir.isDirectory) throw UpdateException(UpdateError.NoSpace)
        val part = File(updatesDir, "${candidate.tag}.apk.part")
        val digest = MessageDigest.getInstance("SHA-256")
        var lastPercent = 0
        val response = try {
            DigestOutputStream(part.outputStream().buffered(), digest).use { out ->
                transport.get(candidate.apkUrl, mapOf("User-Agent" to userAgent()), candidate.size, DownloadTimeouts, out) { bytes ->
                    val percent = (bytes * 100 / candidate.size).toInt()
                    if (percent > lastPercent) {
                        lastPercent = percent
                        onPercent(percent)
                    }
                }
            }
        } catch (e: SSLHandshakeException) {
            throw UpdateException(UpdateError.SecureConnection)
        } catch (e: SSLPeerUnverifiedException) {
            throw UpdateException(UpdateError.SecureConnection)
        } catch (e: IOException) {
            throw UpdateException(UpdateError.Interrupted)
        }
        when {
            response.status == 404 || response.status == 410 -> throw UpdateException(UpdateError.AssetNotFound)
            response.status !in 200..299 -> throw UpdateException(UpdateError.Interrupted)
            !UpdateUrlPolicy.isAllowed(response.finalUrl) -> throw UpdateException(UpdateError.Interrupted)
            response.exceeded || response.bodyBytes != candidate.size || part.length() != candidate.size ->
                throw UpdateException(UpdateError.Corrupt)
            hex(digest.digest()) != candidate.sha256 -> throw UpdateException(UpdateError.Corrupt)
        }
        val apk = File(updatesDir, "${candidate.tag}.apk")
        if (!part.renameTo(apk)) throw UpdateException(UpdateError.Corrupt)
        return apk
    }

    private fun verify(apk: File, candidate: UpdateCandidate) {
        val archive = inspector.archive(apk) ?: throw UpdateException(UpdateError.Inconsistent)
        val installed = inspector.installed() ?: throw UpdateException(UpdateError.Inconsistent)
        if (archive.packageName != selfPackage) throw UpdateException(UpdateError.Inconsistent)
        if (archive.versionCode != candidate.versionCode || archive.versionCode <= installed.versionCode) {
            throw UpdateException(UpdateError.Inconsistent)
        }
        if (archive.certificates.isEmpty() || archive.certificates != installed.certificates) {
            throw UpdateException(UpdateError.SignatureMismatch)
        }
    }

    private fun downloadProfile(profile: ProfileAsset): ByteArray? = runCatching {
        if (!UpdateUrlPolicy.isAllowed(profile.url)) return@runCatching null
        val body = ByteArrayOutputStream()
        val response = transport.get(profile.url, mapOf("User-Agent" to userAgent()), profile.size, ProfileTimeouts, body)
        val bytes = body.toByteArray()
        val valid = response.status in 200..299 &&
            UpdateUrlPolicy.isAllowed(response.finalUrl) &&
            !response.exceeded &&
            bytes.size.toLong() == profile.size &&
            sha256(bytes) == profile.sha256 &&
            DexMetadata.isValid(bytes)
        bytes.takeIf { valid }
    }.getOrNull()

    private suspend fun install(apk: File, candidate: UpdateCandidate, profile: ByteArray?): Int =
        profile?.let { installWithProfile(apk, candidate, it) } ?: installSession(apk, candidate, null)

    private suspend fun installWithProfile(apk: File, candidate: UpdateCandidate, profile: ByteArray): Int? = try {
        installSession(apk, candidate, profile)
    } catch (e: ProfileRejected) {
        null
    }

    private suspend fun installSession(apk: File, candidate: UpdateCandidate, profile: ByteArray?): Int {
        val session = try {
            gateway.create(candidate.size)
        } catch (e: Exception) {
            throw UpdateException(UpdateError.InstallFailed(InstallFailure.OTHER))
        }
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            session.write(candidate.size) { out -> copy(apk, DigestOutputStream(NonClosing(out), digest)) }
            if (hex(digest.digest()) != candidate.sha256) {
                session.abandon()
                throw UpdateException(UpdateError.Corrupt)
            }
            if (profile != null) writeProfile(session, candidate, profile)
            clean()
            store.setRelaunch(candidate.versionCode.takeIf { foreground.isForeground })
            session.commit()
            return session.id
        } catch (e: UpdateException) {
            throw e
        } catch (e: ProfileRejected) {
            throw e
        } catch (e: CancellationException) {
            session.abandon()
            throw e
        } catch (e: Exception) {
            session.abandon()
            throw UpdateException(UpdateError.InstallFailed(InstallFailure.OTHER))
        }
    }

    private fun writeProfile(session: InstallSession, candidate: UpdateCandidate, profile: ByteArray) {
        val expected = candidate.profile?.sha256
        try {
            if (expected == null || sha256(profile) != expected) throw IOException("profile digest")
            session.writeProfile(profile)
        } catch (e: Exception) {
            session.abandon()
            throw ProfileRejected()
        }
    }

    private class ProfileRejected : Exception()

    private fun copy(apk: File, out: OutputStream) {
        apk.inputStream().use { input -> input.copyTo(out) }
        out.flush()
    }

    private class NonClosing(private val out: OutputStream) : OutputStream() {
        override fun write(b: Int) = out.write(b)
        override fun write(b: ByteArray, off: Int, len: Int) = out.write(b, off, len)
        override fun flush() = out.flush()
    }

    companion object {
        val DownloadTimeouts = HttpTimeouts(connectMs = 10_000, readMs = 30_000, totalMs = 10L * 60 * 1000)
        val ProfileTimeouts = HttpTimeouts(connectMs = 10_000, readMs = 15_000, totalMs = 60_000)

        fun sha256(bytes: ByteArray): String = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

        fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }
    }
}
