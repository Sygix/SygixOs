/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import fr.sygix.sygixos.R
import fr.sygix.sygixos.model.*

enum class SetupAction { NONE, AUTO, AUTHORIZE, MANUAL, DEBUG, DISABLE_CONFIRM, DISABLE, KEEP, RESTORE_CONFIRM, RESTORE, DETAILS, RETRY, BACK, CLOSE, HOME_KEY, BOOT, OVERLAY }
data class SetupRow(val tag: String, val label: Int, val detail: Int = 0, val status: Int = 0, val action: SetupAction? = null, val number: Int? = null)
data class SetupButton(val tag: String, val label: Int, val action: SetupAction)
data class SetupPanelView(
    val tag: String,
    val title: Int,
    val description: Int,
    val rows: List<SetupRow>,
    val buttons: List<SetupButton>,
    val initialFocus: String?,
    val hint: Int = R.string.setup_hint_close,
    val mascot: Boolean = true,
    val errorTitle: Int = 0,
    val errorDetail: Int = 0,
    val verticalNavigation: Boolean = false,
)

fun AutomaticHomeState.label(): Int = when (this) {
    AutomaticHomeState.ACTIVE -> R.string.setup_state_active
    AutomaticHomeState.TODO -> R.string.setup_state_configure
    AutomaticHomeState.UNAVAILABLE -> R.string.setup_state_unavailable_tv
}
fun ManualHomeState.label(): Int = when (this) {
    ManualHomeState.ACTIVE -> R.string.setup_state_active
    ManualHomeState.PARTIAL -> R.string.setup_state_partial
    ManualHomeState.BLOCKED -> R.string.setup_state_blocked_route
    ManualHomeState.UNUSED -> R.string.setup_state_unused
    ManualHomeState.TODO -> R.string.setup_state_todo
}
fun SystemControlState.setupLabel(): Int = when (this) {
    SystemControlState.ACTIVE -> R.string.setup_state_done
    SystemControlState.INACTIVE -> R.string.setup_state_todo
    SystemControlState.UNAVAILABLE -> R.string.setup_state_unavailable
    SystemControlState.PENDING -> R.string.setup_state_pending
    SystemControlState.BLOCKED -> R.string.setup_state_blocked
}
fun SetupStepState.label(): Int = when (this) {
    SetupStepState.TODO -> R.string.setup_state_future
    SetupStepState.RUNNING -> R.string.setup_state_running
    SetupStepState.DONE -> R.string.setup_state_done
    SetupStepState.FAILED -> R.string.setup_state_failed
    SetupStepState.CANCELLED -> R.string.setup_state_cancelled
}

private fun stepRow(step: SetupStep, state: DefaultLauncherState): SetupRow {
    val (tag, title, detail) = when (step) {
        SetupStep.VENDOR_BOOT -> Triple("setup-step-boot", R.string.setup_vendor_boot, R.string.setup_vendor_boot_detail)
        SetupStep.HOME -> Triple("setup-step-home", R.string.setup_home, R.string.setup_home_detail)
        SetupStep.VERIFY -> Triple("setup-step-verify", R.string.setup_verify, R.string.setup_verify_detail)
        SetupStep.STOCK -> Triple("setup-step-stock", R.string.setup_stock, R.string.setup_stock_disabled)
        SetupStep.ACCESSIBILITY -> Triple("setup-fallback-home", R.string.setup_home_key, R.string.setup_home_key_detail)
        SetupStep.OVERLAY -> Triple("setup-fallback-overlay", R.string.setup_overlay, R.string.setup_overlay_detail)
        SetupStep.BOOT -> Triple("setup-fallback-boot", R.string.setup_boot, R.string.setup_boot_detail)
    }
    val status = state.steps[step] ?: SetupStepState.TODO
    return SetupRow(tag, title, if (status == SetupStepState.CANCELLED) R.string.setup_not_done else detail, status.label())
}

