/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

data class ReleaseVersion(val name: String, val versionCode: Long, val suffixed: Boolean) {

    companion object {
        private val Pattern = Regex("""v(\d{1,2})\.(\d{1,3})\.(\d{1,3})(?:-(alpha|beta|rc)\.?(\d{1,2}))?""")
        private const val MAX_MAJOR = 20
        private const val MAX_STAGE_NUMBER = 29
        private const val FINAL_SUFFIX = 99
        private val StageBase = mapOf("alpha" to 0, "beta" to 30, "rc" to 60)

        fun parse(tag: String): ReleaseVersion? {
            val match = Pattern.matchEntire(tag) ?: return null
            val (major, minor, patch, stage, number) = match.destructured
            if (major.toInt() > MAX_MAJOR) return null
            val suffix = if (stage.isEmpty()) {
                FINAL_SUFFIX
            } else {
                if (number.toInt() > MAX_STAGE_NUMBER) return null
                StageBase.getValue(stage) + number.toInt()
            }
            val code = major.toLong() * 100_000_000 + minor.toLong() * 100_000 + patch.toLong() * 100 + suffix
            return ReleaseVersion(name = tag.removePrefix("v"), versionCode = code, suffixed = stage.isNotEmpty())
        }
    }
}
