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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.kiebord.feature.ime.R
import io.kiebord.theme.KiebordTheme
import io.kiebord.theme.ThemeColorUtils

data class DrawerTool(
    val action: ToolbarAction,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
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
    scaleFactor: Float = 1.0f,
    isLandscape: Boolean = false,
    isFloating: Boolean = false,
    isToolEnabled: (ToolbarAction) -> Boolean = { true },
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
            ToolbarAction.TEXT_EDIT to DrawerTool(ToolbarAction.TEXT_EDIT, iconResId = R.drawable.ic_text_edit, label = "Text editing"),
            ToolbarAction.CLIPBOARD to DrawerTool(ToolbarAction.CLIPBOARD, icon = Icons.AutoMirrored.Outlined.Assignment, label = "Clipboard"),
            ToolbarAction.THEME_TOGGLE to DrawerTool(ToolbarAction.THEME_TOGGLE, icon = Icons.Outlined.Palette, label = "Theme"),
            ToolbarAction.RESIZER to DrawerTool(ToolbarAction.RESIZER, iconResId = R.drawable.ic_resize, label = "Resize"),
            ToolbarAction.FLOATING to DrawerTool(ToolbarAction.FLOATING, iconResId = R.drawable.ic_floating_keyboard, label = "Floating"),
            ToolbarAction.NUMBER_ROW_TOGGLE to DrawerTool(ToolbarAction.NUMBER_ROW_TOGGLE, icon = Icons.Filled.Pin, label = "Number row"),
            ToolbarAction.SECONDARY_KEYS_TOGGLE to DrawerTool(ToolbarAction.SECONDARY_KEYS_TOGGLE, iconResId = R.drawable.ic_key_hints, label = "Key hints"),
            ToolbarAction.KEY_POPUP_TOGGLE to DrawerTool(ToolbarAction.KEY_POPUP_TOGGLE, iconResId = R.drawable.ic_key_popup, label = "Key popup"),
            ToolbarAction.EMOJI to DrawerTool(ToolbarAction.EMOJI, icon = Icons.Outlined.Mood, label = "Emoji"),
            ToolbarAction.ONE_HANDED to DrawerTool(ToolbarAction.ONE_HANDED, iconResId = R.drawable.ic_one_handed, label = "One-handed"),
            ToolbarAction.SETTINGS to DrawerTool(ToolbarAction.SETTINGS, icon = Icons.Filled.Settings, label = "Settings"),
            ToolbarAction.TRADITIONAL_MODE to DrawerTool(ToolbarAction.TRADITIONAL_MODE, iconResId = R.drawable.ic_traditional, label = "Traditional"),
            ToolbarAction.UNDO to DrawerTool(ToolbarAction.UNDO, iconResId = R.drawable.ic_undo, label = "Undo"),
            ToolbarAction.REDO to DrawerTool(ToolbarAction.REDO, iconResId = R.drawable.ic_redo, label = "Redo"),
            ToolbarAction.NUMBER_PAD to DrawerTool(ToolbarAction.NUMBER_PAD, icon = Icons.Outlined.Dialpad, label = "Number Pad")
        )
    }

    var draggingAction by remember { mutableStateOf<ToolbarAction?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(0) }
    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var itemStartOffsetInRoot by remember { mutableStateOf(Offset.Zero) }

    val addThresholdPx = with(density) { 30.dp.toPx() }

    val scrollState = rememberScrollState()

    val drawerBg = colorScheme.containerBackground.copy(alpha = 1f)

    val hPadding = (12 * scaleFactor).coerceIn(6f, 16f).dp
    val vPadding = (4 * scaleFactor).coerceIn(2f, 6f).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(drawerBg)
            .verticalScroll(scrollState)
            .padding(horizontal = hPadding, vertical = vPadding)
            .onGloballyPositioned { coords ->
                rootCoordinates?.let { root ->
                    if (coords.isAttached && root.isAttached) {
                        onReportContainerBounds(root.localBoundingBoxOf(coords))
                    }
                }
            },
        verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).coerceIn(4f, 10f).dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = (3 * scaleFactor).coerceIn(1f, 4f).dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hold and drag to customize",
                color = colorScheme.keyForeground.copy(alpha = 0.6f),
                fontSize = (11.5f * scaleFactor).coerceIn(9f, 12f).sp,
                fontWeight = FontWeight.Normal
            )
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val totalWidth = maxWidth
            val maxCols = if (!isFloating && !isLandscape) 4 else 6
            val targetColWidth = (78 * scaleFactor).coerceIn(52f, 86f).dp
            val numColumns = (totalWidth / targetColWidth).toInt().coerceIn(2, maxCols)
            val chunkedTools = drawerTools.chunked(numColumns)
            val colWidth = totalWidth / numColumns
            val tileWidth = (colWidth - (6 * scaleFactor).dp).coerceIn(
                (42 * scaleFactor).coerceAtLeast(34f).dp,
                (78 * scaleFactor).coerceAtLeast(48f).dp
            )
            val tileHeight = (42 * scaleFactor).coerceIn(28f, 48f).dp
            val tileRadius = (12 * scaleFactor).coerceIn(6f, 14f).dp

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((8 * scaleFactor).coerceIn(4f, 12f).dp)
            ) {
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
                                    val isEnabled = isToolEnabled(action)

                                    ToolGridItem(
                                        tool = tool,
                                        isActive = isActionActive(tool.action),
                                        isEnabled = isEnabled,
                                        isDragging = isDraggingThisItem,
                                        isHovered = isHovered,
                                        tileWidth = tileWidth,
                                        tileHeight = tileHeight,
                                        tileRadius = tileRadius,
                                        scaleFactor = scaleFactor,
                                        rootCoordinates = rootCoordinates,
                                        onReportBounds = { bounds ->
                                            onReportItemBounds(action, bounds)
                                        },
                                        onCoordinatesPositioned = { coords ->
                                            itemCoordinates = coords
                                        },
                                        onClick = {
                                            if (draggingAction == null && isEnabled) {
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
        }

        // Generous bottom spacer so items at the bottom are fully and easily scrollable
        Spacer(modifier = Modifier.height((20 * scaleFactor).coerceIn(12f, 28f).dp))
    }
}

@Composable
private fun ToolGridItem(
    tool: DrawerTool,
    isActive: Boolean,
    isEnabled: Boolean = true,
    isDragging: Boolean,
    isHovered: Boolean,
    tileWidth: Dp,
    tileHeight: Dp,
    tileRadius: Dp,
    scaleFactor: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rootCoordinates: LayoutCoordinates? = null,
    onReportBounds: ((Rect) -> Unit)? = null,
    onCoordinatesPositioned: ((LayoutCoordinates) -> Unit)? = null
) {
    val colorScheme = KiebordTheme.colorScheme
    val optionLabelColor = when {
        !isEnabled -> colorScheme.keyForeground.copy(alpha = 0.32f)
        isHovered -> colorScheme.accentColor
        isActive -> colorScheme.effectiveActiveOptionHighlight
        else -> ThemeColorUtils.ensureContrast(colorScheme.keyForeground, colorScheme.containerBackground)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = isEnabled,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(width = tileWidth, height = tileHeight)
                .onGloballyPositioned { tileCoords ->
                    onCoordinatesPositioned?.invoke(tileCoords)
                    rootCoordinates?.let { root ->
                        if (tileCoords.isAttached && root.isAttached) {
                            onReportBounds?.invoke(root.localBoundingBoxOf(tileCoords))
                        }
                    }
                }
                .clip(RoundedCornerShape(tileRadius))
                .background(
                    when {
                        !isEnabled -> colorScheme.keyBackground.copy(alpha = 0.38f)
                        isHovered -> colorScheme.accentColor.copy(alpha = 0.25f)
                        isActive -> colorScheme.effectiveActiveOptionHighlight.copy(alpha = 0.20f)
                        else -> colorScheme.keyBackground
                    }
                )
                .border(
                    width = if (isDragging || isHovered || isActive) 1.5.dp else 1.dp,
                    color = when {
                        !isEnabled -> colorScheme.keyForeground.copy(alpha = 0.08f)
                        isHovered || isDragging -> colorScheme.accentColor
                        isActive -> colorScheme.effectiveActiveOptionHighlight
                        else -> colorScheme.keyForeground.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(tileRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            val iconTint = when {
                !isEnabled -> colorScheme.keyForeground.copy(alpha = 0.32f)
                isDragging || isHovered -> colorScheme.accentColor
                isActive -> colorScheme.effectiveActiveOptionHighlight
                else -> colorScheme.keyForeground
            }
            val maxIconInTile = (tileHeight - 6.dp).value.coerceAtLeast(14f)
            val iconSize = ((if (tool.iconResId != null) 25.5f else 22f) * scaleFactor).coerceIn(14f, maxIconInTile).dp
            if (isHovered) {
                Icon(
                    imageVector = Icons.Outlined.SwapHoriz,
                    contentDescription = tool.label,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
            } else if (tool.iconResId != null) {
                Icon(
                    painter = painterResource(tool.iconResId),
                    contentDescription = tool.label,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
            } else if (tool.icon != null) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.label,
                    tint = iconTint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }

        Spacer(modifier = Modifier.height((4 * scaleFactor).coerceIn(2f, 6f).dp))

        // Label below the tile
        Text(
            text = if (isHovered) "Swap" else tool.label,
            color = optionLabelColor,
            fontSize = (11 * scaleFactor).coerceIn(8.5f, 12f).sp,
            lineHeight = (13 * scaleFactor).coerceIn(10f, 14f).sp,
            fontWeight = if (isHovered || isActive) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
