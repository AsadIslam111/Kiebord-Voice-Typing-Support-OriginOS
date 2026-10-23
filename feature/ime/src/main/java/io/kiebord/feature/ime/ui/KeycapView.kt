package io.kiebord.feature.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.ShiftState
import io.kiebord.theme.KiebordTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun KeycapView(
    key: KeyDef,
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    isBottomRow: Boolean = false,
    isSlidingSymbolMode: Boolean = false,
    showKeyBorders: Boolean = true,
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit = {},
    onKeyPositioned: (KeyDef, LayoutCoordinates) -> Unit = { _, _ -> },
    onShowMoreKeys: (KeyDef, LayoutCoordinates) -> Unit = { _, _ -> },
    onMoreKeysHover: (LayoutCoordinates, Offset) -> Unit = { _, _ -> },
    onMoreKeysCommit: () -> Unit = {},
    onSlidingDragStart: (KeyDef, LayoutCoordinates, Offset) -> Unit = { _, _, _ -> },
    onSlidingDragMove: (Offset) -> Unit = {},
    onSlidingDragEnd: (Offset) -> Unit = {},
    onSpacebarTrackpad: (Int) -> Unit = {},
    onDeleteSwipeMove: (Float) -> Unit = {},
    onDeleteSwipeEnd: () -> Unit = {},
    onDeleteSwipeCancel: () -> Unit = {},
    onGestureStart: (LayoutCoordinates, Offset) -> Unit = { _, _ -> },
    onGestureMove: (LayoutCoordinates, Offset) -> Unit = { _, _ -> },
    onGestureEnd: () -> Unit = {}
) {
    val colorScheme = KiebordTheme.colorScheme
    val density = LocalDensity.current
    var isPressed by remember { mutableStateOf(false) }
    var keyCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    // Dynamic key background & foreground colors driven by configurable theme
    // When showKeyBorders is false (flat theme): key color is same as containerBackground
    // EXCEPT for Space bar, Sym key, and Enter key
    val isPillColoredKey = key.type == KeyType.SPACE || key.type == KeyType.SYMBOLS || key.type == KeyType.ACTION
    val isActionOrModifier = key.type == KeyType.SHIFT || key.type == KeyType.DELETE || key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS || key.type == KeyType.SWITCH_LANGUAGE
    val baseBackground = when {
        key.type == KeyType.ACTION -> colorScheme.accentColor
        key.type == KeyType.SYMBOLS -> colorScheme.actionKeyBackground
        key.type == KeyType.SPACE -> colorScheme.keyBackground
        showKeyBorders -> {
            if (isActionOrModifier) colorScheme.actionKeyBackground else colorScheme.keyBackground
        }
        else -> colorScheme.containerBackground
    }

    val activeBackground = if (isPressed) {
        if (key.type == KeyType.ACTION) colorScheme.accentColor.copy(alpha = 0.8f) else colorScheme.accentColor.copy(alpha = 0.28f)
    } else {
        baseBackground
    }

    val foregroundColor = when {
        key.type == KeyType.SHIFT && shiftState != ShiftState.UNSHIFTED -> colorScheme.shiftActiveColor
        key.type == KeyType.ACTION -> androidx.compose.ui.graphics.Color.White
        else -> colorScheme.keyForeground
    }

    val displaySymbolLabel = key.displayLabel(shiftState)

    // Key shape: stadium pills for bottom row action keys; standard for all others
    val isPillKey = isBottomRow && (isPillColoredKey || key.type == KeyType.SWITCH_LANGUAGE)
    val keyShape = when {
        isPillKey -> RoundedCornerShape(20.dp)
        else -> RoundedCornerShape(8.dp)
    }

    // In flat theme (!showKeyBorders), reduce height of Space bar, Sym key, and Enter key
    val isReducedHeightKey = !showKeyBorders && isPillColoredKey
    val verticalPadding = if (isReducedHeightKey) 7.dp else 3.dp

    // Capture latest parameters in updated states to allow pointerInput(Unit) without gesture cancellation
    val currentKey = rememberUpdatedState(key)
    val currentOnTap = rememberUpdatedState(onKeyTap)
    val currentOnLongPress = rememberUpdatedState(onKeyLongPress)
    val currentOnDeleteSwipeMove = rememberUpdatedState(onDeleteSwipeMove)
    val currentOnDeleteSwipeEnd = rememberUpdatedState(onDeleteSwipeEnd)
    val currentOnDeleteSwipeCancel = rememberUpdatedState(onDeleteSwipeCancel)
    val currentOnShowMoreKeys = rememberUpdatedState(onShowMoreKeys)
    val currentOnMoreKeysHover = rememberUpdatedState(onMoreKeysHover)
    val currentOnMoreKeysCommit = rememberUpdatedState(onMoreKeysCommit)
    val currentOnSlidingStart = rememberUpdatedState(onSlidingDragStart)
    val currentOnSlidingMove = rememberUpdatedState(onSlidingDragMove)
    val currentOnSlidingEnd = rememberUpdatedState(onSlidingDragEnd)
    val currentOnSpacebarTrackpad = rememberUpdatedState(onSpacebarTrackpad)
    val currentOnGestureStart = rememberUpdatedState(onGestureStart)
    val currentOnGestureMove = rememberUpdatedState(onGestureMove)
    val currentOnGestureEnd = rememberUpdatedState(onGestureEnd)

    // Key styling: cleanly toggles between keyborder mode (keycap boxes) and flat mode (no boxes except accent pills)
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                keyCoordinates = coords
                onKeyPositioned(key, coords)
            }
            .padding(horizontal = 3.dp, vertical = verticalPadding)
            .clip(keyShape)
            .background(activeBackground)
            .pointerInput(Unit) {
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val initialKey = currentKey.value
                        val initialDownPos = down.position
                        isPressed = true
                        var isMoreKeysActive = false
                        var isSlidingDrag = false
                        var isSpacebarTrackpad = false
                        var isGestureTyping = false
                        var isDeleteSwipeActive = false
                        var gestureEnded = false
                        var lastTrackpadX = 0f
                        val trackpadStepPx = with(density) { 14.dp.toPx() }

                        var isDeleteLongPressActive = false
                        val longPressJob = launch {
                            delay(240L)
                            val coords = keyCoordinates
                            if (initialKey.type == KeyType.DELETE) {
                                isDeleteLongPressActive = true
                                while (isActive) {
                                    currentOnLongPress.value(initialKey)
                                    delay(90L)
                                }
                            } else if (initialKey.moreKeys.isNotEmpty() && coords != null && coords.isAttached) {
                                isMoreKeysActive = true
                                currentOnShowMoreKeys.value(initialKey, coords)
                            } else {
                                currentOnLongPress.value(initialKey)
                            }
                        }

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                // Touch released!
                                longPressJob.cancel()
                                val delta = change.position - initialDownPos
                                if (isDeleteSwipeActive) {
                                    if (delta.x < -18f) {
                                        currentOnDeleteSwipeEnd.value()
                                    } else {
                                        currentOnDeleteSwipeCancel.value()
                                    }
                                } else if (isGestureTyping) {
                                    val coords = keyCoordinates
                                    if (coords != null && coords.isAttached) {
                                        currentOnGestureMove.value(coords, change.position)
                                    }
                                    gestureEnded = true
                                    currentOnGestureEnd.value()
                                } else if (isSlidingDrag) {
                                    currentOnSlidingEnd.value(delta)
                                } else if (isMoreKeysActive) {
                                    currentOnMoreKeysCommit.value()
                                } else if (isSpacebarTrackpad) {
                                    // Trackpad gesture completed, don't emit normal space tap
                                } else if (isDeleteLongPressActive) {
                                    // Already handled during continuous repeat deletion
                                } else {
                                    currentOnTap.value(initialKey)
                                }
                                break
                            }

                            val currentPos = change.position
                            val delta = currentPos - initialDownPos
                            val distance = delta.getDistance()

                            // Swipe on Delete key (Selective Delete): sliding left selects range
                            if (initialKey.type == KeyType.DELETE) {
                                if (delta.x < -14f) {
                                    if (!isDeleteSwipeActive) {
                                        isDeleteSwipeActive = true
                                        longPressJob.cancel()
                                        isPressed = false
                                    }
                                    change.consume()
                                    currentOnDeleteSwipeMove.value(delta.x)
                                } else if (isDeleteSwipeActive) {
                                    change.consume()
                                    currentOnDeleteSwipeMove.value(delta.x.coerceAtMost(0f))
                                }
                            }

                            // Spacebar cursor trackpad gesture: horizontal slide moves caret
                            if (initialKey.type == KeyType.SPACE && Math.abs(delta.x) > 16f) {
                                longPressJob.cancel()
                                isSpacebarTrackpad = true
                                val diffFromLast = delta.x - lastTrackpadX
                                if (diffFromLast > trackpadStepPx) {
                                    val steps = (diffFromLast / trackpadStepPx).toInt()
                                    lastTrackpadX += steps * trackpadStepPx
                                    repeat(steps) {
                                        currentOnSpacebarTrackpad.value(1)
                                    }
                                } else if (diffFromLast < -trackpadStepPx) {
                                    val steps = ((lastTrackpadX - delta.x) / trackpadStepPx).toInt()
                                    lastTrackpadX -= steps * trackpadStepPx
                                    repeat(steps) {
                                        currentOnSpacebarTrackpad.value(-1)
                                    }
                                }
                            }

                            // Sliding input detector for ?123
                            if (initialKey.slidingInput && distance > 18f && !isSlidingDrag) {
                                longPressJob.cancel()
                                isSlidingDrag = true
                                isPressed = false
                                val coords = keyCoordinates
                                if (coords != null && coords.isAttached) {
                                    currentOnSlidingStart.value(initialKey, coords, initialDownPos)
                                }
                            }

                            // Gesture typing detector on character keys
                            if (initialKey.type == KeyType.CHARACTER && !isMoreKeysActive && !isSlidingDrag) {
                                if (!isGestureTyping && distance > 20f) {
                                    longPressJob.cancel()
                                    isGestureTyping = true
                                    isPressed = false
                                    val coords = keyCoordinates
                                    if (coords != null && coords.isAttached) {
                                        currentOnGestureStart.value(coords, initialDownPos)
                                        currentOnGestureMove.value(coords, currentPos)
                                    }
                                } else if (isGestureTyping) {
                                    val coords = keyCoordinates
                                    if (coords != null && coords.isAttached) {
                                        currentOnGestureMove.value(coords, currentPos)
                                    }
                                }
                            }

                            if (isGestureTyping) {
                                change.consume()
                            } else if (isSlidingDrag) {
                                change.consume()
                                currentOnSlidingMove.value(delta)
                            } else if (isMoreKeysActive) {
                                change.consume()
                                val coords = keyCoordinates
                                if (coords != null && coords.isAttached) {
                                    currentOnMoreKeysHover.value(coords, currentPos)
                                }
                            }
                        }
                        if (isGestureTyping && !gestureEnded) {
                            currentOnGestureEnd.value()
                        }
                        isPressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Flick up preview badge in top right corner (hidden when sliding symbol mode is active)
        if (!key.flickUp.isNullOrEmpty() && key.type == KeyType.CHARACTER && !isSlidingSymbolMode) {
            Text(
                text = key.flickUp!!,
                color = colorScheme.keyForeground.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = KiebordTheme.fontFamily,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 5.dp)
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
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    ShiftState.ONE_SHOT -> {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Shift One-Shot",
                            tint = colorScheme.shiftActiveColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    ShiftState.CAPS_LOCK -> {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = "Caps Lock",
                                tint = colorScheme.shiftActiveColor,
                                modifier = Modifier.size(24.dp)
                            )
                            // Underline bar for Caps Lock
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(width = 18.dp, height = 2.5.dp)
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
                    modifier = Modifier.size(24.dp)
                )
            }
            KeyType.ACTION -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = foregroundColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            KeyType.SWITCH_LANGUAGE -> {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = "Switch Language",
                    tint = foregroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            KeyType.SPACE -> {
                Text(
                    text = key.displayLabel(shiftState).ifEmpty { "English" },
                    color = colorScheme.keyForeground.copy(alpha = 0.45f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = KiebordTheme.fontFamily
                )
            }
            else -> {
                val textSize = if (key.type == KeyType.SYMBOLS) 15.sp else 22.sp
                val isUppercase = displaySymbolLabel.length == 1 && displaySymbolLabel.first().isUpperCase()
                val verticalOffset = if (isUppercase) 1.5.dp else 0.dp
                Text(
                    text = displaySymbolLabel,
                    color = foregroundColor,
                    fontSize = textSize,
                    fontWeight = if (key.type == KeyType.SYMBOLS) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = KiebordTheme.fontFamily,
                    modifier = if (verticalOffset > 0.dp) Modifier.padding(top = verticalOffset) else Modifier,
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = KiebordTheme.fontFamily,
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                        lineHeight = textSize,
                        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                            alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                            trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                )
            }
        }
    }
}
