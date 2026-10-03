/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeTest {

    private val url = "https://github.com/Sygix/SygixOs/releases/tag/v0.0.2-rc.1"

    private fun decode(qr: QrCode, scale: Int = 6): String {
        val side = qr.size * scale
        val pixels = IntArray(side * side) { index ->
            val x = (index % side) / scale
            val y = (index / side) / scale
            if (qr[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(side, side, pixels)))
        return QRCodeReader().decode(bitmap).text
    }

    @Test
    fun `release url is encoded in a decodable code`() {
        val qr = requireNotNull(QrCode.encode(url))
        assertEquals(url, decode(qr))
    }

    @Test
    fun `code has a quiet zone of four modules`() {
        val qr = requireNotNull(QrCode.encode(url))
        val zone = QrCode.QUIET_ZONE
        for (i in 0 until qr.size) {
            for (m in 0 until zone) {
                assertFalse(qr[i, m])
                assertFalse(qr[m, i])
                assertFalse(qr[i, qr.size - 1 - m])
                assertFalse(qr[qr.size - 1 - m, i])
            }
        }
        assertTrue(qr[zone, zone])
    }

    @Test
    fun `a release url fits a version four code`() {
        val qr = requireNotNull(QrCode.encode(url))
        assertEquals(33 + 2 * QrCode.QUIET_ZONE, qr.size)
    }
}
