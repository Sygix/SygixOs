/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.data.*
import fr.sygix.sygixos.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLauncherControllerTest {
    private class Fixture(scope: kotlinx.coroutines.CoroutineScope) : AdbAccess, HomeOperations, HomeStateReader, DefaultLauncherStore {
        override val state = MutableStateFlow(AdbAccessState.DISABLED)
        var debug = true
        var authorized = true
        var connectFailure = false
        var effectiveAfterHome = true
        var homeFailure = false
        var vendorFailure = false
        var readFailure = false
        var failDisable: String? = null
        var failRestore: String? = null
        var wait: CompletableDeferred<Unit>? = null
        var snapshot = HomeScreenSnapshot(provisioned = true, ownHomeEnabled = true,
            resolvedHome = "com.google.android.apps.tv.launcherx/.home.HomeActivity", stock = StockHomePackages.associateWith { StockHomeState.ACTIVE })
        var memory = DefaultLauncherMemory()
        var boot = false
        var closed = 0
        val readReplies = ArrayDeque<CompletableDeferred<Result<HomeScreenSnapshot>>>()
        val calls = mutableListOf<String>()
        val controller = DefaultLauncherController(this, this, this, this, scope, { boot = true }, {}, { true }, verificationMs = 300)
        override fun debugEnabled() = debug
        override fun previouslyAuthorized() = authorized
        override suspend fun connect(): Result<Unit> { calls += "connect"; state.value = if (connectFailure) AdbAccessState.UNREACHABLE else AdbAccessState.READY; return if (connectFailure) Result.failure(IllegalStateException()) else Result.success(Unit) }
        override suspend fun execute(command: String) = Result.success("")
        override fun close() { closed++; if (state.value == AdbAccessState.READY) state.value = AdbAccessState.LOST }
        override suspend fun read(): Result<HomeScreenSnapshot> {
            readReplies.removeFirstOrNull()?.let { return it.await() }
            return if (readFailure) Result.failure(IllegalStateException()) else Result.success(snapshot)
        }
        override suspend fun loadMemory(): DefaultLauncherMemory = memory
        override suspend fun rememberPrevious(component: String) { if (memory.previousHome == null) memory = memory.copy(previousHome = component) }
        override suspend fun record(step: SetupStep) { memory = memory.copy(modifications = memory.modifications + step) }
        override suspend fun grantVendorBoot(): Result<VendorBootOutcome> { calls += "vendor"; return if (vendorFailure) Result.failure(IllegalStateException()) else Result.success(VendorBootOutcome(false)) }
        override suspend fun previousHome(): Result<String?> = Result.success(snapshot.resolvedHome)
        override suspend fun setHome(component: String): Result<Unit> { calls += "home:$component"; wait?.await(); if (homeFailure) return Result.failure(IllegalStateException()); snapshot = snapshot.copy(effective = effectiveAfterHome && component == SygixHomeComponent, roleHolder = component.substringBefore('/')); return Result.success(Unit) }
        override suspend fun disable(packageName: String): Result<Unit> { calls += "disable:$packageName"; if (packageName == failDisable) return Result.failure(IllegalStateException()); snapshot = snapshot.copy(stock = snapshot.stock + (packageName to StockHomeState.DISABLED_USER), effective = packageName == StockHomePackages.last()); return Result.success(Unit) }
        override suspend fun restore(packageName: String): Result<Unit> { calls += "restore:$packageName"; if (packageName == failRestore) return Result.failure(IllegalStateException()); snapshot = snapshot.copy(stock = snapshot.stock + (packageName to StockHomeState.ACTIVE), effective = false); return Result.success(Unit) }
        override suspend fun grantOverlay(): Result<Unit> { calls += "overlay"; snapshot = snapshot.copy(overlay = true); return Result.success(Unit) }
        override suspend fun enableAccessibility(): Result<Unit> { calls += "accessibility"; snapshot = snapshot.copy(accessibilityEnabled = true, accessibilityConnected = true); return Result.success(Unit) }
    }

    @Test fun anOlderResumeReadCannotOverwriteANewerOne() = runTest {
        val f = Fixture(backgroundScope)
        val stale = CompletableDeferred<Result<HomeScreenSnapshot>>()
        f.readReplies += stale
        runCurrent()
        assertNull(f.controller.state.value.snapshot)
        f.snapshot = f.snapshot.copy(overlay = true)
        f.controller.onResume(); runCurrent()
        assertEquals(f.snapshot, f.controller.state.value.snapshot)
        stale.complete(Result.success(f.snapshot.copy(overlay = false)))
        runCurrent()
        assertEquals(f.snapshot, f.controller.state.value.snapshot)
    }

    @Test fun restoreDecidesOnItsOwnReadWhileAResumeIsPending() = runTest {
        val previous = "com.google.android.apps.tv.launcherx/.home.HomeActivity"
        val f = Fixture(backgroundScope).apply { wait = CompletableDeferred(); memory = DefaultLauncherMemory(previous) }
        f.snapshot = f.snapshot.copy(stock = StockHomePackages.associateWith { StockHomeState.DISABLED_USER }, enabledHomes = setOf(previous))
        runCurrent()
        f.controller.restore(); runCurrent()
        val decision = CompletableDeferred<Result<HomeScreenSnapshot>>()
        val resumed = CompletableDeferred<Result<HomeScreenSnapshot>>()
        f.readReplies.addAll(listOf(decision, resumed))
        f.wait!!.complete(Unit); runCurrent()
        f.controller.onResume(); runCurrent()
        decision.complete(Result.success(f.snapshot))
        runCurrent()
        assertEquals(SetupScreen.PANEL, f.controller.state.value.screen)
        resumed.complete(Result.success(f.snapshot))
        runCurrent()
        assertEquals(SetupScreen.PANEL, f.controller.state.value.screen)
        assertFalse(f.controller.state.value.busy)
    }

    @Test fun sequenceFinalReadCannotOverwriteANewerResume() = runTest {
        val f = Fixture(backgroundScope).apply { homeFailure = true; wait = CompletableDeferred() }
        f.controller.automatic()
        runCurrent()
        val finalRead = CompletableDeferred<Result<HomeScreenSnapshot>>()
        val resumedRead = CompletableDeferred<Result<HomeScreenSnapshot>>()
        f.readReplies.addAll(listOf(finalRead, resumedRead))
        f.wait!!.complete(Unit)
        runCurrent()
        assertTrue(f.controller.state.value.busy)
        f.controller.onResume()
        runCurrent()
        val current = f.snapshot.copy(overlay = true)
        resumedRead.complete(Result.success(current))
        runCurrent()
        assertEquals(current, f.controller.state.value.snapshot)
        finalRead.complete(Result.success(f.snapshot))
        runCurrent()
        assertEquals(current, f.controller.state.value.snapshot)
        assertFalse(f.controller.state.value.busy)
    }

    @Test fun verificationReadCannotOverwriteANewerResume() = runTest {
        val f = Fixture(backgroundScope).apply { wait = CompletableDeferred() }
        f.controller.automatic()
        runCurrent()
        val verification = CompletableDeferred<Result<HomeScreenSnapshot>>()
        val resumedRead = CompletableDeferred<Result<HomeScreenSnapshot>>()
        f.readReplies.addAll(listOf(verification, resumedRead))
        f.wait!!.complete(Unit)
        runCurrent()
        f.controller.onResume()
        runCurrent()
        f.snapshot = f.snapshot.copy(overlay = true)
        resumedRead.complete(Result.success(f.snapshot))
        runCurrent()
        verification.complete(Result.success(f.snapshot.copy(overlay = false, effective = false)))
        runCurrent()
        assertEquals(f.snapshot, f.controller.state.value.snapshot)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(SetupScreen.DONE, f.controller.state.value.screen)
    }

    @Test fun configurationUsesLeastIntrusiveSequenceAndCloses() = runTest {
        val f = Fixture(backgroundScope)
        f.controller.automatic(); runCurrent()
        assertEquals(listOf("connect", "vendor", "home:$SygixHomeComponent"), f.calls)
        assertEquals(SetupScreen.DONE, f.controller.state.value.screen)
        assertEquals("com.google.android.apps.tv.launcherx/.home.HomeActivity", f.memory.previousHome)
        assertFalse(SetupStep.VENDOR_BOOT in f.controller.state.value.steps)
        assertTrue(f.closed > 0)
    }

    @Test fun newKeyWaitsForContinueBeforeAnyConnection() = runTest {
        val f = Fixture(backgroundScope).apply { authorized = false }
        f.controller.automatic(); runCurrent()
        assertEquals(SetupScreen.AUTHORIZE, f.controller.state.value.screen)
        assertTrue(f.calls.isEmpty())
    }

    @Test fun disabledDebugOnlyShowsInstructions() = runTest {
        val f = Fixture(backgroundScope).apply { debug = false }
        f.controller.automatic(); runCurrent()
        assertEquals(SetupScreen.DEBUG, f.controller.state.value.screen)
        assertTrue(f.calls.isEmpty())
        f.debug = true; f.controller.onResume()
        assertEquals(SetupScreen.AUTHORIZE, f.controller.state.value.screen)
        assertTrue(f.calls.isEmpty())
    }

    @Test fun inaccessiblePortReturnsUnavailableChoice() = runTest {
        val f = Fixture(backgroundScope).apply { connectFailure = true }
        f.controller.automatic(); runCurrent()
        assertEquals(SetupScreen.CHOICE, f.controller.state.value.screen)
        assertEquals(AutomaticHomeState.UNAVAILABLE, f.controller.state.value.automatic)
        assertFalse("vendor" in f.calls)
    }

    @Test fun vendorFailureDoesNotPreventIndependentHomeStep() = runTest {
        val f = Fixture(backgroundScope).apply { vendorFailure = true }
        f.controller.automatic(); runCurrent()
        assertTrue("home:$SygixHomeComponent" in f.calls)
        assertEquals(SetupStepState.FAILED, f.controller.state.value.steps[SetupStep.VENDOR_BOOT])
        assertEquals(SetupScreen.FAILED, f.controller.state.value.screen)
    }

    @Test fun vendorFailureRemainsVisibleAfterKeepingStockAndApplyingFallback() = runTest {
        val f = Fixture(backgroundScope).apply { vendorFailure = true; effectiveAfterHome = false }
        f.controller.automatic(); runCurrent(); advanceTimeBy(400); runCurrent()
        assertEquals(SetupScreen.MORE, f.controller.state.value.screen)
        f.controller.fallback(); runCurrent()
        assertEquals(SetupScreen.FAILED, f.controller.state.value.screen)
        assertEquals(SetupStepState.FAILED, f.controller.state.value.steps[SetupStep.VENDOR_BOOT])
        assertEquals(SetupStepState.DONE, f.controller.state.value.steps[SetupStep.OVERLAY])
        assertTrue(f.boot)
    }

    @Test fun homeFailureCancelsVerificationWithoutRetry() = runTest {
        val f = Fixture(backgroundScope).apply { homeFailure = true }
        f.controller.automatic(); runCurrent()
        assertEquals(SetupScreen.FAILED, f.controller.state.value.screen)
        assertEquals(SetupStepState.CANCELLED, f.controller.state.value.steps[SetupStep.VERIFY])
        assertEquals(1, f.calls.count { it.startsWith("home:") })
    }

    @Test fun lowerPriorityHomeProposesDisableWithoutChangingPackages() = runTest {
        val f = Fixture(backgroundScope).apply { effectiveAfterHome = false }
        f.controller.automatic(); runCurrent(); advanceTimeBy(400); runCurrent()
        assertEquals(SetupScreen.MORE, f.controller.state.value.screen)
        assertTrue(f.calls.none { it.startsWith("disable:") })
        f.controller.confirmDisable()
        assertEquals(SetupScreen.DISABLE, f.controller.state.value.screen)
        f.controller.back()
        assertEquals(SetupScreen.MORE, f.controller.state.value.screen)
    }

    @Test fun keepingStockAppliesAndVerifiesFallback() = runTest {
        val f = Fixture(backgroundScope)
        f.snapshot = f.snapshot.copy(roleHolder = "fr.sygix.sygixos")
        f.controller.fallback(); runCurrent()
        assertEquals(listOf("connect", "accessibility", "overlay"), f.calls)
        assertTrue(f.boot)
        assertEquals(SetupScreen.FALLBACK, f.controller.state.value.screen)
        assertEquals(AutomaticHomeState.ACTIVE, f.controller.state.value.automatic)
    }

    @Test fun guardPreventsDisableAndFallsBack() = runTest {
        val f = Fixture(backgroundScope)
        f.snapshot = f.snapshot.copy(provisioned = false)
        f.controller.disable(); runCurrent()
        assertTrue(f.calls.none { it.startsWith("disable:") })
        assertEquals(SetupScreen.FALLBACK, f.controller.state.value.screen)
    }

    @Test fun successfulDisableUsesClosedSetOrder() = runTest {
        val f = Fixture(backgroundScope)
        f.controller.disable(); runCurrent()
        assertEquals(StockHomePackages.map { "disable:$it" }, f.calls.filter { it.startsWith("disable:") })
        assertEquals(SetupScreen.DONE, f.controller.state.value.screen)
        assertTrue(f.snapshot.stock.values.all { it == StockHomeState.DISABLED_USER })
    }

    @Test fun partialDisableRollsBackOnlyChangedPackagesInReverse() = runTest {
        val f = Fixture(backgroundScope).apply { failDisable = StockHomePackages.last() }
        f.controller.disable(); runCurrent()
        assertEquals(listOf("restore:${StockHomePackages.first()}"), f.calls.filter { it.startsWith("restore:") })
        assertTrue(f.snapshot.stock.values.all { it == StockHomeState.ACTIVE })
        assertEquals(SetupScreen.FALLBACK, f.controller.state.value.screen)
    }

    @Test fun restoreWithoutPreviousDoesNotInventHomeOrRevokePermissions() = runTest {
        val f = Fixture(backgroundScope)
        f.snapshot = f.snapshot.copy(stock = StockHomePackages.associateWith { StockHomeState.DISABLED_USER }, overlay = true, accessibilityEnabled = true)
        f.controller.restore(); runCurrent()
        assertEquals(StockHomePackages.asReversed().map { "restore:$it" }, f.calls.filter { it.startsWith("restore:") })
        assertTrue(f.calls.none { it.startsWith("home:") })
        assertTrue(f.snapshot.overlay && f.snapshot.accessibilityEnabled)
        assertEquals(SetupScreen.PANEL, f.controller.state.value.screen)
    }

    @Test fun restoreReusesRecordedEnabledHome() = runTest {
        val f = Fixture(backgroundScope)
        val previous = "com.google.android.apps.tv.launcherx/.home.HomeActivity"
        f.memory = DefaultLauncherMemory(previous)
        f.snapshot = f.snapshot.copy(stock = StockHomePackages.associateWith { StockHomeState.DISABLED_USER }, enabledHomes = setOf(previous))
        f.controller.restore(); runCurrent()
        assertEquals("home:$previous", f.calls.last())
    }

    @Test fun failedRestoreNeverShowsSuccess() = runTest {
        val f = Fixture(backgroundScope).apply { failRestore = StockHomePackages.last() }
        f.snapshot = f.snapshot.copy(stock = StockHomePackages.associateWith { StockHomeState.DISABLED_USER })
        f.controller.restore(); runCurrent()
        assertEquals(SetupScreen.FAILED, f.controller.state.value.screen)
        assertTrue(f.controller.state.value.snapshot!!.canRestore)
    }

    @Test fun closingUiDoesNotCancelScopeOwnedSequence() = runTest {
        val f = Fixture(backgroundScope).apply { wait = CompletableDeferred() }
        f.controller.automatic(); runCurrent()
        assertEquals(SetupScreen.PROGRESS, f.controller.state.value.screen)
        assertFalse(f.controller.back())
        f.controller.onPanelClosed()
        f.wait!!.complete(Unit); runCurrent()
        assertEquals(SetupScreen.DONE, f.controller.state.value.screen)
        f.controller.openPanel()
        assertEquals(SetupScreen.DONE, f.controller.state.value.screen)
    }

    @Test fun failedReadIsNotAbsenceAndCannotBeActive() = runTest {
        val f = Fixture(backgroundScope)
        f.snapshot = f.snapshot.copy(effective = true)
        f.readFailure = true; f.controller.onResume(); runCurrent()
        assertTrue(f.controller.state.value.readFailed)
        assertEquals(AutomaticHomeState.TODO, f.controller.state.value.automatic)
    }

    @Test fun externalChangesAreReadWithoutAutomaticReapplication() = runTest {
        val f = Fixture(backgroundScope)
        f.controller.automatic(); runCurrent()
        assertEquals(AutomaticHomeState.ACTIVE, f.controller.state.value.automatic)
        val calls = f.calls.toList()
        f.snapshot = f.snapshot.copy(effective = false, roleHolder = null)
        f.controller.onResume(); runCurrent()
        assertEquals(AutomaticHomeState.TODO, f.controller.state.value.automatic)
        assertEquals(calls, f.calls)
    }

    @Test fun inaccessiblePreviousHomeIsNotRestored() = runTest {
        val f = Fixture(backgroundScope)
        f.memory = DefaultLauncherMemory("removed.home/.Home")
        f.snapshot = f.snapshot.copy(stock = StockHomePackages.associateWith { StockHomeState.DISABLED_USER })
        f.controller.restore(); runCurrent()
        assertTrue(f.calls.none { it.startsWith("home:") })
        assertEquals(SetupScreen.PANEL, f.controller.state.value.screen)
    }
}
