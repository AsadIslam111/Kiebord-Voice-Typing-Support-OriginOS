package io.kiebord.feature.ime.ui.resizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme

@Composable
fun LiveResizerOverlay(
    currentRowHeight: Dp,
    currentChinPadding: Dp,
    onRowHeightChange: (Dp) -> Unit,
    onChinPaddingChange: (Dp) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    val accentColor = colorScheme.accentColor

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f))
            .border(2.dp, accentColor, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Drag Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(accentColor)
                    .pointerInput(currentRowHeight) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Dragging up increases height, dragging down decreases height
                            val deltaDp = (-dragAmount.y / 2f).dp
                            val newHeight = (currentRowHeight + deltaDp).coerceIn(48.dp, 76.dp)
                            onRowHeightChange(newHeight)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DragHandle,
                        contentDescription = "Resize Height",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Drag to Resize Height (${currentRowHeight.value.toInt()} dp)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Middle Control Strip: Done and Reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset Button
                IconButton(
                    onClick = {
                        onRowHeightChange(60.dp)
                        onChinPaddingChange(8.dp)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(colorScheme.actionKeyBackground)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.RestartAlt,
                        contentDescription = "Reset Default Height",
                        tint = colorScheme.keyForeground
                    )
                }

                // Done Button
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = " Save Size",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // 3. Bottom Chin Padding Handle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorScheme.actionKeyBackground)
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .pointerInput(currentChinPadding) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaDp = (-dragAmount.y / 2f).dp
                            val newPadding = (currentChinPadding + deltaDp).coerceIn(0.dp, 36.dp)
                            onChinPaddingChange(newPadding)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Bottom Margin: ${currentChinPadding.value.toInt()} dp",
                    color = colorScheme.keyForeground.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
