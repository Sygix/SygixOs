/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.data.*
import fr.sygix.sygixos.model.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLauncherTsuyuTest {
    private class Fixture(scope: CoroutineScope) : AdbAccess, HomeOperations, HomeStateReader, DefaultLauncherStore, TsuyuLink, TsuyuReleases, TsuyuInstaller {
        override val state = MutableStateFlow(AdbAccessState.DISABLED)
        var debug = false
        var options = false
        var adbAuthorized = false
        var port = true
        var probe = TsuyuProbe()
        var release: TsuyuRelease? = TsuyuRelease("v1.0.0", false, "https://github.com/Sygix/Tsuyu/releases/download/v1.0.0/tsuyu.apk", 10, "a".repeat(64))
        var installResult: Result<Unit> = Result.success(Unit)
        var installGate: CompletableDeferred<Unit>? = null
        var permission = TsuyuPermission.GRANTED
        var opened = 0
        var boot: Int? = 4
        var snapshot = HomeScreenSnapshot(provisioned = true, ownHomeEnabled = true, resolvedHome = "com.google.android.apps.tv.launcherx/.home.HomeActivity",
            stock = StockHomePackages.associateWith { StockHomeState.ACTIVE })
        var memory = DefaultLauncherMemory()
        var vendorModified = true
        var failDisable: String? = null
        val calls = mutableListOf<String>()
        var sourceAllowed = true
        val support = TsuyuSupport(this, this, this, includePrereleases = { false }, portReachable = { calls += "port"; port }, usingTsuyu = { probe.authorized && probe.binder }, binderWaitMs = 100, canInstallPackages = { sourceAllowed })
        val controller = DefaultLauncherController(this, this, this, this, scope, {}, { calls += "open-dev"; true }, verificationMs = 200,
            openAbout = { calls += "open-about"; true }, devOptions = { options }, tsuyu = support, bootCount = { boot }, clock = { 42L })

        override fun debugEnabled() = debug || probe.binder && probe.authorized
        override fun previouslyAuthorized() = adbAuthorized || probe.binder && probe.authorized
        override suspend fun connect(): Result<Unit> { calls += "connect"; state.value = AdbAccessState.READY; return Result.success(Unit) }
        override suspend fun execute(command: String) = Result.success("")
        override fun close() { if (state.value == AdbAccessState.READY) state.value = AdbAccessState.LOST }
        override suspend fun read(): Result<HomeScreenSnapshot> = Result.success(snapshot)
        override suspend fun loadMemory() = memory
        override suspend fun rememberPrevious(component: String) = Unit
        override suspend fun record(step: SetupStep) { memory = memory.copy(modifications = memory.modifications + step) }
        override suspend fun markRepairAttempt(step: SetupStep, boot: Int) { memory = memory.copy(repairBoots = memory.repairBoots + (step to boot)) }
        override suspend fun recordRepair(record: RepairRecord) { memory = memory.copy(lastRepair = record) }
        override suspend fun grantVendorBoot(): Result<VendorBootOutcome> { calls += "vendor"; return Result.success(VendorBootOutcome(true, vendorModified)) }
        override suspend fun previousHome(): Result<String?> = Result.success(null)
        override suspend fun setHome(component: String): Result<Unit> { calls += "home"; return Result.success(Unit) }
        override suspend fun disable(packageName: String): Result<Unit> { calls += "disable:$packageName"; if (packageName == failDisable) return Result.failure(IllegalStateException()); snapshot = snapshot.copy(stock = snapshot.stock + (packageName to StockHomeState.DISABLED_USER), effective = packageName == StockHomePackages.last()); return Result.success(Unit) }
        override suspend fun restore(packageName: String): Result<Unit> { calls += "restore:$packageName"; snapshot = snapshot.copy(stock = snapshot.stock + (packageName to StockHomeState.ACTIVE), effective = false); return Result.success(Unit) }
        override suspend fun grantOverlay(): Result<Unit> { calls += "overlay"; snapshot = snapshot.copy(overlay = true); return Result.success(Unit) }
        override suspend fun enableAccessibility(): Result<Unit> { calls += "accessibility"; snapshot = snapshot.copy(accessibilityEnabled = true, accessibilityConnected = true); return Result.success(Unit) }
        override suspend fun rebindAccessibility(): Result<Unit> { calls += "rebind"; return Result.success(Unit) }
        override fun probe() = probe
        override suspend fun awaitBinder(timeoutMs: Long) = probe.binder
        override suspend fun requestPermission(): TsuyuPermission { calls += "permission"; if (permission == TsuyuPermission.GRANTED) probe = probe.copy(authorized = true); return permission }
        override fun open(): Boolean { opened++; return true }
        override suspend fun refresh(includePrereleases: Boolean): Result<TsuyuRelease?> = Result.success(release)
        override suspend fun lastKnown(): TsuyuRelease? = release
        override suspend fun install(onStep: (TsuyuInstallStep) -> Unit): Result<Unit> {
            calls += "install"
            onStep(TsuyuInstallStep.Downloading(42))
            installGate?.await()
            if (installResult.isSuccess) probe = probe.copy(installed = true)
            return installResult
        }
    }

    private val active = TsuyuProbe(installed = true, binder = true, apiVersion = 13, authorized = true)

    @Test fun tsuyuRouteWalksEveryStepWithACounter() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent()
        val s = { f.controller.state.value }
        assertEquals(SetupScreen.DEBUG, s().screen)
        assertEquals(1 to 5, s().counter)
        f.debug = true
        f.controller.onResume(); runCurrent()
        assertEquals(SetupScreen.TSUYU_INSTALL, s().screen)
        assertEquals(2 to 5, s().counter)
        assertTrue("port" in f.calls)
        f.controller.installTsuyu(); runCurrent()
        assertEquals(SetupScreen.TSUYU_START, s().screen)
        assertEquals(3 to 5, s().counter)
        assertTrue(SetupStep.TSUYU in f.memory.modifications)
        f.controller.openTsuyu()
        assertEquals(1, f.opened)
        f.probe = f.probe.copy(binder = true, apiVersion = 13)
        f.controller.onResume(); runCurrent()
        assertEquals(SetupScreen.TSUYU_AUTHORIZE, s().screen)
        assertEquals(4 to 5, s().counter)
        f.controller.authorizeTsuyu(); runCurrent(); advanceTimeBy(5_000); runCurrent()
        assertTrue(s().screen in setOf(SetupScreen.MORE, SetupScreen.DONE))
        assertEquals(5 to 5, s().counter)
        assertTrue(SetupStep.REPAIR in f.memory.modifications)
    }

    @Test fun unreachablePortStopsBeforeAnyDownload() = runTest {
        val f = Fixture(backgroundScope).apply { debug = true; port = false }
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent()
        assertEquals(SetupScreen.CHOICE, f.controller.state.value.screen)
        assertEquals(AutomaticHomeState.UNAVAILABLE, f.controller.state.value.automatic)
        assertFalse("install" in f.calls)
    }

    @Test fun skippingTsuyuRecomputesTheCounter() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent()
        f.debug = true
        f.controller.onResume(); runCurrent()
        f.controller.withoutTsuyu(); runCurrent()
        assertEquals(SetupScreen.AUTHORIZE, f.controller.state.value.screen)
        assertEquals(2 to 3, f.controller.state.value.counter)
        assertEquals(SetupRoute.ADB, f.controller.state.value.route)
    }

    @Test fun installFailureKeepsTheScreenWithItsCause() = runTest {
        val f = Fixture(backgroundScope).apply { debug = true; installResult = Result.failure(TsuyuInstallException(TsuyuInstallError.INVALID)) }
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent()
        f.controller.installTsuyu(); runCurrent()
        assertEquals(SetupScreen.TSUYU_INSTALL, f.controller.state.value.screen)
        assertEquals(TsuyuInstallError.INVALID, f.controller.state.value.tsuyuError)
        assertNull(f.controller.state.value.tsuyuInstall)
        assertFalse(SetupStep.TSUYU in f.memory.modifications)
    }

    @Test fun allowingTheSourceDuringTheAttemptRetriesOnReturn() = runTest {
        val f = Fixture(backgroundScope).apply { debug = true; sourceAllowed = false; installResult = Result.failure(TsuyuInstallException(TsuyuInstallError.REFUSED)) }
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent()
        f.controller.installTsuyu()
        f.sourceAllowed = true
        runCurrent()
        assertNull(f.controller.state.value.tsuyuError)
        assertEquals(TsuyuInstallStep.Installing, f.controller.state.value.tsuyuInstall)
        f.installResult = Result.success(Unit)
        f.controller.onResume(); runCurrent()
        assertEquals(2, f.calls.count { it == "install" })
        assertEquals(SetupScreen.TSUYU_START, f.controller.state.value.screen)
    }

    @Test fun refusalWithTheSourceAlreadyAllowedShowsTheError() = runTest {
        val f = Fixture(backgroundScope).apply { debug = true; installResult = Result.failure(TsuyuInstallException(TsuyuInstallError.REFUSED)) }
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent()
        f.controller.installTsuyu(); runCurrent()
        f.controller.onResume(); runCurrent()
        assertEquals(TsuyuInstallError.REFUSED, f.controller.state.value.tsuyuError)
        assertEquals(1, f.calls.count { it == "install" })
    }

    @Test fun backDuringDownloadCancelsWithoutLeavingTheScreen() = runTest {
        val f = Fixture(backgroundScope).apply { debug = true; installGate = CompletableDeferred() }
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent()
        f.controller.installTsuyu(); runCurrent()
        assertEquals(TsuyuInstallStep.Downloading(42), f.controller.state.value.tsuyuInstall)
        assertTrue(f.controller.back())
        runCurrent()
        assertEquals(SetupScreen.TSUYU_INSTALL, f.controller.state.value.screen)
        assertNull(f.controller.state.value.tsuyuInstall)
    }

    @Test fun refusalAndNoMoreRequests() = runTest {
        val f = Fixture(backgroundScope).apply { debug = true; probe = TsuyuProbe(installed = true, binder = true, apiVersion = 13); permission = TsuyuPermission.DENIED }
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent()
        assertEquals(SetupScreen.TSUYU_AUTHORIZE, f.controller.state.value.screen)
        f.controller.authorizeTsuyu(); runCurrent()
        assertEquals(TsuyuRefusal.REFUSED, f.controller.state.value.tsuyuRefusal)
        f.permission = TsuyuPermission.NO_MORE_REQUESTS
        f.controller.authorizeTsuyu(); runCurrent()
        assertEquals(TsuyuRefusal.NO_MORE_REQUESTS, f.controller.state.value.tsuyuRefusal)
        assertFalse("connect" in f.calls)
    }

    @Test fun activeTsuyuConfiguresWithoutDebugScreens() = runTest {
        val f = Fixture(backgroundScope).apply { probe = active }
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent(); advanceTimeBy(5_000); runCurrent()
        assertEquals(listOf(SetupPhase.SETTINGS), f.controller.state.value.plan)
        assertTrue("connect" in f.calls)
        assertFalse("port" in f.calls)
    }

    @Test fun moreScreenFocusesKeep() = runTest {
        val f = Fixture(backgroundScope).apply { probe = active }
        runCurrent()
        f.controller.automaticWithTsuyu(); runCurrent(); advanceTimeBy(5_000); runCurrent()
        assertEquals(SetupScreen.MORE, f.controller.state.value.screen)
        assertTrue(f.controller.back()); advanceTimeBy(5_000); runCurrent()
        assertEquals(SetupScreen.FALLBACK, f.controller.state.value.screen)
        assertFalse(f.calls.any { it.startsWith("disable") })
    }

    @Test fun replaceFromPanelReturnsToThePanel() = runTest {
        val f = Fixture(backgroundScope).apply { probe = active }
        runCurrent()
        f.controller.openPanel(); runCurrent()
        f.controller.requestReplace(); runCurrent()
        assertEquals(SetupScreen.DISABLE, f.controller.state.value.screen)
        assertNull(f.controller.state.value.counter)
        f.controller.disable(); advanceTimeBy(5_000); runCurrent()
        assertEquals(SetupScreen.PANEL, f.controller.state.value.screen)
        assertEquals("launcher-panel-restore", f.controller.state.value.focusTarget)
        assertEquals(StockHomePackages.map { "disable:$it" }, f.calls.filter { it.startsWith("disable") })
    }

    @Test fun failedReplaceRollsBackAndShowsItsError() = runTest {
        val f = Fixture(backgroundScope).apply { probe = active; failDisable = StockHomePackages.last() }
        runCurrent()
        f.controller.openPanel(); runCurrent()
        f.controller.requestReplace(); runCurrent()
        f.controller.disable(); advanceTimeBy(5_000); runCurrent()
        assertEquals(SetupScreen.FAILED, f.controller.state.value.screen)
        assertTrue(f.controller.state.value.replacing)
        assertEquals(listOf("restore:${StockHomePackages.first()}"), f.calls.filter { it.startsWith("restore") })
        assertFalse("accessibility" in f.calls)
    }

    @Test fun restoreThroughTsuyuSkipsTheDebugScreens() = runTest {
        val f = Fixture(backgroundScope).apply { probe = active; snapshot = snapshot.copy(stock = StockHomePackages.associateWith { StockHomeState.DISABLED_USER }) }
        runCurrent()
        f.controller.openPanel(); runCurrent()
        f.controller.requestRestore(); runCurrent()
        assertEquals(SetupScreen.RESTORE, f.controller.state.value.screen)
        assertTrue("connect" in f.calls)
    }

    @Test fun cancelReplaceReturnsFocusToTheAction() = runTest {
        val f = Fixture(backgroundScope).apply { probe = active }
        runCurrent()
        f.controller.openPanel(); runCurrent()
        f.controller.requestReplace(); runCurrent()
        assertTrue(f.controller.back())
        assertEquals(SetupScreen.PANEL, f.controller.state.value.screen)
        assertEquals("launcher-panel-replace", f.controller.state.value.focusTarget)
    }

    @Test fun repairRunsOncePerBootOnlyForSettingsMadeBySygix() = runTest {
        val f = Fixture(backgroundScope).apply {
            probe = active
            snapshot = snapshot.copy(accessibilityEnabled = true)
            memory = DefaultLauncherMemory(modifications = setOf(SetupStep.REPAIR, SetupStep.VENDOR_BOOT, SetupStep.ACCESSIBILITY))
        }
        runCurrent()
        f.controller.repair(RepairSymptoms(homeKeyBlocked = true, bootMissed = false)); advanceTimeBy(5_000); runCurrent()
        assertEquals(listOf("connect", "vendor", "rebind"), f.calls)
        assertEquals(RepairRecord(SetupStep.ACCESSIBILITY, 42L), f.memory.lastRepair)
        f.calls.clear()
        f.controller.repair(RepairSymptoms(homeKeyBlocked = true, bootMissed = true)); advanceTimeBy(5_000); runCurrent()
        assertTrue(f.calls.isEmpty())
        f.boot = 5
        f.controller.repair(RepairSymptoms(homeKeyBlocked = true, bootMissed = false)); advanceTimeBy(5_000); runCurrent()
        assertEquals(listOf("connect", "vendor", "rebind"), f.calls)
    }

    @Test fun repairNeverTouchesAServiceTheUserDisabled() = runTest {
        val f = Fixture(backgroundScope).apply {
            probe = active
            snapshot = snapshot.copy(accessibilityEnabled = false)
            memory = DefaultLauncherMemory(modifications = setOf(SetupStep.REPAIR, SetupStep.ACCESSIBILITY))
        }
        runCurrent()
        f.controller.repair(RepairSymptoms(homeKeyBlocked = true, bootMissed = false)); advanceTimeBy(5_000); runCurrent()
        assertTrue(f.calls.isEmpty())
    }

    @Test fun repairNeedsActiveTsuyuAndATsuyuConfiguration() = runTest {
        val f = Fixture(backgroundScope).apply {
            probe = active.copy(authorized = false)
            snapshot = snapshot.copy(accessibilityEnabled = true)
            memory = DefaultLauncherMemory(modifications = setOf(SetupStep.REPAIR, SetupStep.ACCESSIBILITY))
        }
        runCurrent()
        f.controller.onResume(); runCurrent()
        f.controller.repair(RepairSymptoms(homeKeyBlocked = true, bootMissed = false)); advanceTimeBy(5_000); runCurrent()
        assertTrue(f.calls.isEmpty())
        f.probe = active
        f.memory = f.memory.copy(modifications = setOf(SetupStep.ACCESSIBILITY))
        f.controller.onResume(); runCurrent()
        f.controller.repair(RepairSymptoms(homeKeyBlocked = true, bootMissed = false)); advanceTimeBy(5_000); runCurrent()
        assertTrue(f.calls.isEmpty())
    }

    @Test fun tsuyuStateFollowsThePriorityOrder() {
        val base = DefaultLauncherState(tsuyuRelease = TsuyuRelease("v1", false, "u", 1, "a"))
        assertEquals(TsuyuState.HIDDEN, DefaultLauncherState().tsuyuState)
        assertEquals(TsuyuState.ABSENT, base.tsuyuState)
        assertEquals(TsuyuState.UNAVAILABLE_TV, base.copy(portUnreachable = true).tsuyuState)
        assertEquals(TsuyuState.STOPPED, base.copy(tsuyu = TsuyuProbe(installed = true)).tsuyuState)
        assertEquals(TsuyuState.OUTDATED, base.copy(tsuyu = TsuyuProbe(installed = true, binder = true, apiVersion = 12)).tsuyuState)
        assertEquals(TsuyuState.UNAUTHORIZED, base.copy(tsuyu = TsuyuProbe(installed = true, binder = true, apiVersion = 13)).tsuyuState)
        assertEquals(TsuyuState.ACTIVE, base.copy(tsuyu = active).tsuyuState)
    }

    @Test fun debugButtonOpensAboutUntilOptionsAreShown() = runTest {
        val f = Fixture(backgroundScope)
        runCurrent()
        f.controller.debugSettings()
        f.options = true
        f.controller.onResume(); runCurrent()
        f.controller.debugSettings()
        assertEquals(listOf("open-about", "open-dev"), f.calls.filter { it.startsWith("open") })
    }
}
