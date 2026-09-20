package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCatalogTest {

    private val apps = listOf(
        TvApp("com.netflix.mediaclient", "Netflix"),
        TvApp("com.example.zeta", "zeta"),
        TvApp("com.example.alpha", "Alpha"),
    )

    @Test
    fun `ordered alphabetically without pins`() {
        val result = AppCatalog.order(apps, emptyList())
        assertEquals(listOf("com.example.alpha", "com.netflix.mediaclient", "com.example.zeta"), result.map { it.packageName })
    }

    @Test
    fun `pinned apps come first in pin order`() {
        val result = AppCatalog.order(apps, listOf("com.example.zeta", "com.netflix.mediaclient"))
        assertEquals(
            listOf("com.example.zeta", "com.netflix.mediaclient", "com.example.alpha"),
            result.map { it.packageName },
        )
    }

    @Test
    fun `togglePin adds and removes`() {
        assertEquals(listOf("a"), AppCatalog.togglePinned(emptyList(), "a"))
        assertEquals(emptyList<String>(), AppCatalog.togglePinned(listOf("a"), "a"))
    }
}
