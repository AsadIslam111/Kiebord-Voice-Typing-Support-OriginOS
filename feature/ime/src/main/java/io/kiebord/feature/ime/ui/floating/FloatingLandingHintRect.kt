package io.kiebord.feature.ime.ui.floating

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom landing hint rectangle displayed while dragging the floating keyboard.
 * Dragging and releasing the keyboard into this rectangle restores regular docked mode.
 */
@Composable
fun FloatingLandingHintRect(
    isVisible: Boolean,
    isHovered: Boolean,
    accentColor: Color,
    onBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.92f),
        exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.92f),
        modifier = modifier
    ) {
        val animatedScale by animateFloatAsState(
            targetValue = if (isHovered) 1.05f else 1.0f,
            animationSpec = tween(120),
            label = "landingScale"
        )

        val shape = RoundedCornerShape(18.dp)
        val backgroundColor = if (isHovered) {
            accentColor.copy(alpha = 0.28f)
        } else {
            accentColor.copy(alpha = 0.10f)
        }
        val borderColor = if (isHovered) {
            accentColor
        } else {
            accentColor.copy(alpha = 0.50f)
        }
        val borderWidth = if (isHovered) 2.dp else 1.5.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(animatedScale)
                    .fillMaxWidth(0.70f)
                    .height(54.dp)
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            onBoundsChanged(coords.boundsInRoot())
                        }
                    }
                    .clip(shape)
                    .background(backgroundColor)
                    .border(borderWidth, borderColor, shape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isHovered) "Release to Dock" else "Drag here to dock",
                    color = if (isHovered) accentColor else accentColor.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = if (isHovered) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

