package io.kiebord.feature.ime.ui.textedit

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.automirrored.outlined.LastPage
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FirstPage
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme

@Composable
fun TextEditPanelView(
    onTextAction: (TextAction, isSelecting: Boolean) -> Unit,
    onClose: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    val accentColor = colorScheme.accentColor
    var isSelecting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // 1. Header with Back button and "Text editing" title (matching Borno Keyboard)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = colorScheme.keyForeground,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "Text editing",
                color = colorScheme.keyForeground,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        }

        // 2. Main Editing Surface: Left D-Pad + Right Clipboard Actions (matching Borno Keyboard)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // A. Left Column: Directional Navigation D-Pad with Jump to Start/End and Center SELECT
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: [|< Start]  [↑ Up]  [>| End]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DpadIconKey(
                            icon = Icons.Outlined.FirstPage,
                            contentDescription = "Jump to Start",
                            onClick = { onTextAction(TextAction.LINE_START, isSelecting) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DpadIconKey(
                            icon = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            onClick = { onTextAction(TextAction.MOVE_UP, isSelecting) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DpadIconKey(
                            icon = Icons.AutoMirrored.Outlined.LastPage,
                            contentDescription = "Jump to End",
                            onClick = { onTextAction(TextAction.LINE_END, isSelecting) }
                        )
                    }
                }

                // Row 2: [← Left]  [SELECT / SELECTING]  [→ Right]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DpadIconKey(
                            icon = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = "Move Left",
                            onClick = { onTextAction(TextAction.MOVE_LEFT, isSelecting) }
                        )
                    }

                    // Center SELECT Toggle Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelecting) accentColor else colorScheme.actionKeyBackground)
                            .clickable { isSelecting = !isSelecting },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSelecting) "SELECTING" else "SELECT",
                            color = if (isSelecting) Color.White else colorScheme.keyForeground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DpadIconKey(
                            icon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = "Move Right",
                            onClick = { onTextAction(TextAction.MOVE_RIGHT, isSelecting) }
                        )
                    }
                }

                // Row 3: [ Spacer ]  [↓ Down]  [ Spacer ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        DpadIconKey(
                            icon = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            onClick = { onTextAction(TextAction.MOVE_DOWN, isSelecting) }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // B. Right Column: Clipboard & Editing Operations
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Select All
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    TextActionTile(
                        icon = Icons.Outlined.SelectAll,
                        label = "Select All",
                        onClick = { onTextAction(TextAction.SELECT_ALL, isSelecting) }
                    )
                }

                // Row 2: Cut & Copy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        TextActionTile(
                            icon = Icons.Outlined.ContentCut,
                            label = "Cut",
                            onClick = { onTextAction(TextAction.CUT, isSelecting) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        TextActionTile(
                            icon = Icons.Outlined.ContentCopy,
                            label = "Copy",
                            onClick = { onTextAction(TextAction.COPY, isSelecting) }
                        )
                    }
                }

                // Row 3: Paste
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    TextActionTile(
                        icon = Icons.Outlined.ContentPaste,
                        label = "Paste",
                        onClick = { onTextAction(TextAction.PASTE, isSelecting) }
                    )
                }

                // Row 4: Backspace & Enter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Backspace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.actionKeyBackground)
                            .clickable { onTextAction(TextAction.DELETE_BACKWARD, isSelecting) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                            contentDescription = "Backspace",
                            tint = colorScheme.keyForeground,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Enter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor)
                            .clickable { onTextAction(TextAction.ENTER, isSelecting) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardReturn,
                            contentDescription = "Enter",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DpadIconKey(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val colorScheme = KiebordTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.actionKeyBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colorScheme.keyForeground,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun TextActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.actionKeyBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colorScheme.keyForeground,
                modifier = Modifier.size(17.dp)
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
