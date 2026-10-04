/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

@Stable
class BackdropLayer(val key: String) {
    var image by mutableStateOf<ImageBitmap?>(null)
    var blurred by mutableStateOf<ImageBitmap?>(null)
    val fade = Animatable(0f)
    val zoom = Animatable(1f)
    private var shader: Pair<ImageBitmap, ShaderBrush>? = null

    internal fun brush(source: ImageBitmap): ShaderBrush {
        shader?.takeIf { it.first === source }?.let { return it.second }
        val native = BitmapShader(source.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        val brush = object : ShaderBrush() {
            override fun createShader(size: Size): Shader = native
        }
        shader = source to brush
        return brush
    }
}

@Stable
class GlassBackdrop {
    val layers = mutableStateListOf<BackdropLayer>()
    internal var origin: LayoutCoordinates? = null

    var overlay: (DrawScope.(Size) -> Unit)? = null

    val ready: Boolean
        get() = layers.any { it.image != null } && layers.all { it.image == null || it.blurred != null }

    val covered: Boolean
        get() = layers.any { it.image != null && it.fade.value >= 1f }

    fun layer(key: String): BackdropLayer {
        return layers.firstOrNull { it.key == key } ?: BackdropLayer(key).also { layers.add(it) }
    }

    fun above(layer: BackdropLayer): List<BackdropLayer> = layers.drop(layers.indexOf(layer) + 1)

    fun keepOnly(layer: BackdropLayer) {
        layers.removeAll { it !== layer }
    }

    fun clear() = layers.clear()
}

fun Modifier.glassBackdropOrigin(backdrop: GlassBackdrop): Modifier = onPlaced { backdrop.origin = it }

internal class GlassAnchor {
    var coordinates: LayoutCoordinates? = null
    val matrix = Matrix()
}

internal fun Modifier.backdropGlass(
    backdrop: GlassBackdrop,
    anchor: GlassAnchor,
    shape: Shape,
    tint: Color,
    fallback: Color,
): Modifier = this
    .onPlaced { anchor.coordinates = it }
    .drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val clip = Path().apply { addOutline(outline) }
        onDrawBehind {
            val offset = backdropOffset(backdrop.origin, anchor.coordinates)
            if (offset == null || !backdrop.covered) drawOutline(outline, fallback)
            if (offset != null) drawLayers(backdrop, outline, clip, offset, anchor.matrix)
            drawOutline(outline, tint)
        }
    }

private fun backdropOffset(origin: LayoutCoordinates?, anchor: LayoutCoordinates?): Offset? {
    if (origin == null || anchor == null || !origin.isAttached || !anchor.isAttached) return null
    return origin.localPositionOf(anchor, Offset.Zero)
}

private fun DrawScope.drawLayers(backdrop: GlassBackdrop, outline: Outline, clip: Path, offset: Offset, matrix: Matrix) {
    val origin = backdrop.origin ?: return
    val width = origin.size.width.toFloat()
    val height = origin.size.height.toFloat()
    backdrop.layers.forEach { layer ->
        val source = layer.blurred ?: return@forEach
        val alpha = layer.fade.value
        if (alpha <= 0f) return@forEach
        val brush = layer.brush(source)
        val zoom = layer.zoom.value
        matrix.setScale(width / source.width, height / source.height)
        matrix.postScale(zoom, zoom, width / 2f, height / 2f)
        matrix.postTranslate(-offset.x, -offset.y)
        brush.createShader(size).setLocalMatrix(matrix)
        drawOutline(outline, brush, alpha = alpha)
    }
    val overlay = backdrop.overlay ?: return
    clipPath(clip) {
        translate(-offset.x, -offset.y) { overlay(Size(width, height)) }
    }
}
