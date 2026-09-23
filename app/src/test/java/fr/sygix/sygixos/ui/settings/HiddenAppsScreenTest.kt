/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows

@RunWith(AndroidJUnit4::class)
class HiddenAppsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val unhid = mutableListOf<String>()
    private var unhideAll = 0

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    @Test
    fun `empty state shows only a centered message with no unhide-all button`() {
        compose.setContent {
            MaterialTheme {
                HiddenAppsScreen(
                    hiddenApps = emptyList(),
                    onUnhide = { unhid.add(it) },
                    onUnhideAll = { unhideAll++ },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("hidden-empty").assertIsDisplayed()
        compose.onNodeWithTag("unhide-all").assertDoesNotExist()
    }

    @Test
    fun `rows with switches and unhide-all are rendered when apps are hidden`() {
        compose.setContent {
            MaterialTheme {
                HiddenAppsScreen(
                    hiddenApps = listOf(TvApp("com.a", "A"), TvApp("com.b", "B")),
                    onUnhide = { unhid.add(it) },
                    onUnhideAll = { unhideAll++ },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("hidden-row-com.a").assertIsDisplayed()
        compose.onNodeWithTag("hidden-switch-com.b").assertExists()
        compose.onNodeWithTag("unhide-all").assertExists()
    }

    @Test
    fun `ok on first row unhides that app`() {
        compose.setContent {
            MaterialTheme {
                HiddenAppsScreen(
                    hiddenApps = listOf(TvApp("com.a", "A"), TvApp("com.b", "B")),
                    onUnhide = { unhid.add(it) },
                    onUnhideAll = { unhideAll++ },
                )
            }
        }
        compose.waitForIdle()
        press(Key.Enter)
        assertEquals(listOf("com.a"), unhid)
    }

    @Test
    fun `hidden rows load the real icon from PackageManager`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Shadows.shadowOf(context.packageManager).setApplicationIcon(
            "com.iconed",
            BitmapDrawable(
                context.resources,
                Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888),
            ),
        )
        compose.setContent {
            MaterialTheme {
                HiddenAppsScreen(
                    hiddenApps = listOf(TvApp("com.iconed", "Iconed")),
                    onUnhide = { unhid.add(it) },
                    onUnhideAll = { unhideAll++ },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app-icon-com.iconed").assertExists()
    }

    @Test
    fun `unknown package keeps the placeholder without icon`() {
        compose.setContent {
            MaterialTheme {
                HiddenAppsScreen(
                    hiddenApps = listOf(TvApp("com.missing", "Missing")),
                    onUnhide = { unhid.add(it) },
                    onUnhideAll = { unhideAll++ },
                )
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app-icon-com.missing").assertDoesNotExist()
    }
}
