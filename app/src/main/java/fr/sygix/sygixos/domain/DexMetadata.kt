/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

object DexMetadata {

    const val PROFILE_ENTRY = "primary.prof"
    const val METADATA_ENTRY = "primary.profm"
    private val Allowed = setOf(PROFILE_ENTRY, METADATA_ENTRY)

    fun isValid(bytes: ByteArray): Boolean = runCatching {
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory || entry.name !in Allowed || entry.name in names) return false
                if (zip.readBytes().isEmpty()) return false
                names += entry.name
            }
        }
        PROFILE_ENTRY in names
    }.getOrDefault(false)
}
