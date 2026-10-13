/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.*
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.ui.settings.LauncherSystemActions
import fr.sygix.sygixos.ui.settings.PreferenceRow
import fr.sygix.sygixos.ui.settings.StateLabel

@Composable
internal fun DefaultLauncherPanel(state: LauncherSystemState, actions: LauncherSystemActions) {
    val p7 = requireNotNull(state.defaultLauncher)
    val view = defaultLauncherView(state)
    val targets = (view.rows.filter { it.action != null }.map { it.tag } + view.buttons.map { it.tag }).ifEmpty { listOf("setup-progress-focus") }
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requester(tag: String) = requesters.getOrPut(tag) { FocusRequester() }
    var focused by remember(view.tag) { mutableStateOf<String?>(null) }
    val currentTargets by rememberUpdatedState(targets)
    val currentView by rememberUpdatedState(view)
    val returnFocus = p7.focusTarget ?: view.initialFocus
    LaunchedEffect(view.tag, p7.focusVersion) {
        withFrameNanos { }
        val target = returnFocus?.takeIf { it in currentTargets } ?: currentView.initialFocus ?: "setup-progress-focus"
        target?.let { requester(it).tryRequestFocus() }
    }
    val manualReturn = state.focusReturn?.takeIf { !state.systemPending }
    LaunchedEffect(manualReturn) {
        if (manualReturn == null) return@LaunchedEffect
        withFrameNanos { }
        currentView.initialFocus?.let { requester(it).tryRequestFocus() }
        actions.focusReturned()
    }
    val navigation = Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.Back -> { actions.setup(SetupAction.BACK); true }
            Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> {
                val permitted = if (view.verticalNavigation) event.key == Key.DirectionUp || event.key == Key.DirectionDown else event.key == Key.DirectionLeft || event.key == Key.DirectionRight
                if (permitted && currentTargets.isNotEmpty()) {
                    val index = currentTargets.indexOf(focused).coerceAtLeast(0)
                    val direction = if (event.key == Key.DirectionDown || event.key == Key.DirectionRight) 1 else -1
                    requester(currentTargets[(index + direction).coerceIn(0, currentTargets.lastIndex)]).tryRequestFocus()
                }
                true
            }
            else -> false
        }
    }
    Box(Modifier.testTag("setup-panel").fillMaxSize().zIndex(LauncherPanelZ).background(SygixColors.Scrim).then(navigation), contentAlignment = Alignment.Center) {
        if (view.initialFocus == null) Box(Modifier.size(1.dp).tvFocusable(focusRequester = requester("setup-progress-focus"), onFocused = { if (it) focused = "setup-progress-focus" }).focusable())
        GlassSurface(Modifier.width(Dimens.OnboardingWidth).testTag(view.tag), shape = RoundedCornerShape(Dimens.OnboardingCorner), look = GlassLook.Menu) {
            Column(Modifier.padding(Dimens.OnboardingPadding), verticalArrangement = Arrangement.spacedBy(Dimens.OnboardingSpacing)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = Dimens.OnboardingHeaderInsetH, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (view.mascot) { OnboardingMascot("setup-mascot"); Spacer(Modifier.width(Dimens.GuideMascotGap)) }
                        Text(stringResource(view.title), style = TextStyles.SettingsTitle, color = SygixColors.OnDark,
                            modifier = Modifier.testTag(if (view.tag.startsWith("confirm-")) "${view.tag}-title" else "setup-title"))
                    }
                    Text(stringResource(view.description), style = SygixTypography.bodyMedium, color = SygixColors.OnDarkSecondary,
                        modifier = Modifier.testTag(if (view.tag.startsWith("confirm-")) "${view.tag}-text" else "setup-description"))
                }
                if (view.errorTitle != 0) {
                    Column(Modifier.testTag("setup-error").fillMaxWidth().background(SygixColors.Error.copy(alpha = .12f), RoundedCornerShape(14.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(view.errorTitle), style = TextStyles.Row, color = SygixColors.Error, modifier = Modifier.testTag("setup-error-title"))
                        Text(stringResource(view.errorDetail), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, modifier = Modifier.testTag("setup-error-detail"))
                    }
                }
                view.rows.forEach { row ->
                    key(row.tag) {
                        if (row.action != null) PreferenceRow(
                            label = stringResource(row.label), detail = if (row.detail == 0) null else stringResource(row.detail), partsTag = row.tag,
                            focusRequester = requester(row.tag), enabled = true, onClick = { if (!p7.busy) actions.setup(row.action) },
                            onFocused = { if (it) focused = row.tag }, modifier = Modifier.testTag(row.tag),
                            dimmed = row.action == SetupAction.NONE,
                        ) { colors -> if (row.status != 0) SetupStateLabel(row, colors.tertiary) }
                        else SetupStaticRow(row)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(start = Dimens.OnboardingHeaderInsetH, top = Dimens.OnboardingFooterTop), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(view.hint), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, modifier = Modifier.testTag("setup-hint"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        view.buttons.forEach { button ->
                            key(button.tag) { SetupPanelButton(button, requester(button.tag), { focused = button.tag }) { if (!p7.busy) actions.setup(button.action) } }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStaticRow(row: SetupRow) {
    Row(Modifier.testTag(row.tag).fillMaxWidth().padding(horizontal = Dimens.OnboardingHeaderInsetH, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        row.number?.let { Text(it.toString(), style = TextStyles.RowState, color = SygixColors.OnDarkSecondary, modifier = Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(stringResource(row.label), style = TextStyles.Row, color = SygixColors.OnDark)
            if (row.detail != 0) Text(stringResource(row.detail), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, modifier = Modifier.testTag("${row.tag}-detail"))
        }
        if (row.status != 0) SetupStateLabel(row, SygixColors.OnDarkSecondary)
    }
}

@Composable
private fun SetupStateLabel(row: SetupRow, color: androidx.compose.ui.graphics.Color) {
    val dot = when (row.status) {
        R.string.setup_state_done, R.string.setup_state_active, R.string.setup_state_active_summary -> SygixColors.SwitchOn
        R.string.setup_state_pending, R.string.setup_state_running -> SygixColors.Badge
        else -> null
    }
    StateLabel(stringResource(row.status), dot, if (row.status == R.string.setup_state_failed) SygixColors.Error else color, row.tag)
}

@Composable
private fun SetupPanelButton(button: SetupButton, requester: FocusRequester, onFocused: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused, rest = SygixColors.PillRest)
    Box(Modifier.testTag(button.tag).tvFocusable(focusRequester = requester, onFocused = { focused = it; if (it) onFocused() })
        .tvClickable(onClick = onClick).focusPill(colors, RoundedCornerShape(Dimens.PillCorner))
        .height(Dimens.MenuActionHeight).padding(horizontal = Dimens.OnboardingButtonPaddingH), contentAlignment = Alignment.Center) {
        Text(stringResource(button.label), style = if (focused) TextStyles.RowFocused else TextStyles.Row, color = colors.content)
    }
}
