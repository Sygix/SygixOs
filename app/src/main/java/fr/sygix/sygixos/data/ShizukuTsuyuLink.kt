/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import fr.sygix.sygixos.domain.TsuyuLink
import fr.sygix.sygixos.domain.TsuyuMinimumApi
import fr.sygix.sygixos.domain.TsuyuPackage
import fr.sygix.sygixos.domain.TsuyuPermission
import fr.sygix.sygixos.domain.TsuyuProbe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

class ShizukuTsuyuLink(
    private val api: ShizukuApi,
    private val installed: () -> Boolean,
    private val launch: (Intent) -> Boolean,
) : TsuyuLink {
    constructor(api: ShizukuApi, packages: PackageManager, launch: (Intent) -> Boolean) : this(api, { packages.installed(TsuyuPackage) }, launch)

    override fun probe(): TsuyuProbe {
        val binder = api.pingBinder()
        val authorized = binder && api.granted()
        return TsuyuProbe(
            installed = runCatching(installed).getOrDefault(false),
            binder = binder,
            apiVersion = if (binder) runCatching { api.version() }.getOrNull() else null,
            authorized = authorized,
            permanentlyDenied = binder && !authorized && api.permanentlyDenied(),
        )
    }

    override suspend fun awaitBinder(timeoutMs: Long): Boolean {
        val received = CompletableDeferred<Unit>()
        val subscription = api.onBinderReceived { received.complete(Unit) }
        return try {
            withTimeoutOrNull(timeoutMs) { received.await() } != null && api.pingBinder()
        } finally {
            subscription.close()
        }
    }

    override suspend fun requestPermission(): TsuyuPermission {
        if (!api.pingBinder()) return TsuyuPermission.DENIED
        if (api.granted()) return TsuyuPermission.GRANTED
        if (api.permanentlyDenied()) return TsuyuPermission.NO_MORE_REQUESTS
        val result = CompletableDeferred<Boolean>()
        val answer = api.onPermissionResult { code, granted -> if (code == RequestCode) result.complete(granted) }
        val death = api.onBinderDead { result.complete(false) }
        return try {
            api.request(RequestCode)
            when {
                result.await() -> TsuyuPermission.GRANTED
                api.permanentlyDenied() -> TsuyuPermission.NO_MORE_REQUESTS
                else -> TsuyuPermission.DENIED
            }
        } catch (_: IllegalStateException) {
            TsuyuPermission.DENIED
        } finally {
            answer.close()
            death.close()
        }
    }

    override fun open(): Boolean = runCatching { launch(openIntent()) }.getOrDefault(false)

    fun active(): Boolean = probe().active

    companion object {
        const val RequestCode = 0x7559

        fun openIntent(): Intent = Intent(Intent.ACTION_MAIN).setComponent(ComponentName(TsuyuPackage, "$TsuyuPackage.MainActivity"))

        private fun PackageManager.installed(packageName: String): Boolean = try {
            getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }
}

val TsuyuProbe.active: Boolean get() = binder && (apiVersion ?: 0) >= TsuyuMinimumApi && authorized
