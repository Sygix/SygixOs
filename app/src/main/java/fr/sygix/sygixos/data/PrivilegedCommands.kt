/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

object PrivilegedCommands {
    const val OwnPackage = "fr.sygix.sygixos"
    const val ComponentPattern = "[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+/\\.?[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)*"
    const val WriteSettings = "cmd appops write-settings"
    val VendorBootOps = listOf("AUTO_START", "APP_AUTO_START")
    private const val OverlayOp = "SYSTEM_ALERT_WINDOW"
    private const val User = "--user [0-9]{1,6}"
    private val own = Regex.escape(OwnPackage)
    private val ops = (VendorBootOps + OverlayOp).joinToString("|")
    private val stock = StockHomePackages.joinToString("|") { Regex.escape(it) }
    private val shapes = listOf(
        "appops get $User $own (?:$ops)",
        "appops set $User $own (?:$ops) allow",
        Regex.escape(WriteSettings),
        "cmd role get-role-holders $User android\\.app\\.role\\.HOME",
        "cmd package set-home-activity $User $ComponentPattern",
        "pm (?:disable-user|default-state) $User (?:$stock)",
        "settings $User put secure enabled_accessibility_services '(?:$ComponentPattern(?::$ComponentPattern)*)?'",
        "settings $User put secure accessibility_enabled 1",
    ).map(::Regex)

    fun allowed(command: String): Boolean = shapes.any { it.matches(command) }

    fun appopsGet(user: Int, op: String) = "appops get --user $user $OwnPackage $op"
    fun appopsAllow(user: Int, op: String) = "appops set --user $user $OwnPackage $op allow"
    fun overlayAllow(user: Int) = appopsAllow(user, OverlayOp)
    fun homeHolders(user: Int) = "cmd role get-role-holders --user $user android.app.role.HOME"
    fun setHome(user: Int, component: String) = "cmd package set-home-activity --user $user $component"
    fun disable(user: Int, packageName: String) = "pm disable-user --user $user $packageName"
    fun restore(user: Int, packageName: String) = "pm default-state --user $user $packageName"
    fun accessibilityServices(user: Int, services: List<String>) =
        "settings --user $user put secure enabled_accessibility_services '${services.joinToString(":")}'"
    fun accessibilityOn(user: Int) = "settings --user $user put secure accessibility_enabled 1"
}
