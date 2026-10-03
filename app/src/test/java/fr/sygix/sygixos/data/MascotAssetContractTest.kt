/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MascotAssetContractTest {

    private class Chunk(val id: String, val body: ByteArray)

    private val bytes: ByteArray = ApplicationProvider.getApplicationContext<android.content.Context>()
        .resources.openRawResource(R.raw.splash_mascot).use { it.readBytes() }

    private fun ByteArray.uint(offset: Int, length: Int): Int =
        (0 until length).fold(0) { acc, i -> acc or ((this[offset + i].toInt() and 0xFF) shl (8 * i)) }

    private fun ByteArray.tag(offset: Int): String = String(this, offset, 4, Charsets.US_ASCII)

    private fun chunks(): List<Chunk> {
        assertEquals("format : en-tête RIFF", "RIFF", bytes.tag(0))
        assertEquals("format : signature WEBP", "WEBP", bytes.tag(8))
        val result = mutableListOf<Chunk>()
        var offset = 12
        while (offset + 8 <= bytes.size) {
            val size = bytes.uint(offset + 4, 4)
            assertTrue("format : chunk ${bytes.tag(offset)} tronqué", offset + 8 + size <= bytes.size)
            result += Chunk(bytes.tag(offset), bytes.copyOfRange(offset + 8, offset + 8 + size))
            offset += 8 + size + (size and 1)
        }
        return result
    }

    @Test
    fun `mascot animation file respects its contract`() {
        assertTrue("poids : ${bytes.size} octets > 1 048 576", bytes.size <= 1_048_576)
        val chunks = chunks()
        val header = chunks.firstOrNull()
        assertEquals("format : premier chunk VP8X (WebP étendu)", "VP8X", header?.id)
        val flags = header!!.body[0].toInt()
        assertTrue("format : drapeau animation absent de VP8X", flags and 0x02 != 0)
        assertTrue("canal alpha : drapeau absent de VP8X", flags and 0x10 != 0)
        assertEquals("canevas : largeur", 360, header.body.uint(4, 3) + 1)
        assertEquals("canevas : hauteur", 360, header.body.uint(7, 3) + 1)
        val anim = chunks.firstOrNull { it.id == "ANIM" }
        assertTrue("format : chunk ANIM absent", anim != null)
        assertEquals("répétitions : boucle infinie attendue (0)", 0, anim!!.body.uint(4, 2))
        val durations = chunks.filter { it.id == "ANMF" }.map { it.body.uint(12, 3) }
        assertEquals("images : nombre d'images ANMF", 120, durations.size)
        val wrong = durations.withIndex().filter { it.value != 16 && it.value != 17 }
        assertTrue("durée des images : ${wrong.map { "#${it.index}=${it.value} ms" }} hors de {16, 17} ms", wrong.isEmpty())
        val total = durations.sum()
        assertTrue("durée de la boucle : $total ms hors de [1 997, 2 003] ms", total in 1_997..2_003)
    }
}
