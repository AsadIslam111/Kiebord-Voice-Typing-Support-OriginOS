package io.kiebord.feature.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import io.kiebord.feature.ime.gesture.KeyCentroid
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
    onGestureCommit: (List<Offset>, List<KeyCentroid>, Float) -> Unit = { _, _, _ -> },
    initialRowHeight: Dp = 60.dp,
    defaultShowNumberRow: Boolean = false,
    showKeyBorders: Boolean = true,
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
    var gestureTrail by remember { mutableStateOf<List<Offset>>(emptyList()) }

    // Phase 5, 6 & 8: Toolbar, Emoji, Resizer, Number row, TextEdit, Clipboard, and Tools Drawer states
    var pinnedTools by remember {
        mutableStateOf(
            listOf(
                ToolbarAction.TEXT_EDIT,
                ToolbarAction.CLIPBOARD,
                ToolbarAction.THEME_TOGGLE,
                ToolbarAction.RESIZER
            )
        )
    }
    var drawerTools by remember {
        mutableStateOf(
            listOf(
                ToolbarAction.NUMBER_ROW_TOGGLE,
                ToolbarAction.EMOJI,
                ToolbarAction.ONE_HANDED,
                ToolbarAction.SETTINGS
            )
        )
    }
    var isToolbarOptionsVisible by remember { mutableStateOf(false) }
    var oneHandedSide by remember { mutableStateOf("OFF") } // "OFF", "RIGHT", "LEFT"
    var isEmojiOpen by remember { mutableStateOf(false) }

    // Hit-testing bounding boxes in root coordinates
    val toolbarItemBounds = remember { mutableMapOf<ToolbarAction, Rect>() }
    val drawerItemBounds = remember { mutableMapOf<ToolbarAction, Rect>() }
    var drawerContainerBounds by remember { mutableStateOf<Rect?>(null) }

    // Active drag session across toolbar and drawer
    var activeDraggingFromToolbar by remember { mutableStateOf<ToolbarAction?>(null) }
    var activeDraggingFromDrawer by remember { mutableStateOf<ToolbarAction?>(null) }
    var currentFingerInRoot by remember { mutableStateOf<Offset?>(null) }

    val hoveredDrawerAction: ToolbarAction? = if (activeDraggingFromToolbar != null && currentFingerInRoot != null) {
        drawerItemBounds.entries.firstOrNull { (_, rect) ->
            rect.contains(currentFingerInRoot!!)
        }?.key
    } else null

    val isOverDrawerBlankSpace: Boolean = if (activeDraggingFromToolbar != null && currentFingerInRoot != null && hoveredDrawerAction == null) {
        drawerContainerBounds?.contains(currentFingerInRoot!!) ?: false
    } else false

    val hoveredToolbarAction: ToolbarAction? = if (activeDraggingFromDrawer != null && currentFingerInRoot != null) {
        toolbarItemBounds.entries.firstOrNull { (_, rect) ->
            rect.contains(currentFingerInRoot!!)
        }?.key
    } else null
    var isClipboardOpen by remember { mutableStateOf(false) }
    var isResizerOpen by remember { mutableStateOf(false) }
    var isTextEditOpen by remember { mutableStateOf(false) }
    var isToolsDrawerOpen by remember { mutableStateOf(false) }
    var showNumberRow by remember(defaultShowNumberRow) { mutableStateOf(defaultShowNumberRow) }
    var keyRowHeight by remember(initialRowHeight) { mutableStateOf(initialRowHeight) }
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

    // Reset toolbar options visibility when suggestions clear
    LaunchedEffect(suggestions.isEmpty()) {
        if (suggestions.isEmpty()) {
            isToolbarOptionsVisible = false
        }
    }

    // When in sliding symbol mode from ?123, seamlessly switch display to Symbols layout
    val displayLayout = if (isSlidingSymbolMode) PredefinedLayouts.Symbols else layout

    // Clear key coordinate cache whenever active layout changes to prevent stale hits
    LaunchedEffect(isSlidingSymbolMode, displayLayout.id) {
        keyCoordinatesMap.clear()
        gestureTrail = emptyList()
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
            ToolbarAction.ONE_HANDED -> {
                oneHandedSide = when (oneHandedSide) {
                    "OFF" -> "RIGHT"
                    "RIGHT" -> "LEFT"
                    else -> "OFF"
                }
            }
            ToolbarAction.SETTINGS -> {
                try {
                    val intent = android.content.Intent(context, Class.forName("io.kiebord.app.MainActivity")).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {}
            }
        }
    }

    val oneHandedStart = if (oneHandedSide == "RIGHT") 56.dp else 0.dp
    val oneHandedEnd = if (oneHandedSide == "LEFT") 56.dp else 0.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                rootCoordinates = coords
                rootWidthPx = coords.size.width.toFloat()
            }
            .background(colorScheme.containerBackground)
            .padding(
                start = 4.dp + horizontalMargin + oneHandedStart,
                end = 4.dp + horizontalMargin + oneHandedEnd,
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
                // Single unified Top Bar row (46dp height)
                // The suggestion strip is shown INSTEAD OF the togglable toolbar options (never a new row!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(toolbarHeight)
                        .background(colorScheme.containerBackground)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isToolsExpanded = isToolsDrawerOpen || isToolbarOptionsVisible

                    // 1. Expand / Back / Toggle button on far left
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isToolsExpanded) Color(0x22FFFFFF) else Color.Transparent
                            )
                            .clickable {
                                when {
                                    isToolsDrawerOpen -> isToolsDrawerOpen = false
                                    isToolbarOptionsVisible -> isToolbarOptionsVisible = false
                                    suggestions.isNotEmpty() -> isToolbarOptionsVisible = true
                                    else -> isToolsDrawerOpen = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isToolsExpanded -> Icons.AutoMirrored.Outlined.ArrowBack
                                else -> Icons.Outlined.GridView
                            },
                            contentDescription = when {
                                isToolsDrawerOpen -> "Close Tools"
                                isToolbarOptionsVisible -> "Show Suggestions"
                                else -> "All Tools"
                            },
                            tint = if (isToolsExpanded) colorScheme.keyForeground else colorScheme.keyForeground.copy(alpha = 0.85f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // 2. Main content: Suggestion Strip INSTEAD OF Toolbar Options in the exact same row
                    if (suggestions.isNotEmpty() && !isToolbarOptionsVisible && !isToolsDrawerOpen) {
                        SuggestionStripView(
                            candidates = suggestions,
                            onCandidateClick = onCandidateSelect,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    } else {
                        ReorderableToolbar(
                            pinnedTools = pinnedTools,
                            onPinnedToolsChange = { pinnedTools = it },
                            onInterchangeWithDrawer = { toolbarAct, drawerAct ->
                                val newPinned = pinnedTools.map { if (it == toolbarAct) drawerAct else it }
                                val newDrawer = drawerTools.map { if (it == drawerAct) toolbarAct else it }
                                pinnedTools = newPinned
                                drawerTools = newDrawer
                            },
                            onMoveToDrawer = { toolbarAct ->
                                if (pinnedTools.size > 1) {
                                    pinnedTools = pinnedTools - toolbarAct
                                    if (!drawerTools.contains(toolbarAct)) {
                                        drawerTools = drawerTools + toolbarAct
                                    }
                                }
                            },
                            onActionClick = handleToolbarAction,
                            rootCoordinates = rootCoordinates,
                            onReportItemBounds = { act, bounds -> toolbarItemBounds[act] = bounds },
                            hoveredDrawerAction = hoveredDrawerAction,
                            isOverDrawerBlankSpace = isOverDrawerBlankSpace,
                            hoveredByDrawerAction = hoveredToolbarAction,
                            isEmojiActive = isEmojiOpen,
                            isResizerActive = isResizerOpen,
                            isNumberRowActive = showNumberRow,
                            isTextEditActive = false,
                            isClipboardActive = isClipboardOpen,
                            isCustomizeMode = isToolsDrawerOpen,
                            onDraggingStateChange = { action, fingerPos ->
                                activeDraggingFromToolbar = action
                                currentFingerInRoot = fingerPos
                                if (action != null && fingerPos != null && !isToolsDrawerOpen) {
                                    val drawerRect = drawerContainerBounds
                                    if (drawerRect != null && fingerPos.y >= drawerRect.top - 20) {
                                        isToolsDrawerOpen = true
                                    } else if (fingerPos.y >= toolbarHeight.value * 2) {
                                        isToolsDrawerOpen = true
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // Switch between Tools Drawer, Clipboard Panel, Emoji Panel, and Keyboard Keys
                if (isToolsDrawerOpen) {
                    ToolsDrawerView(
                        drawerTools = drawerTools,
                        onDrawerToolsChange = { drawerTools = it },
                        onInterchangeWithToolbar = { drawerAct, toolbarAct ->
                            val newPinned = pinnedTools.map { if (it == toolbarAct) drawerAct else it }
                            val newDrawer = drawerTools.map { if (it == drawerAct) toolbarAct else it }
                            pinnedTools = newPinned
                            drawerTools = newDrawer
                        },
                        onMoveToToolbar = { drawerAct ->
                            drawerTools = drawerTools - drawerAct
                            if (!pinnedTools.contains(drawerAct)) {
                                pinnedTools = pinnedTools + drawerAct
                            }
                        },
                        onToolClick = handleToolbarAction,
                        height = keyRowHeight * 4,
                        rootCoordinates = rootCoordinates,
                        onReportItemBounds = { act, bounds -> drawerItemBounds[act] = bounds },
                        onReportContainerBounds = { bounds -> drawerContainerBounds = bounds },
                        hoveredDrawerAction = hoveredDrawerAction,
                        isOverDrawerBlankSpace = isOverDrawerBlankSpace,
                        hoveredToolbarAction = hoveredToolbarAction,
                        onDraggingStateChange = { action, fingerPos ->
                            activeDraggingFromDrawer = action
                            currentFingerInRoot = fingerPos
                        }
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
                            showKeyBorders = showKeyBorders,
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
                        if (row.startPaddingDp > 0f) {
                            Spacer(
                                modifier = Modifier
                                    .width(row.startPaddingDp.dp)
                                    .fillMaxHeight()
                            )
                        }
                        if (row.startPaddingWeight > 0f) {
                            Spacer(
                                modifier = Modifier
                                    .weight(row.startPaddingWeight)
                                    .fillMaxHeight()
                            )
                        }

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
                                showKeyBorders = showKeyBorders,
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
                                },
                                onGestureStart = { keyCoords, posInKey ->
                                    val rCoords = rootCoordinates
                                    if (rCoords != null && rCoords.isAttached && keyCoords.isAttached) {
                                        val rootPos = rCoords.localPositionOf(keyCoords, posInKey)
                                        gestureTrail = listOf(rootPos)
                                    }
                                },
                                onGestureMove = { keyCoords, posInKey ->
                                    val rCoords = rootCoordinates
                                    if (rCoords != null && rCoords.isAttached && keyCoords.isAttached) {
                                        val rootPos = rCoords.localPositionOf(keyCoords, posInKey)
                                        val lastPos = gestureTrail.lastOrNull()
                                        if (lastPos == null || (rootPos - lastPos).getDistance() >= 4f) {
                                            gestureTrail = gestureTrail + rootPos
                                        }
                                    }
                                },
                                onGestureEnd = {
                                    val trail = gestureTrail
                                    gestureTrail = emptyList()
                                    if (trail.size >= 2) {
                                        val rCoords = rootCoordinates
                                        if (rCoords != null && rCoords.isAttached) {
                                            val centroids = mutableListOf<KeyCentroid>()
                                            var totalWidth = 0f
                                            var count = 0
                                            for ((k, coords) in keyCoordinatesMap) {
                                                if (k.type == KeyType.CHARACTER && coords.isAttached) {
                                                    val topLeft = rCoords.localPositionOf(coords, Offset.Zero)
                                                    val width = coords.size.width.toFloat()
                                                    val height = coords.size.height.toFloat()
                                                    val cx = topLeft.x + width / 2f
                                                    val cy = topLeft.y + height / 2f
                                                    val ch = if (k.label.isNotEmpty()) k.label.lowercase() else ""
                                                    if (ch.isNotEmpty()) {
                                                        centroids.add(KeyCentroid(ch, cx, cy))
                                                        totalWidth += width
                                                        count++
                                                    }
                                                }
                                            }
                                            val keyWidthPx = if (count > 0) totalWidth / count else 60f
                                            onGestureCommit(trail, centroids, keyWidthPx)
                                        }
                                    }
                                }
                            )
                        }

                        if (row.endPaddingWeight > 0f) {
                            Spacer(
                                modifier = Modifier
                                    .weight(row.endPaddingWeight)
                                    .fillMaxHeight()
                            )
                        }
                        if (row.endPaddingDp > 0f) {
                            Spacer(
                                modifier = Modifier
                                    .width(row.endPaddingDp.dp)
                                    .fillMaxHeight()
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

        // 4. Single-colored Borno Keyboard-style Gesture Trail Overlay
        GestureTrailOverlay(
            trail = gestureTrail,
            accentColor = colorScheme.accentColor,
            modifier = Modifier.matchParentSize()
        )
    }
}

/**
 * Single-colored, smoothly tapering and fading gesture trail overlay styled like Borno Keyboard.
 * Renders an ink-ribbon comet trailing the fingertip that dynamically evaporates at the tail.
 */
@Composable
fun GestureTrailOverlay(
    trail: List<Offset>,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    if (trail.size < 2) return

    val maxTrailDistance = with(LocalDensity.current) { 220.dp.toPx() }
    val headWidthPx = with(LocalDensity.current) { 9.dp.toPx() }
    val tailWidthPx = with(LocalDensity.current) { 1.5.dp.toPx() }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        // Collect visible trailing points backwards from current fingertip up to maxTrailDistance
        val visiblePoints = mutableListOf<Offset>()
        visiblePoints.add(trail.last())
        var accumulatedDist = 0f

        for (i in trail.size - 1 downTo 1) {
            val pHead = trail[i]
            val pPrev = trail[i - 1]
            val segDist = (pHead - pPrev).getDistance()
            if (accumulatedDist + segDist <= maxTrailDistance) {
                accumulatedDist += segDist
                visiblePoints.add(pPrev)
            } else {
                // Interpolate cutoff point to smoothly terminate the tail
                val remaining = maxTrailDistance - accumulatedDist
                if (remaining > 0f && segDist > 0f) {
                    val ratio = remaining / segDist
                    val cutoff = Offset(
                        pHead.x + (pPrev.x - pHead.x) * ratio,
                        pHead.y + (pPrev.y - pHead.y) * ratio
                    )
                    visiblePoints.add(cutoff)
                }
                break
            }
        }

        if (visiblePoints.size < 2) {
            visiblePoints.clear()
            visiblePoints.addAll(trail.takeLast(2))
        }

        // visiblePoints was collected [head -> tail]. Reverse to draw [tail -> head]
        val points = visiblePoints.asReversed()
        val n = points.size

        // Draw single-colored ribbon tapering quadratically from tail to head
        for (i in 0 until n - 1) {
            val p0 = points[i]
            val p1 = points[i + 1]
            val progress = (i + 1).toFloat() / (n - 1).toFloat()
            // Quadratic easing gives that iconic slender Borno Keyboard comet tail and rounded fingertip
            val widthFactor = progress * progress
            val strokeWidth = tailWidthPx + widthFactor * (headWidthPx - tailWidthPx)
            val alpha = 0.08f + progress * 0.77f // 0.08 at tail -> 0.85 at head

            drawLine(
                color = accentColor.copy(alpha = alpha),
                start = p0,
                end = p1,
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        // Smooth rounded fingertip at the head
        val headPoint = points.last()
        drawCircle(
            color = accentColor.copy(alpha = 0.85f),
            radius = headWidthPx / 2f,
            center = headPoint
        )
    }
}

