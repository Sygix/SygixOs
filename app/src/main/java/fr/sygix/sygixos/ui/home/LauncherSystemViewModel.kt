/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.data.AndroidLauncherSystemGateway
import fr.sygix.sygixos.domain.LauncherSystemController
import fr.sygix.sygixos.domain.LauncherSystemGateway
import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.ui.settings.LauncherSystemActions
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import fr.sygix.sygixos.domain.RepairSymptoms
import fr.sygix.sygixos.model.GuideOrigin
import fr.sygix.sygixos.model.SystemControlState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import fr.sygix.sygixos.domain.DefaultLauncherController

class LauncherSystemViewModel(
    store: LauncherSystemStore,
    gateway: LauncherSystemGateway,
    private val defaultLauncher: DefaultLauncherController,
) : ViewModel() {
    private val controller = LauncherSystemController(store, gateway, viewModelScope)
    val state: StateFlow<LauncherSystemState> =
        combine(controller.state, defaultLauncher.state) { manual, automatic ->
            manual.copy(defaultLauncher = automatic.copy(bootEnabled = manual.bootEnabled))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, controller.state.value.copy(defaultLauncher = defaultLauncher.state.value))
    val actions = LauncherSystemActions(
        openGuide = {
            defaultLauncher.onResume()
            viewModelScope.launch { defaultLauncher.state.first { it.snapshot != null || it.readFailed }; defaultLauncher.openPanel(); controller.openGuide() }
        },
        focusReturned = controller::focusReturned,
        setup = ::setup,
    )

    init {
        viewModelScope.launch {
            defaultLauncher.state.collect { if (!it.busy) controller.onResume() }
        }
        viewModelScope.launch {
            controller.state.map { it.guide }.distinctUntilChanged().collect { if (it == GuideOrigin.AUTOMATIC) defaultLauncher.openChoice() }
        }
        viewModelScope.launch {
            state.map(::symptoms).distinctUntilChanged().collect(defaultLauncher::repair)
        }
    }

    private fun symptoms(state: LauncherSystemState) = RepairSymptoms(
        homeKeyBlocked = state.accessibility == SystemControlState.BLOCKED,
        bootMissed = state.bootEnabled && state.bootNotObserved,
    )

    private fun setup(action: SetupAction) {
        val automatic = defaultLauncher
        when (action) {
            SetupAction.AUTO -> automatic.automatic()
            SetupAction.AUTO_TSUYU -> automatic.automaticWithTsuyu()
            SetupAction.TSUYU_INSTALL -> automatic.installTsuyu()
            SetupAction.TSUYU_SKIP -> automatic.withoutTsuyu()
            SetupAction.TSUYU_OPEN -> automatic.openTsuyu()
            SetupAction.TSUYU_AUTHORIZE -> automatic.authorizeTsuyu()
            SetupAction.TSUYU_LINE -> automatic.tsuyuLine()
            SetupAction.REPLACE_CONFIRM -> automatic.requestReplace()
            SetupAction.AUTHORIZE -> automatic.authorize()
            SetupAction.MANUAL -> automatic.openManual()
            SetupAction.DEBUG -> automatic.debugSettings()
            SetupAction.DISABLE_CONFIRM -> automatic.confirmDisable()
            SetupAction.DISABLE -> automatic.disable()
            SetupAction.KEEP -> automatic.fallback()
            SetupAction.RESTORE_CONFIRM -> automatic.requestRestore()
            SetupAction.RESTORE -> automatic.restore()
            SetupAction.DETAILS -> automatic.details()
            SetupAction.RETRY -> automatic.retry()
            SetupAction.BACK -> if (!automatic.back()) { automatic.onPanelClosed(); controller.closeGuide() }
            SetupAction.CLOSE -> { automatic.onPanelClosed(); controller.closeGuide() }
            SetupAction.HOME_KEY -> controller.activate(fr.sygix.sygixos.model.GuideStep.ACCESSIBILITY)
            SetupAction.BOOT -> controller.activate(fr.sygix.sygixos.model.GuideStep.BOOT)
            SetupAction.OVERLAY -> controller.activate(fr.sygix.sygixos.model.GuideStep.OVERLAY)
            SetupAction.NONE -> Unit
        }
    }

    fun onResume() { controller.onResume(); defaultLauncher.onResume(); defaultLauncher.repair(symptoms(state.value)) }

    fun onStartupFinished() = controller.onStartupFinished()

    fun onPermissionFinished() = controller.onPermissionFinished()

    fun onHome() { defaultLauncher.onPanelClosed(); controller.onHome() }

    fun onBootObserved() = controller.recordBootObserved()

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as SygixOsApp
            val gateway = AndroidLauncherSystemGateway(app, readBootCount = app::bootCount, check = app.accessibilityCheck)
            return LauncherSystemViewModel(app.createLauncherSystemStore(), gateway, app.defaultLauncherController) as T
        }
    }
}
