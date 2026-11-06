/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassLook
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.FocusText
import fr.sygix.sygixos.core.designsystem.rememberPillPaint
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tvFocusable
import fr.sygix.sygixos.tracking.TrackingBubble
import fr.sygix.sygixos.tracking.TrackingBubblePlacement

internal enum class TrackingBubbleReturn { GRID, HERO }

internal class TrackingBubbleRestore(val target: TrackingBubbleReturn)

@Stable
internal class TrackingBubbleFocus {
    var focused = false
        private set
    var anchor: Int? = null
        private set
    private var shown: TrackingBubblePlacement? = null
    private var keys: List<String> = emptyList()
    private var current: String? = null
    private var restore: TrackingBubbleRestore? = null
    private val requesters = mutableMapOf<String, FocusRequester>()

    fun requester(key: String): FocusRequester = requesters.getOrPut(key) { FocusRequester() }

    fun onFocusChanged(value: Boolean): Boolean {
        if (focused == value) return false
        focused = value
        return true
    }

    fun onActionFocused(key: String) { current = key }

    fun focusFirst(): Boolean = keys.firstOrNull()?.let { requester(it).tryRequestFocus() } == true

    fun focusHero(): Boolean = shown == TrackingBubblePlacement.Hero && focusFirst()

    fun move(step: Int): Boolean = keys.getOrNull(keys.indexOf(current) + step)?.let { requester(it).tryRequestFocus() } == true

    fun update(placement: TrackingBubblePlacement?, actions: List<String>): TrackingBubbleRestore? {
        anchor = when (placement) {
            is TrackingBubblePlacement.Card -> placement.index
            TrackingBubblePlacement.Hero -> anchor
            null -> null
        }
        if (placement == shown && actions == keys) return restore
        if (focused) restore = TrackingBubbleRestore(if (shown == TrackingBubblePlacement.Hero) TrackingBubbleReturn.HERO else TrackingBubbleReturn.GRID)
        shown = placement
        keys = actions
        return restore
    }
}

@Composable
internal fun TrackingBubbleRestoreEffect(restore: TrackingBubbleRestore?, blocked: Boolean, grid: FocusRequester, hero: FocusRequester) {
    LaunchedEffect(restore) {
        val target = restore?.target ?: return@LaunchedEffect
        fun request(): Boolean = when (target) {
            TrackingBubbleReturn.GRID -> grid.tryRequestFocus()
            TrackingBubbleReturn.HERO -> hero.tryRequestFocus()
        }
        withFrameNanos { }
        if (blocked || request()) return@LaunchedEffect
        withFrameNanos { }
        request()
    }
}

@Composable
internal fun UpNextBubbleAnchor(index: Int, row: LazyListState, content: @Composable () -> Unit) {
    Layout(content) { measurables, constraints ->
        val placeable = measurables.first().measure(Constraints(maxWidth = constraints.maxWidth))
        layout(constraints.maxWidth, 0) {
            val layout = row.layoutInfo
            val card = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: return@layout
            val x = (card.offset - layout.viewportStartOffset).coerceAtMost(constraints.maxWidth - placeable.width).coerceAtLeast(0)
            placeable.place(x, Dimens.TrackingBubbleGap.roundToPx())
        }
    }
}

@Composable
internal fun BesideAnchor(accessory: @Composable () -> Unit, modifier: Modifier = Modifier, anchor: @Composable () -> Unit) {
    Layout({ anchor(); accessory() }, modifier) { measurables, constraints ->
        val placedAnchor = measurables.first().measure(constraints)
        val placedAccessory = measurables.getOrNull(1)?.measure(Constraints())
        layout(placedAnchor.width, placedAnchor.height) {
            placedAnchor.place(0, 0)
            placedAccessory?.place(placedAnchor.width + Dimens.TrackingBubbleSideGap.roundToPx(), (placedAnchor.height - placedAccessory.height) / 2)
        }
    }
}

@Composable
internal fun TrackingBubbleView(
    bubble: TrackingBubble,
    focus: TrackingBubbleFocus,
    enabled: Boolean,
    onAction: (String) -> Unit,
    onBack: () -> Unit,
    onExit: () -> Unit,
    onFocused: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    beside: Boolean = false,
) {
    val keys = bubble.actions
    val focusChanged by rememberUpdatedState(onFocused)
    DisposableEffect(focus) { onDispose { if (focus.onFocusChanged(false)) focusChanged(false) } }
    GlassSurface(
        modifier
            .testTag("tracking-bubble")
            .widthIn(max = Dimens.TrackingBubbleMaxWidth)
            .onFocusChanged { if (focus.onFocusChanged(it.hasFocus)) onFocused(it.hasFocus) }
            .onPreviewKeyEvent { event ->
                val down = event.type == KeyEventType.KeyDown
                when (event.key) {
                    Key.DirectionUp -> { if (down && !beside) onExit(); true }
                    Key.Back -> { if (down) { onBack(); onExit() }; true }
                    Key.DirectionDown -> true
                    Key.DirectionLeft -> { if (down && !focus.move(-1) && beside) onExit(); true }
                    Key.DirectionRight -> { if (down) focus.move(1); true }
                    else -> false
                }
            }
            .focusGroup(),
        shape = BubbleShape,
        look = GlassLook.Menu,
    ) {
        Row(
            Modifier.padding(Dimens.TrackingBubblePadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.TrackingBubbleSpacing),
        ) {
            bubble.message?.let {
                Text(stringResource(it), style = TextStyles.Row, color = SygixColors.OnDark, maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = Dimens.TrackingBubbleTextInset).testTag("tracking-bubble-message"))
            }
            keys.forEach { key ->
                BubbleAction(key, stringResource(bubble.label(key)), focus.requester(key), enabled, { focus.onActionFocused(key) }) { onAction(key) }
            }
        }
    }
}

@Composable
private fun BubbleAction(tag: String, label: String, requester: FocusRequester, enabled: Boolean, onFocused: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = rememberPillPaint(focused, rest = SygixColors.PillRest)
    Box(
        Modifier.testTag(tag)
            .tvFocusable(onFocused = { focused = it; if (it) onFocused() }, focusRequester = requester, enabled = enabled)
            .tvClickable(onClick = onClick)
            .focusPill(colors, PillShape)
            .height(Dimens.TrackingBubblePillHeight)
            .padding(horizontal = Dimens.TrackingBubblePillPadding),
        contentAlignment = Alignment.Center,
    ) {
        FocusText(label, style = if (colors.emphasized) TextStyles.RowFocused else TextStyles.Row, color = colors.content, maxLines = 1)
    }
}

private val BubbleShape = RoundedCornerShape(Dimens.TrackingBubbleCorner)
private val PillShape = RoundedCornerShape(percent = 50)
