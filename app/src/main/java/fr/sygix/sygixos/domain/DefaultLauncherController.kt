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

data class DefaultLauncherMemory(val previousHome: String? = null, val modifications: Set<SetupStep> = emptySet())

interface DefaultLauncherStore {
    suspend fun loadMemory(): DefaultLauncherMemory
    suspend fun rememberPrevious(component: String)
    suspend fun record(step: SetupStep)
}

class DefaultLauncherController(
    private val access: AdbAccess,
    private val operations: HomeOperations,
    private val reader: HomeStateReader,
    private val store: DefaultLauncherStore,
    private val scope: CoroutineScope,
    private val enableBoot: suspend () -> Unit,
    private val refreshManual: () -> Unit,
    private val openDebug: () -> Boolean,
    private val verificationMs: Long = 3_000,
) {
    private val mutableState = MutableStateFlow(DefaultLauncherState())
    val state: StateFlow<DefaultLauncherState> = mutableState
    private var job: Job? = null
    private var refreshJob: Job? = null
    private var resultPending = false

    init {
        scope.launch { access.state.collect { value -> mutableState.update { it.copy(access = value) } } }
        onResume()
    }

    fun onResume() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            refreshSnapshot()
            refreshManual()
            try {
                val memory = store.loadMemory()
                mutableState.update { it.copy(modifications = memory.modifications) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.update { it.copy(error = true) }
            }
        }
    }

    private suspend fun refreshSnapshot() {
        val debug = runCatching(access::debugEnabled).getOrDefault(false)
        val result = reader.read()
        mutableState.update {
            it.copy(debugEnabled = debug, snapshot = result.getOrNull(), readFailed = result.isFailure,
                screen = if (!it.busy && it.screen == SetupScreen.DEBUG && debug) SetupScreen.AUTHORIZE else it.screen)
        }
    }

    fun openPanel() {
        if (resultPending) { resultPending = false; return }
        if (!state.value.busy) show(SetupScreen.PANEL)
    }
    fun onPanelClosed() { if (state.value.busy) resultPending = true }
    fun openManual() = show(SetupScreen.MANUAL)
    fun debugSettings() { if (!openDebug()) mutableState.update { it.copy(error = true) } }
    fun details() { mutableState.update { it.copy(detailsReturn = it.screen) }; show(SetupScreen.DETAILS) }
    fun back(): Boolean {
        when (state.value.screen) {
            SetupScreen.DISABLE -> show(SetupScreen.MORE, "setup-more-disable")
            SetupScreen.RESTORE -> show(SetupScreen.PANEL, "launcher-panel-restore")
            SetupScreen.DETAILS -> show(state.value.detailsReturn, if (state.value.detailsReturn == SetupScreen.DONE) "setup-done-details" else "setup-fallback-details")
            SetupScreen.DEBUG, SetupScreen.AUTHORIZE -> show(if (state.value.restoring) SetupScreen.PANEL else SetupScreen.CHOICE)
            SetupScreen.MORE -> fallback()
            else -> return false
        }
        return true
    }

    fun automatic() {
        if (state.value.busy) return
        mutableState.update { it.copy(restoring = false, error = false) }
        if (!runCatching(access::debugEnabled).getOrDefault(false)) { show(SetupScreen.DEBUG); return }
        show(SetupScreen.AUTHORIZE)
        if (access.previouslyAuthorized()) configure()
    }

    fun configure() = runSequence(clearErrors = true) {
        if (!connect()) return@runSequence
        mutableState.update { it.copy(steps = emptyMap(), error = false) }
        show(SetupScreen.PROGRESS)
        step(SetupStep.VENDOR_BOOT, hideIfAbsent = true) { operations.grantVendorBoot() }
        val previous = operations.previousHome().getOrThrow()
        if (previous != null && previous != SygixHomeComponent) store.rememberPrevious(previous)
        if (!step(SetupStep.HOME) { operations.setHome() }) {
            setStep(SetupStep.VERIFY, SetupStepState.CANCELLED)
            show(SetupScreen.FAILED)
            return@runSequence
        }
        setStep(SetupStep.VERIFY, SetupStepState.RUNNING)
        val effective = verifyHome()
        setStep(SetupStep.VERIFY, if (effective) SetupStepState.DONE else SetupStepState.FAILED)
        if (effective) show(if (state.value.error) SetupScreen.FAILED else SetupScreen.DONE)
        else if (state.value.snapshot?.canDisable == true) show(SetupScreen.MORE)
        else fallbackConnected()
    }

    fun confirmDisable() { if (state.value.snapshot?.canDisable == true) show(SetupScreen.DISABLE) }

    fun disable() = runSequence {
        if (!connect()) return@runSequence
        if (!reader.read().getOrThrow().canDisable) { fallbackConnected(); return@runSequence }
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
            if (succeeded) succeeded = verifyHome()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            succeeded = false
        }
        if (succeeded) {
            record(SetupStep.STOCK)
            setStep(SetupStep.STOCK, SetupStepState.DONE)
            show(if (state.value.error) SetupScreen.FAILED else SetupScreen.DONE)
        } else {
            for (packageName in changed.asReversed()) {
                val current = reader.read().getOrNull()
                if (current != null && current.stock[packageName] != StockHomeState.DISABLED_USER) continue
                if (access.state.value != AdbAccessState.READY && !connect()) break
                if (operations.restore(packageName).isFailure) mutableState.update { it.copy(error = true) }
            }
            setStep(SetupStep.STOCK, SetupStepState.FAILED)
            fallbackConnected()
        }
    }

    fun fallback() = runSequence { if (connect()) fallbackConnected() }

    private suspend fun fallbackConnected() {
        show(SetupScreen.PROGRESS)
        step(SetupStep.ACCESSIBILITY) { operations.enableAccessibility() }
        step(SetupStep.OVERLAY) { operations.grantOverlay() }
        step(SetupStep.BOOT) { enableBoot(); mutableState.update { it.copy(bootEnabled = true) }; Result.success(Unit) }
        refreshManual()
        val snapshot = reader.read()
        mutableState.update { it.copy(snapshot = snapshot.getOrNull(), readFailed = snapshot.isFailure) }
        show(if (state.value.error) SetupScreen.FAILED else SetupScreen.FALLBACK)
    }

    fun requestRestore() {
        if (state.value.busy) return
        mutableState.update { it.copy(restoring = true, error = false) }
        if (!runCatching(access::debugEnabled).getOrDefault(false)) { show(SetupScreen.DEBUG); return }
        if (access.previouslyAuthorized()) authorizeRestore() else show(SetupScreen.AUTHORIZE)
    }

    private fun authorizeRestore() = runSequence { if (connect()) show(SetupScreen.RESTORE) }
    fun authorize() { if (state.value.restoring) authorizeRestore() else configure() }

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
        refreshSnapshot()
        show(if (success && state.value.snapshot?.canRestore == false) SetupScreen.PANEL else SetupScreen.FAILED)
    }

    fun retry() {
        when {
            state.value.restoring -> restore()
            state.value.screen == SetupScreen.DEBUG -> debugSettings()
            else -> configure()
        }
    }

    private fun runSequence(clearErrors: Boolean = false, block: suspend () -> Unit) {
        if (job?.isActive == true) return
        refreshJob?.cancel()
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
                refreshJob?.cancel()
                access.close()
                val read = reader.read()
                mutableState.update { it.copy(busy = false, snapshot = read.getOrNull(), readFailed = read.isFailure) }
                refreshManual()
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
            AdbAccessState.UNREACHABLE -> show(SetupScreen.CHOICE)
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

    private suspend fun verifyHome(): Boolean {
        var waited = 0L
        while (true) {
            val result = reader.read()
            mutableState.update { it.copy(snapshot = result.getOrNull(), readFailed = result.isFailure) }
            if (result.getOrThrow().effective) return true
            if (waited >= verificationMs) return false
            delay(100)
            waited += 100
        }
    }

    private fun setStep(step: SetupStep, value: SetupStepState) { mutableState.update { it.copy(steps = it.steps + (step to value)) } }
    private fun show(screen: SetupScreen, focus: String? = null) {
        mutableState.update { it.copy(screen = screen, focusTarget = focus, focusVersion = it.focusVersion + 1) }
    }
}
