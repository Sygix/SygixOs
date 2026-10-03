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
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import android.graphics.Bitmap
import fr.sygix.sygixos.data.AppIconCache
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
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
    private val harnesses = mutableListOf<SettingsHarness>()

    @After
    fun clearViewModels() = harnesses.forEach { it.clear() }

    private fun harness(counts: Flow<Map<String, Int>> = flowOf(emptyMap())) =
        SettingsHarness(counts).also { harnesses += it }

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
        counts: Map<String, Int>? = emptyMap(),
        icons: AppIconCache? = null,
        onBack: () -> Unit = {},
    ) {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalAppIcons provides icons) {
                    SettingsScreen(
                        state = SettingsState(
                            sources = sources,
                            hiddenRows = hidden.map { HiddenRow(app = it, hidden = true, hiddenAt = null) },
                            version = "0.1.0",
                        ),
                        counts = mutableStateOf(counts),
                        onToggleSource = { toggled.add(it) },
                        onToggleHidden = {},
                        onUnhideAll = {},
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
    fun `program counts use correct plurals`() {
        setState(
            sources = sourceRows() + SourceRow(app = TvApp("com.c", "C"), enabled = true),
            counts = mapOf("com.a" to 1, "com.b" to 2),
        )
        compose.onNodeWithText("1 programme publié", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("2 programmes publiés", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Aucun programme publié", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `sources show no program while the first count has not arrived`() {
        setState(sources = sourceRows(), counts = null)
        compose.onNodeWithTag("source-row-com.a").assertExists()
        assertEquals(2, compose.onAllNodes(hasText("Aucun programme publié"), useUnmergedTree = true).fetchSemanticsNodes().size)
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
    fun `right on hidden category focuses the first row, not unhide-all, and up reaches unhide-all`() {
        setState(hidden = listOf(TvApp("com.h1", "H1"), TvApp("com.h2", "H2")))
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.h1").assertIsFocused()
        compose.onNode(hasTestTag("hidden-row-com.h1") and hasAnyAncestor(hasTestTag("hidden-pane"))).assertIsDisplayed()
        press(Key.DirectionUp)
        compose.onNodeWithTag("unhide-all").assertIsFocused()
    }

    @Test
    fun `right on sources still focuses the first element of the pane`() {
        setState(sources = sourceRows(), hidden = listOf(TvApp("com.h", "H")))
        press(Key.DirectionRight)
        compose.onNodeWithTag("source-row-com.a").assertIsFocused()
    }

    @Test
    fun `right on an empty hidden category keeps the category focused and back closes`() {
        var backs = 0
        setState(onBack = { backs++ })
        press(Key.DirectionDown)
        compose.onNodeWithTag("hidden-empty").assertExists()
        press(Key.DirectionRight)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.Back)
        assertEquals(1, backs)
    }

    @Test
    fun `a list emptied by unhide-all shows the empty state after leaving and coming back`() {
        val harness = harness()
        harness.install("Alpha", "Keep")
        harness.hideInOrder("Alpha")
        compose.showSettings(harness)
        press(Key.DirectionDown)
        waitForTag("hidden-row-com.alpha")
        press(Key.DirectionRight)
        press(Key.DirectionUp)
        compose.onNodeWithTag("unhide-all").assertIsFocused()
        press(Key.Enter)
        compose.waitForToggle("hidden-row-com.alpha", on = false)
        assertEquals(emptySet<String>(), harness.hiddenNow())
        press(Key.DirectionLeft)
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        compose.onNodeWithTag("hidden-empty").assertExists()
        compose.onNodeWithTag("unhide-all").assertDoesNotExist()
        press(Key.Back)
        assertEquals(1, harness.backs)
    }

    @Test
    fun `an unhidden row stays until the category is left, including after closing the settings`() {
        val harness = harness()
        harness.install("Alpha", "Beta", "Keep")
        harness.hideInOrder("Alpha", "Beta")
        compose.showSettings(harness)
        press(Key.DirectionDown)
        waitForTag("hidden-row-com.alpha")
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsFocused()
        press(Key.Enter)
        compose.waitForToggle("hidden-row-com.beta", on = false)
        assertEquals(setOf("com.alpha"), harness.hiddenNow())

        press(Key.DirectionLeft)
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.beta").assertExists().assertIsOff()

        press(Key.DirectionLeft)
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        compose.onNodeWithTag("hidden-row-com.beta").assertDoesNotExist()
        compose.onNodeWithTag("hidden-row-com.alpha").assertExists()

        press(Key.DirectionRight)
        press(Key.Enter)
        compose.waitForToggle("hidden-row-com.alpha", on = false)
        assertEquals(emptySet<String>(), harness.hiddenNow())
        press(Key.Back)
        compose.onNodeWithTag("settings-screen").assertDoesNotExist()
        compose.runOnIdle { harness.openSettings() }
        compose.waitForIdle()
        press(Key.DirectionDown)
        compose.onNodeWithTag("hidden-empty").assertExists()
        compose.onNodeWithTag("hidden-row-com.alpha").assertDoesNotExist()
    }

    @Test
    fun `sources keep their order and focus while counts update, and show the new count`() {
        val counts = MutableSharedFlow<Map<String, Int>>(replay = 1)
        counts.tryEmit(mapOf("com.a" to 12, "com.c" to 3))
        val harness = harness(counts)
        harness.install("A", "B", "C", "D")
        compose.showSettings(harness)
        waitForTag("source-row-com.a")
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        compose.onNodeWithTag("source-row-com.c").assertIsFocused()
        assertEquals(listOf("com.a", "com.c", "com.b", "com.d"), sourceOrder())

        counts.tryEmit(mapOf("com.a" to 12, "com.c" to 20))
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("20 programmes publiés"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("source-row-com.c").assertIsFocused()
        assertEquals(listOf("com.a", "com.c", "com.b", "com.d"), sourceOrder())

        counts.tryEmit(mapOf("com.a" to 12, "com.c" to 3, "com.d" to 40))
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("40 programmes publiés"), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("source-row-com.c").assertIsFocused()
        assertEquals(listOf("com.a", "com.c", "com.b", "com.d"), sourceOrder())
        press(Key.DirectionLeft)
        press(Key.DirectionRight)
        compose.onNodeWithTag("source-row-com.a").assertIsFocused()
        assertEquals(listOf("com.a", "com.c", "com.b", "com.d"), sourceOrder())
    }

    @Test
    fun `a late first count moves the focused row to the top and keeps its focus`() {
        val counts = MutableSharedFlow<Map<String, Int>>(replay = 1)
        val harness = harness(counts)
        harness.install("Alpha", "Beta", "Zeta")
        compose.showSettings(harness)
        waitForTag("source-row-com.zeta")
        assertEquals(listOf("com.alpha", "com.beta", "com.zeta"), sourceOrder())
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("source-row-com.zeta").assertIsFocused()

        counts.tryEmit(mapOf("com.zeta" to 9))
        compose.waitUntil(5_000) { sourceOrder().first() == "com.zeta" }
        compose.waitForIdle()
        compose.onNodeWithTag("source-row-com.zeta").assertIsFocused()
        assertEquals(listOf("com.zeta", "com.alpha", "com.beta"), sourceOrder())
    }

    @Test
    fun `a late first count brings the focused row of a long list to the top and keeps it on screen`() {
        val counts = MutableSharedFlow<Map<String, Int>>(replay = 1)
        val harness = harness(counts)
        val labels = (1..20).map { "App%02d".format(it) } + "Zeta"
        harness.install(*labels.toTypedArray())
        compose.showSettings(harness)
        waitForTag("source-row-com.app01")
        press(Key.DirectionRight)
        repeat(20) { press(Key.DirectionDown) }
        compose.onNodeWithTag("source-row-com.zeta").assertIsFocused().assertIsDisplayed()
        compose.onNodeWithTag("source-row-com.app01").assertIsNotDisplayed()

        counts.tryEmit(mapOf("com.zeta" to 9))
        compose.waitUntil(5_000) { sourceOrder().first() == "com.zeta" }
        compose.waitForIdle()
        compose.onNodeWithTag("source-row-com.zeta").assertIsFocused().assertIsDisplayed()
        assertEquals("com.zeta", sourceOrder().first())
    }

    private fun waitForTag(tag: String) = compose.waitUntil(5_000) {
        compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }

    private fun sourceOrder(): List<String> = compose.onAllNodes(sourceRow).fetchSemanticsNodes()
        .sortedBy { it.boundsInRoot.top }
        .map { it.config[SemanticsProperties.TestTag].removePrefix("source-row-") }

    private val sourceRow = SemanticsMatcher("source row") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("source-row-") == true
    }

    @Test
    fun `right on about focuses the update check and down reaches the licenses`() {
        setState()
        openAboutAndWaitForLicenses()
        press(Key.DirectionRight)
        compose.onNodeWithTag("update-check").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-prereleases").assertIsFocused()
        press(Key.DirectionDown)
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
