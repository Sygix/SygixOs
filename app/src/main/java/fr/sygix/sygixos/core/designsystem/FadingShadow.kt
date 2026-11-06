/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext

internal fun Modifier.fadingDropShadow(shape: Shape, shadow: Shadow, alpha: () -> Float): Modifier =
    this then FadingShadowElement(shape, shadow, inner = false, alpha)

internal fun Modifier.fadingInnerShadow(shape: Shape, shadow: Shadow, alpha: () -> Float): Modifier =
    this then FadingShadowElement(shape, shadow, inner = true, alpha)

internal data class FadingShadowElement(val shape: Shape, val shadow: Shadow, val inner: Boolean, val alpha: () -> Float) : ModifierNodeElement<FadingShadowNode>() {
    override fun create() = FadingShadowNode(shape, shadow, inner, alpha)
    override fun update(node: FadingShadowNode) = node.update(shape, shadow, inner, alpha)
}

internal class FadingShadowNode(private var shape: Shape, private var shadow: Shadow, private var inner: Boolean, private var alpha: () -> Float) :
    Modifier.Node(), DrawModifierNode {
    private var painter: Painter? = null

    fun update(shape: Shape, shadow: Shadow, inner: Boolean, alpha: () -> Float) {
        if (shape != this.shape || shadow != this.shadow || inner != this.inner) painter = null
        this.shape = shape
        this.shadow = shadow
        this.inner = inner
        this.alpha = alpha
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val fade = alpha().coerceIn(0f, 1f)
        if (fade > 0f) with(painter()) { draw(size, fade) }
        drawContent()
    }

    override fun onDetach() { painter = null }

    private fun painter(): Painter = painter ?: requireGraphicsContext().shadowContext.let { context ->
        if (inner) context.createInnerShadowPainter(shape, shadow) else context.createDropShadowPainter(shape, shadow)
    }.also { painter = it }
}
