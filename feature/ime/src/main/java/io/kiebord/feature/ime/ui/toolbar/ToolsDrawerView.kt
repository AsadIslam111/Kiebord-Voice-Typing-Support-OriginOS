package io.kiebord.feature.ime.ui.toolbar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartButton
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
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
    drawerTools: List<ToolbarAction>,
    onDrawerToolsChange: (List<ToolbarAction>) -> Unit,
    onInterchangeWithToolbar: (drawerAction: ToolbarAction, toolbarAction: ToolbarAction) -> Unit,
    onMoveToToolbar: (drawerAction: ToolbarAction) -> Unit,
    onToolClick: (ToolbarAction) -> Unit,
    height: Dp,
    rootCoordinates: LayoutCoordinates?,
    onReportItemBounds: (ToolbarAction, Rect) -> Unit,
    onReportContainerBounds: (Rect) -> Unit,
    hoveredDrawerAction: ToolbarAction?,
    isOverDrawerBlankSpace: Boolean,
    hoveredToolbarAction: ToolbarAction?,
    modifier: Modifier = Modifier,
    isActionActive: (ToolbarAction) -> Boolean = { false },
    onDraggingStateChange: (draggingAction: ToolbarAction?, fingerPosInRoot: Offset?) -> Unit = { _, _ -> }
) {
    val colorScheme = KiebordTheme.colorScheme
    val density = LocalDensity.current

    val currentDrawerTools by rememberUpdatedState(drawerTools)
    val currentOnOrderChange by rememberUpdatedState(onDrawerToolsChange)
    val currentOnInterchange by rememberUpdatedState(onInterchangeWithToolbar)
    val currentOnMoveToToolbar by rememberUpdatedState(onMoveToToolbar)
    val currentOnDraggingStateChange by rememberUpdatedState(onDraggingStateChange)
    val currentHoveredToolbarAction by rememberUpdatedState(hoveredToolbarAction)
    val currentHoveredDrawerAction by rememberUpdatedState(hoveredDrawerAction)

    val allToolsMap = remember {
        mapOf(
            ToolbarAction.TEXT_EDIT to DrawerTool(ToolbarAction.TEXT_EDIT, Icons.Outlined.TextFields, "Text editing"),
            ToolbarAction.CLIPBOARD to DrawerTool(ToolbarAction.CLIPBOARD, Icons.AutoMirrored.Outlined.Assignment, "Clipboard"),
            ToolbarAction.THEME_TOGGLE to DrawerTool(ToolbarAction.THEME_TOGGLE, Icons.Outlined.Palette, "Theme"),
            ToolbarAction.RESIZER to DrawerTool(ToolbarAction.RESIZER, Icons.Outlined.CropFree, "Resize"),
            ToolbarAction.NUMBER_ROW_TOGGLE to DrawerTool(ToolbarAction.NUMBER_ROW_TOGGLE, Icons.Outlined.Pin, "Number row"),
            ToolbarAction.SECONDARY_KEYS_TOGGLE to DrawerTool(ToolbarAction.SECONDARY_KEYS_TOGGLE, Icons.Outlined.Layers, "Key hints"),
            ToolbarAction.KEY_POPUP_TOGGLE to DrawerTool(ToolbarAction.KEY_POPUP_TOGGLE, Icons.Outlined.SmartButton, "Key popup"),
            ToolbarAction.EMOJI to DrawerTool(ToolbarAction.EMOJI, Icons.Outlined.Mood, "Emoji"),
            ToolbarAction.ONE_HANDED to DrawerTool(ToolbarAction.ONE_HANDED, Icons.Outlined.TouchApp, "One-handed"),
            ToolbarAction.SETTINGS to DrawerTool(ToolbarAction.SETTINGS, Icons.Outlined.Settings, "Settings"),
            ToolbarAction.TRADITIONAL_MODE to DrawerTool(ToolbarAction.TRADITIONAL_MODE, Icons.Outlined.Translate, "Traditional"),
            ToolbarAction.UNDO to DrawerTool(ToolbarAction.UNDO, Icons.AutoMirrored.Outlined.Undo, "Undo"),
            ToolbarAction.REDO to DrawerTool(ToolbarAction.REDO, Icons.AutoMirrored.Outlined.Redo, "Redo")
        )
    }

    var draggingAction by remember { mutableStateOf<ToolbarAction?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(0) }
    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var itemStartOffsetInRoot by remember { mutableStateOf(Offset.Zero) }

    val numColumns = 3
    val addThresholdPx = with(density) { 30.dp.toPx() }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 6.dp)
            .onGloballyPositioned { coords ->
                rootCoordinates?.let { root ->
                    if (coords.isAttached && root.isAttached) {
                        onReportContainerBounds(root.localBoundingBoxOf(coords))
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Clean static subtitle - no jumping banners or layout flickering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hold and drag to customize",
                color = colorScheme.keyForeground.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 3-Column Balanced Grid container (exactly 3 items per row, perfectly centered)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val chunkedTools = drawerTools.chunked(numColumns)
            chunkedTools.forEachIndexed { rowIndex, rowActions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowActions.forEachIndexed { colIndex, action ->
                        val globalIndex = rowIndex * numColumns + colIndex
                        val tool = allToolsMap[action]
                        if (tool != null) {
                            key(action) {
                                val isDraggingThisItem = draggingAction == action
                                val isHovered = hoveredDrawerAction == action
                                var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .zIndex(if (isDraggingThisItem) 100f else 1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .graphicsLayer {
                                            translationX = if (isDraggingThisItem) accumulatedDragX else 0f
                                            translationY = if (isDraggingThisItem) accumulatedDragY else 0f
                                            scaleX = if (isDraggingThisItem || isHovered) 1.08f else 1f
                                            scaleY = if (isDraggingThisItem || isHovered) 1.08f else 1f
                                        }
                                        .pointerInput(action) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { localOffset ->
                                                    draggingAction = action
                                                    dragStartIndex = currentDrawerTools.indexOf(action)
                                                    accumulatedDragX = 0f
                                                    accumulatedDragY = 0f
                                                    itemCoordinates?.let { coords ->
                                                        rootCoordinates?.let { root ->
                                                            if (coords.isAttached && root.isAttached) {
                                                                val myBox = root.localBoundingBoxOf(coords)
                                                                itemStartOffsetInRoot = myBox.topLeft + localOffset
                                                                currentOnDraggingStateChange(action, itemStartOffsetInRoot)
                                                            }
                                                        }
                                                    }
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    accumulatedDragX += dragAmount.x
                                                    accumulatedDragY += dragAmount.y
                                                    val currentFinger = itemStartOffsetInRoot + Offset(accumulatedDragX, accumulatedDragY)
                                                    currentOnDraggingStateChange(action, currentFinger)
                                                },
                                                onDragEnd = {
                                                    val draggedAct = draggingAction
                                                    if (draggedAct != null) {
                                                        if (currentHoveredToolbarAction != null && currentHoveredToolbarAction != draggedAct) {
                                                            // Hovered over specific toolbar option -> SWAP!
                                                            currentOnInterchange(draggedAct, currentHoveredToolbarAction!!)
                                                        } else if (accumulatedDragY < -addThresholdPx) {
                                                            // Dragged up into toolbar gap/space -> MOVE TO TOOLBAR!
                                                            currentOnMoveToToolbar(draggedAct)
                                                        } else if (currentHoveredDrawerAction != null && currentHoveredDrawerAction != draggedAct) {
                                                            // Hovered over another drawer option -> SWAP!
                                                            val fromIndex = currentDrawerTools.indexOf(draggedAct)
                                                            val toIndex = currentDrawerTools.indexOf(currentHoveredDrawerAction)
                                                            if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) {
                                                                val mutable = currentDrawerTools.toMutableList()
                                                                val target = mutable[toIndex]
                                                                mutable[toIndex] = draggedAct
                                                                mutable[fromIndex] = target
                                                                currentOnOrderChange(mutable.distinct())
                                                            }
                                                        } else if (accumulatedDragY > addThresholdPx) {
                                                            // Dragged down to empty gap at bottom of drawer -> PLACE AT END!
                                                            val fromIndex = currentDrawerTools.indexOf(draggedAct)
                                                            if (fromIndex != -1 && fromIndex != currentDrawerTools.lastIndex) {
                                                                val mutable = currentDrawerTools.toMutableList()
                                                                val moved = mutable.removeAt(fromIndex)
                                                                mutable.add(moved)
                                                                currentOnOrderChange(mutable.distinct())
                                                            }
                                                        }
                                                    }
                                                    draggingAction = null
                                                    accumulatedDragX = 0f
                                                    accumulatedDragY = 0f
                                                    currentOnDraggingStateChange(null, null)
                                                },
                                                onDragCancel = {
                                                    draggingAction = null
                                                    accumulatedDragX = 0f
                                                    accumulatedDragY = 0f
                                                    currentOnDraggingStateChange(null, null)
                                                }
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    ToolGridItem(
                                        tool = tool,
                                        isActive = isActionActive(tool.action),
                                        isDragging = isDraggingThisItem,
                                        isHovered = isHovered,
                                        rootCoordinates = rootCoordinates,
                                        onReportBounds = { bounds ->
                                            onReportItemBounds(action, bounds)
                                        },
                                        onCoordinatesPositioned = { coords ->
                                            itemCoordinates = coords
                                        },
                                        onClick = {
                                            if (draggingAction == null) {
                                                onToolClick(tool.action)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                    // Fill remainder of row to keep columns balanced
                    repeat(numColumns - rowActions.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // Generous bottom spacer so items at the bottom are fully and easily scrollable
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ToolGridItem(
    tool: DrawerTool,
    isActive: Boolean,
    isDragging: Boolean,
    isHovered: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rootCoordinates: LayoutCoordinates? = null,
    onReportBounds: ((Rect) -> Unit)? = null,
    onCoordinatesPositioned: ((LayoutCoordinates) -> Unit)? = null
) {
    val colorScheme = KiebordTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        // Tile container: Fixed 54dp compact squircle with keyBackground
        Box(
            modifier = Modifier
                .size(54.dp)
                .onGloballyPositioned { tileCoords ->
                    onCoordinatesPositioned?.invoke(tileCoords)
                    rootCoordinates?.let { root ->
                        if (tileCoords.isAttached && root.isAttached) {
                            onReportBounds?.invoke(root.localBoundingBoxOf(tileCoords))
                        }
                    }
                }
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when {
                        isHovered -> colorScheme.accentColor.copy(alpha = 0.25f)
                        isActive -> colorScheme.accentColor.copy(alpha = 0.20f)
                        else -> colorScheme.keyBackground
                    }
                )
                .border(
                    width = if (isDragging || isHovered || isActive) 1.5.dp else 0.dp,
                    color = when {
                        isHovered || isDragging || isActive -> colorScheme.accentColor
                        else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isHovered) Icons.Outlined.SwapHoriz else tool.icon,
                contentDescription = tool.label,
                tint = if (isDragging || isHovered || isActive) {
                    colorScheme.accentColor
                } else {
                    colorScheme.keyForeground
                },
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Label below the tile
        Text(
            text = if (isHovered) "Swap" else tool.label,
            color = if (isHovered || isActive) {
                colorScheme.accentColor
            } else {
                colorScheme.keyForeground
            },
            fontSize = 12.sp,
            fontWeight = if (isHovered || isActive) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
