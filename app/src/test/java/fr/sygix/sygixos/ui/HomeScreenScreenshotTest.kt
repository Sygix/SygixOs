package fr.sygix.sygixos.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.home.LauncherHome
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w1920dp-h1080dp-xhdpi")
class HomeScreenScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val apps = listOf(
        "Netflix", "Prime Video", "Disney+", "Orange TV", "YouTube", "Canal+", "Jellyfin",
        "Spotify", "Steam Link", "Kodi", "Plex", "Twitch", "Apple TV", "Molotov",
    ).map { TvApp(packageName = "test.$it", label = it) }

    @Test
    fun dockAndGrid() {
        val catalog = Catalog(dock = apps.take(4), grid = apps.drop(4))
        composeRule.setContent {
            SygixOsTheme {
                LauncherHome(catalog, onTogglePin = {})
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/screenshots/home-dock-grid.png")
    }
}
