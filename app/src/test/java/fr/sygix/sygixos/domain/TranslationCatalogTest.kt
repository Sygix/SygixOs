/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class TranslationCatalogTest {
    private val res = File("src/main/res")
    private val specifier = Regex("%(\\d+\\$)?[sd%]")

    private fun catalog(dir: String): Map<String, List<String>> =
        File(res, dir).listFiles { file -> file.name.endsWith("strings.xml") }.orEmpty().flatMap { file ->
            val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
            val nodes = root.childNodes
            (0 until nodes.length).mapNotNull { nodes.item(it) as? Element }.map { element ->
                val texts = if (element.tagName == "plurals") {
                    val items = element.getElementsByTagName("item")
                    (0 until items.length).map { items.item(it).textContent }
                } else {
                    listOf(element.textContent)
                }
                "${element.tagName}:${element.getAttribute("name")}" to texts
            }
        }.toMap()

    private fun directory(language: AppLanguage) = if (language == AppLanguage.Fallback) "values" else "values-${language.tag}"

    @Test
    fun `every language has every string with the same placeholders`() {
        val reference = catalog("values")
        assertTrue(reference.size > 300)
        AppLanguage.entries.filter { it != AppLanguage.Fallback }.forEach { language ->
            val translated = catalog(directory(language))
            assertEquals("keys of ${language.tag}", reference.keys, translated.keys)
            reference.forEach { (key, texts) ->
                val expected = specifier.findAll(texts.last()).map { it.value }.sorted().toList()
                translated.getValue(key).forEach { text ->
                    if (!(key.startsWith("plurals:") && text.none { it == '%' })) {
                        assertEquals("placeholders of $key in ${language.tag}", expected, specifier.findAll(text).map { it.value }.sorted().toList())
                    }
                }
            }
        }
    }

    @Test
    fun `every translated string is filled in`() {
        AppLanguage.entries.forEach { language ->
            catalog(directory(language)).forEach { (key, texts) ->
                texts.forEach { assertTrue("$key in ${language.tag}", it.isNotBlank()) }
            }
        }
    }
}
