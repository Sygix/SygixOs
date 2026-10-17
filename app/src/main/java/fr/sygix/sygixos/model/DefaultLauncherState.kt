/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

import fr.sygix.sygixos.domain.TsuyuInstallError
import fr.sygix.sygixos.domain.TsuyuInstallStep
import fr.sygix.sygixos.domain.TsuyuMinimumApi
import fr.sygix.sygixos.domain.TsuyuProbe
import fr.sygix.sygixos.domain.TsuyuRelease

enum class AdbAccessState { DISABLED, UNREACHABLE, WAITING, REFUSED, READY, LOST }
enum class StockHomeState { ACTIVE, DISABLED_USER, OUT_OF_REACH, ABSENT }
enum class SetupScreen { CHOICE, PANEL, DEBUG, AUTHORIZE, TSUYU_INSTALL, TSUYU_START, TSUYU_AUTHORIZE, PROGRESS, DONE, MORE, FALLBACK, FAILED, MANUAL, DETAILS, DISABLE, RESTORE }
enum class SetupStep { VENDOR_BOOT, HOME, VERIFY, STOCK, ACCESSIBILITY, OVERLAY, BOOT, TSUYU, REPAIR }
enum class SetupRoute { ADB, TSUYU }
enum class SetupPhase { DEBUG, INSTALL, START, AUTHORIZE, SETTINGS }
enum class TsuyuState { HIDDEN, UNAVAILABLE_TV, ABSENT, OUTDATED, STOPPED, UNAUTHORIZED, ACTIVE }
enum class TsuyuRefusal { NONE, REFUSED, NO_MORE_REQUESTS }
data class RepairRecord(val step: SetupStep, val timeMs: Long)
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
    val devOptions: Boolean = false,
    val portUnreachable: Boolean = false,
    val route: SetupRoute = SetupRoute.ADB,
    val plan: List<SetupPhase> = emptyList(),
    val fromPanel: Boolean = false,
    val replacing: Boolean = false,
    val tsuyu: TsuyuProbe = TsuyuProbe(),
    val tsuyuRelease: TsuyuRelease? = null,
    val tsuyuInstall: TsuyuInstallStep? = null,
    val tsuyuError: TsuyuInstallError? = null,
    val tsuyuRefusal: TsuyuRefusal = TsuyuRefusal.NONE,
    val lastRepair: RepairRecord? = null,
) {
    val tsuyuState: TsuyuState get() = when {
        !tsuyu.installed && tsuyuRelease == null -> TsuyuState.HIDDEN
        unreachable -> TsuyuState.UNAVAILABLE_TV
        !tsuyu.installed -> TsuyuState.ABSENT
        tsuyu.binder && (tsuyu.apiVersion ?: 0) < TsuyuMinimumApi -> TsuyuState.OUTDATED
        !tsuyu.binder -> TsuyuState.STOPPED
        !tsuyu.authorized -> TsuyuState.UNAUTHORIZED
        else -> TsuyuState.ACTIVE
    }

    val tsuyuAvailable: Boolean get() = tsuyuState != TsuyuState.HIDDEN && tsuyuState != TsuyuState.UNAVAILABLE_TV

    private val unreachable: Boolean get() = access == AdbAccessState.UNREACHABLE || portUnreachable

    val phase: SetupPhase? get() = when (screen) {
        SetupScreen.DEBUG -> SetupPhase.DEBUG
        SetupScreen.TSUYU_INSTALL -> SetupPhase.INSTALL
        SetupScreen.TSUYU_START -> SetupPhase.START
        SetupScreen.AUTHORIZE, SetupScreen.TSUYU_AUTHORIZE -> SetupPhase.AUTHORIZE
        SetupScreen.PROGRESS, SetupScreen.DONE, SetupScreen.MORE, SetupScreen.FALLBACK, SetupScreen.FAILED -> SetupPhase.SETTINGS
        else -> null
    }

    val counter: Pair<Int, Int>? get() {
        if (fromPanel || restoring || replacing) return null
        val index = phase?.let(plan::indexOf) ?: -1
        return if (index < 0) null else (index + 1) to plan.size
    }

    val automatic: AutomaticHomeState get() = when {
        !readFailed && (snapshot?.effective == true || snapshot?.roleHolder == "fr.sygix.sygixos" && snapshot.accessibilityConnected && snapshot.overlay && bootEnabled) -> AutomaticHomeState.ACTIVE
        unreachable -> AutomaticHomeState.UNAVAILABLE
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
