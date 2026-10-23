package io.kiebord.feature.ime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    val itemWidthDp = 44.dp
    val itemHeightDp = 48.dp
    val itemWidthPx = with(density) { itemWidthDp.toPx() }
    val itemHeightPx = with(density) { itemHeightDp.toPx() }

    val horizontalPaddingDp = 8.dp
    val verticalPaddingDp = 6.dp
    val horizontalPaddingPx = with(density) { horizontalPaddingDp.toPx() }
    val verticalPaddingPx = with(density) { verticalPaddingDp.toPx() }

    val itemCount = state.items.size
    val totalWidthPx = itemCount * itemWidthPx + horizontalPaddingPx * 2
    val totalHeightPx = itemHeightPx + verticalPaddingPx * 2

    // 1. Calculate exact horizontal centering directly above the anchor keycap
    val anchorCenterX = state.anchorRect.center.x
    val idealLeft = anchorCenterX - totalWidthPx / 2f
    val clampedLeft = idealLeft.coerceIn(8f, (keyboardWidthPx - totalWidthPx - 8f).coerceAtLeast(8f))

    // 2. Position directly above anchor key with a clean 6dp gap
    val anchorTop = state.anchorRect.top
    val popupTop = (anchorTop - totalHeightPx - with(density) { 6.dp.toPx() }).coerceAtLeast(4f)

    // 3. Dynamic transform origin for smooth Borno Keyboard bloom originating right from anchor key
    val pivotFractionX = if (totalWidthPx > 0f) {
        ((anchorCenterX - clampedLeft) / totalWidthPx).coerceIn(0.1f, 0.9f)
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
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(26.dp))
                    .clip(RoundedCornerShape(26.dp))
                    .background(colorScheme.actionKeyBackground)
                    .border(1.dp, colorScheme.keyForeground.copy(alpha = 0.16f), RoundedCornerShape(26.dp))
                    .padding(horizontal = horizontalPaddingDp, vertical = verticalPaddingDp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    state.items.forEachIndexed { index, item ->
                        val isSelected = index == state.selectedIndex

                        Box(
                            modifier = Modifier
                                .width(itemWidthDp)
                                .height(itemHeightDp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    if (isSelected) colorScheme.accentColor else colorScheme.actionKeyBackground
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                color = if (isSelected) colorScheme.actionKeyForeground else colorScheme.keyForeground,
                                fontSize = if (isSelected) 21.sp else 19.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
