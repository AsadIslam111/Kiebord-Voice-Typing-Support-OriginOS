package io.kiebord.feature.ime.ui.onehanded

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kiebord.theme.KiebordTheme

@Composable
fun OneHandedSideBar(
    oneHandedSide: String, // "LEFT" or "RIGHT"
    sideWidth: Dp,
    onAdjustPaddingDelta: (Float) -> Unit,
    onCyclePaddingPreset: () -> Unit,
    onExpand: () -> Unit,
    onSwitchDirection: () -> Unit,
    vibrationEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val btnSize = (sideWidth - 14.dp).coerceIn(38.dp, 44.dp)

    Box(
        modifier = modifier
            .width(sideWidth)
            .fillMaxHeight()
            .background(colorScheme.containerBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Padding Adjuster (= drag handle & tap to cycle preset)
            Box(
                modifier = Modifier
                    .size(btnSize)
                    .clip(CircleShape)
                    .background(colorScheme.keyBackground)
                    .pointerInput(oneHandedSide) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onAdjustPaddingDelta(dragAmount.x)
                            }
                        )
                    }
                    .clickable {
                        if (vibrationEnabled) {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } catch (_: Exception) {}
                        }
                        onCyclePaddingPreset()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.DragHandle,
                    contentDescription = "Adjust one-handed padding",
                    tint = colorScheme.keyForeground,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 2. Expand Button (restore full width)
            Box(
                modifier = Modifier
                    .size(btnSize)
                    .clip(CircleShape)
                    .background(colorScheme.keyBackground)
                    .clickable {
                        if (vibrationEnabled) {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } catch (_: Exception) {}
                        }
                        onExpand()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fullscreen,
                    contentDescription = "Expand keyboard to full width",
                    tint = colorScheme.keyForeground,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3. Direction Button (> or <)
            Box(
                modifier = Modifier
                    .size(btnSize)
                    .clip(CircleShape)
                    .background(colorScheme.keyBackground)
                    .clickable {
                        if (vibrationEnabled) {
                            try {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } catch (_: Exception) {}
                        }
                        onSwitchDirection()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (oneHandedSide == "LEFT") {
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight
                    } else {
                        Icons.AutoMirrored.Outlined.KeyboardArrowLeft
                    },
                    contentDescription = "Change one-handed direction",
                    tint = colorScheme.keyForeground,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
