package io.kiebord.feature.ime.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.kiebord.theme.KiebordTheme

enum class ToolbarAction {
    TEXT_EDIT,
    EMOJI,
    RESIZER,
    NUMBER_ROW_TOGGLE,
    THEME_TOGGLE
}

data class ToolbarItem(
    val action: ToolbarAction,
    val icon: ImageVector,
    val description: String
)

@Composable
fun ReorderableToolbar(
    onActionClick: (ToolbarAction) -> Unit,
    onExpandToolsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isExpandActive: Boolean = false,
    isEmojiActive: Boolean = false,
    isResizerActive: Boolean = false,
    isNumberRowActive: Boolean = false,
    isTextEditActive: Boolean = false
) {
    val colorScheme = KiebordTheme.colorScheme
    val density = LocalDensity.current
    val itemSlotWidthPx = with(density) { 44.dp.toPx() }

    val allActionMap = remember {
        mapOf(
            ToolbarAction.TEXT_EDIT to ToolbarItem(ToolbarAction.TEXT_EDIT, Icons.Outlined.TextFields, "Text Edit"),
            ToolbarAction.EMOJI to ToolbarItem(ToolbarAction.EMOJI, Icons.Outlined.Mood, "Emojis"),
            ToolbarAction.RESIZER to ToolbarItem(ToolbarAction.RESIZER, Icons.Outlined.AspectRatio, "Resize Keyboard"),
            ToolbarAction.NUMBER_ROW_TOGGLE to ToolbarItem(ToolbarAction.NUMBER_ROW_TOGGLE, Icons.Outlined.Pin, "Number Row"),
            ToolbarAction.THEME_TOGGLE to ToolbarItem(ToolbarAction.THEME_TOGGLE, Icons.Outlined.Palette, "Toggle Theme")
        )
    }

    var toolOrder by remember {
        mutableStateOf(
            listOf(
                ToolbarAction.TEXT_EDIT,
                ToolbarAction.EMOJI,
                ToolbarAction.RESIZER,
                ToolbarAction.NUMBER_ROW_TOGGLE,
                ToolbarAction.THEME_TOGGLE
            )
        )
    }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Expand / Tools Drawer Icon Button (4-squares Grid, matching Borno Keyboard)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isExpandActive) colorScheme.accentColor.copy(alpha = 0.25f) else Color.Transparent
                )
                .clickable(onClick = onExpandToolsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.GridView,
                contentDescription = "All Tools",
                tint = if (isExpandActive) colorScheme.accentColor else colorScheme.keyForeground.copy(alpha = 0.85f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(22.dp)
                .background(colorScheme.keyForeground.copy(alpha = 0.15f))
        )

        // 2. Horizontal Scrollable & Long-Press Reorderable Toolbar Items
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            toolOrder.forEachIndexed { index, action ->
                val item = allActionMap[action] ?: return@forEachIndexed
                val isActive = when (action) {
                    ToolbarAction.EMOJI -> isEmojiActive
                    ToolbarAction.RESIZER -> isResizerActive
                    ToolbarAction.NUMBER_ROW_TOGGLE -> isNumberRowActive
                    ToolbarAction.TEXT_EDIT -> isTextEditActive
                    else -> false
                }
                val isDragging = draggingIndex == index

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .zIndex(if (isDragging) 10f else 1f)
                        .graphicsLayer {
                            translationX = if (isDragging) dragOffsetX else 0f
                            scaleX = if (isDragging) 1.18f else 1f
                            scaleY = if (isDragging) 1.18f else 1f
                            shadowElevation = if (isDragging) 12f else 0f
                        }
                        .clip(CircleShape)
                        .background(
                            when {
                                isDragging -> colorScheme.accentColor.copy(alpha = 0.35f)
                                isActive -> colorScheme.accentColor.copy(alpha = 0.25f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable { onActionClick(item.action) }
                        .pointerInput(toolOrder) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffsetX = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount.x
                                    val cur = draggingIndex ?: return@detectDragGesturesAfterLongPress

                                    // Check swap with right neighbor
                                    if (dragOffsetX > itemSlotWidthPx * 0.55f && cur < toolOrder.lastIndex) {
                                        val mutable = toolOrder.toMutableList()
                                        val moved = mutable.removeAt(cur)
                                        mutable.add(cur + 1, moved)
                                        toolOrder = mutable
                                        draggingIndex = cur + 1
                                        dragOffsetX -= itemSlotWidthPx
                                    }
                                    // Check swap with left neighbor
                                    else if (dragOffsetX < -itemSlotWidthPx * 0.55f && cur > 0) {
                                        val mutable = toolOrder.toMutableList()
                                        val moved = mutable.removeAt(cur)
                                        mutable.add(cur - 1, moved)
                                        toolOrder = mutable
                                        draggingIndex = cur - 1
                                        dragOffsetX += itemSlotWidthPx
                                    }
                                },
                                onDragEnd = {
                                    draggingIndex = null
                                    dragOffsetX = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffsetX = 0f
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
                            else -> colorScheme.keyForeground.copy(alpha = 0.8f)
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
