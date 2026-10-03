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
import kotlinx.coroutines.launch

interface UpdateController {
    val status: StateFlow<UpdateStatus>
    val systemScreenReturns: StateFlow<Int>
    fun check()
    fun startUpdate(tag: String)
    fun setIncludePrereleases(include: Boolean)
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

    private data class Runtime(val checking: Boolean = false, val step: UpdateStep? = null)

    private class PendingAction(val sessionId: Int, val intent: Intent)

    private val runtime = MutableStateFlow(Runtime())
    private val installed: Long by lazy(installedVersionCode)
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
            UpdateSelector.proposed(persisted.known, persisted.includePrereleases, installed)
        }
        UpdateStatus(
            checking = current.checking,
            includePrereleases = persisted.includePrereleases,
            proposed = proposed,
            lastResult = persisted.lastResult,
            step = step,
        )
    }.stateIn(scope, SharingStarted.Eagerly, UpdateStatus())

    fun coldStart() {
        scope.launch(io) {
            installer.clean()
            runCatching { gateway.abandonAll() }
        }
    }

    override fun check() {
        if (checkJob?.isActive == true) return
        runtime.update { it.copy(checking = true, step = it.step?.takeIf { step -> step.inProgress }) }
        checkJob = scope.launch {
            try {
                performCheck()
            } finally {
                runtime.update { it.copy(checking = false) }
            }
        }
    }

    override fun onHomeShown() {
        homeShown = true
        autoCheck()
    }

    override fun onForeground() {
        pendingAction?.let { action ->
            pendingAction = null
            showSystemScreen(action)
        } ?: run {
            if (systemScreenShown) {
                systemScreenShown = false
                screenReturns.update { it + 1 }
            }
        }
        if (homeShown) autoCheck()
    }

    override fun setIncludePrereleases(include: Boolean) {
        scope.launch { store.setIncludePrereleases(include) }
    }

    override fun startUpdate(tag: String) {
        if (operationJob?.isActive == true || runtime.value.step?.inProgress == true) return
        val candidate = status.value.proposed?.takeIf { it.tag == tag } ?: return
        operationJob = scope.launch {
            val retryAt = store.data.first().retryAt
            if (!DailyCheckPolicy.canRequest(clock(), retryAt)) {
                setStep(UpdateStep.Failed(candidate, UpdateError.RateLimited(retryAt ?: 0L)))
                return@launch
            }
            setStep(UpdateStep.Downloading(candidate, 0))
            when (val outcome = installer.run(candidate, ::setStep)) {
                is InstallOutcome.Committed -> setStep(UpdateStep.Installing(outcome.candidate))
                is InstallOutcome.Failure -> onFailure(outcome)
            }
        }
    }

    fun onInstallStatus(status: InstallStatus) {
        val step = runtime.value.step ?: return
        when (status) {
            InstallStatus.Success -> Unit
            is InstallStatus.PendingUserAction -> onPendingUserAction(step, status)
            InstallStatus.Aborted -> setStep(UpdateStep.Failed(step.candidate, UpdateError.InstallAborted))
            is InstallStatus.Failed -> setStep(UpdateStep.Failed(step.candidate, UpdateError.InstallFailed(status.family)))
        }
    }

    private fun onPendingUserAction(step: UpdateStep, status: InstallStatus.PendingUserAction) {
        val intent = status.intent
        if (intent == null) {
            gateway.abandon(status.sessionId)
            setStep(UpdateStep.Failed(step.candidate, UpdateError.InstallFailed(InstallFailure.OTHER)))
            return
        }
        val action = PendingAction(status.sessionId, intent)
        if (foreground.isForeground) showSystemScreen(action) else pendingAction = action
    }

    private fun showSystemScreen(action: PendingAction) {
        if (screens.launch(action.intent)) {
            systemScreenShown = true
            return
        }
        gateway.abandon(action.sessionId)
        runtime.value.step?.let { setStep(UpdateStep.Failed(it.candidate, UpdateError.SystemScreenUnavailable)) }
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

    private fun autoCheck() {
        if (checkJob?.isActive == true) return
        scope.launch {
            val persisted = store.data.first()
            if (!DailyCheckPolicy.isDue(clock(), persisted.lastCheckAt, persisted.retryAt)) return@launch
            if (!network.hasValidatedNetwork()) return@launch
            check()
        }
    }

    private suspend fun performCheck() {
        val now = clock()
        val persisted = store.data.first()
        if (!DailyCheckPolicy.canRequest(now, persisted.retryAt)) {
            store.setLastResult(CheckResult.Error(UpdateError.RateLimited(persisted.retryAt ?: now)))
            return
        }
        store.setLastCheckAt(now)
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
}
