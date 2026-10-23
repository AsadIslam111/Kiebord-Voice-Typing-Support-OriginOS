package io.kiebord.feature.ime.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme

@Composable
fun ToolsDrawerView(
    onToolClick: (ToolbarAction) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme

    val tools = listOf(
        Triple(ToolbarAction.TEXT_EDIT, Icons.Outlined.TextFields, "Text editing"),
        Triple(ToolbarAction.THEME_TOGGLE, Icons.Outlined.Palette, "Theme"),
        Triple(ToolbarAction.RESIZER, Icons.Outlined.AspectRatio, "Resize"),
        Triple(ToolbarAction.EMOJI, Icons.Outlined.Mood, "Emojis"),
        Triple(ToolbarAction.NUMBER_ROW_TOGGLE, Icons.Outlined.Pin, "Number row")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Hold and drag tools to reorder toolbar",
            color = colorScheme.keyForeground.copy(alpha = 0.5f),
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )

        // Row 1 (first 3 tools)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tools.take(3).forEach { (action, icon, label) ->
                ToolGridCard(
                    icon = icon,
                    label = label,
                    onClick = { onToolClick(action) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row 2 (remaining tools)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tools.drop(3).forEach { (action, icon, label) ->
                ToolGridCard(
                    icon = icon,
                    label = label,
                    onClick = { onToolClick(action) },
                    modifier = Modifier.weight(1f)
                )
            }
            // Empty placeholder box to balance the 3-column grid
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ToolGridCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.actionKeyBackground)
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colorScheme.keyForeground,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = label,
                color = colorScheme.keyForeground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