fun defaultLauncherView(system: LauncherSystemState): SetupPanelView {
    val state = system.defaultLauncher
    val automatic = state.automatic
    val manual = state.manual(system)
    fun button(tag: String, label: Int, action: SetupAction) = SetupButton(tag, label, action)
    val done = listOf(button("setup-done-details", R.string.setup_details, SetupAction.DETAILS), button("setup-done-finish", R.string.setup_finish, SetupAction.CLOSE))
    val debugStatus = SetupRow("setup-debug-status", R.string.setup_debug_label, if (state.screen == SetupScreen.AUTHORIZE) R.string.setup_authorize_status_detail else R.string.setup_debug_status_detail, if (state.debugEnabled) R.string.setup_debug_enabled else R.string.setup_debug_disabled)
    val vendor = if (SetupStep.VENDOR_BOOT in state.steps) listOf(stepRow(SetupStep.VENDOR_BOOT, state)) else emptyList()
    return when (state.screen) {
        SetupScreen.CHOICE -> SetupPanelView(
            "setup-choice", R.string.setup_choice_title, R.string.setup_choice_description,
            listOf(
                SetupRow("setup-choice-auto", if (automatic == AutomaticHomeState.UNAVAILABLE) R.string.setup_auto else R.string.setup_auto_recommended,
                    if (automatic == AutomaticHomeState.UNAVAILABLE) R.string.setup_auto_unavailable_detail else R.string.setup_auto_detail, automatic.label(),
                    if (automatic == AutomaticHomeState.UNAVAILABLE) SetupAction.NONE else SetupAction.AUTO),
                SetupRow("setup-choice-manual", R.string.setup_manual_choice, R.string.setup_manual_choice_detail, manual.label(), SetupAction.MANUAL),
            ), listOf(button("setup-later", R.string.setup_later, SetupAction.CLOSE)),
            if (automatic == AutomaticHomeState.UNAVAILABLE) "setup-choice-manual" else "setup-choice-auto", R.string.setup_hint_later, verticalNavigation = true,
        )
        SetupScreen.DEBUG -> SetupPanelView(
            "setup-debug", R.string.setup_debug_title, R.string.setup_debug_description,
            listOf(SetupRow("setup-debug-step-1", R.string.setup_debug_step_one, R.string.setup_debug_step_one_detail, number = 1), SetupRow("setup-debug-step-2", R.string.setup_debug_step_two, R.string.setup_debug_step_two_detail, number = 2), debugStatus),
            (if (state.error) listOf(button("setup-debug-back", R.string.setup_back, SetupAction.BACK)) else emptyList()) + button("setup-debug-open", R.string.setup_debug_open, SetupAction.DEBUG),
            "setup-debug-open", R.string.setup_hint_previous,
            errorTitle = if (state.error) R.string.setup_debug_error else 0, errorDetail = if (state.error) R.string.setup_debug_error_detail else 0,
        )
        SetupScreen.AUTHORIZE -> SetupPanelView(
            "setup-authorize", R.string.setup_authorize_title, R.string.setup_authorize_description,
            (if (state.error) emptyList() else listOf(SetupRow("setup-authorize-step-1", R.string.setup_authorize_step_one, R.string.setup_authorize_step_one_detail, number = 1), SetupRow("setup-authorize-step-2", R.string.setup_authorize_step_two, R.string.setup_authorize_step_two_detail, number = 2))) + debugStatus,
            if (state.error) listOf(button("setup-authorize-back", R.string.setup_back, SetupAction.BACK), button("setup-authorize-retry", R.string.setup_retry, SetupAction.AUTHORIZE)) else listOf(button("setup-authorize-continue", R.string.setup_continue, SetupAction.AUTHORIZE)),
            if (state.error) "setup-authorize-retry" else "setup-authorize-continue", R.string.setup_hint_previous,
            errorTitle = if (state.error) R.string.setup_authorize_error else 0, errorDetail = if (state.error) R.string.setup_authorize_error_detail else 0,
        )
        SetupScreen.PROGRESS -> SetupPanelView("setup-progress", R.string.setup_progress_title, R.string.setup_progress_description,
            vendor + listOf(stepRow(SetupStep.HOME, state), stepRow(SetupStep.VERIFY, state)) + state.steps.keys.filter { it !in setOf(SetupStep.VENDOR_BOOT, SetupStep.HOME, SetupStep.VERIFY) }.map { stepRow(it, state) },
            emptyList(), null, R.string.setup_hint_wait)
        SetupScreen.DONE -> SetupPanelView("setup-done", R.string.setup_done_title, R.string.setup_done_description,
            vendor + SetupRow("setup-step-home", R.string.setup_home, R.string.setup_home_done_detail, if (!state.readFailed && state.snapshot?.effective == true) R.string.setup_state_done else R.string.setup_state_todo) +
                if (state.debugEnabled) listOf(SetupRow("setup-done-tip", R.string.setup_debug_tip, R.string.setup_debug_tip_detail)) else emptyList(), done, "setup-done-finish")
        SetupScreen.MORE -> SetupPanelView("setup-more", R.string.setup_more_title, R.string.setup_more_description,
            vendor + SetupRow("setup-more-home", R.string.setup_home, R.string.setup_more_home_detail, R.string.setup_state_todo),
            listOf(button("setup-more-keep", R.string.setup_keep, SetupAction.KEEP), button("setup-more-disable", R.string.setup_disable, SetupAction.DISABLE_CONFIRM)), "setup-more-disable", R.string.setup_hint_keep)
        SetupScreen.DISABLE -> SetupPanelView("confirm-disable", R.string.setup_disable_title, R.string.setup_disable_description, emptyList(),
            listOf(button("confirm-disable-cancel", R.string.setup_cancel, SetupAction.BACK), button("confirm-disable-accept", R.string.setup_disable_accept, SetupAction.DISABLE)), "confirm-disable-cancel", R.string.setup_hint_cancel, mascot = false)
        SetupScreen.RESTORE -> SetupPanelView("confirm-restore", R.string.setup_restore_title, R.string.setup_restore_description, emptyList(),
            listOf(button("confirm-restore-cancel", R.string.setup_cancel, SetupAction.BACK), button("confirm-restore-accept", R.string.setup_restore_accept, SetupAction.RESTORE)), "confirm-restore-accept", R.string.setup_hint_cancel, mascot = false)
        SetupScreen.FALLBACK -> SetupPanelView("setup-fallback", R.string.setup_fallback_title, R.string.setup_fallback_description,
            listOf(SetupRow("setup-fallback-home", R.string.setup_home_key, R.string.setup_home_key_detail, system.accessibility.setupLabel()), SetupRow("setup-fallback-overlay", R.string.setup_overlay, R.string.setup_overlay_detail, system.overlay.setupLabel()), SetupRow("setup-fallback-boot", R.string.setup_boot, R.string.setup_boot_detail, if (system.bootEnabled && !system.bootNotObserved) R.string.setup_state_done else R.string.setup_state_todo), SetupRow("setup-fallback-tip", R.string.setup_tip, R.string.setup_tip_detail)),
            listOf(button("setup-fallback-details", R.string.setup_details, SetupAction.DETAILS), button("setup-fallback-finish", R.string.setup_finish, SetupAction.CLOSE)), "setup-fallback-finish")
        SetupScreen.FAILED -> SetupPanelView("setup-failed", R.string.setup_failed_title, R.string.setup_failed_description,
            state.steps.keys.map { step -> stepRow(step, state).let { if (state.steps[step] == SetupStepState.CANCELLED) it else it.copy(detail = 0) } }, listOf(button("setup-failed-close", R.string.setup_close, SetupAction.CLOSE), button("setup-failed-retry", R.string.setup_retry, SetupAction.RETRY)), "setup-failed-retry",
            errorTitle = when { state.restoring -> R.string.setup_restore_error; state.steps[SetupStep.HOME] == SetupStepState.FAILED -> R.string.setup_home_error; state.steps[SetupStep.VENDOR_BOOT] == SetupStepState.FAILED -> R.string.setup_vendor_boot_error; else -> R.string.setup_generic_error },
            errorDetail = when { state.steps[SetupStep.HOME] == SetupStepState.FAILED -> R.string.setup_home_error_detail; state.steps[SetupStep.VENDOR_BOOT] == SetupStepState.FAILED -> R.string.setup_vendor_boot_error_detail; else -> R.string.setup_generic_error_detail })
        SetupScreen.MANUAL -> {
            val rows = buildList {
                add(SetupRow("setup-manual-home", R.string.setup_manual_home, when (system.accessibility) { SystemControlState.BLOCKED -> R.string.setup_manual_home_blocked; SystemControlState.UNAVAILABLE -> R.string.setup_manual_home_unavailable; else -> R.string.setup_manual_home_detail }, system.accessibility.setupLabel(), if (system.accessibility == SystemControlState.UNAVAILABLE) SetupAction.NONE else SetupAction.HOME_KEY))
                add(SetupRow("setup-manual-boot", R.string.setup_manual_boot, when { system.bootBlocked -> R.string.setup_manual_boot_unavailable; system.bootNotObserved -> R.string.setup_manual_boot_not_observed; else -> R.string.setup_boot_detail }, when { system.bootBlocked -> R.string.setup_state_unavailable; system.bootEnabled && system.overlay == SystemControlState.ACTIVE -> R.string.setup_state_done; else -> R.string.setup_state_todo }, if (system.bootBlocked) SetupAction.NONE else SetupAction.BOOT))
                if (system.bootEnabled && system.overlay in setOf(SystemControlState.INACTIVE, SystemControlState.PENDING)) add(SetupRow("setup-manual-overlay", R.string.setup_manual_overlay, R.string.setup_manual_overlay_detail, system.overlay.setupLabel(), SetupAction.OVERLAY))
                if (state.snapshot?.vendorManager == true) add(SetupRow("setup-manual-vendor-boot", R.string.setup_manual_vendor, R.string.setup_manual_vendor_detail, R.string.setup_state_unavailable, SetupAction.NONE))
                add(SetupRow("setup-manual-tip", R.string.setup_tip, R.string.setup_tip_detail))
            }
            val returned = system.focusReturn?.let { when (it) { GuideStep.ACCESSIBILITY -> "setup-manual-home"; GuideStep.BOOT -> "setup-manual-boot"; GuideStep.OVERLAY -> if (rows.any { r -> r.tag == "setup-manual-overlay" }) "setup-manual-overlay" else "setup-manual-boot" } }
            SetupPanelView("setup-manual", R.string.setup_manual_title, R.string.setup_manual_description, rows, listOf(button("setup-manual-finish", R.string.setup_finish, SetupAction.CLOSE)), returned ?: rows.firstOrNull { it.action != null && it.status != R.string.setup_state_done }?.tag ?: "setup-manual-finish", verticalNavigation = true)
        }
        SetupScreen.PANEL -> {
            val rows = buildList {
                add(SetupRow("launcher-panel-auto", R.string.setup_auto, when { automatic != AutomaticHomeState.ACTIVE -> R.string.launcher_panel_auto_todo_detail; state.snapshot?.canRestore == true -> R.string.launcher_panel_auto_active_detail; state.snapshot?.effective == true -> R.string.launcher_panel_auto_home_detail; else -> R.string.launcher_panel_auto_fallback_detail }, automatic.label(), if (automatic == AutomaticHomeState.UNAVAILABLE) SetupAction.NONE else SetupAction.AUTO))
                add(SetupRow("launcher-panel-manual", R.string.setup_manual_title, when (manual) { ManualHomeState.UNUSED -> R.string.launcher_panel_manual_unused_detail; ManualHomeState.TODO -> R.string.launcher_panel_manual_todo_detail; ManualHomeState.BLOCKED -> R.string.setup_manual_home_blocked; ManualHomeState.ACTIVE -> R.string.launcher_panel_manual_active_detail; else -> R.string.launcher_panel_manual_partial_detail }, manual.label(), SetupAction.MANUAL))
                add(SetupRow("launcher-panel-relaunch", R.string.launcher_panel_relaunch, R.string.launcher_panel_relaunch_detail, action = SetupAction.AUTO))
                if (state.snapshot?.canRestore == true) add(SetupRow("launcher-panel-restore", R.string.launcher_panel_restore, R.string.launcher_panel_restore_detail, action = SetupAction.RESTORE_CONFIRM))
            }
            SetupPanelView("launcher-panel", R.string.launcher_panel_title, if (automatic == AutomaticHomeState.ACTIVE || manual == ManualHomeState.ACTIVE) R.string.launcher_panel_active_description else R.string.launcher_panel_inactive_description, rows, emptyList(), "launcher-panel-relaunch", verticalNavigation = true)
        }
        SetupScreen.DETAILS -> {
            val rows = buildList {
                if (SetupStep.VENDOR_BOOT in state.modifications) add(stepRow(SetupStep.VENDOR_BOOT, state).copy(tag = "setup-details-item-boot"))
                if (SetupStep.HOME in state.modifications) add(SetupRow("setup-details-item-home", R.string.setup_home, R.string.setup_details_home_detail, if (state.snapshot?.roleHolder == "fr.sygix.sygixos" && !state.readFailed) R.string.setup_state_done else R.string.setup_state_todo))
                if (SetupStep.OVERLAY in state.modifications) add(SetupRow("setup-details-item-overlay", R.string.setup_overlay, R.string.setup_overlay_detail, system.overlay.setupLabel()))
                if (SetupStep.ACCESSIBILITY in state.modifications) add(SetupRow("setup-details-item-accessibility", R.string.setup_home_key, R.string.setup_details_home_key_detail, system.accessibility.setupLabel()))
                add(SetupRow("setup-details-item-stock", R.string.setup_stock, if (state.snapshot?.canRestore == true) R.string.setup_stock_disabled else R.string.setup_stock_unchanged, if (state.snapshot?.canRestore == true) R.string.setup_state_done else R.string.setup_state_unchanged))
            }
            SetupPanelView("setup-details", R.string.setup_details_title, R.string.setup_details_description, rows, listOf(button("setup-details-close", R.string.setup_close, SetupAction.BACK)), "setup-details-close", mascot = false)
        }
    }
}
