/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import coil.transform.Transformation
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.domain.ImageBounds
import fr.sygix.sygixos.domain.PixelSize
import kotlin.math.max
import kotlin.math.roundToInt

internal data class PosterSpec(val width: Int, val height: Int, val cornerWidth: Float, val cornerHeight: Float) {

    val backdrop: PixelSize
        get() = PixelSize(max(width / BACKDROP_DIVISOR, 1), max(height / BACKDROP_DIVISOR, 1))

    fun scaled(target: PixelSize): PosterSpec {
        val ratio = target.width.toFloat() / width
        return PosterSpec(target.width, target.height, cornerWidth * ratio, cornerHeight * ratio)
    }

    companion object {
        const val BACKDROP_DIVISOR = 8
        const val BACKDROP_BLUR_PX = 28f
        const val BACKDROP_SATURATION = 1.7f

        fun of(viewportWidth: Int, viewportHeight: Int, cornerWidthPx: Float, cornerHeightPx: Float): PosterSpec {
            val size = ImageBounds.screen(viewportWidth, viewportHeight)
            val ratio = if (viewportWidth > 0) size.width.toFloat() / viewportWidth else 1f
            return PosterSpec(size.width, size.height, cornerWidthPx * ratio, cornerHeightPx * ratio)
        }
    }
}

internal class HeroVeils(private val cornerWidth: Float, private val cornerHeight: Float) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private var cachedWidth = -1f
    private var cachedHeight = -1f
    private lateinit var bottom: Shader
    private lateinit var left: Shader
    private lateinit var corner: Shader

    fun draw(canvas: Canvas, width: Float, height: Float) {
        prepare(width, height)
        val bottomTop = height * (1f - BOTTOM_REACH)
        paint.shader = bottom
        canvas.drawRect(0f, bottomTop, width, height, paint)
        paint.shader = left
        canvas.drawRect(0f, 0f, width * LEFT_REACH, height, paint)
        paint.shader = null
    }

    fun drawCorner(canvas: Canvas, width: Float, height: Float) {
        prepare(width, height)
        paint.shader = corner
        canvas.drawRect(width - cornerWidth, 0f, width, cornerHeight, paint)
        paint.shader = null
    }

    private fun prepare(width: Float, height: Float) {
        if (width == cachedWidth && height == cachedHeight) return
        cachedWidth = width
        cachedHeight = height
        val bottomTop = height * (1f - BOTTOM_REACH)
        bottom = LinearGradient(
            0f, bottomTop, 0f, height,
            intArrayOf(veil(0f), veil(0.55f), veil(0.82f)),
            floatArrayOf(0f, (BOTTOM_REACH - 0.26f) / BOTTOM_REACH, 1f),
            Shader.TileMode.CLAMP,
        )
        left = LinearGradient(
            0f, 0f, width * LEFT_REACH, 0f,
            intArrayOf(veil(0.62f), veil(0.28f), veil(0f)),
            floatArrayOf(0f, 0.38f / LEFT_REACH, 1f),
            Shader.TileMode.CLAMP,
        )
        corner = RadialGradient(
            width, 0f, cornerWidth,
            intArrayOf(veil(0.5f), veil(0f)),
            floatArrayOf(0f, 0.7f),
            Shader.TileMode.CLAMP,
        ).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setScale(1f, cornerHeight / cornerWidth, width, 0f) })
        }
    }

    private fun veil(alpha: Float): Int = SygixColors.Veil.copy(alpha = alpha).toArgb()

    companion object {
        const val BOTTOM_REACH = 0.55f
        const val LEFT_REACH = 0.62f
    }
}

internal class VeiledPosterTransformation(private val spec: PosterSpec) : Transformation {

    override val cacheKey: String = "veiled-${spec.width}x${spec.height}-${spec.cornerWidth}x${spec.cornerHeight}"

    override suspend fun transform(input: Bitmap, size: coil.size.Size): Bitmap {
        val output = Bitmap.createBitmap(spec.width, spec.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        drawCover(canvas, input, spec.width, spec.height)
        HeroVeils(spec.cornerWidth, spec.cornerHeight).draw(canvas, spec.width.toFloat(), spec.height.toFloat())
        output.setHasAlpha(false)
        return toHardware(output)
    }
}

internal class GlassBackdropTransformation(private val spec: PosterSpec) : Transformation {

    override val cacheKey: String = "backdrop-${spec.width}x${spec.height}-${spec.cornerWidth}x${spec.cornerHeight}"

