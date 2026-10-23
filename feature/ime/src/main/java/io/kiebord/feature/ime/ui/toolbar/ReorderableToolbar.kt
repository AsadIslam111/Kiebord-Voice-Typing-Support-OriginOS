package io.kiebord.feature.ime.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.kiebord.theme.KiebordTheme

enum class ToolbarAction {
    TEXT_EDIT,
    EMOJI,
    RESIZER,
    NUMBER_ROW_TOGGLE,
    THEME_TOGGLE,
    CLIPBOARD
}

data class ToolbarItem(
    val action: ToolbarAction,
    val icon: ImageVector,
    val description: String
)

@Composable
fun ReorderableToolbar(
    toolOrder: List<ToolbarAction>,
    onToolOrderChange: (List<ToolbarAction>) -> Unit,
    onActionClick: (ToolbarAction) -> Unit,
    onExpandToolsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpandActive: Boolean = false,
    isEmojiActive: Boolean = false,
    isResizerActive: Boolean = false,
    isNumberRowActive: Boolean = false,
    isTextEditActive: Boolean = false,
    isClipboardActive: Boolean = false
) {
    val colorScheme = KiebordTheme.colorScheme
    val density = LocalDensity.current

    val allActionMap = remember {
        mapOf(
            ToolbarAction.TEXT_EDIT to ToolbarItem(ToolbarAction.TEXT_EDIT, Icons.Outlined.TextFields, "Text Edit"),
            ToolbarAction.EMOJI to ToolbarItem(ToolbarAction.EMOJI, Icons.Outlined.Mood, "Emojis"),
            ToolbarAction.RESIZER to ToolbarItem(ToolbarAction.RESIZER, Icons.Outlined.CropFree, "Resize Keyboard"),
            ToolbarAction.NUMBER_ROW_TOGGLE to ToolbarItem(ToolbarAction.NUMBER_ROW_TOGGLE, Icons.Outlined.Pin, "Number Row"),
            ToolbarAction.THEME_TOGGLE to ToolbarItem(ToolbarAction.THEME_TOGGLE, Icons.Outlined.Palette, "Toggle Theme"),
            ToolbarAction.CLIPBOARD to ToolbarItem(ToolbarAction.CLIPBOARD, Icons.AutoMirrored.Outlined.Assignment, "Clipboard")
        )
    }

    var draggingAction by remember { mutableStateOf<ToolbarAction?>(null) }
    var dragOriginIndex by remember { mutableIntStateOf(0) }
    var currentSlotIndex by remember { mutableIntStateOf(0) }
    var accumulatedDragX by remember { mutableFloatStateOf(0f) }
    var toolbarWidthPx by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Expand / Back button on far left
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isExpandActive) Color(0x22FFFFFF) else Color.Transparent
                )
                .clickable(onClick = onExpandToolsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isExpandActive) Icons.AutoMirrored.Outlined.ArrowBack else Icons.Outlined.GridView,
                contentDescription = if (isExpandActive) "Close Tools" else "All Tools",
                tint = if (isExpandActive) colorScheme.keyForeground else colorScheme.keyForeground.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // 2. Toolbar items SPREAD across the entire width (freely customizable & draggable)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .onGloballyPositioned { coords ->
                    toolbarWidthPx = coords.size.width.toFloat()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            val slotWidthPx = if (toolOrder.isNotEmpty() && toolbarWidthPx > 0f) {
                toolbarWidthPx / toolOrder.size
            } else {
                with(density) { 54.dp.toPx() }
            }

            toolOrder.forEachIndexed { index, action ->
                val item = allActionMap[action] ?: return@forEachIndexed
                val isActive = when (action) {
                    ToolbarAction.EMOJI -> isEmojiActive
                    ToolbarAction.RESIZER -> isResizerActive
                    ToolbarAction.NUMBER_ROW_TOGGLE -> isNumberRowActive
                    ToolbarAction.TEXT_EDIT -> isTextEditActive
                    ToolbarAction.CLIPBOARD -> isClipboardActive
                    else -> false
                }
                val isDragging = draggingAction == action

                // Continuous translation: floating tile follows the finger smoothly anywhere
                val visualTranslationX = if (isDragging) {
                    accumulatedDragX - (currentSlotIndex - dragOriginIndex) * slotWidthPx
                } else {
                    0f
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isExpandActive) 44.dp else 38.dp, 36.dp)
                            .zIndex(if (isDragging) 10f else 1f)
                            .graphicsLayer {
                                translationX = visualTranslationX
                                scaleX = if (isDragging) 1.18f else 1f
                                scaleY = if (isDragging) 1.18f else 1f
                                shadowElevation = if (isDragging) 14f else 0f
                            }
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isDragging -> colorScheme.accentColor.copy(alpha = 0.35f)
                                    isActive -> colorScheme.accentColor.copy(alpha = 0.25f)
                                    isExpandActive -> colorScheme.actionKeyBackground
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { onActionClick(item.action) }
                            .pointerInput(toolOrder) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingAction = action
                                        dragOriginIndex = index
                                        currentSlotIndex = index
                                        accumulatedDragX = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        accumulatedDragX += dragAmount.x

                                        if (slotWidthPx > 0f) {
                                            val slotShift = Math.round(accumulatedDragX / slotWidthPx).toInt()
                                            val targetIndex = (dragOriginIndex + slotShift).coerceIn(0, toolOrder.lastIndex)

                                            if (targetIndex != currentSlotIndex && targetIndex in toolOrder.indices) {
                                                val mutable = toolOrder.toMutableList()
                                                val moved = mutable.removeAt(currentSlotIndex)
                                                mutable.add(targetIndex, moved)
                                                onToolOrderChange(mutable)
                                                currentSlotIndex = targetIndex
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggingAction = null
                                        accumulatedDragX = 0f
                                    },
                                    onDragCancel = {
                                        draggingAction = null
                                        accumulatedDragX = 0f
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.description,
                            tint = when {
                                isDragging || isActive -> colorScheme.accentColor
                                else -> colorScheme.keyForeground.copy(alpha = 0.85f)
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
