/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyInputModifierNode
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
): Modifier = this.then(TvClickElement(onClick, onLongClick)).focusable()

private data class TvClickElement(val onClick: () -> Unit, val onLongClick: (() -> Unit)?) : ModifierNodeElement<TvClickNode>() {
    override fun create() = TvClickNode(this)
    override fun update(node: TvClickNode) {
        node.element = this
    }
}

private class TvClickNode(var element: TvClickElement) : Modifier.Node(), KeyInputModifierNode, FocusEventModifierNode {
    private var pressJob: Job? = null
    private var longFired = false
    private var pressed = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.key != Key.DirectionCenter && event.key != Key.Enter && event.key != Key.NumPadEnter) return false
        val long = element.onLongClick
        val native = event.nativeKeyEvent
        when {
            event.type == KeyEventType.KeyDown && native.repeatCount == 0 -> {
                release()
                pressed = true
                pressJob = long?.let { coroutineScope.launch { delay(Motion.LONG_PRESS_MS); longFired = true; it() } }
            }
            event.type == KeyEventType.KeyDown -> if (native.isLongPress && !longFired && long != null) {
                pressJob?.cancel()
                longFired = true
                long()
            }
            event.type == KeyEventType.KeyUp && pressed -> {
                val clicked = !longFired
                release()
                if (clicked) element.onClick()
            }
            else -> return false
        }
        return true
    }

    override fun onPreKeyEvent(event: KeyEvent) = false

    override fun onFocusEvent(focusState: FocusState) {
        if (!focusState.isFocused) release()
    }

    override fun onDetach() = release()

    private fun release() {
        pressJob?.cancel()
        longFired = false
        pressed = false
    }
}

fun FocusRequester.tryRequestFocus(): Boolean =
    runCatching { requestFocus(FocusDirection.Enter) }.getOrDefault(false)