    override suspend fun transform(input: Bitmap, size: coil.size.Size): Bitmap {
        val small = spec.scaled(spec.backdrop)
        val veiled = Bitmap.createBitmap(small.width, small.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(veiled)
        drawCover(canvas, input, small.width, small.height)
        HeroVeils(small.cornerWidth, small.cornerHeight).draw(canvas, small.width.toFloat(), small.height.toFloat())
        BoxBlur.apply(veiled, BoxBlur.radiusFor(PosterSpec.BACKDROP_BLUR_PX * small.width / spec.width))
        val output = Bitmap.createBitmap(small.width, small.height, Bitmap.Config.ARGB_8888)
        val saturate = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(PosterSpec.BACKDROP_SATURATION) })
        }
        Canvas(output).drawBitmap(veiled, 0f, 0f, saturate)
        veiled.recycle()
        output.setHasAlpha(false)
        return toHardware(output)
    }
}

internal object BoxBlur {

    private const val PASSES = 3

    fun radiusFor(sigma: Float): Int = max(((Math.sqrt(12.0 * sigma * sigma / PASSES + 1) - 1) / 2).roundToInt(), 1)

    fun apply(bitmap: Bitmap, radius: Int) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val scratch = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        repeat(PASSES) {
            pass(pixels, scratch, width, height, radius, horizontal = true)
            pass(scratch, pixels, width, height, radius, horizontal = false)
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }

    private fun pass(source: IntArray, target: IntArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val window = radius * 2 + 1
        for (line in 0 until lines) {
            var a = 0
            var r = 0
            var g = 0
            var b = 0
            fun at(i: Int): Int {
                val clamped = i.coerceIn(0, length - 1)
                return if (horizontal) source[line * width + clamped] else source[clamped * width + line]
            }
            for (i in -radius..radius) {
                val c = at(i)
                a += c ushr 24
                r += (c shr 16) and 0xFF
                g += (c shr 8) and 0xFF
                b += c and 0xFF
            }
            for (i in 0 until length) {
                val index = if (horizontal) line * width + i else i * width + line
                target[index] = ((a / window) shl 24) or ((r / window) shl 16) or ((g / window) shl 8) or (b / window)
                val outgoing = at(i - radius)
                val incoming = at(i + radius + 1)
                a += (incoming ushr 24) - (outgoing ushr 24)
                r += ((incoming shr 16) and 0xFF) - ((outgoing shr 16) and 0xFF)
                g += ((incoming shr 8) and 0xFF) - ((outgoing shr 8) and 0xFF)
                b += (incoming and 0xFF) - (outgoing and 0xFF)
            }
        }
    }
}

internal class HeroPosterImages(val poster: ImageBitmap, val backdrop: ImageBitmap?)

internal object HeroPosterLoader {

    suspend fun load(context: Context, url: String, spec: PosterSpec): HeroPosterImages? {
        val loader = context.imageLoader
        val poster = loader.execute(request(context, url, PixelSize(spec.width, spec.height), VeiledPosterTransformation(spec)))
            .bitmap(spec.width, spec.height) ?: return null
        val backdropSize = spec.backdrop
        val backdrop = loader.execute(request(context, url, backdropSize, GlassBackdropTransformation(spec)))
            .bitmap(backdropSize.width, backdropSize.height)
        return HeroPosterImages(poster.asImageBitmap(), backdrop?.asImageBitmap())
    }

    private fun request(context: Context, url: String, size: PixelSize, transformation: Transformation): ImageRequest =
        ImageRequest.Builder(context)
            .data(url)
            .size(size.width, size.height)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .transformations(transformation)
            .build()

    private fun coil.request.ImageResult.bitmap(width: Int, height: Int): Bitmap? {
        val drawable = (this as? SuccessResult)?.drawable ?: return null
        return (drawable as? BitmapDrawable)?.bitmap ?: drawable.toBitmap(width, height)
    }
}

private fun drawCover(canvas: Canvas, input: Bitmap, width: Int, height: Int) {
    val scale = max(width.toFloat() / input.width, height.toFloat() / input.height)
    val cropWidth = (width / scale).roundToInt().coerceAtMost(input.width)
    val cropHeight = (height / scale).roundToInt().coerceAtMost(input.height)
    val left = (input.width - cropWidth) / 2
    val top = (input.height - cropHeight) / 2
    canvas.drawBitmap(
        input,
        Rect(left, top, left + cropWidth, top + cropHeight),
        Rect(0, 0, width, height),
        Paint(Paint.FILTER_BITMAP_FLAG),
    )
}

private fun toHardware(bitmap: Bitmap): Bitmap {
    val hardware = runCatching { bitmap.copy(Bitmap.Config.HARDWARE, false) }.getOrNull() ?: return bitmap
    bitmap.recycle()
    return hardware
}
