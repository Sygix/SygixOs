/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import java.net.URI

object UpdateUrlPolicy {

    private const val SCHEME = "https"
    private const val RELEASE_HOST = "github.com"

    fun isAllowed(url: String?): Boolean = parse(url)?.let { it.scheme.equals(SCHEME, ignoreCase = true) && !it.host.isNullOrEmpty() } == true

    fun isReleasePage(url: String?): Boolean =
        isAllowed(url) && parse(url)?.host.equals(RELEASE_HOST, ignoreCase = true)

    private fun parse(url: String?): URI? = url?.let { runCatching { URI(it) }.getOrNull() }?.takeIf { it.scheme != null }
}
