/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.domain.CheckLine
import fr.sygix.sygixos.domain.InstallDetail
import fr.sygix.sygixos.domain.InstallLine
import fr.sygix.sygixos.domain.QrCode
import fr.sygix.sygixos.domain.UpdateError
import fr.sygix.sygixos.domain.candidate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class UpdateAboutTest {

    @get:Rule
    val compose = createComposeRule()

    private val v2 = candidate("v0.0.2")
    private val notes = QrCode.encode(v2.htmlUrl)
    private val upToDate = AboutUpdateState(checkLine = CheckLine.UpToDate)
    private val available = AboutUpdateState(
        checkLine = CheckLine.Available("0.0.2", prerelease = false),
        installLine = InstallLine(v2, null),
        releaseNotesQr = notes,
        badge = true,
    )
    private var update by mutableStateOf(AboutUpdateState())
    private val checks = mutableListOf<Unit>()
    private val installs = mutableListOf<String>()
    private var toggles = 0

    private fun show(initial: AboutUpdateState = AboutUpdateState()) {
        update = initial
        compose.setContent {
            MaterialTheme {
                SettingsScreen(
                    state = SettingsState(version = "0.0.1", update = update),
                    onToggleSource = {},
                    onToggleHidden = {},
                    onUnhideAll = {},
                    update = UpdateActions(
                        onCheck = { checks += Unit },
                        onInstall = { installs += it },
                        onTogglePrereleases = { toggles++ },
                    ),
                )
            }
        }
        compose.waitForIdle()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-ABOUT").assertIsFocused()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun applyUpdate(state: AboutUpdateState) {
        compose.runOnIdle { update = state }
        compose.waitForIdle()
    }

    private fun focusedTag(): String? = compose.onAllNodes(isFocused()).fetchSemanticsNodes()
        .singleOrNull()?.config?.getOrNull(SemanticsProperties.TestTag)

    private fun detailOf(tag: String): List<String> = compose.onNodeWithTag(tag).fetchSemanticsNode()
        .config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot

    @Test
    fun `right focuses the update check first and down walks the rows then the licenses without wrapping`() {
        show(available)
        press(Key.DirectionRight)
        compose.onNodeWithTag("update-check").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("update-check").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-install").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-prereleases").assertIsFocused()
        compose.waitUntil(10_000) { compose.onAllNodes(SemanticsMatcher("license") { it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("license-row-") == true }).fetchSemanticsNodes().isNotEmpty() }
        press(Key.DirectionDown)
        assertTrue(focusedTag().orEmpty().startsWith("license-row-"))
        var previous: String? = null
        var steps = 0
        while (focusedTag() != previous && steps < 300) {
            previous = focusedTag()
            press(Key.DirectionDown)
            steps++
        }
        assertTrue(previous.orEmpty().startsWith("license-row-"))
        press(Key.DirectionDown)
        assertEquals(previous, focusedTag())
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-ABOUT").assertIsFocused()
    }

    @Test
    fun `without a proposed version down goes from the check to the prereleases`() {
        show(upToDate)
        press(Key.DirectionRight)
        compose.onNodeWithTag("update-check").assertIsFocused()
        compose.onNodeWithTag("update-install").assertDoesNotExist()
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-prereleases").assertIsFocused()
    }

    @Test
    fun `ok on the rows calls the matching actions`() {
        show(available)
        press(Key.DirectionRight)
        press(Key.Enter)
        press(Key.DirectionDown)
        press(Key.Enter)
        press(Key.DirectionDown)
        press(Key.Enter)
        assertEquals(1, checks.size)
        assertEquals(listOf("v0.0.2"), installs)
        assertEquals(1, toggles)
    }

    @Test
    fun `install row appears right under the check without stealing the focus`() {
        show(upToDate)
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-prereleases").assertIsFocused()
        applyUpdate(available)
        compose.onNodeWithTag("update-prereleases").assertIsFocused()
        assertTrue(hasText("Mettre à jour vers 0.0.2").matches(compose.onNodeWithTag("update-install").fetchSemanticsNode()))
        val check = bounds("update-check")
        val install = bounds("update-install")
        val prereleases = bounds("update-prereleases")
        assertTrue(install.top >= check.bottom)
        assertTrue(install.top - check.bottom < 20f)
        assertTrue(prereleases.top >= install.bottom)
        press(Key.DirectionUp)
        press(Key.DirectionUp)
        compose.onNodeWithTag("update-check").assertIsFocused()
        applyUpdate(upToDate)
        applyUpdate(available)
        compose.onNodeWithTag("update-check").assertIsFocused()
    }

    @Test
    fun `focused install row that disappears gives the focus to the check`() {
        show(available)
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-install").assertIsFocused()
        applyUpdate(upToDate)
        compose.onNodeWithTag("update-install").assertDoesNotExist()
        compose.onNodeWithTag("update-check").assertIsFocused()
    }

    @Test
    fun `return from a system screen focuses the install row or else the check`() {
        show(available)
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-prereleases").assertIsFocused()
        applyUpdate(available.copy(systemScreenReturns = 1))
        compose.onNodeWithTag("update-install").assertIsFocused()
        press(Key.DirectionDown)
        applyUpdate(upToDate.copy(systemScreenReturns = 2))
        compose.onNodeWithTag("update-check").assertIsFocused()
    }

    @Test
    fun `release notes code sits right of the install row and is never focused`() {
        show(available)
        val install = bounds("update-install")
        val qr = bounds("update-release-notes-qr")
        assertTrue(qr.left >= install.right)
        assertTrue(qr.top < install.bottom && qr.bottom > install.top)
        assertTrue(bounds("update-release-notes-qr").width >= 239f)
        compose.onNode(hasText("Notes de version") and hasAnyAncestor(hasTestTag("update-release-notes-qr")), useUnmergedTree = true).assertExists()
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-install").assertIsFocused()
        press(Key.DirectionRight)
        compose.onNodeWithTag("update-install").assertIsFocused()
        compose.onNodeWithTag("update-release-notes-qr").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
    }

    @Test
    fun `no release notes code without a valid release page`() {
        show(available.copy(releaseNotesQr = null))
        compose.onNodeWithTag("update-install").assertExists()
        compose.onNodeWithTag("update-release-notes-qr").assertDoesNotExist()
        applyUpdate(upToDate)
        compose.onNodeWithTag("update-release-notes-qr").assertDoesNotExist()
    }

    @Test
    fun `check and install lines show the expected texts`() {
        show()
        fun check(state: AboutUpdateState, vararg texts: String) {
            applyUpdate(state)
            assertEquals(texts.toList(), detailOf("update-check"))
        }
        check(AboutUpdateState(), "Vérifier les mises à jour", "Jamais vérifié")
        check(AboutUpdateState(checkLine = CheckLine.Checking), "Vérifier les mises à jour", "Vérification…")
        check(upToDate, "Vérifier les mises à jour", "SygixOs est à jour")
        check(available, "Vérifier les mises à jour", "Version 0.0.2 disponible")
        check(AboutUpdateState(checkLine = CheckLine.Available("0.0.3-rc.1", prerelease = true)), "Vérifier les mises à jour", "Préversion 0.0.3-rc.1 disponible")
        mapOf(
            UpdateError.NoNetwork to "Pas de connexion à Internet",
            UpdateError.Timeout to "GitHub ne répond pas, réessayez plus tard",
            UpdateError.SecureConnection to "Connexion sécurisée impossible (vérifiez la date et l'heure de la TV)",
            UpdateError.Unavailable(404) to "Releases inaccessibles",
            UpdateError.Unreadable to "Réponse de GitHub illisible",
        ).forEach { (error, text) -> check(AboutUpdateState(checkLine = CheckLine.Failed(error)), "Vérifier les mises à jour", text) }
        applyUpdate(AboutUpdateState(checkLine = CheckLine.Failed(UpdateError.RateLimited(0L))))
        assertTrue(detailOf("update-check")[1].startsWith("Trop de vérifications, réessayez après "))
        compose.onNodeWithTag("update-install").assertDoesNotExist()
        check(available, "Vérifier les mises à jour", "Version 0.0.2 disponible")
        assertEquals(listOf("Mettre à jour vers 0.0.2"), detailOf("update-install"))
        applyUpdate(available.copy(installLine = InstallLine(v2, InstallDetail.Downloading(42))))
        assertEquals(listOf("Mettre à jour vers 0.0.2", "Téléchargement… 42 %"), detailOf("update-install"))
        applyUpdate(available.copy(installLine = InstallLine(v2, InstallDetail.Verifying)))
        assertEquals("Vérification du fichier…", detailOf("update-install")[1])
        applyUpdate(available.copy(installLine = InstallLine(v2, InstallDetail.Installing)))
        assertEquals("Installation…", detailOf("update-install")[1])
        mapOf(
            UpdateError.Corrupt to "Fichier corrompu, réessayez",
            UpdateError.SignatureMismatch to "Signature différente de l'app installée",
            UpdateError.Inconsistent to "Release incohérente",
            UpdateError.Interrupted to "Téléchargement interrompu",
            UpdateError.NoSpace to "Espace insuffisant",
            UpdateError.InstallAborted to "Installation annulée",
            UpdateError.AssetNotFound to "Fichier de la version introuvable",
        ).forEach { (error, text) ->
            applyUpdate(available.copy(installLine = InstallLine(v2, InstallDetail.Failed(error))))
            assertEquals(text, detailOf("update-install")[1])
        }
    }

    @Test
    fun `prerelease row shows the switch state`() {
        show(AboutUpdateState(includePrereleases = false))
        compose.waitForToggle("update-prereleases", on = false)
        applyUpdate(AboutUpdateState(includePrereleases = true))
        compose.waitForToggle("update-prereleases", on = true)
        compose.onNodeWithTag("update-prereleases-switch", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `about category carries the badge only when a version is proposed`() {
        show(available)
        compose.onNode(hasTestTag("update-badge-about") and hasAnyAncestor(hasTestTag("settings-category-ABOUT")), useUnmergedTree = true).assertExists()
        applyUpdate(upToDate)
        compose.onNodeWithTag("update-badge-about", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun `release notes column never pushes the following rows nor covers their text`() {
        show(upToDate)
        val restingHeight = bounds("update-prereleases").height
        val checkHeight = bounds("update-check").height
        val longError = available.copy(installLine = InstallLine(v2, InstallDetail.Failed(UpdateError.SignatureMismatch)))
        listOf(available, longError).forEach { state ->
            applyUpdate(state)
            val check = bounds("update-check")
            val install = bounds("update-install")
            val prereleases = bounds("update-prereleases")
            val qr = bounds("update-release-notes-qr")
            assertEquals(install.top, qr.top, 1f)
            assertTrue(install.top - check.bottom < 20f)
            assertTrue(prereleases.top - install.bottom < 20f)
            assertEquals(restingHeight, prereleases.height, 1f)
            assertEquals(checkHeight, check.height, 1f)
            assertTrue(qr.left >= check.right)
            assertTrue(qr.left >= install.right)
            assertTrue(qr.left >= prereleases.right)
            assertTrue(qr.width >= 239f)
            assertTrue(bounds("about-licenses").top >= qr.bottom)
        }
    }

    @Test
    fun `withdrawn version keeps its notice alone or next to another proposed version`() {
        val rc = candidate("v0.0.3-rc.1")
        show(upToDate.copy(withdrawn = rc))
        compose.onNodeWithTag("update-install").assertDoesNotExist()
        compose.onNodeWithTag("update-release-notes-qr").assertDoesNotExist()
        compose.onNodeWithTag("update-badge-about", useUnmergedTree = true).assertDoesNotExist()
        assertEquals(listOf("Mettre à jour vers 0.0.3-rc.1", "Cette version n'est plus disponible"), detailOf("update-withdrawn"))
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        compose.onNodeWithTag("update-withdrawn").assertIsFocused()
        press(Key.Enter)
        assertTrue(installs.isEmpty())
        applyUpdate(available.copy(withdrawn = rc))
        assertEquals(listOf("Mettre à jour vers 0.0.2"), detailOf("update-install"))
        assertEquals("Cette version n'est plus disponible", detailOf("update-withdrawn")[1])
        assertTrue(bounds("update-withdrawn").top >= bounds("update-install").bottom)
    }
}
