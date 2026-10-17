/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.AppIconCache
import fr.sygix.sygixos.domain.UpNextContentKey
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.home.LocalAppArtwork
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class SettingsThumbnailTest {

    @get:Rule
    val compose = createComposeRule()

    private val pm: PackageManager = ApplicationProvider.getApplicationContext<Context>().packageManager
    private val bannerId = 0x7f0a0001
    private val icons = AppIconCache { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888) }

    private fun install(packageName: String, banner: Boolean) {
        shadowOf(pm).installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                applicationInfo = ApplicationInfo().apply {
                    this.packageName = packageName
                    if (banner) this.banner = bannerId
                }
            },
        )
        if (banner) {
            shadowOf(pm).addDrawableResolution(
                packageName,
                bannerId,
                ShapeDrawable(RectShape()).apply {
                    intrinsicWidth = 32
                    intrinsicHeight = 18
                },
            )
        }
    }

    private fun show(app: TvApp) {
        compose.setContent {
            CompositionLocalProvider(
                LocalAppArtwork provides AppArtworkSource(pm),
                LocalAppIcons provides icons,
            ) {
                AppThumbnail(app)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `row thumbnail shows the tv banner of the app`() {
        install("com.banner", banner = true)
        show(TvApp("com.banner", "Banner"))
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("app-banner-com.banner"), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("app-icon-com.banner", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `row thumbnail falls back to the centred icon without a banner`() {
        install("com.plain", banner = false)
        show(TvApp("com.plain", "Plain"))
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("app-icon-com.plain"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("app-icon-com.plain", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("app-banner-com.plain", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `removed content thumbnail replaces its placeholder once the image is loaded`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File.createTempFile("thumb", ".png", context.cacheDir)
        file.outputStream().use { Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val row = HiddenContentRow(UpNextContentKey(UpNextContentType.MOVIE, "Film"), "Film", emptyList(), file.toURI().toString(), 0)
        val placeholder = "hidden-content-thumbnail-${row.key.serialized}-placeholder"
        compose.setContent { HiddenContentLine(row, focusEnabled = true, focusRequester = remember { FocusRequester() }, onFocused = {}, onRestore = {}) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag(placeholder), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        file.delete()
    }
}
