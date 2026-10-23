package io.kiebord.feature.ime.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.kiebord.theme.KiebordTheme

enum class ToolbarAction {
    EMOJI,
    RESIZER,
    NUMBER_ROW_TOGGLE,
    THEME_TOGGLE,
    TEXT_EDIT
}

data class ToolbarItem(
    val action: ToolbarAction,
    val icon: ImageVector,
    val description: String
)

@Composable
fun ReorderableToolbar(
    onActionClick: (ToolbarAction) -> Unit,
    modifier: Modifier = Modifier,
    isEmojiActive: Boolean = false,
    isResizerActive: Boolean = false,
    isNumberRowActive: Boolean = false,
    isTextEditActive: Boolean = false
) {
    val colorScheme = KiebordTheme.colorScheme

    val items = listOf(
        ToolbarItem(ToolbarAction.EMOJI, Icons.Outlined.Mood, "Emojis"),
        ToolbarItem(ToolbarAction.RESIZER, Icons.Outlined.AspectRatio, "Resize Keyboard"),
        ToolbarItem(ToolbarAction.NUMBER_ROW_TOGGLE, Icons.Outlined.Pin, "Number Row"),
        ToolbarItem(ToolbarAction.THEME_TOGGLE, Icons.Outlined.Palette, "Toggle Theme"),
        ToolbarItem(ToolbarAction.TEXT_EDIT, Icons.Outlined.TextFields, "Text Edit")
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(colorScheme.containerBackground)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isActive = when (item.action) {
                ToolbarAction.EMOJI -> isEmojiActive
                ToolbarAction.RESIZER -> isResizerActive
                ToolbarAction.NUMBER_ROW_TOGGLE -> isNumberRowActive
                ToolbarAction.TEXT_EDIT -> isTextEditActive
                else -> false
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) colorScheme.accentColor.copy(alpha = 0.25f) else colorScheme.containerBackground
                    )
                    .clickable { onActionClick(item.action) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.description,
                    tint = if (isActive) colorScheme.accentColor else colorScheme.keyForeground.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
