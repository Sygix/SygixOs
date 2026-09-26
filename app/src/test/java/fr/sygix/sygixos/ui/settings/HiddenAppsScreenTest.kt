/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
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
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
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

    private fun setScreen(apps: List<TvApp>) {
        compose.setContent {
            MaterialTheme {
                HiddenAppsScreen(
                    hiddenApps = apps,
                    onUnhide = { unhid.add(it) },
                    onUnhideAll = { unhideAll++ },
                )
            }
        }
        compose.waitForIdle()
    }

    // Écran branché sur une liste mutable : réactiver retire réellement la ligne.
    private fun setLiveScreen(initial: List<TvApp>) {
        val hidden = mutableStateOf(initial)
        compose.setContent {
            MaterialTheme {
                HiddenAppsScreen(
                    hiddenApps = hidden.value,
                    onUnhide = { pkg -> hidden.value = hidden.value.filterNot { it.packageName == pkg } },
                    onUnhideAll = { hidden.value = emptyList() },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `empty state shows only a centered message with no unhide-all button`() {
        setScreen(emptyList())
        compose.onNodeWithTag("hidden-empty").assertIsDisplayed()
        compose.onNodeWithTag("unhide-all").assertDoesNotExist()
    }

    @Test
    fun `rows with switches and unhide-all are rendered when apps are hidden`() {
        setScreen(listOf(TvApp("com.a", "A"), TvApp("com.b", "B")))
        compose.onNodeWithTag("hidden-row-com.a").assertIsDisplayed()
        compose.onNodeWithTag("hidden-switch-com.b", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("unhide-all").assertExists()
    }

    @Test
    fun `ok on first row unhides that app`() {
        setScreen(listOf(TvApp("com.a", "A"), TvApp("com.b", "B")))
        press(Key.Enter)
        assertEquals(listOf("com.a"), unhid)
    }

    @Test
    fun `unhiding a row moves the focus to its neighbour, not back to the top`() {
        setLiveScreen(listOf(TvApp("com.a", "A"), TvApp("com.b", "B"), TvApp("com.c", "C")))
        press(Key.DirectionDown)
        compose.onNodeWithTag("hidden-row-com.b").assertIsFocused()
        press(Key.Enter)
        compose.onNodeWithTag("hidden-row-com.b").assertDoesNotExist()
        compose.onNodeWithTag("hidden-row-com.c").assertIsFocused()
        // Dernière ligne réactivée : le focus remonte sur la voisine du dessus.
        press(Key.Enter)
        compose.onNodeWithTag("hidden-row-com.a").assertIsFocused()
        // Plus rien à réactiver : l'état vide garde le focus dans le sous-écran.
        press(Key.Enter)
        compose.onNodeWithTag("hidden-empty").assertIsDisplayed()
        compose.onNodeWithTag("hidden-empty-focus").assertIsFocused()
    }

    @Test
    fun `hidden row is one accessible switch announced as hidden`() {
        setScreen(listOf(TvApp("com.a", "A")))
        val row = compose.onNodeWithTag("hidden-row-com.a")
        row.assertIsOn()
        row.assert(hasClickAction())
        row.assert(hasStateDescription("Cachée"))
        // Le libellé est fusionné dans la ligne, le switch visuel n'a pas de rôle propre.
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Text, listOf(androidx.compose.ui.text.AnnotatedString("A"))))
        compose.onNodeWithTag("hidden-switch-com.a", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
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
        setScreen(listOf(TvApp("com.iconed", "Iconed")))
        compose.onNodeWithTag("app-icon-com.iconed", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `unknown package keeps the placeholder without icon`() {
        setScreen(listOf(TvApp("com.missing", "Missing")))
        compose.onNodeWithTag("app-icon-com.missing", useUnmergedTree = true).assertDoesNotExist()
    }
}
