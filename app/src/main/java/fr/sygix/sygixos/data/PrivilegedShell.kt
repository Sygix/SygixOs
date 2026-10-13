/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.model.StockHomeState

data class VendorBootOutcome(val present: Boolean, val modified: Boolean = false)

interface HomeOperations {
    suspend fun grantVendorBoot(): Result<VendorBootOutcome>
    suspend fun previousHome(): Result<String?>
    suspend fun setHome(component: String = SygixHomeComponent): Result<Unit>
    suspend fun disable(packageName: String): Result<Unit>
    suspend fun restore(packageName: String): Result<Unit>
    suspend fun grantOverlay(): Result<Unit>
    suspend fun enableAccessibility(): Result<Unit>
}

class PrivilegedShell(
    private val access: AdbAccess,
    private val reader: HomeStateReader,
    private val user: Int,
    private val readServices: () -> String?,
    private val accessibilityEnabled: () -> Boolean,
    private val recordedPreviousHome: suspend () -> String? = { null },
) : HomeOperations {
    init { require(user >= 0) }

    private suspend fun command(value: String): String = access.execute(value).getOrThrow()
    private fun snapshot() = reader.read().getOrThrow()
    private suspend fun <T> operation(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    override suspend fun grantVendorBoot(): Result<VendorBootOutcome> = operation {
        val op = listOf("AUTO_START", "APP_AUTO_START").firstOrNull { candidate ->
            val response = access.execute("appops get --user $user fr.sygix.sygixos $candidate")
            val failure = response.exceptionOrNull()
            if (failure != null && !(failure is ShellOperationFailure && failure.output.contains("Unknown operation", ignoreCase = true))) throw failure
            response.isSuccess && !response.getOrThrow().contains("Unknown operation", ignoreCase = true)
        } ?: return@operation VendorBootOutcome(false)
        val before = command("appops get --user $user fr.sygix.sygixos $op")
        val modified = !Regex("\\b$op: allow\\b").containsMatchIn(before)
        if (modified) {
            command("appops set --user $user fr.sygix.sygixos $op allow")
            command("cmd appops write-settings")
            check(Regex("\\b$op: allow\\b").containsMatchIn(command("appops get --user $user fr.sygix.sygixos $op")))
        }
        VendorBootOutcome(true, modified)
    }

    override suspend fun previousHome(): Result<String?> = operation {
        val holders = command("cmd role get-role-holders --user $user android.app.role.HOME").lines().filter { it.isNotBlank() }
        if (holders.isEmpty() || holders.singleOrNull() == "fr.sygix.sygixos") return@operation null
        check(holders.size == 1)
        val candidates = snapshot().enabledHomes.filter { it.substringBefore('/') == holders.single().trim() }
        snapshot().resolvedHome?.takeIf { it in candidates } ?: candidates.singleOrNull()
    }

    override suspend fun setHome(component: String): Result<Unit> = operation {
        require(validComponent(component))
        val allowed = component == SygixHomeComponent || component in snapshot().enabledHomes
        require(allowed && (component == SygixHomeComponent || component == recordedPreviousHome()))
        command("cmd package set-home-activity --user $user $component")
        val holder = command("cmd role get-role-holders --user $user android.app.role.HOME")
        check(holder.lines().any { it.trim() == component.substringBefore('/') })
    }

    override suspend fun disable(packageName: String): Result<Unit> = operation {
        require(packageName in StockHomePackages)
        val before = snapshot()
        check(before.canDisable && before.stock[packageName] == StockHomeState.ACTIVE)
        command("pm disable-user --user $user $packageName")
        check(snapshot().stock[packageName] == StockHomeState.DISABLED_USER)
    }

    override suspend fun restore(packageName: String): Result<Unit> = operation {
        require(packageName in StockHomePackages)
        command("pm default-state --user $user $packageName")
        check(snapshot().stock[packageName] == StockHomeState.ACTIVE)
    }

    override suspend fun grantOverlay(): Result<Unit> = operation {
        if (!snapshot().overlay) {
            command("appops set --user $user fr.sygix.sygixos SYSTEM_ALERT_WINDOW allow")
            command("cmd appops write-settings")
            check(snapshot().overlay)
        }
    }

    override suspend fun enableAccessibility(): Result<Unit> = operation {
        val before = readServices().orEmpty().split(':').filter { it.isNotBlank() && it != "null" }
        require(before.all(::validComponent))
        val ownPresent = before.any { it == SygixHomeService || it == "fr.sygix.sygixos/fr.sygix.sygixos.data.HomeAccessibilityService" }
        val next = if (ownPresent) before else before + SygixHomeService
        if (!ownPresent) command("settings --user $user put secure enabled_accessibility_services '${next.joinToString(":")}'")
        if (!accessibilityEnabled()) command("settings --user $user put secure accessibility_enabled 1")
        val after = readServices().orEmpty().split(':')
        check(after.containsAll(before) && snapshot().accessibilityEnabled && accessibilityEnabled())
    }

    companion object {
        fun validComponent(value: String): Boolean = Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+/\\.?[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)*").matches(value)
    }
}
