/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.invalidateLayer
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SQRT_HALF = 0.70710677f

fun Modifier.tvFocusable(
    onFocused: (Boolean) -> Unit = {},
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
): Modifier = this
    .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
    .focusProperties { canFocus = enabled }
    .onFocusChanged { onFocused(it.isFocused) }

fun Modifier.tvFocus(
    onFocused: (Boolean) -> Unit = {},
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true,
    shape: Shape = TileShape,
): Modifier = this
    .tvFocusable(onFocused = onFocused, focusRequester = focusRequester, enabled = enabled)
    .then(TvLiftElement(shape))
    .then(TvSheenElement(shape))

val TileShape: Shape = RoundedCornerShape(Dimens.TileCorner)

private object TvLiftKey

private data class TvLiftElement(val shape: Shape) : ModifierNodeElement<TvLiftNode>() {
    override fun create(): TvLiftNode = TvLiftNode(shape)
    override fun update(node: TvLiftNode) = node.update(shape)
    override fun InspectorInfo.inspectableProperties() {
        name = "tvLift"
    }
}

private class TvLiftNode(private var shape: Shape) :
    Modifier.Node(),
    FocusEventModifierNode,
    LayoutModifierNode,
    TraversableNode {

    override val traverseKey: Any = TvLiftKey
    val progress = Animatable(0f)
    private var focused = false

    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val p = progress.value
        val scale = 1f + (Dimens.TileFocusScale - 1f) * p
        scaleX = scale
        scaleY = scale
        translationY = -Dimens.TileFocusLift.toPx() * p
        shadowElevation = Dimens.TileFocusElevation.toPx() * p
        shape = this@TvLiftNode.shape
        clip = false
        ambientShadowColor = SygixColors.TileShadow
        spotShadowColor = SygixColors.TileShadow
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.isFocused == focused) return
        focused = focusState.isFocused
        if (!isAttached) return
        val target = if (focused) 1f else 0f
        coroutineScope.launch { progress.animateTo(target, tween(Motion.FOCUS_MS, easing = AppleEasing)) }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.placeWithLayer(0, 0, layerBlock = layerBlock) }
    }

    fun update(shape: Shape) {
        if (shape == this.shape) return
        this.shape = shape
        invalidateLayer()
    }
}

private data class TvSheenElement(val shape: Shape) : ModifierNodeElement<TvSheenNode>() {
    override fun create(): TvSheenNode = TvSheenNode(shape)
    override fun update(node: TvSheenNode) = node.update(shape)
    override fun InspectorInfo.inspectableProperties() {
        name = "tvSheen"
    }
}

private class TvSheenNode(private var shape: Shape) : Modifier.Node(), DrawModifierNode {
    private var lift: TvLiftNode? = null
    private var cachedSize = Size.Unspecified
    private var outline: Outline? = null
    private var sheen: Brush? = null

    override fun onAttach() {
        lift = findNearestAncestor(TvLiftKey) as? TvLiftNode
    }

    override fun onDetach() {
        lift = null
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val p = lift?.progress?.value ?: 0f
        if (p <= 0f) return
        if (size != cachedSize || outline == null) {
            outline = shape.createOutline(size, layoutDirection, this)
            sheen = sheenBrush(size)
            cachedSize = size
        }
        drawOutline(outline ?: return, sheen ?: return, alpha = p)
    }

    fun update(shape: Shape) {
        if (shape == this.shape) return
        this.shape = shape
        cachedSize = Size.Unspecified
        invalidateDraw()
    }
}

private fun sheenBrush(size: Size): Brush {
    val half = (size.width + size.height) * SQRT_HALF / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val direction = Offset(SQRT_HALF, SQRT_HALF) * half
    return Brush.linearGradient(
        0f to SygixColors.TileSheen.copy(alpha = 0.30f),
        0.32f to SygixColors.TileSheen.copy(alpha = 0.08f),
        0.52f to SygixColors.TileSheen.copy(alpha = 0f),
        start = center - direction,
        end = center + direction,
    )
}

fun Modifier.tvClickable(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val click by rememberUpdatedState(onClick)
    val longClick by rememberUpdatedState(onLongClick)
    var pressJob by remember { mutableStateOf<Job?>(null) }
    var longFired by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    this
        .onKeyEvent { e ->
            val select = e.key == Key.DirectionCenter || e.key == Key.Enter || e.key == Key.NumPadEnter
            if (!select) return@onKeyEvent false
            when (e.type) {
                KeyEventType.KeyDown -> {
                    val native = e.nativeKeyEvent
                    if (native.repeatCount == 0) {
                        pressed = true
                        longFired = false
                        pressJob?.cancel()
                        pressJob = longClick?.let { long ->
                            scope.launch {
                                delay(Motion.LONG_PRESS_MS)
                                longFired = true
                                long()
                            }
                        }
                    } else if (native.isLongPress && !longFired && longClick != null) {
                        pressJob?.cancel()
                        longFired = true
                        longClick?.invoke()
                    }
                    true
                }
                KeyEventType.KeyUp -> {
                    if (!pressed) return@onKeyEvent false
                    pressed = false
                    pressJob?.cancel()
                    if (!longFired) click()
                    longFired = false
                    true
                }
                else -> false
            }
        }
        .onFocusChanged { if (!it.isFocused) { pressJob?.cancel(); longFired = false; pressed = false } }
        .focusable()
}

fun FocusRequester.tryRequestFocus(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)
