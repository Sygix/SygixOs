/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import fr.sygix.sygixos.domain.InstallFailure
import java.io.OutputStream

interface InstallSession {
    val id: Int
    fun write(size: Long, block: (OutputStream) -> Unit)
    fun commit()
    fun abandon()
}

interface PackageInstallerGateway {
    fun create(size: Long): InstallSession
    fun abandon(sessionId: Int)
    fun abandonAll()
}

sealed interface InstallStatus {
    data object Success : InstallStatus
    data class PendingUserAction(val sessionId: Int, val intent: Intent?) : InstallStatus
    data object Aborted : InstallStatus
    data class Failed(val family: InstallFailure) : InstallStatus

    companion object {
        fun of(status: Int, sessionId: Int, intent: Intent?): InstallStatus = when (status) {
            PackageInstaller.STATUS_SUCCESS -> Success
            PackageInstaller.STATUS_PENDING_USER_ACTION -> PendingUserAction(sessionId, intent)
            PackageInstaller.STATUS_FAILURE_ABORTED -> Aborted
            PackageInstaller.STATUS_FAILURE_BLOCKED -> Failed(InstallFailure.BLOCKED)
            PackageInstaller.STATUS_FAILURE_CONFLICT -> Failed(InstallFailure.CONFLICT)
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> Failed(InstallFailure.INCOMPATIBLE)
            PackageInstaller.STATUS_FAILURE_INVALID -> Failed(InstallFailure.INVALID)
            PackageInstaller.STATUS_FAILURE_STORAGE -> Failed(InstallFailure.STORAGE)
            PackageInstaller.STATUS_FAILURE_TIMEOUT -> Failed(InstallFailure.TIMEOUT)
            else -> Failed(InstallFailure.OTHER)
        }
    }
}

class SystemPackageInstallerGateway(private val context: Context) : PackageInstallerGateway {

    private val installer: PackageInstaller get() = context.packageManager.packageInstaller

    override fun create(size: Long): InstallSession {
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(context.packageName)
            setSize(size)
            setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }
        val id = installer.createSession(params)
        return SystemInstallSession(id, installer.openSession(id))
    }

    override fun abandon(sessionId: Int) {
        runCatching { installer.abandonSession(sessionId) }
    }

    override fun abandonAll() {
        installer.mySessions.forEach { abandon(it.sessionId) }
    }

    private inner class SystemInstallSession(override val id: Int, private val session: PackageInstaller.Session) : InstallSession {

        override fun write(size: Long, block: (OutputStream) -> Unit) {
            session.openWrite(APK_ENTRY, 0, size).use { out ->
                block(out)
                session.fsync(out)
            }
        }

        override fun commit() {
            val intent = Intent(context, InstallStatusReceiver::class.java).setPackage(context.packageName)
            val pending = PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pending.intentSender)
            session.close()
        }

        override fun abandon() {
            runCatching { session.abandon() }
        }
    }

    private companion object {
        const val APK_ENTRY = "base.apk"
    }
}
