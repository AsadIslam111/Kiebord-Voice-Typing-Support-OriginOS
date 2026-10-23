package io.kiebord.feature.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.KeyboardLayoutDef
import io.kiebord.common.model.ShiftState
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.feature.ime.clipboard.SandboxedClipboardManager
import io.kiebord.feature.ime.ui.clipboard.ClipboardPanelView
import io.kiebord.feature.ime.ui.emoji.EmojiPanelView
import io.kiebord.feature.ime.ui.resizer.LiveResizerOverlay
import io.kiebord.feature.ime.ui.textedit.TextAction
import io.kiebord.feature.ime.ui.textedit.TextEditPanelView
import io.kiebord.feature.ime.ui.suggestion.SuggestionCandidate
import io.kiebord.feature.ime.ui.suggestion.SuggestionStripView
import io.kiebord.feature.ime.ui.toolbar.ReorderableToolbar
import io.kiebord.feature.ime.ui.toolbar.ToolbarAction
import io.kiebord.feature.ime.ui.toolbar.ToolsDrawerView
import io.kiebord.theme.KiebordTheme

@Composable
fun KeyboardRootView(
    layout: KeyboardLayoutDef,
    shiftState: ShiftState,
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit,
    onTextAction: (TextAction, Boolean) -> Unit = { _, _ -> },
    onThemeToggle: () -> Unit = {},
    suggestions: List<SuggestionCandidate> = emptyList(),
    onCandidateSelect: (SuggestionCandidate) -> Unit = {},
    clipboardManager: SandboxedClipboardManager? = null,
    onPasteClip: (String) -> Unit = {},
    initialRowHeight: Dp = 60.dp,
    navBarBottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var rootCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var rootWidthPx by remember { mutableFloatStateOf(0f) }

    val keyCoordinatesMap = remember { mutableMapOf<KeyDef, LayoutCoordinates>() }

    var moreKeysState by remember { mutableStateOf<MoreKeysPopupState?>(null) }
    var quickDragState by remember { mutableStateOf<QuickSymbolDragState?>(null) }
    var isSlidingSymbolMode by remember { mutableStateOf(false) }
    var dragStartPosInRoot by remember { mutableStateOf<Offset?>(null) }

    // Phase 5, 6 & 8: Toolbar, Emoji, Resizer, Number row, TextEdit, Clipboard, and Tools Drawer states
    var toolOrder by remember {
        mutableStateOf(
            listOf(
                ToolbarAction.TEXT_EDIT,
                ToolbarAction.CLIPBOARD,
                ToolbarAction.THEME_TOGGLE,
                ToolbarAction.RESIZER,
                ToolbarAction.NUMBER_ROW_TOGGLE,
                ToolbarAction.EMOJI
            )
        )
    }
    var isEmojiOpen by remember { mutableStateOf(false) }
    var isClipboardOpen by remember { mutableStateOf(false) }
    var isResizerOpen by remember { mutableStateOf(false) }
    var isTextEditOpen by remember { mutableStateOf(false) }
    var isToolsDrawerOpen by remember { mutableStateOf(false) }
    var showNumberRow by remember { mutableStateOf(false) }
    var keyRowHeight by remember { mutableStateOf(initialRowHeight) }
    var chinPadding by remember { mutableStateOf(8.dp) }
    var horizontalMargin by remember { mutableStateOf(0.dp) }
    val toolbarHeight = 46.dp

    // Snapshot state for Resizer Cancel button
    var savedRowHeight by remember { mutableStateOf(initialRowHeight) }
    var savedChinPadding by remember { mutableStateOf(8.dp) }
    var savedHorizontalMargin by remember { mutableStateOf(0.dp) }

    // Obey navigation bar insets: maximum of Compose WindowInsets and platform WindowInsetsCompat
    val composeNavBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val effectiveNavBarBottom = maxOf(navBarBottomInset, composeNavBottom)
    val totalBottomPadding = chinPadding + effectiveNavBarBottom

    // When in sliding symbol mode from ?123, seamlessly switch display to Symbols layout
    val displayLayout = if (isSlidingSymbolMode) PredefinedLayouts.Symbols else layout

    // Clear key coordinate cache whenever active layout changes to prevent stale hits
    LaunchedEffect(isSlidingSymbolMode, displayLayout.id) {
        keyCoordinatesMap.clear()
    }

    val context = LocalContext.current
    val handleToolbarAction: (ToolbarAction) -> Unit = { action ->
        when (action) {
            ToolbarAction.TEXT_EDIT -> {
                isTextEditOpen = true
                isToolsDrawerOpen = false
                isEmojiOpen = false
                isResizerOpen = false
            }
            ToolbarAction.EMOJI -> {
                isEmojiOpen = !isEmojiOpen
                if (isEmojiOpen) {
                    isToolsDrawerOpen = false
                    isResizerOpen = false
                }
            }
            ToolbarAction.RESIZER -> {
                if (!isResizerOpen) {
                    savedRowHeight = keyRowHeight
                    savedChinPadding = chinPadding
                    savedHorizontalMargin = horizontalMargin
                    isEmojiOpen = false
                    isToolsDrawerOpen = false
                }
                isResizerOpen = !isResizerOpen
            }
            ToolbarAction.NUMBER_ROW_TOGGLE -> {
                showNumberRow = !showNumberRow
            }
            ToolbarAction.THEME_TOGGLE -> {
                onThemeToggle()
            }
            ToolbarAction.CLIPBOARD -> {
                isToolsDrawerOpen = false
                isEmojiOpen = false
                isResizerOpen = false
                isClipboardOpen = !isClipboardOpen
                if (isClipboardOpen) {
                    clipboardManager?.syncFromSystemClipboard()
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                rootCoordinates = coords
                rootWidthPx = coords.size.width.toFloat()
            }
            .background(colorScheme.containerBackground)
            .padding(
                start = 4.dp + horizontalMargin,
                end = 4.dp + horizontalMargin,
                top = 4.dp,
                bottom = totalBottomPadding
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isTextEditOpen) {
                // Text Editing panel covers the toolbar and replaces the expand icon with back icon (matching Borno Keyboard)
                TextEditPanelView(
                    onTextAction = onTextAction,
                    onClose = { isTextEditOpen = false },
                    height = toolbarHeight + (keyRowHeight * 4)
                )
            } else {
                // Reorderable Toolbar along the top (46dp height with expand icon on far left)
                ReorderableToolbar(
                    toolOrder = toolOrder,
                    onToolOrderChange = { toolOrder = it },
                    onActionClick = handleToolbarAction,
                    onExpandToolsClick = {
                        isToolsDrawerOpen = !isToolsDrawerOpen
                        if (isToolsDrawerOpen) {
                            isEmojiOpen = false
                            isClipboardOpen = false
                            isResizerOpen = false
                        }
                    },
                    isExpandActive = isToolsDrawerOpen,
                    isEmojiActive = isEmojiOpen,
                    isResizerActive = isResizerOpen,
                    isNumberRowActive = showNumberRow,
                    isTextEditActive = false,
                    isClipboardActive = isClipboardOpen
                )

                // Suggestion Strip (displays transliterated Bengali words and swipe candidates)
                if (suggestions.isNotEmpty()) {
                    SuggestionStripView(
                        candidates = suggestions,
                        onCandidateClick = onCandidateSelect
                    )
                }

                // Switch between Tools Drawer, Clipboard Panel, Emoji Panel, and Keyboard Keys
                if (isToolsDrawerOpen) {
                    ToolsDrawerView(
                        toolOrder = toolOrder,
                        onToolOrderChange = { toolOrder = it },
                        onToolClick = handleToolbarAction,
                        height = keyRowHeight * 4
                    )
                } else if (isClipboardOpen) {
                    ClipboardPanelView(
                        clips = clipboardManager?.clips ?: emptyList(),
                        onClipClick = { clip ->
                            onPasteClip(clip.text)
                            isClipboardOpen = false
                        },
                        onTogglePin = { clip -> clipboardManager?.togglePin(clip) },
                        onDeleteClip = { clip -> clipboardManager?.deleteClip(clip) },
                        onClearAll = { clipboardManager?.clearAllUnpinned() },
                        onClose = { isClipboardOpen = false },
                        modifier = Modifier.height(keyRowHeight * 4)
                    )
                } else if (isEmojiOpen) {
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
                    if (showNumberRow && !displayLayout.id.startsWith("symbols")) {
                        NumberRowBar(
                            language = displayLayout.language,
                            onKeyTap = onKeyTap,
                            onKeyLongPress = onKeyLongPress
                        )
                    }

                    // Keyboard rows (stable layout without recomposition shifts)
                    displayLayout.rows.forEachIndexed { rowIndex, row ->
                    val isBottomRow = rowIndex == displayLayout.rows.lastIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(keyRowHeight)
                    ) {
                        row.keys.forEach { key ->
                            val effectiveKey = if (key.type == KeyType.SPACE && key.label.isBlank()) {
                                val spaceLabel = when {
                                    displayLayout.id.startsWith("symbols") -> "Space"
                                    displayLayout.id == "bn_borno" -> "বাংলা (Borno)"
                                    displayLayout.id == "bn_probhat" -> "বাংলা (Probhat)"
                                    else -> "English"
                                }
                                key.copy(label = spaceLabel)
                            } else {
                                key
                            }

                            KeycapView(
                                key = effectiveKey,
                                shiftState = shiftState,
                                isBottomRow = isBottomRow,
                                isSlidingSymbolMode = isSlidingSymbolMode,
                                modifier = Modifier
                                    .weight(effectiveKey.widthWeight)
                                    .fillMaxHeight(),
                                onKeyTap = onKeyTap,
                                onKeyLongPress = onKeyLongPress,
                                onKeyPositioned = { k, coords ->
                                    keyCoordinatesMap[k] = coords
                                },
                                onSpacebarTrackpad = { direction ->
                                    onTextAction(
                                        if (direction > 0) TextAction.MOVE_RIGHT else TextAction.MOVE_LEFT,
                                        false
                                    )
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}
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

                                        val itemCount = keyDef.moreKeys.size
                                        val rowCount = if (itemCount > 6) 2 else 1
                                        val itemsPerRow = (itemCount + rowCount - 1) / rowCount
                                        val chunkedRows = keyDef.moreKeys.chunked(itemsPerRow)
                                        val maxCols = itemsPerRow

                                        val horizontalPaddingPx = with(density) { 6.dp.toPx() }
                                        val colGapPx = with(density) { 3.dp.toPx() }
                                        val itemWidthPx = with(density) { 42.dp.toPx() }
                                        val totalWidthPx = maxCols * itemWidthPx + (maxCols - 1) * colGapPx + horizontalPaddingPx * 2

                                        val minLeftPx = with(density) { 8.dp.toPx() }
                                        val maxLeftPx = (rootWidthPx - totalWidthPx - minLeftPx).coerceAtLeast(minLeftPx)
                                        val clampedLeft = (keyRect.center.x - totalWidthPx / 2f).coerceIn(minLeftPx, maxLeftPx)

                                        val bottomRowIndex = if (rowCount > 1) rowCount - 1 else 0
                                        val bottomRowItems = chunkedRows[bottomRowIndex]
                                        val relX = keyRect.center.x - (clampedLeft + horizontalPaddingPx)
                                        val initialCol = (relX / (itemWidthPx + colGapPx)).toInt().coerceIn(0, bottomRowItems.lastIndex)
                                        val initialIndex = (bottomRowIndex * itemsPerRow + initialCol).coerceIn(0, keyDef.moreKeys.lastIndex)

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
                                        val itemCount = state.items.size
                                        val rowCount = if (itemCount > 6) 2 else 1
                                        val itemsPerRow = (itemCount + rowCount - 1) / rowCount
                                        val chunkedRows = state.items.chunked(itemsPerRow)
                                        val maxCols = itemsPerRow

                                        val horizontalPaddingPx = with(density) { 6.dp.toPx() }
                                        val verticalPaddingPx = with(density) { 6.dp.toPx() }
                                        val colGapPx = with(density) { 3.dp.toPx() }
                                        val rowGapPx = with(density) { 4.dp.toPx() }
                                        val itemWidthPx = with(density) { 42.dp.toPx() }
                                        val itemHeightPx = with(density) { 42.dp.toPx() }

                                        val totalWidthPx = maxCols * itemWidthPx + (maxCols - 1) * colGapPx + horizontalPaddingPx * 2
                                        val totalHeightPx = rowCount * itemHeightPx + (rowCount - 1) * rowGapPx + verticalPaddingPx * 2

                                        val minLeftPx = with(density) { 8.dp.toPx() }
                                        val maxLeftPx = (rootWidthPx - totalWidthPx - minLeftPx).coerceAtLeast(minLeftPx)
                                        val clampedLeft = (state.anchorRect.center.x - totalWidthPx / 2f).coerceIn(minLeftPx, maxLeftPx)

                                        val gapPx = with(density) { 6.dp.toPx() }
                                        val idealTop = state.anchorRect.top - totalHeightPx - gapPx
                                        val minTopPx = with(density) { 6.dp.toPx() }
                                        val popupTop = if (idealTop < minTopPx) minTopPx else idealTop

                                        val relX = fingerPosInRoot.x - (clampedLeft + horizontalPaddingPx)
                                        val relY = fingerPosInRoot.y - (popupTop + verticalPaddingPx)

                                        val hoveredRow = if (rowCount > 1) {
                                            (relY / (itemHeightPx + rowGapPx)).toInt().coerceIn(0, rowCount - 1)
                                        } else {
                                            0
                                        }

                                        val rowItems = chunkedRows[hoveredRow]
                                        val hoveredCol = (relX / (itemWidthPx + colGapPx)).toInt().coerceIn(0, rowItems.lastIndex)
                                        val newIndex = (hoveredRow * itemsPerRow + hoveredCol).coerceIn(0, state.items.lastIndex)

                                        if (newIndex != state.selectedIndex) {
                                            moreKeysState = state.copy(selectedIndex = newIndex)
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            } catch (_: Exception) {}
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
                                onSlidingDragStart = { _, keyCoords, initialDownPos ->
                                    val rCoords = rootCoordinates
                                    if (rCoords != null && rCoords.isAttached && keyCoords.isAttached) {
                                        isSlidingSymbolMode = true
                                        val downInRoot = rCoords.localPositionOf(keyCoords, initialDownPos)
                                        val keyCenterInRoot = rCoords.localPositionOf(
                                            keyCoords,
                                            Offset(keyCoords.size.width / 2f, keyCoords.size.height / 2f)
                                        )
                                        dragStartPosInRoot = downInRoot
                                        quickDragState = QuickSymbolDragState(
                                            startOffset = keyCenterInRoot,
                                            currentOffset = downInRoot
                                        )
                                    }
                                },
                                onSlidingDragMove = { deltaFromDown ->
                                    val rCoords = rootCoordinates ?: return@KeycapView
                                    val startPos = dragStartPosInRoot ?: return@KeycapView
                                    val currentFingerInRoot = startPos + deltaFromDown

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
                                            if (rect.contains(currentFingerInRoot)) {
                                                hoveredKey = k
                                                hoveredBounds = rect
                                                break
                                            }
                                        }
                                    }

                                    val symbol = if (hoveredKey != null && hoveredKey.type == KeyType.CHARACTER) {
                                        hoveredKey.label
                                    } else {
                                        null
                                    }

                                    quickDragState = quickDragState?.copy(
                                        currentOffset = currentFingerInRoot,
                                        hoveredKeyBounds = hoveredBounds,
                                        hoveredSymbol = symbol
                                    )
                                },
                                onSlidingDragEnd = { deltaFromDown ->
                                    val rCoords = rootCoordinates
                                    val startPos = dragStartPosInRoot
                                    if (rCoords != null && startPos != null) {
                                        val finalFingerInRoot = startPos + deltaFromDown
                                        for ((k, coords) in keyCoordinatesMap) {
                                            if (coords.isAttached) {
                                                val topLeft = rCoords.localPositionOf(coords, Offset.Zero)
                                                val rect = Rect(
                                                    topLeft,
                                                    Size(coords.size.width.toFloat(), coords.size.height.toFloat())
                                                )
                                                if (rect.contains(finalFingerInRoot)) {
                                                    if (k.type == KeyType.CHARACTER && k.label.isNotEmpty()) {
                                                        onKeyTap(k)
                                                    }
                                                    break
                                                }
                                            }
                                        }
                                    }
                                    isSlidingSymbolMode = false
                                    quickDragState = null
                                    dragStartPosInRoot = null
                                }
                            )
                        }
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

        // 2. Borno Keyboard rounded pill MoreKeys floating popup
        MoreKeysPopup(
            state = moreKeysState,
            keyboardWidthPx = if (rootWidthPx > 0f) rootWidthPx else 1080f
        )

        // 3. Live Keyboard Resizer Overlay (Borno Keyboard 4-way drag handles)
        if (isResizerOpen) {
            LiveResizerOverlay(
                currentRowHeight = keyRowHeight,
                currentChinPadding = chinPadding,
                currentHorizontalMargin = horizontalMargin,
                onRowHeightChange = { keyRowHeight = it },
                onChinPaddingChange = { chinPadding = it },
                onHorizontalMarginChange = { horizontalMargin = it },
                onReset = {
                    keyRowHeight = 60.dp
                    chinPadding = 8.dp
                    horizontalMargin = 0.dp
                },
                onDone = {
                    isResizerOpen = false
                },
                onCancel = {
                    keyRowHeight = savedRowHeight
                    chinPadding = savedChinPadding
                    horizontalMargin = savedHorizontalMargin
                    isResizerOpen = false
                },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}
