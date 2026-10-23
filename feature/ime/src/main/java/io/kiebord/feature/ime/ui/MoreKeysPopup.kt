package io.kiebord.feature.ime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    modifier: Modifier = Modifier
) {
    if (state == null || state.items.isEmpty()) return

    val density = LocalDensity.current
    val colorScheme = KiebordTheme.colorScheme

    val itemCount = state.items.size
    val horizontalPaddingDp = 6.dp
    val verticalPaddingDp = 5.dp
    val horizontalPaddingPx = with(density) { horizontalPaddingDp.toPx() }
    val verticalPaddingPx = with(density) { verticalPaddingDp.toPx() }

    // Safe maximum width so popup never extends off screen
    val maxAllowedWidthPx = (keyboardWidthPx - with(density) { 16.dp.toPx() }).coerceAtLeast(with(density) { 180.dp.toPx() })

    // Adaptive item width: 40dp for standard popups, down to ~28-32dp for rich punctuation popups
    val baseItemWidthDp = if (itemCount > 7) 32.dp else 40.dp
    val idealItemWidthPx = with(density) { baseItemWidthDp.toPx() }
    val requiredWidthPx = itemCount * idealItemWidthPx + horizontalPaddingPx * 2

    val itemWidthPx = if (requiredWidthPx > maxAllowedWidthPx) {
        ((maxAllowedWidthPx - horizontalPaddingPx * 2) / itemCount).coerceAtLeast(with(density) { 26.dp.toPx() })
    } else {
        idealItemWidthPx
    }
    val itemWidthDp = with(density) { itemWidthPx.toDp() }
    val itemHeightDp = 44.dp
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    val totalWidthPx = itemCount * itemWidthPx + horizontalPaddingPx * 2
    val totalHeightPx = itemHeightPx + verticalPaddingPx * 2

    // 1. Safe horizontal clamping: at least 8dp from left and right edges so it never goes off-screen
    val anchorCenterX = state.anchorRect.center.x
    val idealLeft = anchorCenterX - totalWidthPx / 2f
    val minLeftPx = with(density) { 8.dp.toPx() }
    val maxLeftPx = (keyboardWidthPx - totalWidthPx - minLeftPx).coerceAtLeast(minLeftPx)
    val clampedLeft = idealLeft.coerceIn(minLeftPx, maxLeftPx)

    // 2. Safe vertical clamping: above anchor keycap, but never negative (off screen)
    val anchorTop = state.anchorRect.top
    val gapPx = with(density) { 6.dp.toPx() }
    val idealTop = anchorTop - totalHeightPx - gapPx
    val minTopPx = with(density) { 6.dp.toPx() }
    val popupTop = if (idealTop < minTopPx) minTopPx else idealTop

    // 3. Dynamic transform origin for smooth Borno Keyboard bloom originating right from anchor key
    val pivotFractionX = if (totalWidthPx > 0f) {
        ((anchorCenterX - clampedLeft) / totalWidthPx).coerceIn(0.08f, 0.92f)
    } else {
        0.5f
    }

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
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorScheme.moreKeysBackground)
                    .padding(horizontal = horizontalPaddingDp, vertical = verticalPaddingDp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.items.forEachIndexed { index, item ->
                        val isSelected = index == state.selectedIndex

                        Box(
                            modifier = Modifier
                                .width(itemWidthDp)
                                .height(itemHeightDp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    if (isSelected) colorScheme.accentColor else Color.Transparent
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                color = if (isSelected) Color.White else colorScheme.keyForeground,
                                fontSize = if (isSelected) 20.sp else 18.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
