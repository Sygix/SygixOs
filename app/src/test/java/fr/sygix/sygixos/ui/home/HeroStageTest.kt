package fr.sygix.sygixos.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HeroStageTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `hero shows current item title`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = emptyList()),
                hero = heroStateOf(items = listOf(heroItem("h1", "Titre Héro", progress = 0.5f))),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithText("Titre Héro").assertExists()
        compose.onNodeWithTag("hero-progress").assertExists()
    }

    @Test
    fun `hero without progress hides the progress bar`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = emptyList()),
                hero = heroStateOf(items = listOf(heroItem("h1", "Titre Héro"))),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("hero-progress").assertDoesNotExist()
    }

    @Test
    fun `hero with no items composes without crash`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = emptyList()),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("zone-hero").assertExists()
    }
}
