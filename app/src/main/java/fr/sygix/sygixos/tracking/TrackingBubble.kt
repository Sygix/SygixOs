/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.R

enum class TrackingBubbleKind { RETURN, UNDO, FAILURE }

data class TrackingBubble(
    val kind: TrackingBubbleKind,
    val origin: PlaybackOrigin,
    val parent: TrackingIdentity,
    val reconnect: Boolean = false,
    val undoing: Boolean = false,
    val blocked: Boolean = false,
) {
    val actions: List<String> get() = when (kind) {
        TrackingBubbleKind.RETURN -> listOf(SEEN, LATER)
        TrackingBubbleKind.UNDO -> listOf(UNDO)
        TrackingBubbleKind.FAILURE -> listOf(RETRY)
    }

    val message: Int? get() = when {
        kind == TrackingBubbleKind.RETURN -> R.string.tracking_return
        kind == TrackingBubbleKind.FAILURE -> if (undoing) R.string.tracking_undo_error else R.string.tracking_error
        blocked -> R.string.tracking_action_blocked
        else -> null
    }

    fun label(key: String): Int = when (key) {
        SEEN -> R.string.tracking_bubble_seen
        LATER -> R.string.tracking_bubble_later
        RETRY -> if (reconnect) R.string.tracking_reconnect else R.string.tracking_retry
        else -> R.string.tracking_undo
    }

    companion object {
        const val SEEN = "tracking-bubble-seen"
        const val LATER = "tracking-bubble-later"
        const val UNDO = "tracking-bubble-undo"
        const val RETRY = "tracking-bubble-retry"

        fun of(notice: TrackingNotice?): TrackingBubble? = when {
            notice == null -> null
            notice.returnPrompt -> TrackingBubble(TrackingBubbleKind.RETURN, notice.origin, notice.content.parent)
            notice.failure != null -> TrackingBubble(TrackingBubbleKind.FAILURE, notice.origin, notice.content.parent,
                reconnect = notice.failure.requiresReconnection, undoing = notice.undoing)
            notice.action == TrackingAction.ADD -> null
            else -> TrackingBubble(TrackingBubbleKind.UNDO, notice.origin, notice.content.parent, blocked = notice.blocked)
        }
    }
}

sealed interface TrackingBubblePlacement {
    data class Card(val index: Int) : TrackingBubblePlacement
    data object Hero : TrackingBubblePlacement

    companion object {
        fun of(bubble: TrackingBubble?, parents: List<TrackingIdentity?>, previous: Int?): TrackingBubblePlacement? {
            if (bubble == null) return null
            if (bubble.origin == PlaybackOrigin.HERO || parents.isEmpty()) return Hero
            val index = parents.indexOf(bubble.parent).takeIf { it >= 0 } ?: previous?.coerceAtMost(parents.lastIndex) ?: return Hero
            return Card(index)
        }
    }
}
