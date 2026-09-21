package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.model.TvApp

@Composable
internal fun AppContextMenu(
    app: TvApp,
    pinned: Boolean,
    onTogglePin: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onMove: (() -> Unit)? = null,
) {
    val confirmFocus = remember { FocusRequester() }
    LaunchedEffect(app.packageName) {
        withFrameNanos { }
        confirmFocus.tryRequestFocus()
    }
    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(Modifier.width(520.dp)) {
            Column(Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
                Text(app.label, style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(app.packageName, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MenuButton(text = "Fermer", onClick = onDismiss)
                    MenuButton(
                        text = if (pinned) "Retirer du dock" else "Épingler au dock",
                        onClick = onTogglePin,
                        focusRequester = confirmFocus,
                    )
                    if (onMove != null) MenuButton(text = "Déplacer", onClick = onMove)
                }
            }
        }
    }
}

@Composable
private fun MenuButton(text: String, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    var focused by remember { mutableStateOf(false) }
    Box(
        Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(12.dp))
            .background(if (focused) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.10f))
            .tvClickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = if (focused) Color(0xFF15151A) else Color.White,
            maxLines = 1,
            softWrap = false,
        )
    }
}
