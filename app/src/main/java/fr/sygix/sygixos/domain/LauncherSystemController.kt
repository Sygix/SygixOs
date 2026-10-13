/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.GuideOrigin
import fr.sygix.sygixos.model.GuideStep
import fr.sygix.sygixos.model.LauncherSystemPreferences
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.model.SystemControlState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface LauncherSystemStore {
    val launcherSystem: Flow<LauncherSystemPreferences>
    suspend fun markLauncherGuideSeen()
    suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?)
    suspend fun recordBootStartObserved(currentBoot: Int?)
    suspend fun recordAccessibilityConnected(currentBoot: Int) = Unit
}

interface LauncherSystemGateway {
    fun accessibilityState(): SystemControlState
    fun overlayState(): SystemControlState
    fun bootCount(): Int?
    fun openAccessibility(): Boolean
    fun openOverlay(): Boolean
    fun verifyAccessibility() = Unit
    fun accessibilityChanges(): Flow<Unit> = emptyFlow()
}

class LauncherSystemController(
    private val store: LauncherSystemStore,
    private val gateway: LauncherSystemGateway,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    private enum class Screen { ACCESSIBILITY, OVERLAY }

    private val mutableState = MutableStateFlow(LauncherSystemState())
    val state: StateFlow<LauncherSystemState> = mutableState.asStateFlow()
    private var preferences: LauncherSystemPreferences? = null
    private var startupFinished = false
    private var permissionFinished = false
    private var seenRequested = false
    private var openedFromSettings = false
    private var bootTarget: Boolean? = null
    private var bootWrites = 0
    private val failed = mutableSetOf<Screen>()
    private var currentBoot = gateway.bootCount()
    private var accessibilityScreenOpen = false

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
        scope.launch(dispatcher) {
            try {
                gateway.accessibilityChanges().collect {
                    if (!accessibilityScreenOpen) publish { it.copy(accessibility = read(Screen.ACCESSIBILITY)) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Unit
            }
        }
    }

    fun onResume() {
        accessibilityScreenOpen = false
        currentBoot = gateway.bootCount()
        runCatching(gateway::verifyAccessibility)
        publish {
            it.copy(accessibility = read(Screen.ACCESSIBILITY), overlay = read(Screen.OVERLAY))
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

    fun activate(step: GuideStep) = when (step) {
        GuideStep.ACCESSIBILITY -> requestAccessibility()
        GuideStep.BOOT -> toggleBootStart()
        GuideStep.OVERLAY -> requestOverlay()
    }

    fun openGuide() {
        if (state.value.guide != null) return
        openedFromSettings = true
        publish { it.copy(focusReturn = null) }
        publishPreferences()
    }

    fun closeGuide() {
        when (state.value.guide) {
            GuideOrigin.AUTOMATIC -> markSeen()
            GuideOrigin.SETTINGS -> {
                openedFromSettings = false
                publish { it.copy(focusReturn = null, settingsFocus = it.settingsFocus + 1) }
                publishPreferences()
            }
            null -> Unit
        }
    }

    fun focusReturned() {
        publish { it.copy(focusReturn = null) }
    }

    fun onHome() {
        if (!startupFinished) return
        if (state.value.guide == GuideOrigin.AUTOMATIC) markSeen()
        openedFromSettings = false
        publish { it.copy(focusReturn = null, homeRequest = it.homeRequest + 1) }
        publishPreferences()
    }

    fun recordBootObserved() {
        persist { store.recordBootStartObserved(gateway.bootCount()) }
    }

    private fun publish(transform: (LauncherSystemState) -> LauncherSystemState) {
        mutableState.update(transform)
    }

    private fun markSeen() {
        if (seenRequested) return
        seenRequested = true
        publish { it.copy(focusReturn = null) }
        publishPreferences()
        persist { store.markLauncherGuideSeen() }
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
        if (state.value.accessibility == SystemControlState.UNAVAILABLE || accessibilityScreenOpen) return
        val next = open(Screen.ACCESSIBILITY)
        accessibilityScreenOpen = next == SystemControlState.PENDING
        publish {
            it.copy(accessibility = next, focusReturn = if (next == SystemControlState.PENDING) GuideStep.ACCESSIBILITY else it.focusReturn)
        }
    }

    private fun requestOverlay() {
        if (!state.value.bootEnabled || state.value.overlay != SystemControlState.INACTIVE) return
        val next = open(Screen.OVERLAY)
        if (next == SystemControlState.UNAVAILABLE) {
            publish { it.copy(overlay = next, focusReturn = GuideStep.BOOT) }
            writeBootStart(false)
        } else {
            publish { it.copy(overlay = next, focusReturn = GuideStep.OVERLAY) }
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
        publish { it.copy(bootEnabled = enabled, bootNotObserved = false) }
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
        val automatic = startupFinished && permissionFinished && !prefs.guideSeen && !seenRequested
        publish {
            it.copy(
                bootEnabled = target ?: stored,
                bootNotObserved = target == null && prefs.bootStart.notObserved(currentBoot),
                guide = when {
                    openedFromSettings -> GuideOrigin.SETTINGS
                    automatic -> GuideOrigin.AUTOMATIC
                    else -> null
                },
                preferencesLoaded = true,
            )
        }
    }

    private fun read(screen: Screen): SystemControlState {
        val current = runCatching {
            when (screen) {
                Screen.ACCESSIBILITY -> gateway.accessibilityState()
                Screen.OVERLAY -> gateway.overlayState()
            }
        }.getOrDefault(SystemControlState.UNAVAILABLE)
        return if (screen in failed && current != SystemControlState.ACTIVE) SystemControlState.UNAVAILABLE else current
    }

    private fun open(screen: Screen): SystemControlState {
        val opened = runCatching {
            when (screen) {
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
