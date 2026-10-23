package io.kiebord.feature.ime.ui.textedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.FirstPage
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LastPage
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TextEditPanelView(
    onTextAction: (TextAction, isSelecting: Boolean) -> Unit,
    onClose: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    getSurroundingText: () -> Triple<String, String, String> = { Triple("", "", "") }
) {
    val colorScheme = KiebordTheme.colorScheme
    val accentColor = colorScheme.accentColor
    var isSelecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var surrounding by remember { mutableStateOf(Triple("", "", "")) }

    val refreshSurrounding: () -> Unit = {
        scope.launch {
            delay(35)
            surrounding = getSurroundingText()
        }
    }

    LaunchedEffect(Unit) {
        surrounding = getSurroundingText()
    }

    val dispatch: (TextAction) -> Unit = { action ->
        onTextAction(action, isSelecting)
        refreshSurrounding()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        // 1. Header with Close and Title / Selection Status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to Keyboard",
                        tint = colorScheme.keyForeground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (isSelecting) "Text Editing (Selecting)" else "Text Editing",
                    color = if (isSelecting) accentColor else colorScheme.keyForeground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Quick Select Toggle Pill in Header
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelecting) accentColor else colorScheme.actionKeyBackground)
                    .clickable {
                        isSelecting = !isSelecting
                        refreshSurrounding()
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isSelecting) "SELECTING" else "SELECT",
                    color = if (isSelecting) Color.White else colorScheme.keyForeground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // 2. Caret Loupe: Live surrounding text & selection readout
        val (beforeText, selText, afterText) = surrounding
        if (beforeText.isNotEmpty() || selText.isNotEmpty() || afterText.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.keyBackground)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = beforeText.takeLast(25),
                        color = colorScheme.keyForeground.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    if (selText.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.35f))
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = selText.take(30),
                                color = accentColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(14.dp)
                                .background(accentColor)
                        )
                    }
                    Text(
                        text = afterText.take(25),
                        color = colorScheme.keyForeground.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // 3. Main Editing Surface: Left D-Pad + Right Clipboard Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // A. Left Column: Directional Navigation D-Pad & Word / Line jumps
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Directional D-Pad (3x3 grid)
                // Row 1: UP Arrow
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.actionKeyBackground)
                        .clickable { dispatch(TextAction.MOVE_UP) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Move Up",
                        tint = colorScheme.keyForeground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Row 2: LEFT | SELECT / CENTER | RIGHT
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT Arrow
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.actionKeyBackground)
                            .clickable { dispatch(TextAction.MOVE_LEFT) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowLeft,
                            contentDescription = "Move Left",
                            tint = colorScheme.keyForeground,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // CENTER: Select Anchor Toggle Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isSelecting) accentColor else colorScheme.keyBackground)
                            .clickable {
                                isSelecting = !isSelecting
                                refreshSurrounding()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSelecting) "SEL" else "SEL",
                            color = if (isSelecting) Color.White else colorScheme.keyForeground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // RIGHT Arrow
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.actionKeyBackground)
                            .clickable { dispatch(TextAction.MOVE_RIGHT) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowRight,
                            contentDescription = "Move Right",
                            tint = colorScheme.keyForeground,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Row 3: DOWN Arrow
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorScheme.actionKeyBackground)
                        .clickable { dispatch(TextAction.MOVE_DOWN) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Move Down",
                        tint = colorScheme.keyForeground,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Row 4: Boundary Jumps (|◀ Home, ◀◀ Word Left, ▶▶ Word Right, ▶| End)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavigationPillButton(
                        icon = Icons.Outlined.FirstPage,
                        label = "Start",
                        onClick = { dispatch(TextAction.LINE_START) }
                    )
                    NavigationPillButton(
                        icon = Icons.Outlined.FastRewind,
                        label = "Word ◀",
                        onClick = { dispatch(TextAction.WORD_LEFT) }
                    )
                    NavigationPillButton(
                        icon = Icons.Outlined.FastForward,
                        label = "▶ Word",
                        onClick = { dispatch(TextAction.WORD_RIGHT) }
                    )
                    NavigationPillButton(
                        icon = Icons.Outlined.LastPage,
                        label = "End",
                        onClick = { dispatch(TextAction.LINE_END) }
                    )
                }
            }

            // B. Right Column: Clipboard & Editing Operations
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Select All
                TextActionTile(
                    icon = Icons.Outlined.SelectAll,
                    label = "Select All",
                    onClick = { dispatch(TextAction.SELECT_ALL) }
                )

                // Cut & Copy Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        TextActionTile(
                            icon = Icons.Outlined.ContentCut,
                            label = "Cut",
                            onClick = { dispatch(TextAction.CUT) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        TextActionTile(
                            icon = Icons.Outlined.ContentCopy,
                            label = "Copy",
                            onClick = { dispatch(TextAction.COPY) }
                        )
                    }
                }

                // Paste
                TextActionTile(
                    icon = Icons.Outlined.ContentPaste,
                    label = "Paste",
                    onClick = { dispatch(TextAction.PASTE) }
                )

                // Backspace & Enter Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Backspace
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colorScheme.actionKeyBackground)
                            .clickable { dispatch(TextAction.DELETE_BACKWARD) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Backspace,
                            contentDescription = "Backspace",
                            tint = colorScheme.keyForeground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Enter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor)
                            .clickable { dispatch(TextAction.ENTER) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardReturn,
                            contentDescription = "Enter",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
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
            .height(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colorScheme.actionKeyBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colorScheme.keyForeground,
                modifier = Modifier.size(18.dp)
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

@Composable
private fun NavigationPillButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colorScheme = KiebordTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colorScheme.keyBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = colorScheme.keyForeground.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
    }
}
