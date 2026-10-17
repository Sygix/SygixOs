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
    suspend fun rebindAccessibility(): Result<Unit> = Result.failure(UnsupportedOperationException("Rebind unavailable"))
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

    private suspend fun send(value: String): Result<String> {
        require(PrivilegedCommands.allowed(value))
        return access.execute(value)
    }
    private suspend fun command(value: String): String = send(value).getOrThrow()
    private fun services(): List<String> = readServices().orEmpty().split(':').filter { it.isNotBlank() && it != "null" }
    private suspend fun snapshot() = reader.read().getOrThrow()
    private suspend fun <T> operation(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        Result.failure(failure)
    }

    override suspend fun grantVendorBoot(): Result<VendorBootOutcome> = operation {
        val op = PrivilegedCommands.VendorBootOps.firstOrNull { candidate ->
            val response = send(PrivilegedCommands.appopsGet(user, candidate))
            val failure = response.exceptionOrNull()
            if (failure != null && !(failure is ShellOperationFailure && failure.output.contains("Unknown operation", ignoreCase = true))) throw failure
            response.isSuccess && !response.getOrThrow().contains("Unknown operation", ignoreCase = true)
        } ?: return@operation VendorBootOutcome(false)
        val before = command(PrivilegedCommands.appopsGet(user, op))
        val modified = !Regex("\\b$op: allow\\b").containsMatchIn(before)
        if (modified) {
            command(PrivilegedCommands.appopsAllow(user, op))
            command(PrivilegedCommands.WriteSettings)
            check(Regex("\\b$op: allow\\b").containsMatchIn(command(PrivilegedCommands.appopsGet(user, op))))
        }
        VendorBootOutcome(true, modified)
    }

    override suspend fun previousHome(): Result<String?> = operation {
        val holders = command(PrivilegedCommands.homeHolders(user)).lines().filter { it.isNotBlank() }
        if (holders.isEmpty() || holders.singleOrNull() == "fr.sygix.sygixos") return@operation null
        check(holders.size == 1)
        val candidates = snapshot().enabledHomes.filter { it.substringBefore('/') == holders.single().trim() }
        snapshot().resolvedHome?.takeIf { it in candidates } ?: candidates.singleOrNull()
    }

    override suspend fun setHome(component: String): Result<Unit> = operation {
        require(validComponent(component))
        val allowed = component == SygixHomeComponent || component in snapshot().enabledHomes
        require(allowed && (component == SygixHomeComponent || component == recordedPreviousHome()))
        command(PrivilegedCommands.setHome(user, component))
        val holder = command(PrivilegedCommands.homeHolders(user))
        check(holder.lines().any { it.trim() == component.substringBefore('/') })
    }

    override suspend fun disable(packageName: String): Result<Unit> = operation {
        require(packageName in StockHomePackages)
        val before = snapshot()
        check(before.canDisable && before.stock[packageName] == StockHomeState.ACTIVE)
        command(PrivilegedCommands.disable(user, packageName))
        check(snapshot().stock[packageName] == StockHomeState.DISABLED_USER)
    }

    override suspend fun restore(packageName: String): Result<Unit> = operation {
        require(packageName in StockHomePackages)
        command(PrivilegedCommands.restore(user, packageName))
        check(snapshot().stock[packageName] == StockHomeState.ACTIVE)
    }

    override suspend fun grantOverlay(): Result<Unit> = operation {
        if (!snapshot().overlay) {
            command(PrivilegedCommands.overlayAllow(user))
            command(PrivilegedCommands.WriteSettings)
            check(snapshot().overlay)
        }
    }

    override suspend fun enableAccessibility(): Result<Unit> = operation {
        val before = services()
        require(before.all(::validComponent))
        val ownPresent = before.any(::ownService)
        val next = if (ownPresent) before else before + SygixHomeService
        if (!ownPresent) command(PrivilegedCommands.accessibilityServices(user, next))
        if (!accessibilityEnabled()) command(PrivilegedCommands.accessibilityOn(user))
        val after = readServices().orEmpty().split(':')
        check(after.containsAll(before) && snapshot().accessibilityEnabled && accessibilityEnabled())
    }

    override suspend fun rebindAccessibility(): Result<Unit> = operation {
        val before = services()
        require(before.all(::validComponent) && before.any(::ownService))
        command(PrivilegedCommands.accessibilityServices(user, before.filterNot(::ownService)))
        command(PrivilegedCommands.accessibilityServices(user, before))
        check(services().containsAll(before))
    }

    companion object {
        private val component = Regex(PrivilegedCommands.ComponentPattern)
        fun validComponent(value: String): Boolean = component.matches(value)
        fun ownService(value: String): Boolean = value == SygixHomeService || value == "fr.sygix.sygixos/fr.sygix.sygixos.data.HomeAccessibilityService"
    }
}
