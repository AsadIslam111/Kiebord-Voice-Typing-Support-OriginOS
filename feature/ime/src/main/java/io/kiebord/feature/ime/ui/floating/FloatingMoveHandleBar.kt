package io.kiebord.feature.ime.ui.floating

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Bottom move handle bar for repositioning the floating keyboard.
 * Dragging this bar translates the floating keyboard and reveals the bottom landing hint rect.
 */
@Composable
fun FloatingMoveHandleBar(
    isDragging: Boolean,
    handleColor: Color,
    accentColor: Color,
    onDragStart: () -> Unit,
    onDrag: (deltaX: Float, deltaY: Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val onDragStartState = rememberUpdatedState(onDragStart)
    val onDragState = rememberUpdatedState(onDrag)
    val onDragEndState = rememberUpdatedState(onDragEnd)
    val onDragCancelState = rememberUpdatedState(onDragCancel)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(26.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        onDragStartState.value()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragState.value(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        onDragEndState.value()
                    },
                    onDragCancel = {
                        onDragCancelState.value()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val pillColor = if (isDragging) accentColor else handleColor.copy(alpha = 0.40f)
        val pillWidth = if (isDragging) 64.dp else 48.dp

        Box(
            modifier = Modifier
                .size(width = pillWidth, height = 4.5.dp)
                .clip(CircleShape)
                .background(pillColor)
        )
    }
}

