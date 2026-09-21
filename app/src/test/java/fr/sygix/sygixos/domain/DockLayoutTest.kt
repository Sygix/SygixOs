package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DockLayoutTest {

    // grille : 864 dp de large, 5 colonnes, 16 dp d'espacement => tuile 160 dp
    private val available = 864f
    private val spacing = 16f

    @Test
    fun `up to five tiles keep the grid tile width`() {
        assertEquals(160f, DockLayout.tileWidth(available, 1, spacing, 5))
        assertEquals(160f, DockLayout.tileWidth(available, 5, spacing, 5))
    }

    @Test
    fun `more than five tiles shrink to fit`() {
        assertEquals((864f - 16f * 7) / 8, DockLayout.tileWidth(available, 8, spacing, 5))
    }

    @Test
    fun `dock width wraps its tiles`() {
        assertEquals(160f * 2 + 16f + 28f, DockLayout.dockWidth(160f, 2, spacing, 14f))
        assertEquals(0f, DockLayout.tileWidth(available, 0, spacing, 5))
    }
}
