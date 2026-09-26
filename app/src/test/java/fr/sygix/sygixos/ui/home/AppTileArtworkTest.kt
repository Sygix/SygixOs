/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.data.AppArtwork
import fr.sygix.sygixos.data.AppArtworkSource
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class AppTileArtworkTest {

    @get:Rule
    val compose = createComposeRule()

    private val pm: PackageManager = ApplicationProvider.getApplicationContext<Context>().packageManager

    private fun installWithIcon(packageName: String) {
        shadowOf(pm).installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
            },
        )
        shadowOf(pm).setApplicationIcon(
            packageName,
            ShapeDrawable(RectShape()).apply {
                intrinsicWidth = 8
                intrinsicHeight = 8
            },
        )
    }

    @Test
    fun `artwork follows the app when a tile slot is reused for another app`() {
        installWithIcon("com.a")
        installWithIcon("com.b")
        val source = AppArtworkSource(pm)
        val alpha = app("com.a", "Alpha")
        val beta = app("com.b", "Beta")
        source.preload(listOf(alpha, beta))
        assertNotNull(source.cached(alpha))
        assertNotNull(source.cached(beta))
        var shown by mutableStateOf(alpha)
        var artwork: AppArtwork? = null
        compose.setContent {
            CompositionLocalProvider(LocalAppArtwork provides source) {
                artwork = rememberAppArtwork(shown).value
            }
        }
        compose.waitForIdle()
        assertSame(source.cached(alpha), artwork)

        shown = beta
        compose.waitForIdle()
        assertSame(source.cached(beta), artwork)
    }
}
