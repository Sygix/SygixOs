/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

class QrCode private constructor(val size: Int, private val dark: BooleanArray) {

    operator fun get(x: Int, y: Int): Boolean = dark[y * size + x]

    override fun equals(other: Any?): Boolean = other is QrCode && other.size == size && other.dark.contentEquals(dark)

    override fun hashCode(): Int = 31 * size + dark.contentHashCode()

    companion object {
        const val QUIET_ZONE = 4

        fun encode(text: String): QrCode? = runCatching {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to QUIET_ZONE,
            )
            val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, 0, 0, hints)
            val size = matrix.width
            QrCode(size, BooleanArray(size * size) { matrix[it % size, it / size] })
        }.getOrNull()
    }
}
