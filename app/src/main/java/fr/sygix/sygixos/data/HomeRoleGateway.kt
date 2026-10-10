/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import fr.sygix.sygixos.model.SystemControlState

interface HomeRoleGateway {
    fun state(): SystemControlState
    fun request(): Boolean
    fun fallback(): Boolean
}

interface HomeRoleSource {
    fun available(): Boolean
    fun held(): Boolean
    fun requestIntent(): Intent?
}

class AndroidHomeRoleSource(context: Context) : HomeRoleSource {
    private val roles = context.getSystemService(RoleManager::class.java)
    override fun available(): Boolean = runCatching { roles?.isRoleAvailable(RoleManager.ROLE_HOME) == true }.getOrDefault(false)
    override fun held(): Boolean = runCatching { roles?.isRoleHeld(RoleManager.ROLE_HOME) == true }.getOrDefault(false)
    override fun requestIntent(): Intent? = runCatching { roles?.createRequestRoleIntent(RoleManager.ROLE_HOME) }.getOrNull()
}

class AndroidHomeRoleGateway(
    private val context: Context,
    private val launchDialog: (Intent) -> Boolean,
    private val source: HomeRoleSource = AndroidHomeRoleSource(context),
    private val resolves: (Intent) -> Boolean = {
        context.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null
    },
    private val open: (Intent) -> Boolean = {
        runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
    },
    private val defaultHome: () -> String? = {
        context.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName
    },
) : HomeRoleGateway {
    private var requestFailed = false

    private fun fallbackScreens(): List<Intent> = listOf(Intent(Settings.ACTION_HOME_SETTINGS), Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    private fun resolved(intent: Intent): Boolean = runCatching { resolves(intent) }.getOrDefault(false)

    override fun state(): SystemControlState {
        val available = runCatching(source::available).getOrDefault(false)
        val active = (available && runCatching(source::held).getOrDefault(false)) ||
            ((!available || requestFailed) && runCatching(defaultHome).getOrNull() == context.packageName)
        if (active) return SystemControlState.ACTIVE
        return if ((available && !requestFailed) || fallbackScreens().any(::resolved)) SystemControlState.INACTIVE
            else SystemControlState.UNAVAILABLE
    }

    override fun request(): Boolean {
        if (state() != SystemControlState.INACTIVE) return false
        if (!requestFailed && runCatching(source::available).getOrDefault(false)) {
            val intent = runCatching(source::requestIntent).getOrNull()
            if (intent != null && runCatching { launchDialog(intent) }.getOrDefault(false)) return true
            requestFailed = true
        }
        return openFallback()
    }

    override fun fallback(): Boolean {
        requestFailed = true
        if (state() == SystemControlState.ACTIVE) return false
        return openFallback()
    }

    private fun openFallback(): Boolean = fallbackScreens().filter(::resolved).any { runCatching { open(it) }.getOrDefault(false) }
}
