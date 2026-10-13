/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class PrivilegedShellTest {
    private class Access : AdbAccess {
        override val state = MutableStateFlow(AdbAccessState.READY)
        val commands = mutableListOf<String>()
        var response: (String) -> Result<String> = { Result.success("") }
        override fun debugEnabled() = true
        override suspend fun connect() = Result.success(Unit)
        override suspend fun execute(command: String): Result<String> { commands += command; return response(command) }
        override fun close() = Unit
    }
    private var snapshot = HomeScreenSnapshot(provisioned = true, ownHomeEnabled = true, stock = StockHomePackages.associateWith { StockHomeState.ACTIVE })
    private var services: String? = null
    private var enabled = false
    private val access = Access()
    private fun shell(previous: String? = null) = PrivilegedShell(access, HomeStateReader { Result.success(snapshot) }, 12, { services }, { enabled }, { previous })

    @Test fun arbitraryTargetsAreRejectedWithoutSending() = runTest {
        assertTrue(shell().disable("another.app").isFailure)
        assertTrue(shell().restore("fr.sygix.sygixos").isFailure)
        assertTrue(shell().setHome("another.app/.Home").isFailure)
        assertTrue(shell().setHome("com.google.android.apps.tv.launcherx/.Home;id").isFailure)
        assertTrue(access.commands.isEmpty())
    }

    @Test fun disableRequiresReadBackAndCurrentUser() = runTest {
        val packageName = StockHomePackages.first()
        assertTrue(shell().disable(packageName).isFailure)
        assertEquals(listOf("pm disable-user --user 12 $packageName"), access.commands)
        access.response = { snapshot = snapshot.copy(stock = snapshot.stock + (packageName to StockHomeState.DISABLED_USER)); Result.success("") }
        assertTrue(shell().disable(packageName).isSuccess)
    }

    @Test fun unconfiguredDeviceCannotBeDisabled() = runTest {
        snapshot = snapshot.copy(provisioned = false)
        assertTrue(shell().disable(StockHomePackages.first()).isFailure)
        assertTrue(access.commands.isEmpty())
    }

    @Test fun accessibilityPreservesOtherServicesAndVerifiesBothSettings() = runTest {
        services = "another.accessibility/.Service"
        access.response = { command ->
            if (command.contains("enabled_accessibility_services")) services = "another.accessibility/.Service:$SygixHomeService"
            if (command.endsWith("accessibility_enabled 1")) { enabled = true; snapshot = snapshot.copy(accessibilityEnabled = true) }
            Result.success("")
        }
        assertTrue(shell().enableAccessibility().isSuccess)
        assertEquals(listOf("settings --user 12 put secure enabled_accessibility_services 'another.accessibility/.Service:$SygixHomeService'", "settings --user 12 put secure accessibility_enabled 1"), access.commands)
        assertTrue(services!!.startsWith("another.accessibility/.Service:"))
    }

    @Test fun malformedAccessibilityListCannotInjectCommands() = runTest {
        services = "another.accessibility/.Service';id"
        assertTrue(shell().enableAccessibility().isFailure)
        assertTrue(access.commands.isEmpty())
    }

    @Test fun accessibilityWithoutReadBackCannotSucceed() = runTest {
        assertTrue(shell().enableAccessibility().isFailure)
    }

    @Test fun overlayIsFlushedAndReadBackBeforeSuccess() = runTest {
        assertTrue(shell().grantOverlay().isFailure)
        assertEquals(listOf("appops set --user 12 fr.sygix.sygixos SYSTEM_ALERT_WINDOW allow", "cmd appops write-settings"), access.commands)
        access.response = { snapshot = snapshot.copy(overlay = true); Result.success("") }
        assertTrue(shell().grantOverlay().isSuccess)
    }

    @Test fun noVendorOperationIsNotAnError() = runTest {
        access.response = { Result.failure(ShellOperationFailure("Unknown operation")) }
        assertEquals(VendorBootOutcome(false), shell().grantVendorBoot().getOrThrow())
        assertEquals(2, access.commands.size)
        assertTrue(access.commands.all { it.startsWith("appops get --user 12 fr.sygix.sygixos ") })
    }

    @Test fun alreadyAllowedVendorBootDoesNotWrite() = runTest {
        access.response = { Result.success("AUTO_START: allow") }
        assertEquals(VendorBootOutcome(true, false), shell().grantVendorBoot().getOrThrow())
        assertTrue(access.commands.none { it.startsWith("appops set") })
    }

    @Test fun vendorReadFailureIsNotAbsence() = runTest {
        access.response = { Result.failure(ShellOperationFailure("Permission denied")) }
        assertTrue(shell().grantVendorBoot().isFailure)
    }

    @Test fun previousHomeMustBeRecordedAndEnabled() = runTest {
        val component = "another.home/.Home"
        snapshot = snapshot.copy(enabledHomes = setOf(component))
        assertTrue(shell().setHome(component).isFailure)
        assertTrue(access.commands.isEmpty())
        access.response = { if (it.startsWith("cmd role")) Result.success("another.home") else Result.success("Success") }
        assertTrue(shell(component).setHome(component).isSuccess)
    }

    @Test fun effectiveHomeRequiresResolutionAndHighestPriority() {
        val own = "fr.sygix.sygixos"
        assertFalse(isEffectiveHome(SygixHomeComponent, listOf(own to 0, "stock.home" to 2), own))
        assertFalse(isEffectiveHome("android/.ResolverActivity", listOf(own to 0), own))
        assertTrue(isEffectiveHome(SygixHomeComponent, listOf(own to 0, "settings.home" to -1000), own))
    }
}
