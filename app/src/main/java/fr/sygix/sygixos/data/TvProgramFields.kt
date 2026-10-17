/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.database.Cursor
import android.media.tv.TvContract
import fr.sygix.sygixos.model.ProgramKind

internal fun Cursor.optString(column: String): String? =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getString)?.takeIf { it.isNotBlank() }

internal fun Cursor.optLong(column: String): Long =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getLong) ?: 0L

internal fun Cursor.optInt(column: String, default: Int): Int =
    getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getInt) ?: default

internal fun landscapeImage(c: Cursor, posterColumn: String, aspectColumn: String, thumbnailColumn: String): String? {
    val poster = c.optString(posterColumn)
    val thumbnail = c.optString(thumbnailColumn)
    val portrait = isPortraitRatio(c.optInt(aspectColumn, -1))
    return if (portrait && thumbnail != null) thumbnail else poster ?: thumbnail
}

internal fun isPortraitRatio(ratio: Int): Boolean = ratio == TvContract.PreviewPrograms.ASPECT_RATIO_2_3 || ratio == 5

internal fun watchNextKind(type: Int): ProgramKind = when (type) {
    TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_CONTINUE -> ProgramKind.CONTINUE
    TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_NEXT -> ProgramKind.NEXT
    TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_NEW -> ProgramKind.NEW
    TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_WATCHLIST -> ProgramKind.WATCHLIST
    else -> ProgramKind.WATCH_NEXT
}
