package io.kiebord.feature.ime.ui.resizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme

@Composable
fun LiveResizerOverlay(
    currentRowHeight: Dp,
    currentChinPadding: Dp,
    currentHorizontalMargin: Dp,
    onRowHeightChange: (Dp) -> Unit,
    onChinPaddingChange: (Dp) -> Unit,
    onHorizontalMarginChange: (Dp) -> Unit,
    onReset: () -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    val accentColor = colorScheme.accentColor
    val density = LocalDensity.current

    val currentHeightState = rememberUpdatedState(currentRowHeight)
    val onHeightChangeState = rememberUpdatedState(onRowHeightChange)

    val currentChinState = rememberUpdatedState(currentChinPadding)
    val onChinChangeState = rememberUpdatedState(onChinPaddingChange)

    val currentMarginState = rememberUpdatedState(currentHorizontalMargin)
    val onMarginChangeState = rememberUpdatedState(onHorizontalMarginChange)

    // Bounding wireframe outline with touch consumption (prevents keyboard touches beneath)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                        if (event.changes.all { !it.pressed }) break
                    }
                }
            }
            .border(2.dp, accentColor, RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.32f))
    ) {

        // 2. Top Drag Handle (pill centered on top border for smooth height resizing)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(56.dp)
                .height(10.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(5.dp))
                .clip(RoundedCornerShape(5.dp))
                .background(accentColor)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Keyboard has 4 rows; delta per row is total delta / 4 for smooth 1:1 finger tracking
                        val deltaRowDp = with(density) { (-dragAmount.y / 4f).toDp() }
                        val newHeight = (currentHeightState.value + deltaRowDp).coerceIn(46.dp, 78.dp)
                        onHeightChangeState.value(newHeight)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
            )
        }

        // 3. Left Drag Handle (pill centered on left edge for horizontal width / margin)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(10.dp)
                .height(48.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(5.dp))
                .clip(RoundedCornerShape(5.dp))
                .background(accentColor)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging right (inward) narrows keyboard, left (outward) widens
                        val deltaMarginDp = with(density) { (dragAmount.x / 2f).toDp() }
                        val newMargin = (currentMarginState.value + deltaMarginDp).coerceIn(0.dp, 48.dp)
                        onMarginChangeState.value(newMargin)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
            )
        }

        // 4. Right Drag Handle (pill centered on right edge for horizontal width / margin)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(10.dp)
                .height(48.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(5.dp))
                .clip(RoundedCornerShape(5.dp))
                .background(accentColor)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging left (inward) narrows keyboard, right (outward) widens
                        val deltaMarginDp = with(density) { (-dragAmount.x / 2f).toDp() }
                        val newMargin = (currentMarginState.value + deltaMarginDp).coerceIn(0.dp, 48.dp)
                        onMarginChangeState.value(newMargin)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(16.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
            )
        }

        // 5. Bottom Drag Handle (pill centered on bottom edge for bottom chin / margin)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .width(56.dp)
                .height(10.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(5.dp))
                .clip(RoundedCornerShape(5.dp))
                .background(accentColor)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaChin = with(density) { (-dragAmount.y).toDp() }
                        val newChin = (currentChinState.value + deltaChin).coerceIn(0.dp, 36.dp)
                        onChinChangeState.value(newChin)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(1.dp))
            )
        }

        // 6. Centered Floating Action Island matching active keyboard theme
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(colorScheme.containerBackground)
                .border(1.dp, colorScheme.keyForeground.copy(alpha = 0.18f), RoundedCornerShape(26.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Move / Drag Handle (Dragging here moves keyboard chin & horizontal margin)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.keyForeground.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val deltaChin = with(density) { (-dragAmount.y).toDp() }
                                val newChin = (currentChinState.value + deltaChin).coerceIn(0.dp, 48.dp)
                                onChinChangeState.value(newChin)

                                val deltaMargin = with(density) { (dragAmount.x / 2f).toDp() }
                                val newMargin = (currentMarginState.value + deltaMargin).coerceIn(0.dp, 48.dp)
                                onMarginChangeState.value(newMargin)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DragHandle,
                        contentDescription = "Move Keyboard",
                        tint = colorScheme.keyForeground.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(colorScheme.keyForeground.copy(alpha = 0.15f))
                )

                // Reset Button
                IconButton(
                    onClick = onReset,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "Reset",
                        tint = colorScheme.keyForeground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Scale / Height readout percentage
                Text(
                    text = "${(currentRowHeight.value / 60f * 100).toInt()}%",
                    color = colorScheme.keyForeground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .background(colorScheme.keyForeground.copy(alpha = 0.15f))
                )

                // Cancel Button
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cancel",
                        tint = colorScheme.keyForeground.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Save / Confirm Button (Accent circle with white checkmark)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onDone,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Save",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
