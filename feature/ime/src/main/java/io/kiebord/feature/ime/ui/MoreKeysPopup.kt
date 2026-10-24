package io.kiebord.feature.ime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.common.model.KeyDef
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme
import kotlin.math.roundToInt

data class MoreKeysPopupState(
    val anchorKey: KeyDef,
    val anchorRect: Rect,
    val items: List<String>,
    val selectedIndex: Int = 0
)

@Composable
fun MoreKeysPopup(
    state: MoreKeysPopupState?,
    keyboardWidthPx: Float,
    scaleFactor: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    if (state == null || state.items.isEmpty()) return

    val density = LocalDensity.current
    val colorScheme = KiebordTheme.colorScheme

    val itemCount = state.items.size
    // Multi-row support: honor custom moreKeysColumns if defined, else split into 2 rows if > 6 items
    val itemsPerRow = state.anchorKey.moreKeysColumns ?: ((itemCount + (if (itemCount > 6) 2 else 1) - 1) / (if (itemCount > 6) 2 else 1))
    val rowCount = (itemCount + itemsPerRow - 1) / itemsPerRow
    val chunkedRows = state.items.chunked(itemsPerRow)
    val maxCols = itemsPerRow

    val horizontalPaddingDp = (6 * scaleFactor).dp
    val verticalPaddingDp = (5 * scaleFactor).dp
    val colGapDp = (3 * scaleFactor).dp
    val rowGapDp = (4 * scaleFactor).dp
    val itemWidthDp = (42 * scaleFactor).dp
    val itemHeightDp = (42 * scaleFactor).dp

    val horizontalPaddingPx = with(density) { horizontalPaddingDp.toPx() }
    val verticalPaddingPx = with(density) { verticalPaddingDp.toPx() }
    val colGapPx = with(density) { colGapDp.toPx() }
    val rowGapPx = with(density) { rowGapDp.toPx() }
    val itemWidthPx = with(density) { itemWidthDp.toPx() }
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    val totalWidthPx = maxCols * itemWidthPx + (maxCols - 1) * colGapPx + horizontalPaddingPx * 2
    val totalHeightPx = rowCount * itemHeightPx + (rowCount - 1) * rowGapPx + verticalPaddingPx * 2

    // 1. Safe horizontal clamping: at least 8dp from screen edges
    val anchorCenterX = state.anchorRect.center.x
    val idealLeft = anchorCenterX - totalWidthPx / 2f
    val minLeftPx = with(density) { (8 * scaleFactor).dp.toPx() }
    val maxLeftPx = (keyboardWidthPx - totalWidthPx - minLeftPx).coerceAtLeast(minLeftPx)
    val clampedLeft = idealLeft.coerceIn(minLeftPx, maxLeftPx)

    // 2. Safe vertical clamping: above anchor keycap, never off top of screen
    val anchorTop = state.anchorRect.top
    val gapDp = (12 * scaleFactor).coerceIn(8f, 16f).dp
    val gapPx = with(density) { gapDp.toPx() }
    val idealTop = anchorTop - totalHeightPx - gapPx
    val minTopDp = 6.dp
    val minTopPx = with(density) { minTopDp.toPx() }
    val popupTop = if (idealTop < minTopPx) minTopPx else idealTop

    // 3. Dynamic transform origin for smooth bloom originating right from anchor key
    val pivotFractionX = if (totalWidthPx > 0f) {
        ((anchorCenterX - clampedLeft) / totalWidthPx).coerceIn(0.08f, 0.92f)
    } else {
        0.5f
    }

    // 4. Calculate target coordinates for the sliding highlight pill
    val safeSelectedIndex = state.selectedIndex.coerceIn(0, state.items.lastIndex)
    val selectedRow = (safeSelectedIndex / itemsPerRow).coerceIn(0, rowCount - 1)
    val selectedCol = (safeSelectedIndex % itemsPerRow)

    val targetPillX = (itemWidthDp + colGapDp) * selectedCol
    val targetPillY = (itemHeightDp + rowGapDp) * selectedRow

    // Snappy, silky-smooth sliding highlight indicator (80ms glide, zero lag, no bouncy spring delay)
    val animatedPillX by animateDpAsState(
        targetValue = targetPillX,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "pillX"
    )
    val animatedPillY by animateDpAsState(
        targetValue = targetPillY,
        animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing),
        label = "pillY"
    )

    val isLight = colorScheme == KeyboardColorScheme.DefaultLight

    // Outer Box holds the FIXED position so there is ZERO jumping
    Box(
        modifier = modifier
            .offset { IntOffset(clampedLeft.roundToInt(), popupTop.roundToInt()) }
            .wrapContentSize()
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(pivotFractionX, 1.0f),
                animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 100)),
            exit = scaleOut(
                targetScale = 0.85f,
                transformOrigin = TransformOrigin(pivotFractionX, 1.0f),
                animationSpec = tween(durationMillis = 60)
            ) + fadeOut(animationSpec = tween(durationMillis = 60))
        ) {
            // Material 3 Borderless Floating Capsule with Soft Shadow
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .shadow(
                        elevation = (4 * scaleFactor).dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0x38000000),
                        spotColor = Color(0x30000000)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorScheme.moreKeysBackground)
                    .border(
                        width = 0.5.dp,
                        color = if (isLight) Color(0x1A000000) else Color(0x22FFFFFF),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = horizontalPaddingDp, vertical = verticalPaddingDp)
            ) {
                // 1. Sliding Accent Highlight (glides smoothly under the selected key as a full circle)
                Box(
                    modifier = Modifier
                        .offset(x = animatedPillX, y = animatedPillY)
                        .size(itemWidthDp, itemHeightDp)
                        .clip(CircleShape)
                        .background(colorScheme.accentColor)
                )

                // 2. Keys Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(rowGapDp),
                    horizontalAlignment = Alignment.Start
                ) {
                    chunkedRows.forEachIndexed { rowIndex, rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(colGapDp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEachIndexed { colIndex, item ->
                                val flatIndex = rowIndex * itemsPerRow + colIndex
                                val isSelected = flatIndex == safeSelectedIndex

                                val animatedTextColor by animateColorAsState(
                                    targetValue = if (isSelected) Color.White else colorScheme.keyForeground,
                                    animationSpec = tween(durationMillis = 80),
                                    label = "moreKeyText"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(itemWidthDp, itemHeightDp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val actionIconSize = (22 * scaleFactor).coerceAtLeast(15f).dp
                                    when (item) {
                                        "ACTION_EMOJI", "action:emoji", "EMOJI" -> {
                                            Icon(
                                                imageVector = Icons.Outlined.Mood,
                                                contentDescription = "Emoji",
                                                tint = animatedTextColor,
                                                modifier = Modifier.size(actionIconSize)
                                            )
                                        }
                                        "ACTION_SELECT_ALL", "action:select_all", "SELECT_ALL" -> {
                                            Icon(
                                                imageVector = Icons.Outlined.SelectAll,
                                                contentDescription = "Select All",
                                                tint = animatedTextColor,
                                                modifier = Modifier.size(actionIconSize)
                                            )
                                        }
                                        "ACTION_CUT", "action:cut", "CUT" -> {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCut,
                                                contentDescription = "Cut",
                                                tint = animatedTextColor,
                                                modifier = Modifier.size(actionIconSize)
                                            )
                                        }
                                        "ACTION_COPY", "action:copy", "COPY" -> {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "Copy",
                                                tint = animatedTextColor,
                                                modifier = Modifier.size(actionIconSize)
                                            )
                                        }
                                        "ACTION_PASTE", "action:paste", "PASTE" -> {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentPaste,
                                                contentDescription = "Paste",
                                                tint = animatedTextColor,
                                                modifier = Modifier.size(actionIconSize)
                                            )
                                        }
                                        "ACTION_UNDO", "action:undo", "UNDO" -> {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.Undo,
                                                contentDescription = "Undo",
                                                tint = animatedTextColor,
                                                modifier = Modifier.size(actionIconSize)
                                            )
                                        }
                                        "ACTION_REDO", "action:redo", "REDO" -> {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Outlined.Redo,
                                                contentDescription = "Redo",
                                                tint = animatedTextColor,
                                                modifier = Modifier.size(actionIconSize)
                                            )
                                        }
                                        else -> {
                                            Text(
                                                text = item,
                                                color = animatedTextColor,
                                                fontSize = (19 * scaleFactor).sp,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
