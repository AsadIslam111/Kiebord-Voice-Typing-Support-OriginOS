package io.kiebord.feature.ime.ui.floating

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ResizerCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

/**
 * Padded, concentric rounded resizer corner handles for the floating keyboard.
 * The roundness of the outside resizers perfectly matches the keyboard card roundness (18.dp) + padding/gap.
 * Placed on the outside of the 4 card corners with solid, refined stroke thickness.
 * Dragging from any corner adjusts width and height scale within configured clamp limits.
 */
@Composable
fun FloatingCornerResizers(
    currentWidth: Dp,
    currentHeightScale: Float,
    minWidth: Dp = 240.dp,
    maxWidth: Dp = 600.dp,
    minHeightScale: Float = 0.65f,
    maxHeightScale: Float = 1.35f,
    accentColor: Color,
    onSizeChange: (width: Dp, heightScale: Float) -> Unit,
    onCommitSize: (width: Dp, heightScale: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerTouchSize = 36.dp
    val outwardOffset = 8.dp

    val currentWidthState = rememberUpdatedState(currentWidth)
    val currentHeightScaleState = rememberUpdatedState(currentHeightScale)
    val onSizeChangeState = rememberUpdatedState(onSizeChange)
    val onCommitSizeState = rememberUpdatedState(onCommitSize)

    Box(modifier = modifier) {
        ResizerCorner.values().forEach { corner ->
            var isDraggingThisCorner by remember { mutableStateOf(false) }

            val alignment = when (corner) {
                ResizerCorner.TOP_LEFT -> Alignment.TopStart
                ResizerCorner.TOP_RIGHT -> Alignment.TopEnd
                ResizerCorner.BOTTOM_LEFT -> Alignment.BottomStart
                ResizerCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
            }

            val offsetModifier = when (corner) {
                ResizerCorner.TOP_LEFT -> Modifier.offset(x = -outwardOffset, y = -outwardOffset)
                ResizerCorner.TOP_RIGHT -> Modifier.offset(x = outwardOffset, y = -outwardOffset)
                ResizerCorner.BOTTOM_LEFT -> Modifier.offset(x = -outwardOffset, y = outwardOffset)
                ResizerCorner.BOTTOM_RIGHT -> Modifier.offset(x = outwardOffset, y = outwardOffset)
            }

            Box(
                modifier = Modifier
                    .align(alignment)
                    .then(offsetModifier)
                    .size(cornerTouchSize)
                    .pointerInput(corner) {
                        detectDragGestures(
                            onDragStart = {
                                isDraggingThisCorner = true
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val w = currentWidthState.value
                                val hScale = currentHeightScaleState.value

                                val deltaX = dragAmount.x / density
                                val deltaY = dragAmount.y / density

                                val widthMultiplier = when (corner) {
                                    ResizerCorner.TOP_LEFT, ResizerCorner.BOTTOM_LEFT -> -2f
                                    ResizerCorner.TOP_RIGHT, ResizerCorner.BOTTOM_RIGHT -> 2f
                                }
                                val newWidth = (w + (deltaX * widthMultiplier).dp).coerceIn(minWidth, maxWidth)

                                val heightMultiplier = when (corner) {
                                    ResizerCorner.TOP_LEFT, ResizerCorner.TOP_RIGHT -> -0.005f
                                    ResizerCorner.BOTTOM_LEFT, ResizerCorner.BOTTOM_RIGHT -> 0.005f
                                }
                                val newHeightScale = (hScale + (deltaY * heightMultiplier)).coerceIn(minHeightScale, maxHeightScale)

                                onSizeChangeState.value(newWidth, newHeightScale)
                            },
                            onDragEnd = {
                                isDraggingThisCorner = false
                                onCommitSizeState.value(currentWidthState.value, currentHeightScaleState.value)
                            },
                            onDragCancel = {
                                isDraggingThisCorner = false
                                onCommitSizeState.value(currentWidthState.value, currentHeightScaleState.value)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(cornerTouchSize)) {
                    val strokeWidth = if (isDraggingThisCorner) 4.2.dp.toPx() else 3.4.dp.toPx()
                    val color = if (isDraggingThisCorner) accentColor else accentColor.copy(alpha = 0.92f)
                    val shadowColor = Color.Black.copy(alpha = 0.22f)

                    // Keyboard corner radius = 18dp; padding/gap = 3.5dp; concentric radius = 21.5dp
                    val r = 21.5.dp.toPx()
                    val ext = 3.5.dp.toPx()
                    val path = Path()

                    when (corner) {
                        ResizerCorner.TOP_LEFT -> {
                            val center = Offset(26.dp.toPx(), 26.dp.toPx())
                            val arcRect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
                            path.moveTo(center.x + ext, center.y - r)
                            path.lineTo(center.x, center.y - r)
                            path.arcTo(arcRect, 270f, -90f, false)
                            path.lineTo(center.x - r, center.y + ext)
                        }
                        ResizerCorner.TOP_RIGHT -> {
                            val center = Offset(10.dp.toPx(), 26.dp.toPx())
                            val arcRect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
                            path.moveTo(center.x - ext, center.y - r)
                            path.lineTo(center.x, center.y - r)
                            path.arcTo(arcRect, 270f, 90f, false)
                            path.lineTo(center.x + r, center.y + ext)
                        }
                        ResizerCorner.BOTTOM_LEFT -> {
                            val center = Offset(26.dp.toPx(), 10.dp.toPx())
                            val arcRect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
                            path.moveTo(center.x - r, center.y - ext)
                            path.lineTo(center.x - r, center.y)
                            path.arcTo(arcRect, 180f, -90f, false)
                            path.lineTo(center.x + ext, center.y + r)
                        }
                        ResizerCorner.BOTTOM_RIGHT -> {
                            val center = Offset(10.dp.toPx(), 10.dp.toPx())
                            val arcRect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
                            path.moveTo(center.x + r, center.y - ext)
                            path.lineTo(center.x + r, center.y)
                            path.arcTo(arcRect, 0f, 90f, false)
                            path.lineTo(center.x - ext, center.y + r)
                        }
                    }

                    drawPath(
                        path = path,
                        color = shadowColor,
                        style = Stroke(
                            width = strokeWidth + 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}
