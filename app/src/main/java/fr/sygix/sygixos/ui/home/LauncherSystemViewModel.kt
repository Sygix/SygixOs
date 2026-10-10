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

class LauncherSystemViewModel(
    store: LauncherSystemStore,
    gateway: LauncherSystemGateway,
    private val roleRequests: RoleRequestLauncher = RoleRequestLauncher(),
) : ViewModel() {
    private val controller = LauncherSystemController(store, gateway, viewModelScope)
    val state: StateFlow<LauncherSystemState> = controller.state
    val actions = LauncherSystemActions(
        activate = controller::activate,
        dismiss = controller::dismissOnboarding,
        confirmOverlay = controller::confirmOverlay,
        declineOverlay = controller::declineOverlay,
        focusReturned = controller::focusReturned,
    )

    fun onResume() = controller.onResume()

    fun onStartupFinished() = controller.onStartupFinished()

    fun onPermissionFinished() = controller.onPermissionFinished()

    fun onHome() = controller.onHome()

    fun onBootObserved() = controller.recordBootObserved()

    fun onHomeRoleResult(granted: Boolean) = controller.onHomeRoleResult(granted)

    fun bindRoleRequest(launcher: (Intent) -> Unit) = roleRequests.bind(launcher)

    fun unbindRoleRequest(launcher: (Intent) -> Unit) = roleRequests.unbind(launcher)

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as SygixOsApp
            val roleRequests = RoleRequestLauncher()
            val gateway = AndroidLauncherSystemGateway(app, AndroidHomeRoleGateway(app, roleRequests::launch))
            return LauncherSystemViewModel(app.launcherPrefs, gateway, roleRequests) as T
        }
    }
}
