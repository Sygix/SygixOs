/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.net.toUri
import fr.sygix.sygixos.domain.AccessibilityCheck
import fr.sygix.sygixos.domain.AccessibilityCheckStatus
import fr.sygix.sygixos.domain.LauncherSystemGateway
import fr.sygix.sygixos.model.SystemControlState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class AndroidLauncherSystemGateway(
    private val context: Context,
    private val resolves: (Intent) -> Boolean = { context.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null },
    private val open: (Intent) -> Boolean = { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess },
    private val serviceEnabled: () -> Boolean = { serviceEnabledIn(context) },
    private val serviceRunning: () -> Boolean = { AccessibilityConnection.connected.value },
    private val overlayGranted: () -> Boolean = { Settings.canDrawOverlays(context) },
    private val readBootCount: () -> Int? = { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) },
    private val check: AccessibilityCheck? = null,
) : LauncherSystemGateway {
    private fun accessibilityIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    private fun overlayIntent() = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
    private fun resolved(intent: Intent) = runCatching { resolves(intent) }.getOrDefault(false)

    override fun accessibilityState(): SystemControlState {
        val enabled = runCatching(serviceEnabled).getOrDefault(false)
        if (enabled && runCatching(serviceRunning).getOrDefault(false)) return SystemControlState.ACTIVE
        if (enabled) return if (check?.status?.value == AccessibilityCheckStatus.BLOCKED) SystemControlState.BLOCKED else SystemControlState.PENDING
        return if (resolved(accessibilityIntent())) SystemControlState.INACTIVE else SystemControlState.UNAVAILABLE
    }
    override fun overlayState(): SystemControlState {
        if (runCatching(overlayGranted).getOrDefault(false)) return SystemControlState.ACTIVE
        return if (resolved(overlayIntent())) SystemControlState.INACTIVE else SystemControlState.UNAVAILABLE
    }
    override fun bootCount(): Int? = runCatching(readBootCount).getOrNull()?.takeIf { it >= 0 }
    override fun openAccessibility() = openResolved(accessibilityIntent())
    override fun openOverlay() = openResolved(overlayIntent())
    override fun verifyAccessibility() {
        check?.check()
    }
    override fun accessibilityChanges(): Flow<Unit> =
        combine(AccessibilityConnection.connected, check?.status ?: flowOf(AccessibilityCheckStatus.IDLE)) { connected, status -> connected to status }
            .drop(1)
            .map { }
    private fun openResolved(intent: Intent): Boolean = resolved(intent) && runCatching { open(intent) }.getOrDefault(false)

    companion object {
        fun serviceEnabledIn(context: Context): Boolean = AccessibilityCheck.isEnabled(
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            context.packageName,
            HomeAccessibilityService::class.java.name,
        )
    }
}
