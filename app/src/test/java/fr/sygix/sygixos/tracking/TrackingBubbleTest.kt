/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.junit.Assert.*
import org.junit.Test

class TrackingBubbleTest {
    private val show = TrackingIdentity("fixture", TrackingType.SHOW, "show")
    private val other = TrackingIdentity("fixture", TrackingType.SHOW, "other")
    private val episode = TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "1"), show, "Fixture", season = 1, episode = 1)

    @Test fun noticeStatesMapToBubblesWithoutSyncProgress() {
        assertEquals(listOf(TrackingBubble.SEEN, TrackingBubble.LATER), TrackingBubble.of(TrackingNotice(episode, TrackingAction.SEEN, 1, returnPrompt = true, origin = PlaybackOrigin.HERO))?.let {
            assertEquals(PlaybackOrigin.HERO, it.origin); it.actions })
        assertEquals(TrackingBubbleKind.UNDO, TrackingBubble.of(TrackingNotice(episode, TrackingAction.SEEN, 1, busy = true))?.kind)
        assertEquals(TrackingBubbleKind.UNDO, TrackingBubble.of(TrackingNotice(episode, TrackingAction.SEEN, 1, locallySaved = true, confirmed = true))?.kind)
        assertEquals(listOf(TrackingBubble.UNDO), TrackingBubble.of(TrackingNotice(episode, TrackingAction.REMOVE, 1))?.actions)
        assertTrue(TrackingBubble.of(TrackingNotice(episode, TrackingAction.ARCHIVE, 1, blocked = true))!!.blocked)
        val failure = TrackingBubble.of(TrackingNotice(episode, TrackingAction.SEEN, 1, failure = TrackingFailure.ACCESS, undoing = true))!!
        assertEquals(listOf(TrackingBubble.RETRY), failure.actions)
        assertTrue(failure.reconnect && failure.undoing)
        assertEquals(fr.sygix.sygixos.R.string.tracking_undo_error, failure.message)
        assertEquals(fr.sygix.sygixos.R.string.tracking_reconnect, failure.label(TrackingBubble.RETRY))
        assertEquals(fr.sygix.sygixos.R.string.tracking_action_blocked, TrackingBubble.of(TrackingNotice(episode, TrackingAction.SEEN, 1, blocked = true))?.message)
        assertNull(TrackingBubble.of(TrackingNotice(episode, TrackingAction.SEEN, 1))?.message)
        assertNull(TrackingBubble.of(TrackingNotice(episode, TrackingAction.ADD, 1, busy = true)))
        assertEquals(TrackingBubbleKind.FAILURE, TrackingBubble.of(TrackingNotice(episode, TrackingAction.ADD, 1, failure = TrackingFailure.NETWORK))?.kind)
        assertNull(TrackingBubble.of(null))
    }

    @Test fun placementFollowsTheSeriesThenKeepsTheRowPositionThenFallsBackToTheHero() {
        val bubble = TrackingBubble(TrackingBubbleKind.UNDO, PlaybackOrigin.UP_NEXT, show)
        assertEquals(TrackingBubblePlacement.Card(2), TrackingBubblePlacement.of(bubble, listOf(null, other, show), 0))
        assertEquals(TrackingBubblePlacement.Card(1), TrackingBubblePlacement.of(bubble, listOf(null, other), 1))
        assertEquals(TrackingBubblePlacement.Card(1), TrackingBubblePlacement.of(bubble, listOf(null, other), 4))
        assertEquals(TrackingBubblePlacement.Hero, TrackingBubblePlacement.of(bubble, listOf(null, other), null))
        assertEquals(TrackingBubblePlacement.Hero, TrackingBubblePlacement.of(bubble, emptyList(), 1))
        assertEquals(TrackingBubblePlacement.Hero, TrackingBubblePlacement.of(bubble.copy(origin = PlaybackOrigin.HERO), listOf(show), null))
        assertNull(TrackingBubblePlacement.of(null, listOf(show), 0))
    }
}
