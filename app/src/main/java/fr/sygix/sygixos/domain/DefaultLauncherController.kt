/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.data.AdbAccess
import fr.sygix.sygixos.data.HomeOperations
import fr.sygix.sygixos.data.HomeStateReader
import fr.sygix.sygixos.data.StockHomePackages
import fr.sygix.sygixos.data.SygixHomeComponent
import fr.sygix.sygixos.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DefaultLauncherMemory(
    val previousHome: String? = null,
    val modifications: Set<SetupStep> = emptySet(),
    val lastRepair: RepairRecord? = null,
    val repairBoots: Map<SetupStep, Int> = emptyMap(),
)

interface DefaultLauncherStore {
    suspend fun loadMemory(): DefaultLauncherMemory
    suspend fun rememberPrevious(component: String)
    suspend fun record(step: SetupStep)
    suspend fun markRepairAttempt(step: SetupStep, boot: Int) {}
    suspend fun recordRepair(record: RepairRecord) {}
}

class TsuyuSupport(
    val link: TsuyuLink,
    val releases: TsuyuReleases,
    val installer: TsuyuInstaller,
    val includePrereleases: suspend () -> Boolean,
    val portReachable: suspend () -> Boolean,
    val usingTsuyu: () -> Boolean,
    val onForegroundReturn: () -> Unit = {},
    val binderWaitMs: Long = 3_000,
    val canInstallPackages: () -> Boolean = { true },
)

data class RepairSymptoms(val homeKeyBlocked: Boolean, val bootMissed: Boolean)

