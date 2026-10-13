/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import fr.sygix.sygixos.model.HomeScreenSnapshot
import fr.sygix.sygixos.model.StockHomeState

fun interface HomeStateReader {
    fun read(): Result<HomeScreenSnapshot>
}

class HomeScreenStateReader(private val context: Context) : HomeStateReader {
    override fun read(): Result<HomeScreenSnapshot> = runCatching {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val homes = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        val own = homes.firstOrNull { it.activityInfo.packageName == context.packageName }
        val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.let {
            ComponentName(it.packageName, it.name).flattenToShortString()
        }
        val role = if (context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)) context.packageName else null
        HomeScreenSnapshot(
            effective = isEffectiveHome(resolved, homes.map { it.activityInfo.packageName to it.priority }, context.packageName),
            roleHolder = role,
            resolvedHome = resolved,
            stock = StockHomePackages.associateWith { packageName ->
                try {
                    pm.getApplicationInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS)
                    when (pm.getApplicationEnabledSetting(packageName)) {
                        PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> StockHomeState.ACTIVE
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER -> StockHomeState.DISABLED_USER
                        else -> StockHomeState.OUT_OF_REACH
                    }
                } catch (_: PackageManager.NameNotFoundException) {
                    StockHomeState.ABSENT
                }
            },
            provisioned = Settings.Global.getInt(context.contentResolver, Settings.Global.DEVICE_PROVISIONED, 0) == 1 &&
                Settings.Secure.getInt(context.contentResolver, "user_setup_complete", 0) == 1,
            ownHomeEnabled = own?.activityInfo?.enabled == true,
            accessibilityEnabled = AndroidLauncherSystemGateway.serviceEnabledIn(context),
            accessibilityConnected = AccessibilityConnection.connected.value,
            overlay = Settings.canDrawOverlays(context),
            vendorManager = try { pm.getApplicationInfo("com.tcl.guard", 0); true } catch (_: PackageManager.NameNotFoundException) { false },
            enabledHomes = homes.map { ComponentName(it.activityInfo.packageName, it.activityInfo.name).flattenToShortString() }.toSet(),
        )
    }

    companion object {
        fun currentUser(): Int = Process.myUid() / 100_000
    }
}

val StockHomePackages = listOf("com.google.android.tungsten.setupwraith", "com.google.android.apps.tv.launcherx")
const val SygixHomeComponent = "fr.sygix.sygixos/.ui.MainActivity"
const val SygixHomeService = "fr.sygix.sygixos/.data.HomeAccessibilityService"

fun isEffectiveHome(resolved: String?, homes: List<Pair<String, Int>>, ownPackage: String): Boolean {
    if (resolved?.substringBefore('/') != ownPackage) return false
    val ownPriority = homes.filter { it.first == ownPackage }.maxOfOrNull { it.second } ?: return false
    return homes.none { it.first != ownPackage && it.second > ownPriority }
}
