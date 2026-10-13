/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.data.AndroidHomeRoleGateway
import fr.sygix.sygixos.data.AndroidLauncherSystemGateway
import fr.sygix.sygixos.data.RoleRequestLauncher
import fr.sygix.sygixos.domain.LauncherSystemController
import fr.sygix.sygixos.domain.LauncherSystemGateway
import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.ui.settings.LauncherSystemActions
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import fr.sygix.sygixos.domain.DefaultLauncherController

class LauncherSystemViewModel(
    store: LauncherSystemStore,
    gateway: LauncherSystemGateway,
    private val roleRequests: RoleRequestLauncher = RoleRequestLauncher(),
    private val defaultLauncher: DefaultLauncherController? = null,
) : ViewModel() {
    private val controller = LauncherSystemController(store, gateway, viewModelScope)
    val state: StateFlow<LauncherSystemState> = if (defaultLauncher == null) controller.state else
        combine(controller.state, defaultLauncher.state) { legacy, automatic ->
            legacy.copy(defaultLauncher = automatic.copy(bootEnabled = legacy.bootEnabled))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, controller.state.value.copy(defaultLauncher = defaultLauncher.state.value))
    val actions = LauncherSystemActions(
        activate = controller::activate,
        close = { if (defaultLauncher?.back() != true) { defaultLauncher?.onPanelClosed(); controller.closeGuide() } },
        openGuide = { defaultLauncher?.onResume(); defaultLauncher?.openPanel(); controller.openGuide() },
        focusReturned = controller::focusReturned,
        setup = ::setup,
    )

    init {
        if (defaultLauncher != null) viewModelScope.launch {
            defaultLauncher.state.collect { if (!it.busy) controller.onResume() }
        }
    }

    private fun setup(action: SetupAction) {
        val automatic = defaultLauncher ?: return
        when (action) {
            SetupAction.AUTO -> automatic.automatic()
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

    fun onResume() { controller.onResume(); defaultLauncher?.onResume() }

    fun onStartupFinished() = controller.onStartupFinished()

    fun onPermissionFinished() = controller.onPermissionFinished()

    fun onHome() { defaultLauncher?.onPanelClosed(); controller.onHome() }

    fun onBootObserved() = controller.recordBootObserved()

    fun onHomeRoleResult(granted: Boolean) = controller.onHomeRoleResult(granted)

    fun bindRoleRequest(launcher: (Intent) -> Unit) = roleRequests.bind(launcher)

    fun unbindRoleRequest(launcher: (Intent) -> Unit) = roleRequests.unbind(launcher)

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as SygixOsApp
            val roleRequests = RoleRequestLauncher()
            val gateway = AndroidLauncherSystemGateway(app, AndroidHomeRoleGateway(app, roleRequests::launch), check = app.accessibilityCheck)
            return LauncherSystemViewModel(app.launcherPrefs, gateway, roleRequests, app.defaultLauncherController) as T
        }
    }
}
