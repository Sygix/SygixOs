/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Intent
import fr.sygix.sygixos.domain.CheckResult
import fr.sygix.sygixos.domain.DailyCheckPolicy
import fr.sygix.sygixos.domain.InstallFailure
import fr.sygix.sygixos.domain.UpdateError
import fr.sygix.sygixos.domain.UpdateException
import fr.sygix.sygixos.domain.UpdateSelector
import fr.sygix.sygixos.domain.UpdateStatus
import fr.sygix.sygixos.domain.UpdateStep
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface UpdateController {
    val status: StateFlow<UpdateStatus>
    val systemScreenReturns: StateFlow<Int>
    fun check()
    fun startUpdate(tag: String)
    fun togglePrereleases()
    fun onHomeShown()
    fun onForeground()
}

class UpdateRepository(
    private val store: UpdateStore,
    private val source: ReleaseSource,
    private val installer: UpdateInstaller,
    private val gateway: PackageInstallerGateway,
    private val network: NetworkStatus,
    private val foreground: ForegroundState,
    private val screens: SystemScreenLauncher,
    private val installedVersionCode: () -> Long,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : UpdateController {

    private data class Runtime(val checking: Boolean = false, val step: UpdateStep? = null, val sessionId: Int? = null)

    private class PendingAction(val sessionId: Int, val intent: Intent)

    private val runtime = MutableStateFlow(Runtime())
    private var installed: Long? = null
    private var checkJob: Job? = null
    private var operationJob: Job? = null
    private var pendingAction: PendingAction? = null
    private var systemScreenShown = false
    private var homeShown = false
    private val screenReturns = MutableStateFlow(0)

    override val systemScreenReturns: StateFlow<Int> = screenReturns.asStateFlow()

    override val status: StateFlow<UpdateStatus> by lazy { statusFlow() }

    private fun statusFlow(): StateFlow<UpdateStatus> = combine(store.data, runtime) { persisted, current ->
        val step = current.step
        val proposed = if (step != null && step.inProgress) {
            step.candidate
        } else {
            UpdateSelector.proposed(persisted.known, persisted.includePrereleases, installedCode())
        }
        UpdateStatus(
            checking = current.checking,
            includePrereleases = persisted.includePrereleases,
            proposed = proposed,
            lastResult = persisted.lastResult,
            step = step,
        )
    }.stateIn(scope, SharingStarted.Eagerly, UpdateStatus())

    private suspend fun installedCode(): Long =
        installed ?: withContext(io) { installedVersionCode() }.also { installed = it }

    fun coldStart() {
        scope.launch(io) {
            installer.clean()
            val abandoned = runCatching { gateway.abandonAll() }.getOrDefault(0)
            if (abandoned > 0) store.setRelaunch(null)
        }
    }

    override fun check() = launchCheck(manual = true)

    override fun onHomeShown() {
        homeShown = true
        autoCheck()
    }

    override fun onForeground() {
        val action = pendingAction
        if (action != null) {
            pendingAction = null
            showSystemScreen(action)
        } else if (systemScreenShown) {
            systemScreenShown = false
            screenReturns.update { it + 1 }
            abandonIfLeftWithoutStatus()
        }
        if (homeShown) autoCheck()
    }

    override fun togglePrereleases() {
        scope.launch { store.togglePrereleases() }
    }

    override fun startUpdate(tag: String) {
        if (operationJob?.isActive == true || runtime.value.step?.inProgress == true) return
        val candidate = status.value.proposed?.takeIf { it.tag == tag } ?: return
        runtime.update { it.copy(step = UpdateStep.Downloading(candidate, 0), sessionId = null) }
        operationJob = scope.launch {
            when (val outcome = installer.run(candidate, ::setStep)) {
                is InstallOutcome.Committed -> runtime.update {
                    it.copy(step = UpdateStep.Installing(outcome.candidate), sessionId = outcome.sessionId)
                }
                is InstallOutcome.Failure -> onFailure(outcome)
            }
        }
    }

    fun onInstallStatus(status: InstallStatus) {
        val current = runtime.value
        val step = current.step
        if (step == null) {
            if (status is InstallStatus.Aborted || status is InstallStatus.Failed) scope.launch { store.setRelaunch(null) }
            return
        }
        if (status.sessionId != current.sessionId) return
        when (status) {
            is InstallStatus.Success -> Unit
            is InstallStatus.PendingUserAction -> onPendingUserAction(step, status)
            is InstallStatus.Aborted -> failInstall(UpdateError.InstallAborted)
            is InstallStatus.Failed -> failInstall(UpdateError.InstallFailed(status.family))
        }
    }

    private fun onPendingUserAction(step: UpdateStep, status: InstallStatus.PendingUserAction) {
        val intent = status.intent
        if (intent == null) {
            gateway.abandon(status.sessionId)
            failInstall(UpdateError.InstallFailed(InstallFailure.OTHER))
            return
        }
        val action = PendingAction(status.sessionId, intent)
        if (foreground.isForeground) showSystemScreen(action) else pendingAction = action
    }

    private fun showSystemScreen(action: PendingAction) {
        val target = runtime.value.step?.candidate?.versionCode ?: return
        scope.launch {
            store.setRelaunch(target)
            if (!foreground.isForeground) {
                store.setRelaunch(null)
                pendingAction = action
                return@launch
            }
            if (screens.launch(action.intent)) {
                systemScreenShown = true
            } else {
                gateway.abandon(action.sessionId)
                failInstall(UpdateError.SystemScreenUnavailable)
            }
        }
    }

    private fun abandonIfLeftWithoutStatus() {
        val sessionId = runtime.value.sessionId ?: return
        scope.launch {
            delay(RETURN_GRACE_MS)
            val current = runtime.value
            if (current.sessionId != sessionId || current.step !is UpdateStep.Installing) return@launch
            if (withContext(io) { gateway.isActive(sessionId) }) return@launch
            gateway.abandon(sessionId)
            failInstall(UpdateError.InstallAborted)
        }
    }

    private fun failInstall(error: UpdateError) {
        val step = runtime.value.step ?: return
        pendingAction = null
        systemScreenShown = false
        runtime.update { it.copy(step = UpdateStep.Failed(step.candidate, error), sessionId = null) }
        scope.launch { store.setRelaunch(null) }
    }

    private suspend fun onFailure(outcome: InstallOutcome.Failure) {
        val error = outcome.error
        if (error is UpdateError.RateLimited) store.setRetryAt(error.retryAt)
        if (error == UpdateError.Withdrawn) {
            store.setKnown(store.data.first().known.without(outcome.candidate.tag))
        }
        setStep(UpdateStep.Failed(outcome.candidate, error))
    }

    private fun setStep(step: UpdateStep) {
        runtime.update { it.copy(step = step) }
    }

    private fun launchCheck(manual: Boolean) {
        if (checkJob?.isActive == true) return
        runtime.update { current ->
            current.copy(checking = true, step = if (manual) current.step?.takeIf { it.inProgress } else current.step)
        }
        checkJob = scope.launch {
            try {
                performCheck()
            } finally {
                runtime.update { it.copy(checking = false) }
            }
        }
    }

    private fun autoCheck() {
        if (checkJob?.isActive == true) return
        scope.launch {
            val persisted = store.data.first()
            if (!DailyCheckPolicy.isDue(clock(), persisted.lastCheckAt, persisted.retryAt)) return@launch
            if (!withContext(io) { network.hasValidatedNetwork() }) return@launch
            launchCheck(manual = false)
        }
    }

    private suspend fun performCheck() {
        store.setLastCheckAt(clock())
        source.list()
            .onSuccess { releases ->
                store.setKnown(UpdateSelector.known(releases))
                store.setLastResult(CheckResult.Ok)
            }
            .onFailure { failure ->
                val error = (failure as? UpdateException)?.error ?: UpdateError.Unreadable
                if (error is UpdateError.RateLimited) store.setRetryAt(error.retryAt)
                store.setLastResult(CheckResult.Error(error))
            }
    }

    private companion object {
        const val RETURN_GRACE_MS = 1_500L
    }
}
