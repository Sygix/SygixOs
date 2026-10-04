/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import java.io.File
import java.util.zip.ZipFile

object DexMetadata {

    const val PROFILE_ENTRY = "primary.prof"
    const val METADATA_ENTRY = "primary.profm"
    private val Expected = setOf(PROFILE_ENTRY, METADATA_ENTRY)

    fun isValid(file: File): Boolean = runCatching {
        ZipFile(file).use { zip ->
            val entries = zip.entries().toList()
            entries.size == Expected.size &&
                entries.map { it.name }.toSet() == Expected &&
                entries.all { !it.isDirectory && it.size > 0 }
        }
    }.getOrDefault(false)
}
