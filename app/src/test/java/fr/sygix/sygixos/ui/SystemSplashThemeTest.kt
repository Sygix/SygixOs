/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SystemSplashThemeTest {

    @get:Rule
    val compose = createComposeRule()

    private val themed = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.Theme_SygixOs)

    private fun resolve(attr: Int): TypedValue {
        val value = TypedValue()
        assertTrue("attribut absent du thème : $attr", themed.theme.resolveAttribute(attr, value, true))
        return value
    }

    private fun color(attr: Int): Int {
        val value = resolve(attr)
        assertTrue(value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT)
        return value.data
    }

    @Test
    fun `system splash background is the splash screen black`() {
        var background = 0
        compose.setContent { SygixOsTheme { background = MaterialTheme.colorScheme.background.toArgb() } }
        compose.waitForIdle()
        val splash = color(android.R.attr.windowSplashScreenBackground)
        assertEquals(background, splash)
        assertEquals(color(android.R.attr.windowBackground), splash)
    }

    @Test
    fun `system splash icon draws no visible pixel`() {
        val icon = checkNotNull(themed.getDrawable(resolve(android.R.attr.windowSplashScreenAnimatedIcon).resourceId))
        val size = 432
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, size, size)
        icon.draw(Canvas(bitmap))
        val pixels = IntArray(size * size).also { bitmap.getPixels(it, 0, size, 0, 0, size, size) }
        assertTrue(pixels.all { it ushr 24 == 0 })
    }

    @Test
    fun `system splash has no icon background`() {
        val value = TypedValue()
        val resolved = themed.theme.resolveAttribute(android.R.attr.windowSplashScreenIconBackgroundColor, value, true)
        assertTrue(!resolved || value.type == TypedValue.TYPE_NULL || value.data ushr 24 == 0)
    }
}
