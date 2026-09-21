package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.unit.dp

/** Courbe standard d'Apple (ease-in-out). */
val AppleEasing: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

object Motion {
    const val FOCUS_MS = 300
    const val LAYER_FADE_MS = 400
    const val HERO_CROSSFADE_MS = 900
    const val HERO_VIDEO_FADE_MS = 700
    const val HERO_DWELL_MS = 8_000L
    const val HERO_KEN_BURNS_MS = 12_000
    const val HERO_VIDEO_START_TIMEOUT_MS = 12_000L
}

/** Visuels du héro et du Top Shelf : en dessous, l'image ou la vidéo est écartée (upscale flou). */
object Quality {
    const val MIN_VISUAL_WIDTH_PX = 960
}

/** Canevas TV : 960 x 540 dp (1080p en xhdpi). */
object Dimens {
    val ScreenMarginH = 48.dp
    val GridTopMargin = 40.dp
    val GridSpacing = 16.dp
    val GridRowSpacing = 20.dp
    val TileCorner = 12.dp
    const val GridColumns = 5
    val DockBottomMargin = 24.dp
    val DockPadding = 14.dp
    /** Marge externe du dock : ses tuiles s'alignent sur les colonnes de la grille. */
    val DockOuterMargin = ScreenMarginH - DockPadding
    /** Format Apple Top Shelf (1920x720). */
    const val ShelfAspectRatio = 1920f / 720f
    /** Hauteur visible de la rangée qui dépasse au-dessus du panneau. */
    val ShelfPeek = 40.dp
    const val SHELF_SCROLL_MS = 400
}
