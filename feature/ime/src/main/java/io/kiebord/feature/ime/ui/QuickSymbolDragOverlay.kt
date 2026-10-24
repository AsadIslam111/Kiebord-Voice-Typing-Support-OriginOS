package io.kiebord.feature.ime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.common.model.KeyDef
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme
import kotlin.math.roundToInt

data class QuickSymbolDragState(
    val startOffset: Offset,
    val currentOffset: Offset,
    val hoveredKey: KeyDef? = null,
    val hoveredKeyBounds: Rect? = null,
    val hoveredSymbol: String? = null
)

@Composable
fun QuickSymbolDragOverlay(
    state: QuickSymbolDragState?,
    isMoreKeysOpen: Boolean = false,
    scaleFactor: Float = 1.0f,
    keyboardWidthPx: Float = 1080f,
    modifier: Modifier = Modifier
) {
    if (state == null) return
    val dragDistance = (state.currentOffset - state.startOffset).getDistance()
    if (dragDistance <= 20f && state.hoveredSymbol == null) return

    val colorScheme = KiebordTheme.colorScheme
    val accentColor = colorScheme.accentColor
    val density = LocalDensity.current

    Box(modifier = modifier) {
        // 1. Single-colored semi-transparent guideline matching gesture trail style
        Canvas(modifier = Modifier.fillMaxSize()) {
            val start = state.startOffset
            val current = state.currentOffset

            val lineWidthPx = with(density) { (6.5f * scaleFactor).coerceIn(4.5f, 9f).dp.toPx() }
            val lineAlpha = 0.55f

            // Smooth semi-transparent connecting line
            drawLine(
                color = accentColor.copy(alpha = lineAlpha),
                start = start,
                end = current,
                strokeWidth = lineWidthPx,
                cap = StrokeCap.Round
            )

            // Anchor dot at start key (?123)
            drawCircle(
                color = accentColor.copy(alpha = lineAlpha),
                radius = with(density) { (6f * scaleFactor).coerceIn(4.5f, 8f).dp.toPx() },
                center = start
            )
            drawCircle(
                color = accentColor,
                radius = with(density) { (3f * scaleFactor).coerceIn(2f, 4f).dp.toPx() },
                center = start
            )

            // Tactile drag handle circle at current finger position
            drawCircle(
                color = accentColor.copy(alpha = 0.30f),
                radius = with(density) { (12f * scaleFactor).coerceIn(9f, 15f).dp.toPx() },
                center = current
            )
            drawCircle(
                color = accentColor,
                radius = with(density) { (8.5f * scaleFactor).coerceIn(6.5f, 11f).dp.toPx() },
                center = current
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.95f),
                radius = with(density) { (3.5f * scaleFactor).coerceIn(2.5f, 5f).dp.toPx() },
                center = current
            )
        }

        // 2. Standard circular key preview popup above the hovered key (if morekeys popup is not currently open)
        if (!isMoreKeysOpen && !state.hoveredSymbol.isNullOrEmpty() && state.hoveredKeyBounds != null) {
            val popupDiameter = (52 * scaleFactor).coerceIn(44f, 60f).dp
            val popupDiameterPx = with(density) { popupDiameter.toPx() }
            val halfPopupPx = popupDiameterPx / 2f
            val gapDp = (12 * scaleFactor).coerceIn(8f, 16f).dp
            val gapPx = with(density) { gapDp.toPx() }

            val keyCenterX = state.hoveredKeyBounds.center.x
            val keyTopY = state.hoveredKeyBounds.top

            val marginPx = with(density) { 6.dp.toPx() }
            val minCenterX = halfPopupPx + marginPx
            val maxCenterX = (keyboardWidthPx - halfPopupPx - marginPx).coerceAtLeast(minCenterX)
            val clampedCenterX = keyCenterX.coerceIn(minCenterX, maxCenterX)

            val idealTop = keyTopY - popupDiameterPx - gapPx
            val minTopPx = with(density) { 6.dp.toPx() }
            val clampedTop = if (idealTop < minTopPx) minTopPx else idealTop

            val popupLeft = clampedCenterX - halfPopupPx
            val isLight = colorScheme == KeyboardColorScheme.DefaultLight
            val popupBg = colorScheme.moreKeysBackground

            Box(
                modifier = Modifier
                    .offset { IntOffset(popupLeft.roundToInt(), clampedTop.roundToInt()) }
                    .requiredSize(popupDiameter)
                    .shadow(
                        elevation = (4 * scaleFactor).dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color(0x38000000),
                        spotColor = Color(0x30000000)
                    )
                    .clip(CircleShape)
                    .background(popupBg)
                    .border(
                        width = 0.5.dp,
                        color = if (isLight) Color(0x1A000000) else Color(0x22FFFFFF),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.hoveredSymbol,
                    color = colorScheme.keyForeground,
                    fontSize = (25 * scaleFactor).coerceIn(19f, 32f).sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = KiebordTheme.fontFamily,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
