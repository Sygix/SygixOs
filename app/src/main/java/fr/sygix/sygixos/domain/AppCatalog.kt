/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.TvApp

object AppCatalog {

    fun dock(apps: List<TvApp>, pinned: List<String>): List<TvApp> =
        pinned.mapNotNull { pkg -> apps.find { it.packageName == pkg } }

    fun grid(apps: List<TvApp>, order: List<String> = emptyList()): List<TvApp> {
        val byPackage = apps.associateBy { it.packageName }
        val ordered = order.mapNotNull(byPackage::get)
        val rest = apps.filter { it.packageName !in order }.sortedBy { it.label.lowercase() }
        return ordered + rest
    }

    fun move(order: List<String>, packageName: String, delta: Int): List<String> {
        val from = order.indexOf(packageName)
        if (from < 0 || order.size < 2) return order
        val to = (from + delta).coerceIn(0, order.lastIndex)
        if (to == from) return order
        return order.toMutableList().apply {
            removeAt(from)
            add(to, packageName)
        }
    }

    fun togglePinned(current: List<String>, packageName: String): List<String> =
        if (packageName in current) current - packageName else current + packageName
}
