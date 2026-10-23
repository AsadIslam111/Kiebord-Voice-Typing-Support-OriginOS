package io.kiebord.feature.ime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.kiebord.theme.KiebordTheme

data class QuickSymbolDragState(
    val startOffset: Offset,
    val currentOffset: Offset,
    val hoveredKeyBounds: Rect? = null
)

@Composable
fun QuickSymbolDragOverlay(
    state: QuickSymbolDragState?,
    modifier: Modifier = Modifier
) {
    if (state == null) return

    val colorScheme = KiebordTheme.colorScheme
    val accentColor = colorScheme.accentColor

    Canvas(modifier = modifier.fillMaxSize()) {
        val start = state.startOffset
        val current = state.currentOffset

        // 1. Draw hovered key highlight border (if over a symbol key)
        state.hoveredKeyBounds?.let { bounds ->
            drawRoundRect(
                color = accentColor.copy(alpha = 0.25f),
                topLeft = bounds.topLeft,
                size = bounds.size,
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
            )
            drawRoundRect(
                color = accentColor,
                topLeft = bounds.topLeft,
                size = bounds.size,
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = 2.5.dp.toPx())
            )
        }

        // 2. Outer glowing neon line
        drawLine(
            color = accentColor.copy(alpha = 0.4f),
            start = start,
            end = current,
            strokeWidth = 9.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 3. Core crisp laser connecting line
        drawLine(
            color = Color.White,
            start = start,
            end = current,
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 4. Anchor origin circle at ?123
        drawCircle(
            color = accentColor,
            radius = 8.dp.toPx(),
            center = start
        )

        // 5. Finger cursor glow & core dot
        drawCircle(
            color = accentColor.copy(alpha = 0.45f),
            radius = 16.dp.toPx(),
            center = current
        )
        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = current
        )
    }
}
