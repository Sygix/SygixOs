/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

enum class AdbAccessState { DISABLED, UNREACHABLE, WAITING, REFUSED, READY, LOST }
enum class StockHomeState { ACTIVE, DISABLED_USER, OUT_OF_REACH, ABSENT }
enum class SetupScreen { CHOICE, PANEL, DEBUG, AUTHORIZE, PROGRESS, DONE, MORE, FALLBACK, FAILED, MANUAL, DETAILS, DISABLE, RESTORE }
enum class SetupStep { VENDOR_BOOT, HOME, VERIFY, STOCK, ACCESSIBILITY, OVERLAY, BOOT }
enum class SetupStepState { TODO, RUNNING, DONE, FAILED, CANCELLED }
enum class AutomaticHomeState { ACTIVE, TODO, UNAVAILABLE }
enum class ManualHomeState { ACTIVE, PARTIAL, BLOCKED, UNUSED, TODO }

data class HomeScreenSnapshot(
    val effective: Boolean = false,
    val roleHolder: String? = null,
    val resolvedHome: String? = null,
    val stock: Map<String, StockHomeState> = emptyMap(),
    val provisioned: Boolean = false,
    val ownHomeEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val accessibilityConnected: Boolean = false,
    val overlay: Boolean = false,
    val vendorManager: Boolean = false,
    val enabledHomes: Set<String> = emptySet(),
) {
    val canDisable: Boolean get() = provisioned && ownHomeEnabled && stock.values.any { it == StockHomeState.ACTIVE }
    val canRestore: Boolean get() = stock.values.any { it == StockHomeState.DISABLED_USER }
}

data class DefaultLauncherState(
    val screen: SetupScreen = SetupScreen.CHOICE,
    val access: AdbAccessState = AdbAccessState.DISABLED,
    val debugEnabled: Boolean = false,
    val snapshot: HomeScreenSnapshot? = null,
    val readFailed: Boolean = false,
    val busy: Boolean = false,
    val error: Boolean = false,
    val restoring: Boolean = false,
    val steps: Map<SetupStep, SetupStepState> = emptyMap(),
    val modifications: Set<SetupStep> = emptySet(),
    val focusTarget: String? = null,
    val focusVersion: Long = 0,
    val detailsReturn: SetupScreen = SetupScreen.DONE,
    val bootEnabled: Boolean = false,
) {
    val automatic: AutomaticHomeState get() = when {
        !readFailed && (snapshot?.effective == true || snapshot?.roleHolder == "fr.sygix.sygixos" && snapshot.accessibilityConnected && snapshot.overlay && bootEnabled) -> AutomaticHomeState.ACTIVE
        access == AdbAccessState.UNREACHABLE -> AutomaticHomeState.UNAVAILABLE
        else -> AutomaticHomeState.TODO
    }

    fun manual(system: LauncherSystemState): ManualHomeState = when {
        readFailed -> ManualHomeState.TODO
        system.accessibility == SystemControlState.BLOCKED -> ManualHomeState.BLOCKED
        system.accessibility == SystemControlState.ACTIVE && system.bootEnabled && system.overlay == SystemControlState.ACTIVE && !system.bootNotObserved -> ManualHomeState.ACTIVE
        system.accessibility != SystemControlState.INACTIVE && system.accessibility != SystemControlState.UNAVAILABLE || system.bootEnabled -> ManualHomeState.PARTIAL
        automatic == AutomaticHomeState.ACTIVE -> ManualHomeState.UNUSED
        else -> ManualHomeState.TODO
    }
}
