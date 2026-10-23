package io.kiebord.feature.ime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme
import kotlin.math.roundToInt

data class QuickSymbolDragState(
    val startOffset: Offset,
    val currentOffset: Offset,
    val hoveredKeyBounds: Rect? = null,
    val hoveredSymbol: String? = null
)

@Composable
fun QuickSymbolDragOverlay(
    state: QuickSymbolDragState?,
    modifier: Modifier = Modifier
) {
    if (state == null) return

    val colorScheme = KiebordTheme.colorScheme
    val accentColor = colorScheme.accentColor
    val density = LocalDensity.current

    Box(modifier = modifier) {
        // 1. Neon laser line and key highlight canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val start = state.startOffset
            val current = state.currentOffset

            // Draw hovered key highlight border (if over a symbol key)
            state.hoveredKeyBounds?.let { bounds ->
                drawRoundRect(
                    color = accentColor.copy(alpha = 0.28f),
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

            // Outer glowing neon line
            drawLine(
                color = accentColor.copy(alpha = 0.45f),
                start = start,
                end = current,
                strokeWidth = 10.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Core crisp laser connecting line
            drawLine(
                color = Color.White,
                start = start,
                end = current,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Anchor origin circle at ?123
            drawCircle(
                color = accentColor,
                radius = 8.dp.toPx(),
                center = start
            )
        }

        // 2. Magnified floating symbol badge above finger
        if (!state.hoveredSymbol.isNullOrEmpty()) {
            val badgeSizeDp = 48.dp
            val badgeSizePx = with(density) { badgeSizeDp.toPx() }
            val badgeLeft = (state.currentOffset.x - badgeSizePx / 2f).coerceAtLeast(4f)
            val badgeTop = (state.currentOffset.y - badgeSizePx - with(density) { 16.dp.toPx() }).coerceAtLeast(4f)

            Box(
                modifier = Modifier
                    .offset { IntOffset(badgeLeft.roundToInt(), badgeTop.roundToInt()) }
                    .size(badgeSizeDp)
                    .shadow(elevation = 12.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(accentColor)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.hoveredSymbol,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
