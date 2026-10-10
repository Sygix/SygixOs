/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import fr.sygix.sygixos.domain.LauncherSystemGateway
import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.model.BootStartState
import fr.sygix.sygixos.model.LauncherSystemPreferences
import fr.sygix.sygixos.model.SystemControlState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.rules.ExternalResource

internal class FakeLauncherStore(prefs: LauncherSystemPreferences = LauncherSystemPreferences()) : LauncherSystemStore {
    override val launcherSystem = MutableStateFlow(prefs)
    val dismissed: Boolean get() = launcherSystem.value.onboardingDismissed
    val boot: BootStartState get() = launcherSystem.value.bootStart

    override suspend fun dismissLauncherOnboarding() {
        launcherSystem.value = launcherSystem.value.copy(onboardingDismissed = true)
    }

    override suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?) {
        launcherSystem.value = launcherSystem.value.copy(
            bootStart = if (enabled) BootStartState(true, currentBoot, null) else BootStartState(),
        )
    }

    override suspend fun recordBootStartObserved(currentBoot: Int?) {
        launcherSystem.value = launcherSystem.value.copy(bootStart = launcherSystem.value.bootStart.copy(observedAtBoot = currentBoot))
    }
}

internal class FakeLauncherGateway(
    var home: SystemControlState = SystemControlState.INACTIVE,
    var accessibility: SystemControlState = SystemControlState.INACTIVE,
    var overlay: SystemControlState = SystemControlState.INACTIVE,
    var boot: Int? = 8,
    var succeeds: Boolean = true,
) : LauncherSystemGateway {
    val opened = mutableListOf<String>()
    override fun homeRoleState() = home
    override fun accessibilityState() = accessibility
    override fun overlayState() = overlay
    override fun bootCount() = boot
    override fun openHomeRole() = record("home")
    override fun openHomeRoleFallback() = record("home-fallback")
    override fun openAccessibility() = record("accessibility")
    override fun openOverlay() = record("overlay")

    private fun record(name: String): Boolean {
        opened += name
        return succeeds
    }
}

class LauncherViewModelRule : ExternalResource() {
    private val owner = ViewModelStore()
    private var created = 0

    fun create(
        store: LauncherSystemStore = FakeLauncherStore(),
        gateway: LauncherSystemGateway = FakeLauncherGateway(),
        started: Boolean = true,
        onboarding: Boolean = false,
    ): LauncherSystemViewModel {
        if (!onboarding && store is FakeLauncherStore) {
            store.launcherSystem.value = store.launcherSystem.value.copy(onboardingDismissed = true)
        }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LauncherSystemViewModel(store, gateway) as T
        }
        return ViewModelProvider(owner, factory)["launcher-${created++}", LauncherSystemViewModel::class.java].apply {
            if (started) {
                onStartupFinished()
                onPermissionFinished()
            }
        }
    }

    override fun after() = owner.clear()
}
