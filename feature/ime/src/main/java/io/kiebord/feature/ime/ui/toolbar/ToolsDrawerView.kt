package io.kiebord.feature.ime.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.kiebord.theme.KiebordTheme

data class DrawerTool(
    val action: ToolbarAction,
    val icon: ImageVector,
    val label: String
)

@Composable
fun ToolsDrawerView(
    toolOrder: List<ToolbarAction>,
    onToolOrderChange: (List<ToolbarAction>) -> Unit,
    onToolClick: (ToolbarAction) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme

    val allToolsMap = remember {
        mapOf(
            ToolbarAction.TEXT_EDIT to DrawerTool(ToolbarAction.TEXT_EDIT, Icons.Outlined.TextFields, "Text editing"),
            ToolbarAction.CLIPBOARD to DrawerTool(ToolbarAction.CLIPBOARD, Icons.AutoMirrored.Outlined.Assignment, "Clipboard"),
            ToolbarAction.THEME_TOGGLE to DrawerTool(ToolbarAction.THEME_TOGGLE, Icons.Outlined.Palette, "Theme"),
            ToolbarAction.RESIZER to DrawerTool(ToolbarAction.RESIZER, Icons.Outlined.CropFree, "Resize"),
            ToolbarAction.NUMBER_ROW_TOGGLE to DrawerTool(ToolbarAction.NUMBER_ROW_TOGGLE, Icons.Outlined.Pin, "Number row"),
            ToolbarAction.EMOJI to DrawerTool(ToolbarAction.EMOJI, Icons.Outlined.Mood, "Emoji")
        )
    }

    // Grid geometry measurement for continuous 2D free dragging
    var gridWidthPx by remember { mutableFloatStateOf(0f) }
    var gridHeightPx by remember { mutableFloatStateOf(0f) }
    var draggingAction by remember { mutableStateOf<ToolbarAction?>(null) }
    var dragOriginIndex by remember { mutableIntStateOf(0) }
    var currentSlotIndex by remember { mutableIntStateOf(0) }
    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    val numColumns = 3
    val totalRows = (toolOrder.size + numColumns - 1) / numColumns
    val colWidthPx = if (gridWidthPx > 0f) gridWidthPx / numColumns else 100f
    val rowHeightPx = if (gridHeightPx > 0f && totalRows > 0) gridHeightPx / totalRows else 80f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
            // Generous 32dp horizontal margin so items are never stretched too wide
            .padding(horizontal = 32.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Subtitle centered: "Hold and drag to customize"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hold and drag to customize",
                color = colorScheme.keyForeground.copy(alpha = 0.8f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 3-Column Balanced Grid container (exactly 3 items per row, perfectly centered)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    gridWidthPx = coords.size.width.toFloat()
                    gridHeightPx = coords.size.height.toFloat()
                },
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val chunkedTools = toolOrder.chunked(numColumns)
            chunkedTools.forEachIndexed { rowIndex, rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowActions.forEachIndexed { colIndex, action ->
                        val globalIndex = rowIndex * numColumns + colIndex
                        val tool = allToolsMap[action] ?: return@forEachIndexed
                        val isDragging = draggingAction == action

                        val originCol = dragOriginIndex % numColumns
                        val originRow = dragOriginIndex / numColumns
                        val curCol = currentSlotIndex % numColumns
                        val curRow = currentSlotIndex / numColumns

                        val visualTranslationX = if (isDragging) {
                            accumulatedDragX - (curCol - originCol) * colWidthPx
                        } else {
                            0f
                        }
                        val visualTranslationY = if (isDragging) {
                            accumulatedDragY - (curRow - originRow) * rowHeightPx
                        } else {
                            0f
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .zIndex(if (isDragging) 10f else 1f)
                                .graphicsLayer {
                                    translationX = visualTranslationX
                                    translationY = visualTranslationY
                                    scaleX = if (isDragging) 1.15f else 1f
                                    scaleY = if (isDragging) 1.15f else 1f
                                    shadowElevation = if (isDragging) 16f else 0f
                                }
                                .pointerInput(toolOrder) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingAction = action
                                            dragOriginIndex = globalIndex
                                            currentSlotIndex = globalIndex
                                            accumulatedDragX = 0f
                                            accumulatedDragY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            accumulatedDragX += dragAmount.x
                                            accumulatedDragY += dragAmount.y

                                            val colShift = Math.round(accumulatedDragX / colWidthPx).toInt()
                                            val rowShift = Math.round(accumulatedDragY / rowHeightPx).toInt()

                                            val targetCol = (originCol + colShift).coerceIn(0, numColumns - 1)
                                            val targetRow = (originRow + rowShift).coerceIn(0, totalRows - 1)
                                            val targetIndex = (targetRow * numColumns + targetCol).coerceIn(0, toolOrder.lastIndex)

                                            if (targetIndex != currentSlotIndex && targetIndex in toolOrder.indices) {
                                                val mutable = toolOrder.toMutableList()
                                                val moved = mutable.removeAt(currentSlotIndex)
                                                mutable.add(targetIndex, moved)
                                                onToolOrderChange(mutable)
                                                currentSlotIndex = targetIndex
                                            }
                                        },
                                        onDragEnd = {
                                            draggingAction = null
                                            accumulatedDragX = 0f
                                            accumulatedDragY = 0f
                                        },
                                        onDragCancel = {
                                            draggingAction = null
                                            accumulatedDragX = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            ToolGridItem(
                                tool = tool,
                                isDragging = isDragging,
                                onClick = {
                                    if (draggingAction == null) {
                                        onToolClick(tool.action)
                                    }
                                }
                            )
                        }
                    }

                    // Fill remainder of row to keep columns balanced
                    repeat(numColumns - rowActions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolGridItem(
    tool: DrawerTool,
    isDragging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        // Tile container: Fixed 54dp compact squircle (never stretched out wide)
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isDragging) colorScheme.accentColor.copy(alpha = 0.35f) else colorScheme.actionKeyBackground
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
                tint = if (isDragging) colorScheme.accentColor else colorScheme.keyForeground,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Label below the tile
        Text(
            text = tool.label,
            color = colorScheme.keyForeground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