class DefaultLauncherController(
    private val access: AdbAccess,
    private val operations: HomeOperations,
    private val reader: HomeStateReader,
    private val store: DefaultLauncherStore,
    private val scope: CoroutineScope,
    private val enableBoot: suspend () -> Unit,
    private val openDebug: () -> Boolean,
    private val verificationMs: Long = 3_000,
    private val openAbout: () -> Boolean = openDebug,
    private val devOptions: () -> Boolean = { false },
    private val tsuyu: TsuyuSupport? = null,
    private val bootCount: () -> Int? = { null },
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutableState = MutableStateFlow(DefaultLauncherState())
    val state: StateFlow<DefaultLauncherState> = mutableState
    private var job: Job? = null
    private var snapshotRequest = 0L
    private var resultPending = false
    private var tsuyuJob: Job? = null
    private var repairJob: Job? = null
    private var retryInstall = false

    init {
        scope.launch { access.state.collect { value -> mutableState.update { it.copy(access = value) } } }
        tsuyu?.let { support -> scope.launch { runCatching { support.releases.lastKnown() }.getOrNull()?.let { known -> mutableState.update { it.copy(tsuyuRelease = known) } } } }
        onResume()
    }

    fun onResume() {
        val debug = runCatching(access::debugEnabled).getOrDefault(false)
        val options = debug || runCatching(devOptions).getOrDefault(false)
        val probe = probeTsuyu()
        mutableState.update { it.copy(debugEnabled = debug, devOptions = options, tsuyu = probe ?: it.tsuyu) }
        tsuyu?.onForegroundReturn?.invoke()
        val current = state.value
        if (!current.busy && tsuyuJob?.isActive != true) when (current.screen) {
            SetupScreen.DEBUG -> if (debug) {
                mutableState.update { it.copy(error = false) }
                if (current.restoring || current.replacing) show(SetupScreen.AUTHORIZE) else next()
            }
            SetupScreen.TSUYU_START -> awaitTsuyuStart()
            SetupScreen.TSUYU_INSTALL -> if (retryInstall) { retryInstall = false; installTsuyu() }
            else -> Unit
        }
        scope.launch {
            refreshSnapshot()
            try {
                val memory = store.loadMemory()
                mutableState.update { it.copy(modifications = memory.modifications, lastRepair = memory.lastRepair) }
            } catch (_: Exception) {
                mutableState.update { it.copy(error = true) }
            }
        }
    }

    private fun probeTsuyu(): fr.sygix.sygixos.domain.TsuyuProbe? = tsuyu?.let { runCatching(it.link::probe).getOrNull() }

    fun refreshTsuyu() {
        val support = tsuyu ?: return
        scope.launch {
            runCatching { support.releases.lastKnown() }.getOrNull()?.let { known -> mutableState.update { it.copy(tsuyuRelease = known) } }
            val include = runCatching { support.includePrereleases() }.getOrDefault(false)
            support.releases.refresh(include).onSuccess { fresh -> mutableState.update { it.copy(tsuyuRelease = fresh) } }
            probeTsuyu()?.let { probe -> mutableState.update { it.copy(tsuyu = probe) } }
        }
    }

    private suspend fun refreshSnapshot(): Result<HomeScreenSnapshot> {
        val request = ++snapshotRequest
        return reader.read().also { result ->
            if (request == snapshotRequest) mutableState.update { it.copy(snapshot = result.getOrNull(), readFailed = result.isFailure) }
        }
    }

    fun openPanel() {
        refreshTsuyu()
        if (resultPending) { resultPending = false; return }
        mutableState.update { it.copy(fromPanel = true) }
        if (!state.value.busy) show(SetupScreen.PANEL)
    }

    fun openChoice() {
        refreshTsuyu()
        mutableState.update { it.copy(fromPanel = false, restoring = false, replacing = false) }
        if (!state.value.busy) show(SetupScreen.CHOICE)
    }
    fun onPanelClosed() { if (state.value.busy) resultPending = true }
    fun openManual() = show(SetupScreen.MANUAL)
    fun debugSettings() {
        val options = state.value.devOptions || runCatching(devOptions).getOrDefault(false)
        if (!(if (options) openDebug() else openAbout())) mutableState.update { it.copy(error = true) }
    }
    fun details() { mutableState.update { it.copy(detailsReturn = it.screen) }; show(SetupScreen.DETAILS) }
    fun back(): Boolean {
        when (state.value.screen) {
            SetupScreen.DISABLE -> if (state.value.replacing) { mutableState.update { it.copy(replacing = false) }; show(SetupScreen.PANEL, "launcher-panel-replace") } else show(SetupScreen.MORE, "setup-more-disable")
            SetupScreen.RESTORE -> show(SetupScreen.PANEL, "launcher-panel-restore")
            SetupScreen.DETAILS -> show(state.value.detailsReturn, if (state.value.detailsReturn == SetupScreen.DONE) "setup-done-details" else "setup-fallback-details")
            SetupScreen.DEBUG, SetupScreen.AUTHORIZE, SetupScreen.TSUYU_INSTALL, SetupScreen.TSUYU_START, SetupScreen.TSUYU_AUTHORIZE -> {
                if (state.value.screen == SetupScreen.TSUYU_INSTALL && state.value.tsuyuInstall != null) { tsuyuJob?.cancel(); mutableState.update { it.copy(tsuyuInstall = null) }; return true }
                mutableState.update { it.copy(restoring = false, replacing = false, error = false, tsuyuError = null, tsuyuRefusal = TsuyuRefusal.NONE) }
                show(if (state.value.fromPanel) SetupScreen.PANEL else SetupScreen.CHOICE)
            }
            SetupScreen.MORE -> fallback()
            else -> return false
        }
        return true
    }

    fun automatic() = start(SetupRoute.ADB)

    fun automaticWithTsuyu() = start(SetupRoute.TSUYU)

    private fun start(route: SetupRoute) {
        if (state.value.busy) return
        probeTsuyu()?.let { probe -> mutableState.update { it.copy(tsuyu = probe) } }
        val current = state.value
        val debug = runCatching(access::debugEnabled).getOrDefault(false)
        val plan = buildList {
            if (!debug) add(SetupPhase.DEBUG)
            if (route == SetupRoute.TSUYU) {
                if (!current.tsuyu.installed || current.tsuyuState == TsuyuState.OUTDATED) add(SetupPhase.INSTALL)
                if (!current.tsuyu.binder || current.tsuyuState == TsuyuState.OUTDATED) add(SetupPhase.START)
                if (!current.tsuyu.authorized) add(SetupPhase.AUTHORIZE)
            } else if (!access.previouslyAuthorized()) add(SetupPhase.AUTHORIZE)
            add(SetupPhase.SETTINGS)
        }
        mutableState.update { it.copy(route = route, plan = plan, restoring = false, replacing = false, error = false, tsuyuError = null, tsuyuRefusal = TsuyuRefusal.NONE, portUnreachable = false) }
        next()
    }

    private fun next() {
        val current = state.value
        val debug = runCatching(access::debugEnabled).getOrDefault(false)
        if (current.route == SetupRoute.ADB || tsuyu == null) {
            if (!debug) { show(SetupScreen.DEBUG); return }
            show(SetupScreen.AUTHORIZE)
            if (access.previouslyAuthorized()) configure()
            return
        }
        val probe = probeTsuyu() ?: current.tsuyu
        mutableState.update { it.copy(tsuyu = probe) }
        if (state.value.tsuyuState == TsuyuState.ACTIVE) { configure(); return }
        if (!debug) { show(SetupScreen.DEBUG); return }
        tsuyuJob = scope.launch {
            if (!runCatching { tsuyu.portReachable() }.getOrDefault(false)) {
                mutableState.update { it.copy(portUnreachable = true) }
                show(if (state.value.fromPanel) SetupScreen.PANEL else SetupScreen.CHOICE)
                return@launch
            }
            mutableState.update { it.copy(portUnreachable = false) }
            when (state.value.tsuyuState) {
                TsuyuState.ABSENT, TsuyuState.OUTDATED -> show(SetupScreen.TSUYU_INSTALL)
                TsuyuState.STOPPED -> show(SetupScreen.TSUYU_START)
                TsuyuState.UNAUTHORIZED -> show(SetupScreen.TSUYU_AUTHORIZE)
                TsuyuState.ACTIVE -> configure()
                else -> show(SetupScreen.CHOICE)
            }
        }
    }

    fun withoutTsuyu() {
        if (state.value.busy) return
        tsuyuJob?.cancel()
        mutableState.update { current ->
            current.copy(route = SetupRoute.ADB, tsuyuInstall = null, tsuyuError = null,
                plan = current.plan.filter { it == SetupPhase.DEBUG } + listOfNotNull(SetupPhase.AUTHORIZE.takeIf { !access.previouslyAuthorized() }) + SetupPhase.SETTINGS)
        }
        next()
    }

    fun installTsuyu() {
        val support = tsuyu ?: return
        if (state.value.busy || tsuyuJob?.isActive == true) return
        val sourceAllowed = runCatching(support.canInstallPackages).getOrDefault(true)
        mutableState.update { it.copy(tsuyuError = null, tsuyuInstall = fr.sygix.sygixos.domain.TsuyuInstallStep.Downloading(0)) }
        tsuyuJob = scope.launch {
            val result = support.installer.install { step -> mutableState.update { it.copy(tsuyuInstall = step) } }
            val probe = probeTsuyu()
            mutableState.update { it.copy(tsuyuInstall = null, tsuyu = probe ?: it.tsuyu) }
            result.onSuccess {
                record(SetupStep.TSUYU)
                show(if (state.value.tsuyu.binder && state.value.tsuyuState != TsuyuState.OUTDATED) SetupScreen.TSUYU_AUTHORIZE else SetupScreen.TSUYU_START)
            }.onFailure { failure ->
                val error = (failure as? fr.sygix.sygixos.domain.TsuyuInstallException)?.error ?: fr.sygix.sygixos.domain.TsuyuInstallError.REFUSED
                if (error == fr.sygix.sygixos.domain.TsuyuInstallError.REFUSED && !sourceAllowed && runCatching(support.canInstallPackages).getOrDefault(false)) {
                    retryInstall = true
                    mutableState.update { it.copy(tsuyuInstall = fr.sygix.sygixos.domain.TsuyuInstallStep.Installing) }
                    return@onFailure
                }
                mutableState.update { it.copy(tsuyuError = error) }
                show(SetupScreen.TSUYU_INSTALL, "tsuyu-install-retry")
            }
        }
    }

    fun openTsuyu() {
        val support = tsuyu ?: return
        if (!runCatching(support.link::open).getOrDefault(false)) mutableState.update { it.copy(tsuyu = probeTsuyu() ?: it.tsuyu) }
    }

    private fun awaitTsuyuStart() {
        val support = tsuyu ?: return
        tsuyuJob = scope.launch {
            support.link.awaitBinder(support.binderWaitMs)
            val probe = probeTsuyu() ?: return@launch
            mutableState.update { it.copy(tsuyu = probe) }
            if (state.value.screen != SetupScreen.TSUYU_START || !probe.binder) return@launch
            when (state.value.tsuyuState) {
                TsuyuState.OUTDATED -> show(SetupScreen.TSUYU_INSTALL)
                TsuyuState.UNAUTHORIZED -> show(SetupScreen.TSUYU_AUTHORIZE)
                TsuyuState.ACTIVE -> if (state.value.fromPanel) show(SetupScreen.PANEL, "launcher-panel-tsuyu") else configure()
                else -> Unit
            }
        }
    }

    fun authorizeTsuyu() {
        val support = tsuyu ?: return
        if (state.value.busy || tsuyuJob?.isActive == true) return
        tsuyuJob = scope.launch {
            val answer = support.link.requestPermission()
            mutableState.update { it.copy(tsuyu = probeTsuyu() ?: it.tsuyu) }
            when (answer) {
                fr.sygix.sygixos.domain.TsuyuPermission.GRANTED -> {
                    mutableState.update { it.copy(tsuyuRefusal = TsuyuRefusal.NONE) }
                    if (state.value.fromPanel && state.value.route != SetupRoute.TSUYU) show(SetupScreen.PANEL, "launcher-panel-tsuyu") else configure()
                }
                fr.sygix.sygixos.domain.TsuyuPermission.DENIED -> { mutableState.update { it.copy(tsuyuRefusal = TsuyuRefusal.REFUSED) }; show(SetupScreen.TSUYU_AUTHORIZE, "tsuyu-authorize-retry") }
                fr.sygix.sygixos.domain.TsuyuPermission.NO_MORE_REQUESTS -> { mutableState.update { it.copy(tsuyuRefusal = TsuyuRefusal.NO_MORE_REQUESTS) }; show(SetupScreen.TSUYU_AUTHORIZE, "tsuyu-authorize-skip") }
            }
        }
    }

    fun tsuyuLine() {
        if (state.value.busy) return
        when (state.value.tsuyuState) {
            TsuyuState.ABSENT, TsuyuState.OUTDATED -> start(SetupRoute.TSUYU)
            TsuyuState.STOPPED -> { mutableState.update { it.copy(route = SetupRoute.TSUYU) }; show(SetupScreen.TSUYU_START) }
            TsuyuState.UNAUTHORIZED -> { mutableState.update { it.copy(route = SetupRoute.TSUYU) }; show(SetupScreen.TSUYU_AUTHORIZE) }
            TsuyuState.ACTIVE -> openTsuyu()
            else -> Unit
        }
    }

    fun configure() = runSequence(clearErrors = true) {
        if (!connect()) return@runSequence
        mutableState.update { it.copy(steps = emptyMap(), error = false) }
        show(SetupScreen.PROGRESS)
        if (tsuyu?.usingTsuyu?.invoke() == true) record(SetupStep.REPAIR)
        step(SetupStep.VENDOR_BOOT, hideIfAbsent = true) { operations.grantVendorBoot() }
        val previous = operations.previousHome().getOrThrow()
        if (previous != null && previous != SygixHomeComponent) store.rememberPrevious(previous)
        if (!step(SetupStep.HOME) { operations.setHome() }) {
            setStep(SetupStep.VERIFY, SetupStepState.CANCELLED)
            show(SetupScreen.FAILED)
            return@runSequence
        }
        setStep(SetupStep.VERIFY, SetupStepState.RUNNING)
        val verified = verifyHome()
        setStep(SetupStep.VERIFY, if (verified.effective) SetupStepState.DONE else SetupStepState.FAILED)
        if (verified.effective) show(if (state.value.error) SetupScreen.FAILED else SetupScreen.DONE)
        else if (verified.canDisable) show(SetupScreen.MORE)
        else fallbackConnected()
    }

    fun confirmDisable() { if (state.value.snapshot?.canDisable == true) show(SetupScreen.DISABLE) }

    fun requestReplace() {
        if (state.value.busy || state.value.snapshot?.canDisable != true) return
        mutableState.update { it.copy(replacing = true, restoring = false, error = false) }
        if (!runCatching(access::debugEnabled).getOrDefault(false)) { show(SetupScreen.DEBUG); return }
        if (access.previouslyAuthorized()) show(SetupScreen.DISABLE) else show(SetupScreen.AUTHORIZE)
    }

    fun disable() = runSequence {
        if (!connect()) return@runSequence
        if (!reader.read().getOrThrow().canDisable) { if (state.value.replacing) show(SetupScreen.PANEL, "launcher-panel-relaunch") else fallbackConnected(); return@runSequence }
        show(SetupScreen.PROGRESS)
        setStep(SetupStep.STOCK, SetupStepState.RUNNING)
        val changed = mutableListOf<String>()
        var succeeded = true
        try {
            for (packageName in StockHomePackages) {
                if (reader.read().getOrThrow().stock[packageName] != StockHomeState.ACTIVE) continue
                changed += packageName
                if (operations.disable(packageName).isFailure) { succeeded = false; break }
            }
            if (succeeded) succeeded = verifyHome().effective
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            succeeded = false
        }
        if (succeeded) {
            record(SetupStep.STOCK)
            setStep(SetupStep.STOCK, SetupStepState.DONE)
            if (state.value.replacing && !state.value.error) {
                refreshSnapshot()
                mutableState.update { it.copy(replacing = false) }
                show(SetupScreen.PANEL, "launcher-panel-restore")
            } else show(if (state.value.error) SetupScreen.FAILED else SetupScreen.DONE)
        } else {
            for (packageName in changed.asReversed()) {
                val current = reader.read().getOrNull()
                if (current != null && current.stock[packageName] != StockHomeState.DISABLED_USER) continue
                if (access.state.value != AdbAccessState.READY && !connect()) break
                if (operations.restore(packageName).isFailure) mutableState.update { it.copy(error = true) }
            }
            setStep(SetupStep.STOCK, SetupStepState.FAILED)
            if (state.value.replacing) {
                mutableState.update { it.copy(error = true) }
                refreshSnapshot()
                show(SetupScreen.FAILED)
            } else fallbackConnected()
        }
    }

    fun fallback() = runSequence { if (connect()) fallbackConnected() }

    private suspend fun fallbackConnected() {
        show(SetupScreen.PROGRESS)
        step(SetupStep.ACCESSIBILITY) { operations.enableAccessibility() }
        step(SetupStep.OVERLAY) { operations.grantOverlay() }
        step(SetupStep.BOOT) { enableBoot(); mutableState.update { it.copy(bootEnabled = true) }; Result.success(Unit) }
        refreshSnapshot()
        show(if (state.value.error) SetupScreen.FAILED else SetupScreen.FALLBACK)
    }

    fun requestRestore() {
        if (state.value.busy) return
        mutableState.update { it.copy(restoring = true, replacing = false, error = false) }
        if (!runCatching(access::debugEnabled).getOrDefault(false)) { show(SetupScreen.DEBUG); return }
        if (access.previouslyAuthorized()) authorizeRestore() else show(SetupScreen.AUTHORIZE)
    }

    private fun authorizeRestore() = runSequence { if (connect()) show(SetupScreen.RESTORE) }
    fun authorize() = when {
        state.value.restoring -> authorizeRestore()
        state.value.replacing -> runSequence { if (connect()) show(SetupScreen.DISABLE) }
        else -> configure()
    }

    fun restore() = runSequence(clearErrors = true) {
        if (!connect()) return@runSequence
        show(SetupScreen.PROGRESS)
        var success = true
        for (packageName in StockHomePackages.asReversed()) {
            if (reader.read().getOrThrow().stock[packageName] == StockHomeState.DISABLED_USER && operations.restore(packageName).isFailure) success = false
        }
        val snapshot = reader.read().getOrThrow()
        val previous = store.loadMemory().previousHome
        if (success && previous != null && previous in snapshot.enabledHomes) success = operations.setHome(previous).isSuccess
        show(if (success && refreshSnapshot().getOrNull()?.canRestore == false) SetupScreen.PANEL else SetupScreen.FAILED)
    }

    fun retry() {
        when {
            state.value.restoring -> restore()
            state.value.replacing -> disable()
            state.value.screen == SetupScreen.TSUYU_INSTALL -> installTsuyu()
            state.value.screen == SetupScreen.TSUYU_AUTHORIZE -> authorizeTsuyu()
            state.value.screen == SetupScreen.DEBUG -> debugSettings()
            else -> configure()
        }
    }

    private fun runSequence(clearErrors: Boolean = false, block: suspend () -> Unit) {
        if (job?.isActive == true) return
        mutableState.update { it.copy(busy = true, error = if (clearErrors) false else it.error) }
        job = scope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update {
                    val cancelled = it.steps.mapValues { (_, value) -> if (value == SetupStepState.RUNNING || value == SetupStepState.TODO) SetupStepState.CANCELLED else value }
                    it.copy(error = true, steps = if (it.steps[SetupStep.HOME] == SetupStepState.FAILED) cancelled + (SetupStep.VERIFY to SetupStepState.CANCELLED) else cancelled)
                }
                show(SetupScreen.FAILED)
            } finally {
                access.close()
                refreshSnapshot()
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private suspend fun connect(): Boolean {
        if (!runCatching(access::debugEnabled).getOrDefault(false)) {
            mutableState.update { it.copy(debugEnabled = false, error = true) }
            show(SetupScreen.DEBUG)
            return false
        }
        mutableState.update { it.copy(debugEnabled = true) }
        if (access.connect().isSuccess) return true
        when (access.state.value) {
            AdbAccessState.UNREACHABLE -> show(if (state.value.fromPanel) SetupScreen.PANEL else SetupScreen.CHOICE)
            AdbAccessState.DISABLED -> show(SetupScreen.DEBUG)
            else -> { mutableState.update { it.copy(error = true) }; show(SetupScreen.AUTHORIZE) }
        }
        return false
    }

    private suspend fun <T> step(step: SetupStep, hideIfAbsent: Boolean = false, block: suspend () -> Result<T>): Boolean {
        setStep(step, SetupStepState.RUNNING)
        val before = reader.read().getOrThrow()
        val changed = when (step) {
            SetupStep.HOME -> before.roleHolder != "fr.sygix.sygixos"
            SetupStep.ACCESSIBILITY -> !before.accessibilityEnabled
            SetupStep.OVERLAY -> !before.overlay
            SetupStep.BOOT -> !state.value.bootEnabled
            else -> true
        }
        val result = block()
        val vendor = result.getOrNull() as? fr.sygix.sygixos.data.VendorBootOutcome
        if (hideIfAbsent && vendor?.present == false) {
            mutableState.update { it.copy(steps = it.steps - step) }
            return true
        }
        setStep(step, if (result.isSuccess) SetupStepState.DONE else SetupStepState.FAILED)
        if (result.isSuccess) {
            if (changed && (vendor == null || vendor.modified)) record(step)
        } else {
            mutableState.update { it.copy(error = true) }
            if (access.state.value == AdbAccessState.LOST) throw IllegalStateException("Local ADB lost")
        }
        return result.isSuccess
    }

    private suspend fun record(step: SetupStep) {
        store.record(step)
        mutableState.update { it.copy(modifications = it.modifications + step) }
    }

    private suspend fun verifyHome(): HomeScreenSnapshot {
        var waited = 0L
        while (true) {
            val snapshot = refreshSnapshot().getOrThrow()
            if (snapshot.effective || waited >= verificationMs) return snapshot
            delay(100)
            waited += 100
        }
    }

    fun repair(symptoms: RepairSymptoms) {
        if (tsuyu == null || state.value.busy || job?.isActive == true || repairJob?.isActive == true) return
        if (!symptoms.homeKeyBlocked && !symptoms.bootMissed) return
        probeTsuyu()?.let { probe -> mutableState.update { it.copy(tsuyu = probe) } }
        if (state.value.tsuyuState != TsuyuState.ACTIVE) return
        val boot = bootCount() ?: return
        repairJob = scope.launch {
            try {
                val memory = store.loadMemory()
                if (SetupStep.REPAIR !in memory.modifications) return@launch
                val snapshot = reader.read().getOrNull() ?: return@launch
                val vendor = SetupStep.VENDOR_BOOT in memory.modifications && memory.repairBoots[SetupStep.VENDOR_BOOT] != boot
                val home = symptoms.homeKeyBlocked && snapshot.accessibilityEnabled && SetupStep.ACCESSIBILITY in memory.modifications && memory.repairBoots[SetupStep.ACCESSIBILITY] != boot
                if (!vendor && !home) return@launch
                if (access.connect().isFailure) return@launch
                if (vendor) {
                    store.markRepairAttempt(SetupStep.VENDOR_BOOT, boot)
                    val outcome = operations.grantVendorBoot()
                    if (outcome.getOrNull()?.modified == true) store.recordRepair(RepairRecord(SetupStep.VENDOR_BOOT, clock()))
                }
                if (home) {
                    store.markRepairAttempt(SetupStep.ACCESSIBILITY, boot)
                    if (operations.rebindAccessibility().isSuccess) store.recordRepair(RepairRecord(SetupStep.ACCESSIBILITY, clock()))
                }
                val updated = store.loadMemory()
                mutableState.update { it.copy(lastRepair = updated.lastRepair) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Unit
            } finally {
                access.close()
                refreshSnapshot()
            }
        }
    }

    private fun setStep(step: SetupStep, value: SetupStepState) { mutableState.update { it.copy(steps = it.steps + (step to value)) } }
    private fun show(screen: SetupScreen, focus: String? = null) {
        mutableState.update { it.copy(screen = screen, focusTarget = focus, focusVersion = it.focusVersion + 1) }
    }
}
