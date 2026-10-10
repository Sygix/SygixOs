/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.LauncherSystemPreferences
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.model.SystemControl
import fr.sygix.sygixos.model.SystemControlState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface LauncherSystemStore {
    val launcherSystem: Flow<LauncherSystemPreferences>
    suspend fun dismissLauncherOnboarding()
    suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?)
    suspend fun recordBootStartObserved(currentBoot: Int?)
}

interface LauncherSystemGateway {
    fun homeRoleState(): SystemControlState
    fun accessibilityState(): SystemControlState
    fun overlayState(): SystemControlState
    fun bootCount(): Int?
    fun openHomeRole(): Boolean
    fun openHomeRoleFallback(): Boolean
    fun openAccessibility(): Boolean
    fun openOverlay(): Boolean
}

class LauncherSystemController(
    private val store: LauncherSystemStore,
    private val gateway: LauncherSystemGateway,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val clock: () -> Long = { System.nanoTime() / NANOS_PER_MILLI },
) {
    private enum class Screen { HOME_ROLE, ACCESSIBILITY, OVERLAY }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
        const val ROLE_AUTO_DENIAL_MS = 700L
    }

    private val mutableState = MutableStateFlow(LauncherSystemState())
    val state: StateFlow<LauncherSystemState> = mutableState.asStateFlow()
    private var preferences: LauncherSystemPreferences? = null
    private var startupFinished = false
    private var permissionFinished = false
    private var dismissalRequested = false
    private var bootTarget: Boolean? = null
    private var bootWrites = 0
    private val failed = mutableSetOf<Screen>()
    private var currentBoot = gateway.bootCount()
    private var roleRequestedAt: Long? = null

    init {
        onResume()
        scope.launch(dispatcher) {
            try {
                store.launcherSystem.collect {
                    preferences = it
                    publishPreferences()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                preferences = preferences ?: LauncherSystemPreferences()
                publishPreferences()
            }
        }
    }

    fun onResume() {
        currentBoot = gateway.bootCount()
        mutableState.update {
            it.copy(homeRole = read(Screen.HOME_ROLE), accessibility = read(Screen.ACCESSIBILITY), overlay = read(Screen.OVERLAY))
        }
        publishPreferences()
    }

    fun onStartupFinished() {
        startupFinished = true
        publishPreferences()
    }

    fun onPermissionFinished() {
        permissionFinished = true
        publishPreferences()
    }

    fun activate(control: SystemControl) = when (control) {
        SystemControl.HOME_ROLE -> requestHomeRole()
        SystemControl.BOOT_START -> toggleBootStart()
        SystemControl.ACCESSIBILITY -> requestAccessibility()
    }

    fun dismissOnboarding() {
        if (dismissalRequested) return
        dismissalRequested = true
        mutableState.update { it.copy(showOverlayConfirmation = false) }
        publishPreferences()
        persist { store.dismissLauncherOnboarding() }
    }

    fun confirmOverlay() {
        if (!state.value.showOverlayConfirmation) return
        val next = if (state.value.overlay == SystemControlState.INACTIVE) open(Screen.OVERLAY) else state.value.overlay
        mutableState.update { it.copy(overlay = next, showOverlayConfirmation = false, focusReturn = SystemControl.BOOT_START) }
        if (next == SystemControlState.UNAVAILABLE && state.value.bootEnabled) writeBootStart(false)
    }

    fun declineOverlay() {
        if (!state.value.showOverlayConfirmation) return
        mutableState.update { it.copy(showOverlayConfirmation = false, focusReturn = SystemControl.BOOT_START) }
    }

    fun focusReturned() {
        mutableState.update { it.copy(focusReturn = null) }
    }

    fun onHome() {
        if (!startupFinished) return
        if (state.value.showOnboarding) dismissOnboarding()
        mutableState.update { it.copy(showOverlayConfirmation = false, focusReturn = null, homeRequest = it.homeRequest + 1) }
    }

    fun recordBootObserved() {
        persist { store.recordBootStartObserved(gateway.bootCount()) }
    }

    fun onHomeRoleResult(granted: Boolean) {
        val requestedAt = roleRequestedAt ?: return
        roleRequestedAt = null
        if (granted || clock() - requestedAt >= ROLE_AUTO_DENIAL_MS) return
        if (read(Screen.HOME_ROLE) == SystemControlState.ACTIVE) return
        val opened = runCatching(gateway::openHomeRoleFallback).getOrDefault(false)
        val next = if (opened) SystemControlState.PENDING else read(Screen.HOME_ROLE).let {
            if (it == SystemControlState.ACTIVE) it else SystemControlState.UNAVAILABLE.also { failed += Screen.HOME_ROLE }
        }
        mutableState.update { it.copy(homeRole = next, focusReturn = SystemControl.HOME_ROLE) }
    }

    private fun requestHomeRole() {
        if (state.value.homeRole != SystemControlState.INACTIVE) return
        val next = open(Screen.HOME_ROLE)
        roleRequestedAt = if (next == SystemControlState.PENDING) clock() else null
        mutableState.update { it.copy(homeRole = next, focusReturn = if (next == SystemControlState.PENDING) SystemControl.HOME_ROLE else it.focusReturn) }
    }

    private fun persist(write: suspend () -> Unit) {
        scope.launch(dispatcher) {
            try {
                write()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                publishPreferences()
            }
        }
    }

    private fun requestAccessibility() {
        if (state.value.accessibility == SystemControlState.UNAVAILABLE || state.value.accessibility == SystemControlState.PENDING) return
        val next = open(Screen.ACCESSIBILITY)
        mutableState.update {
            it.copy(accessibility = next, focusReturn = if (next == SystemControlState.PENDING) SystemControl.ACCESSIBILITY else it.focusReturn)
        }
    }

    private fun toggleBootStart() {
        val enabled = !state.value.bootEnabled
        if (enabled && state.value.overlay == SystemControlState.UNAVAILABLE) return
        writeBootStart(enabled)
    }

    private fun writeBootStart(enabled: Boolean) {
        bootTarget = enabled
        bootWrites++
        mutableState.update {
            it.copy(
                bootEnabled = enabled,
                bootNotObserved = false,
                showOverlayConfirmation = enabled && it.overlay == SystemControlState.INACTIVE,
            )
        }
        scope.launch(dispatcher) {
            try {
                store.setBootStartEnabled(enabled, gateway.bootCount())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (bootTarget == enabled) bootTarget = null
            } finally {
                bootWrites--
                publishPreferences()
            }
        }
    }

    private fun publishPreferences() {
        val prefs = preferences ?: return
        val stored = prefs.bootStart.enabled
        if (bootWrites == 0 && bootTarget == stored) bootTarget = null
        val target = bootTarget
        mutableState.update {
            it.copy(
                bootEnabled = target ?: stored,
                bootNotObserved = target == null && prefs.bootStart.notObserved(currentBoot),
                showOnboarding = startupFinished && permissionFinished && !prefs.onboardingDismissed && !dismissalRequested,
                preferencesLoaded = true,
            )
        }
    }

    private fun read(screen: Screen): SystemControlState {
        val current = runCatching {
            when (screen) {
                Screen.HOME_ROLE -> gateway.homeRoleState()
                Screen.ACCESSIBILITY -> gateway.accessibilityState()
                Screen.OVERLAY -> gateway.overlayState()
            }
        }.getOrDefault(SystemControlState.UNAVAILABLE)
        return if (screen in failed && current != SystemControlState.ACTIVE) SystemControlState.UNAVAILABLE else current
    }

    private fun open(screen: Screen): SystemControlState {
        val opened = runCatching {
            when (screen) {
                Screen.HOME_ROLE -> gateway.openHomeRole()
                Screen.ACCESSIBILITY -> gateway.openAccessibility()
                Screen.OVERLAY -> gateway.openOverlay()
            }
        }.getOrDefault(false)
        if (opened) return SystemControlState.PENDING
        val current = read(screen)
        if (current == SystemControlState.ACTIVE) return current
        failed += screen
        return SystemControlState.UNAVAILABLE
    }
}
