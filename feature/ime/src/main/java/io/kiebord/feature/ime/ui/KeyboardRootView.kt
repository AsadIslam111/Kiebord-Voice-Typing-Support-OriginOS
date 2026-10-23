package io.kiebord.feature.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.KeyboardLayoutDef
import io.kiebord.common.model.ShiftState
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.feature.ime.ui.emoji.EmojiPanelView
import io.kiebord.feature.ime.ui.resizer.LiveResizerOverlay
import io.kiebord.feature.ime.ui.toolbar.ReorderableToolbar
import io.kiebord.feature.ime.ui.toolbar.ToolbarAction
import io.kiebord.theme.KiebordTheme

@Composable
fun KeyboardRootView(
    layout: KeyboardLayoutDef,
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    initialRowHeight: Dp = 60.dp,
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit = {},
    onThemeToggle: () -> Unit = {}
) {
    val colorScheme = KiebordTheme.colorScheme
    val density = LocalDensity.current

    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var rootWidthPx by remember { mutableFloatStateOf(0f) }

    val keyCoordinatesMap = remember { mutableStateMapOf<KeyDef, LayoutCoordinates>() }

    var moreKeysState by remember { mutableStateOf<MoreKeysPopupState?>(null) }
    var quickDragState by remember { mutableStateOf<QuickSymbolDragState?>(null) }

    // Phase 5: Toolbar, Emoji, Number row, and Live Resizer states
    var isEmojiOpen by remember { mutableStateOf(false) }
    var isResizerOpen by remember { mutableStateOf(false) }
    var showNumberRow by remember { mutableStateOf(false) }
    var keyRowHeight by remember { mutableStateOf(initialRowHeight) }
    var chinPadding by remember { mutableStateOf(8.dp) }

    // On sliding input from ?123, switch layout view to Symbols page seamlessly
    val effectiveLayout = if (quickDragState != null) {
        PredefinedLayouts.Symbols
    } else {
        layout
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                rootCoordinates = coords
                rootWidthPx = coords.size.width.toFloat()
            }
            .background(colorScheme.containerBackground)
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = chinPadding)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Reorderable Toolbar along the top
            ReorderableToolbar(
                onActionClick = { action ->
                    when (action) {
                        ToolbarAction.EMOJI -> isEmojiOpen = !isEmojiOpen
                        ToolbarAction.RESIZER -> isResizerOpen = !isResizerOpen
                        ToolbarAction.NUMBER_ROW_TOGGLE -> showNumberRow = !showNumberRow
                        ToolbarAction.THEME_TOGGLE -> onThemeToggle()
                        ToolbarAction.TEXT_EDIT -> {}
                    }
                },
                isEmojiActive = isEmojiOpen,
                isResizerActive = isResizerOpen,
                isNumberRowActive = showNumberRow
            )

            // Switch between Emoji Panel and Keyboard Keys
            if (isEmojiOpen) {
                EmojiPanelView(
                    onEmojiSelected = { emoji ->
                        onKeyTap(KeyDef(label = emoji, type = KeyType.CHARACTER))
                    },
                    onDelete = {
                        onKeyTap(KeyDef(type = KeyType.DELETE))
                    },
                    onClose = { isEmojiOpen = false },
                    height = keyRowHeight * 4
                )
            } else {
                // Optional 5th Number Row at the top
                if (showNumberRow && !effectiveLayout.id.startsWith("symbols")) {
                    NumberRowBar(
                        language = effectiveLayout.language,
                        onKeyTap = onKeyTap,
                        onKeyLongPress = onKeyLongPress
                    )
                }

                // Keyboard rows
                effectiveLayout.rows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(keyRowHeight)
                    ) {
                        row.keys.forEach { key ->
                            val effectiveKey = if (key.type == KeyType.SPACE && key.label.isBlank()) {
                                val spaceLabel = when {
                                    effectiveLayout.id.startsWith("symbols") -> "Space"
                                    effectiveLayout.id == "bn_borno" -> "বাংলা (Borno)"
                                    effectiveLayout.id == "bn_probhat" -> "বাংলা (Probhat)"
                                    else -> "English"
                                }
                                key.copy(label = spaceLabel)
                            } else {
                                key
                            }

                            KeycapView(
                                key = effectiveKey,
                                shiftState = shiftState,
                                modifier = Modifier
                                    .weight(effectiveKey.widthWeight)
                                    .fillMaxHeight(),
                                onKeyTap = onKeyTap,
                                onKeyLongPress = onKeyLongPress,
                                onKeyPositioned = { k, coords ->
                                    keyCoordinatesMap[k] = coords
                                },
                                onShowMoreKeys = { keyDef, keyCoords ->
                                    val rCoords = rootCoordinates
                                    if (rCoords != null && rCoords.isAttached && keyCoords.isAttached) {
                                        val localTopLeft = rCoords.localPositionOf(keyCoords, Offset.Zero)
                                        val size = keyCoords.size
                                        val keyRect = Rect(
                                            localTopLeft,
                                            Size(size.width.toFloat(), size.height.toFloat())
                                        )

                                        val itemWidthPx = with(density) { 44.dp.toPx() }
                                        val horizontalPaddingPx = with(density) { 8.dp.toPx() }
                                        val totalWidthPx = keyDef.moreKeys.size * itemWidthPx + horizontalPaddingPx * 2
                                        val idealLeft = keyRect.center.x - totalWidthPx / 2f
                                        val clampedLeft = idealLeft.coerceIn(
                                            8f,
                                            (rootWidthPx - totalWidthPx - 8f).coerceAtLeast(8f)
                                        )
                                        val initialRelativeX = keyRect.center.x - (clampedLeft + horizontalPaddingPx)
                                        val initialIndex = (initialRelativeX / itemWidthPx).toInt().coerceIn(0, keyDef.moreKeys.lastIndex)

                                        moreKeysState = MoreKeysPopupState(
                                            anchorKey = keyDef,
                                            anchorRect = keyRect,
                                            items = keyDef.moreKeys,
                                            selectedIndex = initialIndex
                                        )
                                    }
                                },
                                onMoreKeysHover = { keyCoords, touchPosInKey ->
                                    val state = moreKeysState ?: return@KeycapView
                                    val rCoords = rootCoordinates ?: return@KeycapView
                                    if (rCoords.isAttached && keyCoords.isAttached) {
                                        val fingerPosInRoot = rCoords.localPositionOf(keyCoords, touchPosInKey)
                                        val itemWidthPx = with(density) { 44.dp.toPx() }
                                        val horizontalPaddingPx = with(density) { 8.dp.toPx() }
                                        val totalWidthPx = state.items.size * itemWidthPx + horizontalPaddingPx * 2
                                        val idealLeft = state.anchorRect.center.x - totalWidthPx / 2f
                                        val clampedLeft = idealLeft.coerceIn(
                                            8f,
                                            (rootWidthPx - totalWidthPx - 8f).coerceAtLeast(8f)
                                        )

                                        val relativeX = fingerPosInRoot.x - (clampedLeft + horizontalPaddingPx)
                                        val index = (relativeX / itemWidthPx).toInt().coerceIn(0, state.items.lastIndex)

                                        if (index != state.selectedIndex) {
                                            moreKeysState = state.copy(selectedIndex = index)
                                        }
                                    }
                                },
                                onMoreKeysCommit = {
                                    val state = moreKeysState
                                    if (state != null) {
                                        val selectedChar = state.items.getOrNull(state.selectedIndex)
                                            ?: state.items.firstOrNull()
                                        if (!selectedChar.isNullOrEmpty()) {
                                            onKeyTap(KeyDef(label = selectedChar, type = KeyType.CHARACTER))
                                        }
                                        moreKeysState = null
                                    }
                                },
                                onSlidingDragStart = { _, keyCoords ->
                                    val rCoords = rootCoordinates
                                    if (rCoords != null && rCoords.isAttached && keyCoords.isAttached) {
                                        val localCenter = rCoords.localPositionOf(
                                            keyCoords,
                                            Offset(keyCoords.size.width / 2f, keyCoords.size.height / 2f)
                                        )
                                        quickDragState = QuickSymbolDragState(
                                            startOffset = localCenter,
                                            currentOffset = localCenter
                                        )
                                    }
                                },
                                onSlidingDragMove = { keyCoords, touchPosInKey ->
                                    val rCoords = rootCoordinates ?: return@KeycapView
                                    if (rCoords.isAttached && keyCoords.isAttached) {
                                        val fingerPosInRoot = rCoords.localPositionOf(keyCoords, touchPosInKey)

                                        // Find hovered key in root
                                        var hoveredKey: KeyDef? = null
                                        var hoveredBounds: Rect? = null
                                        for ((k, coords) in keyCoordinatesMap) {
                                            if (coords.isAttached) {
                                                val topLeft = rCoords.localPositionOf(coords, Offset.Zero)
                                                val rect = Rect(
                                                    topLeft,
                                                    Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                                                )
                                                if (rect.contains(fingerPosInRoot)) {
                                                    hoveredKey = k
                                                    hoveredBounds = rect
                                                    break
                                                }
                                            }
                                        }

                                        val symbol = hoveredKey?.let {
                                            if (it.label.isNotEmpty() && it.type == KeyType.CHARACTER) it.label else if (!it.flickUp.isNullOrEmpty()) it.flickUp else null
                                        }

                                        quickDragState = quickDragState?.copy(
                                            currentOffset = fingerPosInRoot,
                                            hoveredKeyBounds = hoveredBounds,
                                            hoveredSymbol = symbol
                                        )
                                    }
                                },
                                onSlidingDragEnd = { keyCoords, touchPosInKey ->
                                    val rCoords = rootCoordinates
                                    if (rCoords != null && rCoords.isAttached && keyCoords.isAttached) {
                                        val fingerPosInRoot = rCoords.localPositionOf(keyCoords, touchPosInKey)
                                        for ((k, coords) in keyCoordinatesMap) {
                                            if (coords.isAttached) {
                                                val topLeft = rCoords.localPositionOf(coords, Offset.Zero)
                                                val rect = Rect(
                                                    topLeft,
                                                    Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                                                )
                                                if (rect.contains(fingerPosInRoot)) {
                                                    val symbol = if (k.label.isNotEmpty() && k.type == KeyType.CHARACTER) k.label else if (!k.flickUp.isNullOrEmpty()) k.flickUp!! else ""
                                                    if (symbol.isNotEmpty()) {
                                                        onKeyTap(KeyDef(label = symbol, type = KeyType.CHARACTER))
                                                    }
                                                    break
                                                }
                                            }
                                        }
                                    }
                                    quickDragState = null
                                }
                            )
                        }
                    }
                }
            }
        }

        // 1. Neon laser guideline overlay for sliding symbol input (matchParentSize prevents shifting)
        QuickSymbolDragOverlay(
            state = quickDragState,
            modifier = Modifier.matchParentSize()
        )

        // 2. Borno Keyboard-style rounded pill MoreKeys floating popup
        MoreKeysPopup(
            state = moreKeysState,
            keyboardWidthPx = if (rootWidthPx > 0f) rootWidthPx else 1080f
        )

        // 3. Live Keyboard Resizer Overlay
        if (isResizerOpen) {
            LiveResizerOverlay(
                currentRowHeight = keyRowHeight,
                currentChinPadding = chinPadding,
                onRowHeightChange = { keyRowHeight = it },
                onChinPaddingChange = { chinPadding = it },
                onDone = { isResizerOpen = false },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
