/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.graphics.Bitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.data.AppIconCache
import fr.sygix.sygixos.domain.QrCode
import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class SettingsRowCornerTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `row thumbnail sits at the declared margin from the row edges`() {
        val app = TvApp("com.row", "Ligne")
        compose.setContent {
            CompositionLocalProvider(LocalAppIcons provides AppIconCache { Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888) }) {
                HiddenRowLine(
                    row = HiddenRow(app, hidden = false, hiddenAt = null),
                    focusEnabled = false,
                    focusRequester = FocusRequester(),
                    onFocused = {},
                    onToggle = {},
                )
            }
        }
        compose.waitForIdle()
        val density = compose.density.density
        val row = compose.onNodeWithTag("hidden-row-com.row").fetchSemanticsNode().boundsInRoot
        val thumbnail = compose.onNodeWithTag("app-thumbnail-com.row", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val pair = Dimens.Nested.first { it.name == "settings-row-thumbnail" }
        assertEquals(Dimens.SettingsRowHeight.value * density, row.height, 1f)
        assertEquals(pair.margin.value * density, thumbnail.top - row.top, 1f)
        assertEquals(pair.margin.value * density, row.bottom - thumbnail.bottom, 1f)
        assertEquals(Dimens.SettingsRowPadding.value * density, thumbnail.left - row.left, 1f)
    }

    @Test
    fun `qr card corner equals its quiet zone so no module is cut`() {
        val qr = requireNotNull(QrCode.encode("https://github.com/Sygix/SygixOs/releases/tag/v0.0.1"))
        val module = Dimens.QrSize.value / qr.size
        val corner = qrCorner(qr).value
        assertEquals(module * QrCode.QUIET_ZONE, corner, 0.001f)
        for (y in 0 until qr.size) {
            for (x in 0 until qr.size) {
                if (!qr[x, y]) continue
                assertTrue(x * module >= corner - 0.001f && y * module >= corner - 0.001f)
                assertTrue((x + 1) * module <= Dimens.QrSize.value - corner + 0.001f)
                assertTrue((y + 1) * module <= Dimens.QrSize.value - corner + 0.001f)
            }
        }
    }
}
