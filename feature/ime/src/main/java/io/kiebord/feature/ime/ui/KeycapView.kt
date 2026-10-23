package io.kiebord.feature.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.Language
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.ShiftState
import io.kiebord.theme.KiebordTheme

@Composable
fun KeycapView(
    key: KeyDef,
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit = {}
) {
    val colorScheme = KiebordTheme.colorScheme
    var isPressed by remember { mutableStateOf(false) }

    // Dynamic key background & foreground colors driven by configurable theme
    val isActionOrModifier = key.type == KeyType.SHIFT || key.type == KeyType.DELETE || key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS
    val baseBackground = when {
        key.type == KeyType.ACTION -> colorScheme.actionKeyBackground
        isActionOrModifier -> colorScheme.actionKeyBackground
        else -> colorScheme.keyBackground
    }

    val activeBackground = if (isPressed) {
        colorScheme.accentColor.copy(alpha = 0.35f)
    } else {
        baseBackground
    }

    val foregroundColor = when {
        key.type == KeyType.SHIFT && shiftState != ShiftState.UNSHIFTED -> colorScheme.shiftActiveColor
        key.type == KeyType.ACTION -> colorScheme.actionKeyForeground
        else -> colorScheme.keyForeground
    }

    Box(
        modifier = modifier
            .padding(horizontal = 3.dp, vertical = 4.dp)
            .shadow(elevation = if (isPressed) 0.dp else 1.5.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(activeBackground)
            .pointerInput(key, shiftState) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onKeyTap(key) },
                    onLongPress = { onKeyLongPress(key) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Flick up preview badge in top right corner (if present)
        if (!key.flickUp.isNullOrEmpty() && key.type == KeyType.CHARACTER) {
            Text(
                text = key.flickUp!!,
                color = colorScheme.keyForeground.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
        }

        // Key content: Vector Material Icons vs Typography label
        when (key.type) {
            KeyType.SHIFT -> {
                when (shiftState) {
                    ShiftState.UNSHIFTED -> {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = "Shift",
                            tint = foregroundColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    ShiftState.ONE_SHOT -> {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Shift One-Shot",
                            tint = colorScheme.shiftActiveColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    ShiftState.CAPS_LOCK -> {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = "Caps Lock",
                                tint = colorScheme.shiftActiveColor,
                                modifier = Modifier.size(22.dp)
                            )
                            // Underline bar for Caps Lock
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(width = 16.dp, height = 2.dp)
                                    .background(colorScheme.shiftActiveColor)
                            )
                        }
                    }
                }
            }
            KeyType.DELETE -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = foregroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            KeyType.ACTION -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = foregroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            KeyType.SWITCH_LANGUAGE -> {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = "Switch Language",
                    tint = foregroundColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            KeyType.SPACE -> {
                Text(
                    text = key.displayLabel(shiftState).ifEmpty { "English" },
                    color = colorScheme.keyForeground.copy(alpha = 0.4f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            else -> {
                Text(
                    text = key.displayLabel(shiftState),
                    color = foregroundColor,
                    fontSize = if (key.type == KeyType.SYMBOLS) 14.sp else 19.sp,
                    fontWeight = if (key.type == KeyType.SYMBOLS) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
