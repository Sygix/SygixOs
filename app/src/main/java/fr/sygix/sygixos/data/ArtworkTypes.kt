/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import java.security.MessageDigest

internal enum class ArtworkFailure { ABSENT, QUALITY, DENIED, TIMEOUT, PROVIDER, TRANSPORT }
internal const val IMAGE_REQUEST_TIMEOUT_MS = 8_000L
internal val ArtworkDecodeDispatcher = kotlinx.coroutines.Dispatchers.IO.limitedParallelism(2)
internal fun artworkHash(value: String): String = artworkHash(value.toByteArray())
internal fun artworkHash(value: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }
