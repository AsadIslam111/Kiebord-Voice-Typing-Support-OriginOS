package io.kiebord.feature.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.ShiftState
import io.kiebord.theme.KeyboardColorScheme
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
    isSlidingShiftMode: Boolean = false,
    showKeyBorders: Boolean = true,
    showSecondaryKeys: Boolean = true,
    showKeyPopups: Boolean = true,
    rowHeight: Dp = 58.dp,
    onKeyPress: (KeyDef) -> Unit = {},
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
    onSpacebarSwipeMove: (Float) -> Unit = {},
    onSpacebarSwipeEnd: (Float) -> Unit = {},
    onSpacebarDwellReady: () -> Unit = {},
    onDeleteSwipeMove: (Float) -> Unit = {},
    onDeleteSwipeEnd: () -> Unit = {},
    onDeleteSwipeCancel: () -> Unit = {},
    onGestureStart: (LayoutCoordinates, Offset) -> Unit = { _, _ -> },
    onGestureMove: (LayoutCoordinates, Offset) -> Unit = { _, _ -> },
    onGestureEnd: () -> Unit = {},
    isInteractive: Boolean = true,
    gestureTypingEnabled: Boolean = true,
    longPressDelayMs: Long = 300L
) {
    val colorScheme = KiebordTheme.colorScheme
    val density = LocalDensity.current
    val scaleFactor = (rowHeight.value / 58f).coerceIn(0.4f, 1.6f)
    var isPressed by remember { mutableStateOf(false) }
    var isMoreKeysActive by remember { mutableStateOf(false) }
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
        if (key.type == KeyType.ACTION) {
            colorScheme.accentColor.copy(alpha = 0.8f)
        } else if (colorScheme.keyPressedBackground != Color.Unspecified) {
            colorScheme.keyPressedBackground
        } else {
            colorScheme.accentColor.copy(alpha = 0.28f)
        }
    } else {
        baseBackground
    }

    val foregroundColor = when {
        key.type == KeyType.ACTION -> colorScheme.actionKeyForeground
        isPressed && colorScheme.keyPressedForeground != Color.Unspecified -> colorScheme.keyPressedForeground
        key.type == KeyType.SHIFT && shiftState != ShiftState.UNSHIFTED -> colorScheme.shiftActiveColor
        else -> colorScheme.keyForeground
    }

    val displaySymbolLabel = key.displayLabel(shiftState)

    // Key shape: Enter key and Sym key (?123) are always rounded stadium pills.
    // In flat mode (!showKeyBorders), spacebar is also a stadium pill.
    // When key borders are enabled, spacebar and standard character keys use standard scaled corner radius.
    val isPillShape = key.type == KeyType.ACTION || key.type == KeyType.SYMBOLS || (!showKeyBorders && key.type == KeyType.SPACE)
    val keyShape = if (isPillShape) {
        RoundedCornerShape(percent = 50)
    } else {
        RoundedCornerShape((8 * scaleFactor).coerceAtLeast(3f).dp)
    }

    // In flat theme (!showKeyBorders), reduce height of Space bar, Sym key, and Enter key
    val isReducedHeightKey = !showKeyBorders && isPillColoredKey
    val horizontalPadding = (3 * scaleFactor).coerceIn(1f, 4f).dp
    val verticalPadding = if (isReducedHeightKey) (7 * scaleFactor).coerceAtLeast(2f).dp else (3 * scaleFactor).coerceIn(1f, 4f).dp

    // Capture latest parameters in updated states to allow pointerInput without gesture cancellation
    val currentKey = rememberUpdatedState(key)
    val currentOnKeyPress = rememberUpdatedState(onKeyPress)
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
    val currentOnSpacebarSwipeMove = rememberUpdatedState(onSpacebarSwipeMove)
    val currentOnSpacebarSwipeEnd = rememberUpdatedState(onSpacebarSwipeEnd)
    val currentOnSpacebarDwellReady = rememberUpdatedState(onSpacebarDwellReady)
    val currentOnGestureStart = rememberUpdatedState(onGestureStart)
    val currentOnGestureMove = rememberUpdatedState(onGestureMove)
    val currentOnGestureEnd = rememberUpdatedState(onGestureEnd)
    val currentGestureTypingEnabled = rememberUpdatedState(gestureTypingEnabled)
    val currentLongPressDelay = rememberUpdatedState(longPressDelayMs)

    val showCircularPopup = showKeyPopups && isPressed && key.type == KeyType.CHARACTER && !isSlidingSymbolMode && !isSlidingShiftMode && !isMoreKeysActive

    // Key styling: cleanly toggles between keyborder mode (keycap boxes) and flat mode (no boxes except accent pills)
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(if (showCircularPopup) 50f else 0f)
            .onGloballyPositioned { coords ->
                keyCoordinates = coords
                onKeyPositioned(key, coords)
            }
            .pointerInput(isInteractive) {
                    if (!isInteractive) return@pointerInput
                    coroutineScope {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val initialKey = currentKey.value
                            val initialDownPos = down.position
                            isPressed = true
                            isMoreKeysActive = false
                            currentOnKeyPress.value(initialKey)
                            var isSlidingDrag = false
                            var isSpacebarDwellReady = false
                        var isSpacebarSwipingLayout = false
                        var initialSpacebarSwipeX = 0f
                        var isSpacebarTrackpadActive = false
                        var isGestureTyping = false
                        var isDeleteSwipeActive = false
                        var gestureEnded = false
                        var lastTrackpadX = 0f
                        val trackpadStepPx = with(density) { 14.dp.toPx() }

                        // Immediate layout modifier keys: ABC and =\< switch instantaneously on pointer down
                        val isImmediateModifier = initialKey.type == KeyType.SYMBOLS && !initialKey.slidingInput
                        val isSlidingInitiator = initialKey.slidingInput || initialKey.type == KeyType.SHIFT
                        var isHandledOnDown = false

                        if (isImmediateModifier) {
                            isHandledOnDown = true
                            currentOnTap.value(initialKey)
                        }

                        val spacebarDwellJob = if (initialKey.type == KeyType.SPACE) {
                            launch {
                                delay(400L)
                                isSpacebarDwellReady = true
                                currentOnSpacebarDwellReady.value()
                            }
                        } else null

                        var isLongPressTriggered = false
                        var isDeleteLongPressActive = false
                        val longPressJob = if (isImmediateModifier || isSlidingInitiator) null else launch {
                            delay(currentLongPressDelay.value)
                            val coords = keyCoordinates
                            isLongPressTriggered = true
                            if (initialKey.type == KeyType.DELETE) {
                                isDeleteLongPressActive = true
                                while (isActive) {
                                    currentOnLongPress.value(initialKey)
                                    delay(90L)
                                }
                            } else if (initialKey.type == KeyType.SPACE) {
                                // Handled via spacebarDwellJob
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
                                spacebarDwellJob?.cancel()
                                longPressJob?.cancel()
                                if (isHandledOnDown) {
                                    break
                                }
                                val wasMoreKeysActive = isMoreKeysActive
                                isMoreKeysActive = false
                                val delta = change.position - initialDownPos
                                if (isDeleteSwipeActive) {
                                    if (delta.x < -18f) {
                                        currentOnDeleteSwipeEnd.value()
                                    } else {
                                        currentOnDeleteSwipeCancel.value()
                                    }
                                } else if (isSpacebarSwipingLayout) {
                                    val effectiveDeltaX = delta.x - initialSpacebarSwipeX
                                    currentOnSpacebarSwipeEnd.value(effectiveDeltaX)
                                } else if (isSpacebarTrackpadActive) {
                                    // Trackpad gesture completed, don't emit normal space tap
                                } else if (isSpacebarDwellReady) {
                                    // Held down space for cursor dwell without dragging
                                } else if (isGestureTyping) {
                                    val coords = keyCoordinates
                                    if (coords != null && coords.isAttached) {
                                        currentOnGestureMove.value(coords, change.position)
                                    }
                                    gestureEnded = true
                                    currentOnGestureEnd.value()
                                } else if (isSlidingDrag) {
                                    currentOnSlidingEnd.value(delta)
                                } else if (wasMoreKeysActive) {
                                    change.consume()
                                    currentOnMoreKeysCommit.value()
                                } else if (isDeleteLongPressActive || isLongPressTriggered) {
                                    change.consume()
                                    // Handled during long-press, do NOT emit tap on release
                                } else {
                                    change.consume()
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
                                        longPressJob?.cancel()
                                        isPressed = false
                                    }
                                    change.consume()
                                    currentOnDeleteSwipeMove.value(delta.x)
                                } else if (isDeleteSwipeActive) {
                                    change.consume()
                                    currentOnDeleteSwipeMove.value(delta.x.coerceAtMost(0f))
                                }
                            }

                            // Spacebar handling:
                            // 1. Swiping immediately (before 220ms dwell) triggers active layout sliding switch!
                            // 2. Holding still for 220ms (dwell ready) then sliding left/right activates cursor movement!
                            if (initialKey.type == KeyType.SPACE) {
                                if (!isSpacebarDwellReady) {
                                    val swipeActivationPx = with(density) { 30.dp.toPx() }
                                    val absX = Math.abs(delta.x)
                                    val absY = Math.abs(delta.y)
                                    if (isSpacebarSwipingLayout || (absX > swipeActivationPx && absX > absY * 1.35f)) {
                                        if (!isSpacebarSwipingLayout) {
                                            spacebarDwellJob?.cancel()
                                            longPressJob?.cancel()
                                            isSpacebarSwipingLayout = true
                                            isPressed = false
                                            initialSpacebarSwipeX = delta.x
                                        }
                                        change.consume()
                                        val effectiveDeltaX = delta.x - initialSpacebarSwipeX
                                        currentOnSpacebarSwipeMove.value(effectiveDeltaX)
                                    }
                                } else {
                                    change.consume()
                                    val diffFromLast = delta.x - lastTrackpadX
                                    if (diffFromLast > trackpadStepPx) {
                                        isSpacebarTrackpadActive = true
                                        val steps = (diffFromLast / trackpadStepPx).toInt()
                                        lastTrackpadX += steps * trackpadStepPx
                                        repeat(steps) {
                                            currentOnSpacebarTrackpad.value(1)
                                        }
                                    } else if (diffFromLast < -trackpadStepPx) {
                                        isSpacebarTrackpadActive = true
                                        val steps = ((lastTrackpadX - delta.x) / trackpadStepPx).toInt()
                                        lastTrackpadX -= steps * trackpadStepPx
                                        repeat(steps) {
                                            currentOnSpacebarTrackpad.value(-1)
                                        }
                                    }
                                }
                            }

                            // Sliding input detector for ?123 and Shift:
                            // Only active if dragged outside the key boundaries and covered at least the key length minimum
                            if (isSlidingInitiator) {
                                val coords = keyCoordinates
                                val keyWidth = coords?.size?.width?.toFloat() ?: with(density) { 50.dp.toPx() }
                                val keyHeight = coords?.size?.height?.toFloat() ?: with(density) { 44.dp.toPx() }
                                val minKeyLength = minOf(keyWidth, keyHeight)

                                val isOutsideKey = if (coords != null && coords.isAttached) {
                                    currentPos.x < 0f || currentPos.x > keyWidth ||
                                    currentPos.y < 0f || currentPos.y > keyHeight
                                } else {
                                    distance >= minKeyLength
                                }

                                if (isOutsideKey && distance >= minKeyLength) {
                                    if (!isSlidingDrag) {
                                        isSlidingDrag = true
                                        isPressed = false
                                        longPressJob?.cancel()
                                        if (coords != null && coords.isAttached) {
                                            currentOnSlidingStart.value(initialKey, coords, initialDownPos)
                                        }
                                    }
                                }
                            }

                            // Gesture typing detector on character keys
                            if (currentGestureTypingEnabled.value && initialKey.type == KeyType.CHARACTER && !isMoreKeysActive && !isSlidingDrag) {
                                val gestureThresholdPx = with(density) { 28.dp.toPx() }
                                if (!isGestureTyping && distance > gestureThresholdPx) {
                                    longPressJob?.cancel()
                                    isGestureTyping = true
                                    isPressed = false
                                    isMoreKeysActive = false
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
                        if (isMoreKeysActive) {
                            isMoreKeysActive = false
                            currentOnMoreKeysCommit.value()
                        }
                        isPressed = false
                        isMoreKeysActive = false
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .then(
                    if (showKeyBorders && colorScheme.keyShadow.alpha > 0.01f) {
                        Modifier.shadow(
                            elevation = (1.5f * scaleFactor).dp,
                            shape = keyShape,
                            clip = false,
                            ambientColor = colorScheme.keyShadow,
                            spotColor = colorScheme.keyShadow
                        )
                    } else {
                        Modifier
                    }
                )
                .clip(keyShape)
                .background(activeBackground),
            contentAlignment = Alignment.Center
        ) {
        // Flick up preview badge in top right corner (hidden when sliding symbol/shift mode is active or secondary keys disabled)
        if (showSecondaryKeys && !key.flickUp.isNullOrEmpty() && key.type == KeyType.CHARACTER && !isSlidingSymbolMode && !isSlidingShiftMode) {
            Text(
                text = key.flickUp!!,
                color = if (isPressed && colorScheme.keyPressedForeground != Color.Unspecified) {
                    colorScheme.keyPressedForeground.copy(alpha = 0.7f)
                } else {
                    colorScheme.keyForeground.copy(alpha = 0.5f)
                },
                fontSize = (11 * scaleFactor).coerceAtLeast(6.5f).sp,
                fontWeight = FontWeight.Normal,
                fontFamily = KiebordTheme.fontFamily,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = (2.5f * scaleFactor).dp, end = (4 * scaleFactor).dp)
            )
        }

        // Key content: Vector Material Icons vs Typography label
        val actionIconSize = (22 * scaleFactor).coerceAtLeast(12f).dp
        when (key.type) {
            KeyType.SHIFT -> {
                when (shiftState) {
                    ShiftState.UNSHIFTED -> {
                        Icon(
                            imageVector = Icons.Outlined.ArrowUpward,
                            contentDescription = "Shift",
                            tint = foregroundColor,
                            modifier = Modifier.size(actionIconSize)
                        )
                    }
                    ShiftState.ONE_SHOT -> {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Shift One-Shot",
                            tint = colorScheme.shiftActiveColor,
                            modifier = Modifier.size(actionIconSize)
                        )
                    }
                    ShiftState.CAPS_LOCK -> {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = "Caps Lock",
                                tint = colorScheme.shiftActiveColor,
                                modifier = Modifier.size(actionIconSize)
                            )
                            // Underline bar for Caps Lock
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .size(width = (18 * scaleFactor).dp, height = (2.5f * scaleFactor).coerceAtLeast(1.5f).dp)
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
                    modifier = Modifier.size(actionIconSize)
                )
            }
            KeyType.ACTION -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardReturn,
                    contentDescription = "Enter",
                    tint = foregroundColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            KeyType.SWITCH_LANGUAGE -> {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = "Switch Language",
                    tint = foregroundColor,
                    modifier = Modifier.size((20 * scaleFactor).coerceAtLeast(11f).dp)
                )
            }
            KeyType.SPACE -> {
                Text(
                    text = key.displayLabel(shiftState).ifEmpty { "English" },
                    color = if (isPressed && colorScheme.keyPressedForeground != Color.Unspecified) {
                        colorScheme.keyPressedForeground
                    } else {
                        colorScheme.keyForeground.copy(alpha = 0.45f)
                    },
                    fontSize = (15.5f * scaleFactor).coerceAtLeast(8f).sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = KiebordTheme.fontFamily
                )
            }
            else -> {
                val baseCharSize = if (displaySymbolLabel.length > 2) 17f else if (displaySymbolLabel.length > 1) 20.5f else 25.5f
                val rawSize = if (key.type == KeyType.SYMBOLS) 17f else baseCharSize
                val textSize = (rawSize * scaleFactor).coerceAtLeast(8.5f).sp
                val isUppercase = displaySymbolLabel.length == 1 && displaySymbolLabel.first().isUpperCase()
                val verticalShift = if (isUppercase) (1.5f * scaleFactor).dp else 0.dp
                Text(
                    text = displaySymbolLabel,
                    color = foregroundColor,
                    fontSize = textSize,
                    fontWeight = if (key.type == KeyType.SYMBOLS) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = KiebordTheme.fontFamily,
                    modifier = if (verticalShift > 0.dp) Modifier.offset(y = verticalShift) else Modifier,
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

        // Circular Keypress Preview Popup
        if (showCircularPopup) {
            val popupDiameter = (52 * scaleFactor).coerceIn(44f, 60f).dp
            val defaultGapDp = (12 * scaleFactor).coerceIn(8f, 16f).dp
            val defaultOffsetY = -(popupDiameter + defaultGapDp)

            val coords = keyCoordinates
            val (clampedOffsetX, clampedOffsetY) = if (coords != null && coords.isAttached) {
                try {
                    val rootCoords = coords.findRootCoordinates()
                    val rootWidth = rootCoords.size.width.toFloat()
                    val rootPos = coords.positionInRoot()
                    val keyCenterX = rootPos.x + coords.size.width / 2f
                    val halfPopupPx = with(density) { (popupDiameter / 2).toPx() }
                    val marginPx = with(density) { 6.dp.toPx() }
                    val minCenterX = halfPopupPx + marginPx
                    val maxCenterX = rootWidth - halfPopupPx - marginPx
                    val nudgePx = when {
                        keyCenterX < minCenterX -> minCenterX - keyCenterX
                        keyCenterX > maxCenterX -> maxCenterX - keyCenterX
                        else -> 0f
                    }
                    val offsetX = with(density) { nudgePx.toDp() }

                    // Prevent popup from being cut off at the top of the keyboard window
                    val keyTopInRoot = with(density) { rootPos.y.toDp() }
                    val minTopDp = 6.dp
                    val offsetY = if (keyTopInRoot + defaultOffsetY < minTopDp) {
                        minTopDp - keyTopInRoot
                    } else {
                        defaultOffsetY
                    }
                    offsetX to offsetY
                } catch (_: Exception) {
                    0.dp to defaultOffsetY
                }
            } else {
                0.dp to defaultOffsetY
            }

            val isLight = colorScheme == KeyboardColorScheme.DefaultLight
            val popupBg = colorScheme.moreKeysBackground

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(x = clampedOffsetX, y = clampedOffsetY)
                    .wrapContentSize(align = Alignment.Center, unbounded = true)
                    .requiredSize(popupDiameter)
                    .aspectRatio(1f, matchHeightConstraintsFirst = false)
                    .shadow(
                        elevation = (4 * scaleFactor).dp,
                        shape = CircleShape,
                        clip = false,
                        ambientColor = Color(0x38000000),
                        spotColor = Color(0x30000000)
                    )
                    .clip(CircleShape)
                    .background(popupBg)
                    .then(
                        if (!isLight && colorScheme.keyBorder.alpha > 0.01f) {
                            Modifier.border(
                                width = 0.5.dp,
                                color = Color(0x22FFFFFF),
                                shape = CircleShape
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displaySymbolLabel,
                    color = colorScheme.keyForeground,
                    fontSize = (27 * scaleFactor).coerceIn(21f, 34f).sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = KiebordTheme.fontFamily,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(
                        platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                            alignment = androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center,
                            trim = androidx.compose.ui.text.style.LineHeightStyle.Trim.Both
                        )
                    )
                )
            }
        }
    }
}
