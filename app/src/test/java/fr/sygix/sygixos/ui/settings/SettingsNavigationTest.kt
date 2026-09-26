/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import android.graphics.Bitmap
import fr.sygix.sygixos.data.AppIconCache
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class SettingsNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private val toggled = mutableListOf<String>()
    private val unhid = mutableListOf<String>()
    private var unhideAll = 0

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun setState(
        sources: List<SourceRow> = emptyList(),
        hidden: List<TvApp> = emptyList(),
        counts: Map<String, Int> = emptyMap(),
        icons: AppIconCache? = null,
        onBack: () -> Unit = {},
    ) {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalAppIcons provides icons) {
                    SettingsScreen(
                        state = SettingsState(sources = sources, hiddenApps = hidden, version = "0.1.0"),
                        counts = mutableStateOf(counts),
                        onToggleSource = { toggled.add(it) },
                        onUnhide = { unhid.add(it) },
                        onUnhideAll = { unhideAll++ },
                        onBack = onBack,
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun sourceRows() = listOf(
        SourceRow(app = TvApp("com.a", "A"), enabled = true),
        SourceRow(app = TvApp("com.b", "B"), enabled = false),
    )

    private fun longSourceRows() = (1..30).map { SourceRow(app = TvApp("com.s$it", "S$it"), enabled = true) }

    private val licenseRow = SemanticsMatcher("license row") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("license-row-") == true
    }

    private fun openAboutAndWaitForLicenses() {
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.waitUntil(10_000) { compose.onAllNodes(licenseRow).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun `first category has focus at opening and right pane shows its content`() {
        setState()
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
        compose.onNodeWithTag("settings-sources").assertExists()
    }

    @Test
    fun `up and down in left pane switch the active category`() {
        setState()
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-ABOUT").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
    }

    @Test
    fun `category list stops at its edges instead of looping`() {
        setState()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
        repeat(3) { press(Key.DirectionDown) }
        compose.onNodeWithTag("settings-category-ABOUT").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-ABOUT").assertIsFocused()
    }

    @Test
    fun `source row is one accessible switch carrying label, state and action`() {
        setState(sources = sourceRows())
        val row = compose.onNodeWithTag("source-row-com.a")
        row.assertIsOn()
        row.assert(hasClickAction())
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, androidx.compose.ui.semantics.Role.Switch))
        compose.onNodeWithTag("source-row-com.b").assertIsOff()
        compose.onNodeWithTag("source-switch-com.a", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
    }

    @Test
    fun `program counts and hidden counts use correct plurals`() {
        setState(
            sources = sourceRows() + SourceRow(app = TvApp("com.c", "C"), enabled = true),
            hidden = listOf(TvApp("com.h1", "H1"), TvApp("com.h2", "H2")),
            counts = mapOf("com.a" to 1, "com.b" to 2),
        )
        compose.onNodeWithText("1 programme publié", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("2 programmes publiés", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Aucun programme publié", useUnmergedTree = true).assertExists()
        press(Key.DirectionDown)
        compose.onNodeWithText("2 applications cachées", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `an app icon is loaded once even when its row leaves and re-enters the list`() {
        val loads = mutableMapOf<String, Int>()
        val icons = AppIconCache { pkg ->
            loads[pkg] = (loads[pkg] ?: 0) + 1
            Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        }
        setState(sources = longSourceRows(), icons = icons)
        compose.onNodeWithTag("app-icon-com.s1", useUnmergedTree = true).assertExists()
        press(Key.DirectionRight)
        repeat(12) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-icon-com.s1", useUnmergedTree = true).assertDoesNotExist()
        press(Key.DirectionLeft)
        press(Key.DirectionRight)
        compose.onNodeWithTag("app-icon-com.s1", useUnmergedTree = true).assertExists()
        assertEquals(1, loads["com.s1"])
    }

    @Test
    fun `right moves focus to content and left returns it to categories`() {
        setState(sources = sourceRows())
        press(Key.DirectionRight)
        compose.onNodeWithTag("source-row-com.a").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
    }

    @Test
    fun `ok on a source row toggles it`() {
        setState(sources = sourceRows())
        press(Key.DirectionRight)
        press(Key.Enter)
        assertEquals(listOf("com.a"), toggled)
    }

    @Test
    fun `right on hidden category focuses the entry and ok opens the sub screen`() {
        setState(hidden = listOf(TvApp("com.h", "H")))
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        // Droite ne fait que focaliser le volet droit : le sous-écran reste fermé.
        compose.onNodeWithTag("open-hidden").assertIsFocused()
        compose.onNodeWithTag("hidden-apps-screen").assertDoesNotExist()
        // Seule la validation (OK) ouvre le sous-écran ; Retour revient sur la catégorie.
        press(Key.Enter)
        compose.onNodeWithTag("hidden-apps-screen").assertExists()
        press(Key.Back)
        compose.onNodeWithTag("hidden-apps-screen").assertDoesNotExist()
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
    }

    @Test
    fun `empty hidden list still focuses the entry and ok shows the empty sub screen`() {
        var backs = 0
        setState(onBack = { backs++ })
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        compose.onNodeWithTag("open-hidden").assertIsFocused()
        press(Key.Enter)
        // État vide navigable : le sous-écran reste ouvert, Retour fonctionne.
        compose.onNodeWithTag("hidden-apps-screen").assertExists()
        compose.onNodeWithTag("hidden-empty").assertExists()
        press(Key.Back)
        compose.onNodeWithTag("hidden-apps-screen").assertDoesNotExist()
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.Back)
        assertEquals(1, backs)
    }

    @Test
    fun `emptying the hidden list while sub-screen is open keeps it navigable`() {
        val hidden = mutableStateOf(listOf(TvApp("com.h", "H")))
        var backs = 0
        compose.setContent {
            MaterialTheme {
                SettingsScreen(
                    state = SettingsState(sources = emptyList(), hiddenApps = hidden.value, version = "0.1.0"),
                    onToggleSource = { toggled.add(it) },
                    onUnhide = { pkg -> hidden.value = hidden.value.filterNot { it.packageName == pkg } },
                    onUnhideAll = { hidden.value = emptyList() },
                    onBack = { backs++ },
                )
            }
        }
        compose.waitForIdle()
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        press(Key.Enter)
        compose.onNodeWithTag("hidden-apps-screen").assertExists()
        press(Key.Enter)
        // La liste est vidée : le sous-écran reste ouvert sur son état vide (pas d'auto-fermeture).
        compose.onNodeWithTag("hidden-empty").assertExists()
        compose.onNodeWithTag("hidden-apps-screen").assertExists()
        press(Key.Back)
        compose.onNodeWithTag("hidden-apps-screen").assertDoesNotExist()
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.Back)
        assertEquals(1, backs)
        compose.onNodeWithTag("settings-screen").assertExists()
    }

    @Test
    fun `right on about focuses the first license and down reaches the next one`() {
        setState()
        openAboutAndWaitForLicenses()
        press(Key.DirectionRight)
        compose.onAllNodes(licenseRow)[0].assertIsFocused()
        press(Key.DirectionDown)
        compose.onAllNodes(licenseRow)[1].assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-ABOUT").assertIsFocused()
    }

    @Test
    fun `long sources list scrolls with down and left returns to the category`() {
        setState(sources = longSourceRows())
        press(Key.DirectionRight)
        compose.onNodeWithTag("source-row-com.s1").assertIsFocused()
        repeat(10) { press(Key.DirectionDown) }
        compose.onNodeWithTag("source-row-com.s11").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
    }

    @Test
    fun `re-entering a scrolled sources list focuses its first row and back still works`() {
        var backs = 0
        setState(sources = longSourceRows(), onBack = { backs++ })
        press(Key.DirectionRight)
        repeat(12) { press(Key.DirectionDown) }
        compose.onNodeWithTag("source-row-com.s13").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
        press(Key.DirectionRight)
        compose.onNodeWithTag("source-row-com.s1").assertIsFocused()
        press(Key.Back)
        assertEquals(1, backs)
    }

    @Test
    fun `ok on sources with an empty list keeps the category focused and back still works`() {
        var backs = 0
        setState(onBack = { backs++ })
        press(Key.Enter)
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.Back)
        assertEquals(1, backs)
    }

    @Test
    fun `about category shows version and licenses`() {
        setState()
        openAboutAndWaitForLicenses()
        compose.onNodeWithTag("about-version").assertExists()
        compose.onNodeWithTag("about-licenses").assertExists()
    }
}
