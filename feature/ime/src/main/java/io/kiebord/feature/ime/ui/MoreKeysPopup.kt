package io.kiebord.feature.ime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
    val anchorBounds: Rect,
    val items: List<String>,
    val selectedIndex: Int = 0
)

@Composable
fun MoreKeysPopup(
    state: MoreKeysPopupState?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state != null && state.items.isNotEmpty(),
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        if (state == null || state.items.isEmpty()) return@AnimatedVisibility

        val density = LocalDensity.current
        val colorScheme = KiebordTheme.colorScheme

        val itemWidthDp = 44.dp
        val itemHeightDp = 50.dp
        val totalWidthDp = itemWidthDp * state.items.size + 16.dp

        val totalWidthPx = with(density) { totalWidthDp.toPx() }
        val popupHeightPx = with(density) { (itemHeightDp + 16.dp).toPx() }

        // Center popup horizontally above the anchor key, clamped inside keyboard bounds
        val targetLeftPx = (state.anchorBounds.center.x - totalWidthPx / 2f).coerceAtLeast(16f)
        val targetTopPx = (state.anchorBounds.top - popupHeightPx - 8f).coerceAtLeast(8f)

        Box(
            modifier = modifier
                .offset { IntOffset(targetLeftPx.roundToInt(), targetTopPx.roundToInt()) }
                .wrapContentSize()
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(colorScheme.actionKeyBackground)
                .border(1.dp, colorScheme.accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.items.forEachIndexed { index, item ->
                    val isSelected = index == state.selectedIndex

                    Box(
                        modifier = Modifier
                            .size(width = itemWidthDp, height = itemHeightDp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) colorScheme.accentColor else colorScheme.keyBackground
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item,
                            color = if (isSelected) colorScheme.actionKeyForeground else colorScheme.keyForeground,
                            fontSize = 20.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
