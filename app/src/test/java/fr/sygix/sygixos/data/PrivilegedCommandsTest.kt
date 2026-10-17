/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.model.AdbAccessState
import fr.sygix.sygixos.model.HomeScreenSnapshot
import fr.sygix.sygixos.model.StockHomeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivilegedCommandsTest {
    private val stock = StockHomePackages.first()
    private val other = "another.accessibility/.Service"

    @Test fun everyRealShapeIsAllowed() {
        listOf(
            "appops get --user 0 fr.sygix.sygixos AUTO_START",
            "appops get --user 10 fr.sygix.sygixos APP_AUTO_START",
            "appops get --user 0 fr.sygix.sygixos SYSTEM_ALERT_WINDOW",
            "appops set --user 0 fr.sygix.sygixos AUTO_START allow",
            "appops set --user 0 fr.sygix.sygixos APP_AUTO_START allow",
            "appops set --user 0 fr.sygix.sygixos SYSTEM_ALERT_WINDOW allow",
            "cmd appops write-settings",
            "cmd role get-role-holders --user 0 android.app.role.HOME",
            "cmd package set-home-activity --user 0 $SygixHomeComponent",
            "cmd package set-home-activity --user 0 com.vendor.home/com.vendor.home.Launcher",
            "pm disable-user --user 0 com.google.android.apps.tv.launcherx",
            "pm default-state --user 0 com.google.android.tungsten.setupwraith",
            "settings --user 0 put secure enabled_accessibility_services '$other:$SygixHomeService'",
            "settings --user 0 put secure enabled_accessibility_services '$other'",
            "settings --user 0 put secure enabled_accessibility_services ''",
            "settings --user 0 put secure accessibility_enabled 1",
        ).forEach { assertTrue(it, PrivilegedCommands.allowed(it)) }
    }

    @Test fun otherTargetsAndCommandsAreRefused() {
        listOf(
            "appops get --user 0 another.app AUTO_START",
            "appops set --user 0 fr.sygix.sygixos.perf AUTO_START allow",
            "appops set --user 0 fr.sygix.sygixos AUTO_START deny",
            "appops set --user 0 fr.sygix.sygixos CAMERA allow",
            "appops set fr.sygix.sygixos AUTO_START allow",
            "cmd role get-role-holders --user 0 android.app.role.SMS",
            "cmd package set-home-activity --user 0 not-a-component",
            "pm disable-user --user 0 com.android.settings",
            "pm uninstall --user 0 $stock",
            "pm enable --user 0 $stock",
            "settings --user 0 put secure accessibility_enabled 0",
            "settings --user 0 put global adb_enabled 0",
            "settings --user 0 put secure enabled_accessibility_services $other",
            "pm grant fr.sygix.sygixos android.permission.WRITE_SECURE_SETTINGS",
            "reboot",
            "id",
            "",
        ).forEach { assertFalse(it, PrivilegedCommands.allowed(it)) }
    }

    @Test fun injectionsAreRefused() {
        listOf(
            "cmd appops write-settings; reboot",
            "cmd appops write-settings && reboot",
            "cmd appops write-settings | sh",
            "cmd appops write-settings\nreboot",
            "pm disable-user --user 0 $stock\$(reboot)",
            "pm disable-user --user 0 `reboot`",
            "appops get --user 0\$(id) fr.sygix.sygixos AUTO_START",
            "settings --user 0 put secure enabled_accessibility_services '$other';reboot;'$SygixHomeService'",
            "settings --user 0 put secure enabled_accessibility_services '$other' ; reboot '",
            "settings --user 0 put secure enabled_accessibility_services '$other",
            "settings --user 0 put secure enabled_accessibility_services '$other:'",
            "settings --user 0 put secure enabled_accessibility_services '\$(reboot)'",
            "cmd package set-home-activity --user 0 a.b/.C > /data/local/tmp/x",
            " cmd appops write-settings",
            "cmd appops write-settings ",
        ).forEach { assertFalse(it, PrivilegedCommands.allowed(it)) }
    }

    @Test fun everyCommandSentByTheShellIsAllowed() = runTest {
        val access = RecordingAccess()
        var snapshot = HomeScreenSnapshot(provisioned = true, ownHomeEnabled = true, enabledHomes = setOf(SygixHomeComponent, "another.home/.Home"), stock = StockHomePackages.associateWith { StockHomeState.ACTIVE })
        var services = other
        var enabled = false
        access.response = { command ->
            when {
                command.startsWith("appops get") -> Result.success(if (access.commands.any { it.startsWith("appops set") }) "AUTO_START: allow" else "AUTO_START: ignore")
                command.startsWith("cmd role") -> Result.success(access.commands.lastOrNull { it.startsWith("cmd package") }?.substringAfterLast(' ')?.substringBefore('/') ?: "another.home")
                command.startsWith("pm disable-user") -> { snapshot = snapshot.copy(stock = snapshot.stock + (stock to StockHomeState.DISABLED_USER)); Result.success("") }
                command.startsWith("pm default-state") -> { snapshot = snapshot.copy(stock = snapshot.stock + (stock to StockHomeState.ACTIVE)); Result.success("") }
                command.contains("SYSTEM_ALERT_WINDOW") -> { snapshot = snapshot.copy(overlay = true); Result.success("") }
                command.contains("enabled_accessibility_services") -> { services = command.substringAfter('\'').removeSuffix("'"); Result.success("") }
                command.endsWith("accessibility_enabled 1") -> { enabled = true; snapshot = snapshot.copy(accessibilityEnabled = true); Result.success("") }
                else -> Result.success("")
            }
        }
        val shell = PrivilegedShell(access, HomeStateReader { Result.success(snapshot) }, 0, { services }, { enabled }, { "another.home/.Home" })
        assertEquals(VendorBootOutcome(true, true), shell.grantVendorBoot().getOrThrow())
        assertEquals("another.home/.Home", shell.previousHome().getOrThrow())
        assertTrue(shell.setHome().isSuccess)
        assertTrue(shell.setHome("another.home/.Home").isSuccess)
        assertTrue(shell.disable(stock).isSuccess)
        assertTrue(shell.restore(stock).isSuccess)
        assertTrue(shell.grantOverlay().isSuccess)
        assertTrue(shell.enableAccessibility().isSuccess)
        assertTrue(shell.rebindAccessibility().isSuccess)
        assertEquals(18, access.commands.size)
        access.commands.forEach { assertTrue(it, PrivilegedCommands.allowed(it)) }
    }

    @Test fun rebindRemovesThenRestoresTheServiceKeepingOthersInOrder() = runTest {
        val access = RecordingAccess()
        var services = "first.app/.One:$SygixHomeService:second.app/.Two"
        access.response = { command -> services = command.substringAfter('\'').removeSuffix("'"); Result.success("") }
        val shell = PrivilegedShell(access, HomeStateReader { Result.success(HomeScreenSnapshot()) }, 3, { services }, { true })
        assertTrue(shell.rebindAccessibility().isSuccess)
        assertEquals(
            listOf(
                "settings --user 3 put secure enabled_accessibility_services 'first.app/.One:second.app/.Two'",
                "settings --user 3 put secure enabled_accessibility_services 'first.app/.One:$SygixHomeService:second.app/.Two'",
            ),
            access.commands,
        )
        assertEquals("first.app/.One:$SygixHomeService:second.app/.Two", services)
    }

    @Test fun rebindWithOnlyOwnServiceWritesAnEmptyListFirst() = runTest {
        val access = RecordingAccess()
        var services = SygixHomeService
        access.response = { command -> services = command.substringAfter('\'').removeSuffix("'"); Result.success("") }
        val shell = PrivilegedShell(access, HomeStateReader { Result.success(HomeScreenSnapshot()) }, 0, { services }, { true })
        assertTrue(shell.rebindAccessibility().isSuccess)
        assertEquals("settings --user 0 put secure enabled_accessibility_services ''", access.commands.first())
    }

    @Test fun rebindIsRefusedWhenTheServiceIsNotListed() = runTest {
        val access = RecordingAccess()
        val shell = PrivilegedShell(access, HomeStateReader { Result.success(HomeScreenSnapshot()) }, 0, { other }, { true })
        assertTrue(shell.rebindAccessibility().isFailure)
        assertTrue(access.commands.isEmpty())
    }

    @Test fun rebindFailsWhenTheListIsNotRestored() = runTest {
        val access = RecordingAccess()
        var services = "$other:$SygixHomeService"
        access.response = { _ -> services = other; Result.success("") }
        val shell = PrivilegedShell(access, HomeStateReader { Result.success(HomeScreenSnapshot()) }, 0, { services }, { true })
        assertTrue(shell.rebindAccessibility().isFailure)
    }

    private class RecordingAccess : AdbAccess {
        override val state = MutableStateFlow(AdbAccessState.READY)
        val commands = mutableListOf<String>()
        var response: (String) -> Result<String> = { Result.success("") }
        override fun debugEnabled() = true
        override suspend fun connect() = Result.success(Unit)
        override suspend fun execute(command: String): Result<String> { commands += command; return response(command) }
        override fun close() = Unit
    }
}
