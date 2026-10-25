package io.kiebord.feature.ime.ui.toolbar

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
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
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.kiebord.theme.KiebordTheme

enum class ToolbarAction {
    TEXT_EDIT,
    CLIPBOARD,
    THEME_TOGGLE,
    RESIZER,
    NUMBER_ROW_TOGGLE,
    SECONDARY_KEYS_TOGGLE,
    KEY_POPUP_TOGGLE,
    EMOJI,
    ONE_HANDED,
    SETTINGS,
    TRADITIONAL_MODE,
    UNDO,
    REDO
}

data class ToolbarItem(
    val action: ToolbarAction,
    val icon: ImageVector,
    val description: String
)

@Composable
fun ReorderableToolbar(
    pinnedTools: List<ToolbarAction>,
    onPinnedToolsChange: (List<ToolbarAction>) -> Unit,
    onInterchangeWithDrawer: (toolbarAction: ToolbarAction, drawerAction: ToolbarAction) -> Unit,
    onMoveToDrawer: (toolbarAction: ToolbarAction) -> Unit,
    onActionClick: (ToolbarAction) -> Unit,
    rootCoordinates: LayoutCoordinates?,
    onReportItemBounds: (ToolbarAction, Rect) -> Unit,
    hoveredDrawerAction: ToolbarAction?,
    isOverDrawerBlankSpace: Boolean,
    hoveredByDrawerAction: ToolbarAction?,
    modifier: Modifier = Modifier,
    isActionActive: ((ToolbarAction) -> Boolean)? = null,
    isEmojiActive: Boolean = false,
    isResizerActive: Boolean = false,
    isNumberRowActive: Boolean = false,
    isSecondaryKeysActive: Boolean = false,
    isOneHandedActive: Boolean = false,
    isTextEditActive: Boolean = false,
    isClipboardActive: Boolean = false,
    isTraditionalModeActive: Boolean = false,
    isCustomizeMode: Boolean = false,
    scaleFactor: Float = 1.0f,
    onDraggingStateChange: (draggingAction: ToolbarAction?, fingerPosInRoot: Offset?) -> Unit = { _, _ -> }
) {
    val colorScheme = KiebordTheme.colorScheme
    val density = LocalDensity.current

    val currentPinnedTools by rememberUpdatedState(pinnedTools)
    val currentOnPinnedChange by rememberUpdatedState(onPinnedToolsChange)
    val currentOnInterchange by rememberUpdatedState(onInterchangeWithDrawer)
    val currentOnMoveToDrawer by rememberUpdatedState(onMoveToDrawer)
    val currentOnDraggingStateChange by rememberUpdatedState(onDraggingStateChange)
    val currentHoveredDrawerAction by rememberUpdatedState(hoveredDrawerAction)
    val currentIsOverBlankSpace by rememberUpdatedState(isOverDrawerBlankSpace)

    val allActionMap = remember {
        mapOf(
            ToolbarAction.TEXT_EDIT to ToolbarItem(ToolbarAction.TEXT_EDIT, Icons.Outlined.TextFields, "Text Edit"),
            ToolbarAction.CLIPBOARD to ToolbarItem(ToolbarAction.CLIPBOARD, Icons.AutoMirrored.Outlined.Assignment, "Clipboard"),
            ToolbarAction.THEME_TOGGLE to ToolbarItem(ToolbarAction.THEME_TOGGLE, Icons.Outlined.Palette, "Themes"),
            ToolbarAction.RESIZER to ToolbarItem(ToolbarAction.RESIZER, Icons.Outlined.CropFree, "Resize Keyboard"),
            ToolbarAction.NUMBER_ROW_TOGGLE to ToolbarItem(ToolbarAction.NUMBER_ROW_TOGGLE, Icons.Outlined.Pin, "Number Row"),
            ToolbarAction.SECONDARY_KEYS_TOGGLE to ToolbarItem(ToolbarAction.SECONDARY_KEYS_TOGGLE, Icons.Outlined.Layers, "Key Hints"),
            ToolbarAction.KEY_POPUP_TOGGLE to ToolbarItem(ToolbarAction.KEY_POPUP_TOGGLE, Icons.Outlined.SmartButton, "Key Popup"),
            ToolbarAction.EMOJI to ToolbarItem(ToolbarAction.EMOJI, Icons.Outlined.Mood, "Emojis"),
            ToolbarAction.ONE_HANDED to ToolbarItem(ToolbarAction.ONE_HANDED, Icons.Outlined.TouchApp, "One-Handed"),
            ToolbarAction.SETTINGS to ToolbarItem(ToolbarAction.SETTINGS, Icons.Outlined.Settings, "Settings"),
            ToolbarAction.TRADITIONAL_MODE to ToolbarItem(ToolbarAction.TRADITIONAL_MODE, Icons.Outlined.Translate, "Traditional Mode"),
            ToolbarAction.UNDO to ToolbarItem(ToolbarAction.UNDO, Icons.AutoMirrored.Outlined.Undo, "Undo"),
            ToolbarAction.REDO to ToolbarItem(ToolbarAction.REDO, Icons.AutoMirrored.Outlined.Redo, "Redo")
        )
    }

    var draggingAction by remember { mutableStateOf<ToolbarAction?>(null) }
    var dragStartIndex by remember { mutableIntStateOf(0) }
    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }
    var itemStartOffsetInRoot by remember { mutableStateOf(Offset.Zero) }
    var toolbarWidthPx by remember { mutableFloatStateOf(0f) }

    val removeThresholdPx = with(density) { 36.dp.toPx() }
    val isOverRemoveThreshold = draggingAction != null && accumulatedDragY > removeThresholdPx
    val isDraggingInsideToolbar = draggingAction != null && !isOverRemoveThreshold

    val slotWidthPx = if (pinnedTools.isNotEmpty() && toolbarWidthPx > 0f) {
        toolbarWidthPx / pinnedTools.size
    } else {
        with(density) { 58.dp.toPx() }
    }

    val hoveredSlot = if (draggingAction != null && slotWidthPx > 0f) {
        val fingerCenterX = (dragStartIndex + 0.5f) * slotWidthPx + accumulatedDragX
        (fingerCenterX / slotWidthPx).toInt().coerceIn(0, pinnedTools.lastIndex)
    } else {
        dragStartIndex
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .onGloballyPositioned { coords ->
                toolbarWidthPx = coords.size.width.toFloat()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        pinnedTools.forEachIndexed { index, action ->
            val item = allActionMap[action]
            if (item != null) {
                key(action) {
                    val isActive = isActionActive?.invoke(action) ?: when (action) {
                        ToolbarAction.EMOJI -> isEmojiActive
                        ToolbarAction.RESIZER -> isResizerActive
                        ToolbarAction.NUMBER_ROW_TOGGLE -> isNumberRowActive
                        ToolbarAction.SECONDARY_KEYS_TOGGLE -> isSecondaryKeysActive
                        ToolbarAction.ONE_HANDED -> isOneHandedActive
                        ToolbarAction.TEXT_EDIT -> isTextEditActive
                        ToolbarAction.CLIPBOARD -> isClipboardActive
                        ToolbarAction.TRADITIONAL_MODE -> isTraditionalModeActive
                        else -> false
                    }
                    val isDraggingThisItem = draggingAction == action
                val isHoveredByDrawerItem = hoveredByDrawerAction == action

                // Smooth animated transition for non-dragged items to slide aside!
                val targetShiftX = when {
                    !isDraggingInsideToolbar -> 0f
                    isDraggingThisItem -> 0f
                    hoveredSlot > dragStartIndex && index in (dragStartIndex + 1)..hoveredSlot -> -slotWidthPx
                    hoveredSlot < dragStartIndex && index in hoveredSlot until dragStartIndex -> slotWidthPx
                    else -> 0f
                }

                val animatedShiftX by animateFloatAsState(
                    targetValue = targetShiftX,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "shiftX_$action"
                )

                // Dragged item tracks finger continuously; other items slide smoothly
                val visualTranslationX = if (isDraggingThisItem) accumulatedDragX else animatedShiftX
                val visualTranslationY = if (isDraggingThisItem) accumulatedDragY else 0f

                val isDropTargetSlot = isDraggingInsideToolbar && index == hoveredSlot

                var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .onGloballyPositioned { coords ->
                            itemCoordinates = coords
                            rootCoordinates?.let { root ->
                                if (coords.isAttached && root.isAttached) {
                                    onReportItemBounds(action, root.localBoundingBoxOf(coords))
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val tileWidth = (if (isCustomizeMode) 42.dp else 36.dp) * scaleFactor
                    val tileHeight = (34 * scaleFactor).dp
                    val tileShape = RoundedCornerShape((10 * scaleFactor).coerceAtLeast(4f).dp)

                    // The Tool Tile
                    Box(
                        modifier = Modifier
                            .size(tileWidth, tileHeight)
                            .zIndex(if (isDraggingThisItem) 100f else 1f)
                            .graphicsLayer {
                                translationX = visualTranslationX
                                translationY = visualTranslationY
                                scaleX = if (isDraggingThisItem || isHoveredByDrawerItem) 1.08f else 1f
                                scaleY = if (isDraggingThisItem || isHoveredByDrawerItem) 1.08f else 1f
                            }
                            .clip(tileShape)
                            .background(
                                when {
                                    isHoveredByDrawerItem -> colorScheme.accentColor.copy(alpha = 0.35f)
                                    isDraggingThisItem -> colorScheme.keyBackground
                                    isActive -> colorScheme.accentColor.copy(alpha = 0.20f)
                                    isCustomizeMode -> colorScheme.keyBackground
                                    else -> Color.Transparent
                                }
                            )
                            .border(
                                width = if (isDraggingThisItem || isHoveredByDrawerItem || isActive) (1.5f * scaleFactor).coerceAtLeast(1f).dp else 0.dp,
                                color = when {
                                    isHoveredByDrawerItem || isDraggingThisItem || isActive -> colorScheme.accentColor
                                    else -> Color.Transparent
                                },
                                shape = tileShape
                            )
                            .pointerInput(action) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { localOffset ->
                                        draggingAction = action
                                        dragStartIndex = currentPinnedTools.indexOf(action)
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
                                            if (currentHoveredDrawerAction != null && currentHoveredDrawerAction != draggedAct) {
                                                // Dragged directly over a specific drawer item -> SWAP!
                                                currentOnInterchange(draggedAct, currentHoveredDrawerAction!!)
                                            } else if (accumulatedDragY > removeThresholdPx) {
                                                // Dragged down to drawer gap or blank space -> PLACE IN DRAWER!
                                                currentOnMoveToDrawer(draggedAct)
                                            } else {
                                                val fromIndex = currentPinnedTools.indexOf(draggedAct)
                                                if (fromIndex != -1 && hoveredSlot in 0..currentPinnedTools.lastIndex && fromIndex != hoveredSlot) {
                                                    // Reorder within toolbar with safe distinct list
                                                    val mutable = currentPinnedTools.toMutableList()
                                                    val moved = mutable.removeAt(fromIndex)
                                                    mutable.add(hoveredSlot, moved)
                                                    currentOnPinnedChange(mutable.distinct())
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
                            }
                            .clickable(enabled = draggingAction == null) {
                                onActionClick(item.action)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isHoveredByDrawerItem) Icons.Outlined.SwapHoriz else item.icon,
                            contentDescription = item.description,
                            tint = when {
                                isDraggingThisItem || isActive || isHoveredByDrawerItem -> colorScheme.accentColor
                                else -> colorScheme.keyForeground.copy(alpha = 0.85f)
                            },
                            modifier = Modifier.size((22 * scaleFactor).coerceAtLeast(15f).dp)
                        )
                    }
                }
            }
        }
    }
}
}
