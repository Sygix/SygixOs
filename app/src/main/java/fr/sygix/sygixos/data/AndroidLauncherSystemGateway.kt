/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.net.toUri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import fr.sygix.sygixos.domain.LauncherSystemGateway
import fr.sygix.sygixos.model.SystemControlState

class AndroidLauncherSystemGateway(
    private val context: Context,
    private val homeRole: HomeRoleGateway,
    private val resolves: (Intent) -> Boolean = { context.packageManager.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY) != null },
    private val open: (Intent) -> Boolean = { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess },
    private val serviceEnabled: () -> Boolean = {
        context.getSystemService(AccessibilityManager::class.java)?.getEnabledAccessibilityServiceList(-1)?.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName && it.resolveInfo.serviceInfo.name == HomeAccessibilityService::class.java.name
        } == true
    },
    private val serviceRunning: () -> Boolean = { HomeAccessibilityService.running },
    private val overlayGranted: () -> Boolean = { Settings.canDrawOverlays(context) },
    private val readBootCount: () -> Int? = { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) },
) : LauncherSystemGateway {
    private fun accessibilityIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    private fun overlayIntent() = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
    private fun resolved(intent: Intent) = runCatching { resolves(intent) }.getOrDefault(false)

    override fun homeRoleState(): SystemControlState = homeRole.state()
    override fun accessibilityState(): SystemControlState {
        if (runCatching { serviceEnabled() && serviceRunning() }.getOrDefault(false)) return SystemControlState.ACTIVE
        return if (resolved(accessibilityIntent())) SystemControlState.INACTIVE else SystemControlState.UNAVAILABLE
    }
    override fun overlayState(): SystemControlState {
        if (runCatching(overlayGranted).getOrDefault(false)) return SystemControlState.ACTIVE
        return if (resolved(overlayIntent())) SystemControlState.INACTIVE else SystemControlState.UNAVAILABLE
    }
    override fun bootCount(): Int? = runCatching(readBootCount).getOrNull()?.takeIf { it >= 0 }
    override fun openHomeRole() = homeRole.request()
    override fun openHomeRoleFallback() = homeRole.fallback()
    override fun openAccessibility() = openResolved(accessibilityIntent())
    override fun openOverlay() = openResolved(overlayIntent())
    private fun openResolved(intent: Intent): Boolean = resolved(intent) && runCatching { open(intent) }.getOrDefault(false)
}
