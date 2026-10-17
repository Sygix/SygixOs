/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import fr.sygix.sygixos.domain.LauncherSystemGateway
import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.domain.DefaultLauncherController
import fr.sygix.sygixos.domain.DefaultLauncherMemory
import fr.sygix.sygixos.domain.DefaultLauncherStore
import fr.sygix.sygixos.data.AdbAccessGateway
import fr.sygix.sygixos.data.HomeStateReader
import fr.sygix.sygixos.data.LocalAdbTransport
import fr.sygix.sygixos.data.PrivilegedShell
import fr.sygix.sygixos.model.HomeScreenSnapshot
import fr.sygix.sygixos.model.SetupStep
import fr.sygix.sygixos.model.BootStartState
import fr.sygix.sygixos.model.LauncherSystemPreferences
import fr.sygix.sygixos.model.SystemControlState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.rules.ExternalResource

internal class FakeLauncherStore(prefs: LauncherSystemPreferences = LauncherSystemPreferences()) : LauncherSystemStore {
    override val launcherSystem = MutableStateFlow(prefs)
    val dismissed: Boolean get() = launcherSystem.value.guideSeen
    val boot: BootStartState get() = launcherSystem.value.bootStart

    var seenWrites = 0

    override suspend fun markLauncherGuideSeen() {
        seenWrites++
        launcherSystem.value = launcherSystem.value.copy(guideSeen = true)
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
    var accessibility: SystemControlState = SystemControlState.INACTIVE,
    var overlay: SystemControlState = SystemControlState.INACTIVE,
    var boot: Int? = 8,
    var succeeds: Boolean = true,
) : LauncherSystemGateway {
    val opened = mutableListOf<String>()
    val accessibilityUpdates = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    var verifications = 0
    override fun verifyAccessibility() { verifications++ }
    override fun accessibilityChanges() = accessibilityUpdates
    override fun accessibilityState() = accessibility
    override fun overlayState() = overlay
    override fun bootCount() = boot
    override fun openAccessibility() = record("accessibility")
    override fun openOverlay() = record("overlay")

    private fun record(name: String): Boolean {
        opened += name
        return succeeds
    }
}

class LauncherViewModelRule : ExternalResource() {
    private val owner = ViewModelStore()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var created = 0

    fun create(
        store: LauncherSystemStore = FakeLauncherStore(),
        gateway: LauncherSystemGateway = FakeLauncherGateway(),
        started: Boolean = true,
        onboarding: Boolean = false,
    ): LauncherSystemViewModel {
        if (!onboarding && store is FakeLauncherStore) {
            store.launcherSystem.value = store.launcherSystem.value.copy(guideSeen = true)
        }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LauncherSystemViewModel(store, gateway, setupController(store)) as T
        }
        return ViewModelProvider(owner, factory)["launcher-${created++}", LauncherSystemViewModel::class.java].apply {
            if (started) {
                onStartupFinished()
                onPermissionFinished()
            }
        }
    }

    private fun setupController(store: LauncherSystemStore): DefaultLauncherController {
        val access = AdbAccessGateway({ false }, object : LocalAdbTransport {
            override suspend fun reachable(): Boolean = error("Unexpected ADB connection")
            override suspend fun authenticate(): String = error("Unexpected ADB authentication")
            override suspend fun command(command: String): String = error("Unexpected shell operation")
            override fun close() = Unit
        })
        val reader = HomeStateReader { Result.success(HomeScreenSnapshot()) }
        val memory = object : DefaultLauncherStore {
            override suspend fun loadMemory() = DefaultLauncherMemory()
            override suspend fun rememberPrevious(component: String) = Unit
            override suspend fun record(step: SetupStep) = Unit
        }
        return DefaultLauncherController(access, PrivilegedShell(access, reader, 0, { null }, { false }), reader, memory, scope,
            enableBoot = { store.setBootStartEnabled(true, null) }, openDebug = { false })
    }

    override fun after() {
        owner.clear()
        scope.cancel()
    }
}
