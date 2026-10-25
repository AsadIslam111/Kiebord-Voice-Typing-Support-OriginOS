package io.kiebord.feature.ime.ui.textedit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
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

    val buttonShape = RoundedCornerShape(12.dp)
    val navKeyBg = colorScheme.keyBackground
    val actionKeyBg = colorScheme.actionKeyBackground
    val buttonFg = colorScheme.keyForeground

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Header with back arrow circle and "Text editing" title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = buttonFg,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "Text editing",
                color = buttonFg,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp
            )
        }

        // 2. Upper Control Deck: 3 rows tall (Rows 1, 2, 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Column 1: Tall Left Chevron (<) spanning full 3-row height
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .clip(buttonShape)
                    .background(navKeyBg)
                    .clickable { onTextAction(TextAction.MOVE_LEFT, isSelecting) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = "Left",
                    tint = buttonFg,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Column 2: Center 3 buttons (Up ^, Select, Down v)
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Up (^)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(buttonShape)
                        .background(navKeyBg)
                        .clickable { onTextAction(TextAction.MOVE_UP, isSelecting) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Up",
                        tint = buttonFg,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Select
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(buttonShape)
                        .background(if (isSelecting) accentColor else navKeyBg)
                        .clickable {
                            isSelecting = !isSelecting
                            onTextAction(TextAction.SELECT, isSelecting)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select",
                        color = if (isSelecting) Color.White else buttonFg,
                        fontSize = 14.sp,
                        fontWeight = if (isSelecting) FontWeight.SemiBold else FontWeight.Normal
                    )
                }

                // Down (v)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(buttonShape)
                        .background(navKeyBg)
                        .clickable { onTextAction(TextAction.MOVE_DOWN, isSelecting) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Down",
                        tint = buttonFg,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Column 3: Tall Right Chevron (>) spanning full 3-row height
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxHeight()
                    .clip(buttonShape)
                    .background(navKeyBg)
                    .clickable { onTextAction(TextAction.MOVE_RIGHT, isSelecting) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = "Right",
                    tint = buttonFg,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Column 4: Right Actions (Select all, Copy, Paste)
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Select all
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(buttonShape)
                        .background(actionKeyBg)
                        .clickable {
                            isSelecting = true
                            onTextAction(TextAction.SELECT_ALL, true)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Select all",
                        color = buttonFg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Copy (dimmed if not selecting, active when selecting)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(buttonShape)
                        .background(actionKeyBg)
                        .clickable { onTextAction(TextAction.COPY, isSelecting) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Copy",
                        color = buttonFg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Paste
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(buttonShape)
                        .background(actionKeyBg)
                        .clickable { onTextAction(TextAction.PASTE, isSelecting) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Paste",
                        color = buttonFg,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        // 3. Lower Control Row (Row 4): [ |< ]  [ >| ]  [ ⌫ ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Jump to Start (|<) spanning width of Col 1 + half of Col 2
            Box(
                modifier = Modifier
                    .weight(1.55f)
                    .fillMaxHeight()
                    .clip(buttonShape)
                    .background(navKeyBg)
                    .clickable { onTextAction(TextAction.LINE_START, isSelecting) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "|<",
                    color = buttonFg,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Jump to End (>|) spanning width of half of Col 2 + Col 3
            Box(
                modifier = Modifier
                    .weight(1.55f)
                    .fillMaxHeight()
                    .clip(buttonShape)
                    .background(navKeyBg)
                    .clickable { onTextAction(TextAction.LINE_END, isSelecting) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ">|",
                    color = buttonFg,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Backspace key spanning width of Col 4
            Box(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .clip(buttonShape)
                    .background(actionKeyBg)
                    .clickable { onTextAction(TextAction.DELETE_BACKWARD, isSelecting) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = buttonFg,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
