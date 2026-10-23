package io.kiebord.feature.ime.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme

data class DrawerTool(
    val action: ToolbarAction,
    val icon: ImageVector,
    val label: String
)

@Composable
fun ToolsDrawerView(
    onToolClick: (ToolbarAction) -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme

    val tools = listOf(
        DrawerTool(ToolbarAction.SETTINGS, Icons.Outlined.Settings, "Settings"),
        DrawerTool(ToolbarAction.TEXT_EDIT, Icons.Outlined.TextFields, "Text editing"),
        DrawerTool(ToolbarAction.SHARE, Icons.Outlined.Share, "Share Borno Keyboard"),
        DrawerTool(ToolbarAction.ONE_HANDED, Icons.Outlined.Smartphone, "One-handed"),
        DrawerTool(ToolbarAction.FLOATING, Icons.Outlined.PictureInPicture, "Floating"),
        DrawerTool(ToolbarAction.RESIZER, Icons.Outlined.CropFree, "Resize"),
        DrawerTool(ToolbarAction.EMOJI, Icons.Outlined.Mood, "Emoji"),
        DrawerTool(ToolbarAction.TRANSLATE, Icons.Outlined.Translate, "Translate"),
        DrawerTool(ToolbarAction.THEME_TOGGLE, Icons.Outlined.Palette, "Theme"),
        DrawerTool(ToolbarAction.NUMBER_ROW_TOGGLE, Icons.Outlined.Pin, "Number row"),
        DrawerTool(ToolbarAction.CLIPBOARD, Icons.Outlined.Assignment, "Clipboard")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Subtitle centered: "Hold and drag to customize"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Hold and drag to customize",
                color = colorScheme.keyForeground.copy(alpha = 0.9f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 4-Column Grid: Sleek icon tiles with labels outside below (not big chunky blocks)
        val chunkedTools = tools.chunked(4)
        chunkedTools.forEach { rowTools ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowTools.forEach { tool ->
                    ToolGridItem(
                        tool = tool,
                        onClick = { onToolClick(tool.action) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remainder of row to keep columns aligned
                repeat(4 - rowTools.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ToolGridItem(
    tool: DrawerTool,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        // Tile container with ONLY the icon inside
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.actionKeyBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = tool.label,
                tint = colorScheme.keyForeground,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

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
