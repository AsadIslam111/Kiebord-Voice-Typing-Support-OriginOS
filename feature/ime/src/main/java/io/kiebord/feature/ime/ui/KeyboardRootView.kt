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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.roundToInt
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
import io.kiebord.feature.ime.ui.suggestion.ExpandedSuggestionsPanelView
import io.kiebord.feature.ime.ui.suggestion.SuggestionCandidate
import io.kiebord.feature.ime.ui.suggestion.SuggestionStripView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import io.kiebord.feature.ime.ui.toolbar.ReorderableToolbar
import io.kiebord.feature.ime.ui.toolbar.ToolbarAction
import io.kiebord.feature.ime.ui.toolbar.ToolsDrawerView
import io.kiebord.feature.ime.ui.dialog.LanguageSelectionDialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme

@Composable
fun KeyboardRootView(
    layout: KeyboardLayoutDef,
    shiftState: ShiftState,
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit,
    onTextAction: (TextAction, Boolean) -> Boolean = { _, _ -> false },
    onThemeToggle: () -> Unit = {},
    suggestions: List<SuggestionCandidate> = emptyList(),
    onCandidateSelect: (SuggestionCandidate) -> Unit = {},
    onUnlearnWord: (String) -> Unit = {},
    clipboardManager: SandboxedClipboardManager? = null,
    onPasteClip: (String) -> Unit = {},
    onGestureCommit: (List<Offset>, List<KeyCentroid>, Float) -> Unit = { _, _, _ -> },
    initialRowHeight: Dp = 58.dp,
    defaultShowNumberRow: Boolean = false,
    onNumberRowToggle: ((Boolean) -> Unit)? = null,
    showKeyBorders: Boolean = true,
    defaultShowSecondaryKeys: Boolean = true,
    onSecondaryKeysToggle: ((Boolean) -> Unit)? = null,
    defaultShowKeyPopups: Boolean = true,
    onKeyPopupsToggle: ((Boolean) -> Unit)? = null,
    onKeyPress: (KeyDef) -> Unit = {},
    onDeleteSwipeMove: (Float) -> Unit = {},
    onDeleteSwipeEnd: () -> Unit = {},
    onDeleteSwipeCancel: () -> Unit = {},
    navBarBottomInset: Dp = 0.dp,
    applySystemInsets: Boolean = true,
    isInteractive: Boolean = true,
    initialPinnedTools: List<ToolbarAction>? = null,
    initialDrawerTools: List<ToolbarAction>? = null,
    onToolbarToolsChange: ((List<ToolbarAction>, List<ToolbarAction>) -> Unit)? = null,
    onHeightScaleChange: ((Float) -> Unit)? = null,
    enabledLayoutIds: List<String> = emptyList(),
    onSwitchToLayout: (String) -> Unit = {},
    onOpenLanguagesSettings: () -> Unit = {},
    onOpenInputMethodPicker: () -> Unit = {},
    gestureTypingEnabled: Boolean = true,
    showGestureTrail: Boolean = true,
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

    var isLanguagePickerOpen by remember { mutableStateOf(false) }
    var unlearnCandidateTarget by remember { mutableStateOf<SuggestionCandidate?>(null) }
    var isSpacebarSwiping by remember { mutableStateOf(false) }
    var spacebarSwipeDeltaX by remember { mutableFloatStateOf(0f) }

    val effectiveLayoutList = remember(enabledLayoutIds, layout.id) {
        val list = enabledLayoutIds.ifEmpty { listOf(layout.id) }
        if (list.contains(layout.id)) list else list + layout.id
    }
    val currentLayoutIndex = remember(effectiveLayoutList, layout.id) {
        effectiveLayoutList.indexOf(layout.id).coerceAtLeast(0)
    }
    val commitThresholdPx = with(density) { 56.dp.toPx() }
    val n = effectiveLayoutList.size
    val targetSwipeIndex = if (isSpacebarSwiping && n > 1) {
        if (spacebarSwipeDeltaX >= commitThresholdPx) {
            (currentLayoutIndex + 1) % n
        } else if (spacebarSwipeDeltaX <= -commitThresholdPx) {
            (currentLayoutIndex - 1 + n) % n
        } else {
            currentLayoutIndex
        }
    } else currentLayoutIndex

    var lastHapticIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(isSpacebarSwiping, targetSwipeIndex) {
        if (isSpacebarSwiping && lastHapticIndex != targetSwipeIndex) {
            lastHapticIndex = targetSwipeIndex
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        } else if (!isSpacebarSwiping) {
            lastHapticIndex = null
        }
    }

    // Phase 5, 6 & 8: Toolbar, Emoji, Resizer, Number row, TextEdit, Clipboard, and Tools Drawer states
    val defaultPinned = listOf(
        ToolbarAction.TEXT_EDIT,
        ToolbarAction.CLIPBOARD,
        ToolbarAction.THEME_TOGGLE,
        ToolbarAction.RESIZER
    )
    val defaultDrawer = listOf(
        ToolbarAction.NUMBER_ROW_TOGGLE,
        ToolbarAction.SECONDARY_KEYS_TOGGLE,
        ToolbarAction.EMOJI,
        ToolbarAction.ONE_HANDED,
        ToolbarAction.SETTINGS
    )
    var pinnedTools by remember(initialPinnedTools) {
        val pinned = (initialPinnedTools ?: defaultPinned).distinct().ifEmpty { defaultPinned }
        mutableStateOf(pinned)
    }
    var drawerTools by remember(initialDrawerTools, initialPinnedTools) {
        val pSet = (initialPinnedTools ?: defaultPinned).toSet()
        val dList = ((initialDrawerTools ?: defaultDrawer).filter { it !in pSet } +
            ToolbarAction.values().filter { it !in pSet }).distinct()
        mutableStateOf(dList)
    }

    // Hit-testing bounding boxes in root coordinates
    val toolbarItemBounds = remember { mutableMapOf<ToolbarAction, Rect>() }
    val drawerItemBounds = remember { mutableMapOf<ToolbarAction, Rect>() }
    var drawerContainerBounds by remember { mutableStateOf<Rect?>(null) }

    val updateTools: (List<ToolbarAction>, List<ToolbarAction>) -> Unit = { newPinned, newDrawer ->
        val sanitizedPinned = newPinned.distinct()
        val pinnedSet = sanitizedPinned.toSet()
        val allActions = ToolbarAction.values().toList()
        val sanitizedDrawer = (newDrawer.filter { it !in pinnedSet } +
            allActions.filter { it !in pinnedSet }).distinct()

        toolbarItemBounds.keys.retainAll(sanitizedPinned.toSet())
        drawerItemBounds.keys.retainAll(sanitizedDrawer.toSet())

        pinnedTools = sanitizedPinned
        drawerTools = sanitizedDrawer
        onToolbarToolsChange?.invoke(sanitizedPinned, sanitizedDrawer)
    }

    val interchangeTools: (ToolbarAction, ToolbarAction) -> Unit = { act1, act2 ->
        if (act1 != act2) {
            val isAct1Pinned = pinnedTools.contains(act1)
            val isAct2Pinned = pinnedTools.contains(act2)
            if (isAct1Pinned && !isAct2Pinned) {
                val pIdx = pinnedTools.indexOf(act1)
                val dIdx = drawerTools.indexOf(act2)
                val newPinned = pinnedTools.toMutableList()
                val newDrawer = drawerTools.toMutableList()
                if (pIdx != -1) newPinned[pIdx] = act2
                if (dIdx != -1) newDrawer[dIdx] = act1
                updateTools(newPinned, newDrawer)
            } else if (!isAct1Pinned && isAct2Pinned) {
                val dIdx = drawerTools.indexOf(act1)
                val pIdx = pinnedTools.indexOf(act2)
                val newPinned = pinnedTools.toMutableList()
                val newDrawer = drawerTools.toMutableList()
                if (pIdx != -1) newPinned[pIdx] = act1
                if (dIdx != -1) newDrawer[dIdx] = act2
                updateTools(newPinned, newDrawer)
            }
        }
    }

    val moveToolbarToDrawer: (ToolbarAction) -> Unit = { toolbarAct ->
        if (pinnedTools.contains(toolbarAct)) {
            val newPinned = (pinnedTools - toolbarAct).distinct()
            val newDrawer = (drawerTools + toolbarAct).distinct()
            updateTools(newPinned, newDrawer)
        }
    }

    val moveDrawerToToolbar: (ToolbarAction) -> Unit = { drawerAct ->
        if (drawerTools.contains(drawerAct)) {
            val newDrawer = (drawerTools - drawerAct).distinct()
            val newPinned = (pinnedTools + drawerAct).distinct()
            updateTools(newPinned, newDrawer)
        }
    }

    var isToolbarOptionsVisible by remember { mutableStateOf(false) }
    var oneHandedSide by remember { mutableStateOf("OFF") } // "OFF", "RIGHT", "LEFT"
    var isEmojiOpen by remember { mutableStateOf(false) }

    // Active drag session across toolbar and drawer
    var activeDraggingFromToolbar by remember { mutableStateOf<ToolbarAction?>(null) }
    var activeDraggingFromDrawer by remember { mutableStateOf<ToolbarAction?>(null) }
    var currentFingerInRoot by remember { mutableStateOf<Offset?>(null) }

    val hoveredDrawerAction: ToolbarAction? = if (currentFingerInRoot != null) {
        val finger = currentFingerInRoot!!
        val activeDrag = activeDraggingFromToolbar ?: activeDraggingFromDrawer
        if (activeDrag != null) {
            val validDrawerBounds = drawerItemBounds.filterKeys { it in drawerTools && it != activeDrag }
            validDrawerBounds.entries.firstOrNull { (_, rect) ->
                rect.inflate(16f).contains(finger)
            }?.key
        } else null
    } else null

    val isOverDrawerBlankSpace: Boolean = if (activeDraggingFromToolbar != null && currentFingerInRoot != null && hoveredDrawerAction == null) {
        drawerContainerBounds?.contains(currentFingerInRoot!!) ?: false
    } else false

    var keyRowHeight by remember(initialRowHeight) { mutableStateOf(initialRowHeight) }
    val scaleFactor = (keyRowHeight.value / 58f).coerceIn(0.4f, 1.6f)
    val toolbarHeight = (44 * scaleFactor).dp

    val hoveredToolbarAction: ToolbarAction? = if (activeDraggingFromDrawer != null && currentFingerInRoot != null) {
        val finger = currentFingerInRoot!!
        if (finger.y <= toolbarHeight.value * 3f && pinnedTools.isNotEmpty() && rootWidthPx > 0f) {
            val slotWidth = rootWidthPx / pinnedTools.size
            val slotIndex = (finger.x / slotWidth).toInt().coerceIn(0, pinnedTools.lastIndex)
            pinnedTools.getOrNull(slotIndex)
        } else {
            val validToolbarBounds = toolbarItemBounds.filterKeys { it in pinnedTools && it != activeDraggingFromDrawer }
            validToolbarBounds.entries.firstOrNull { (_, rect) ->
                rect.inflate(24f).contains(finger)
            }?.key
        }
    } else null
    var isClipboardOpen by remember { mutableStateOf(false) }
    var isResizerOpen by remember { mutableStateOf(false) }
    var isTextEditOpen by remember { mutableStateOf(false) }
    var isToolsDrawerOpen by remember { mutableStateOf(false) }
    var isSuggestionsExpanded by remember { mutableStateOf(false) }
    var showNumberRow by remember(defaultShowNumberRow) { mutableStateOf(defaultShowNumberRow) }
    var showSecondaryKeys by remember(defaultShowSecondaryKeys) { mutableStateOf(defaultShowSecondaryKeys) }
    var showKeyPopups by remember(defaultShowKeyPopups) { mutableStateOf(defaultShowKeyPopups) }
    var chinPadding by remember(initialRowHeight) {
        val initialScale = (initialRowHeight.value / 58f).coerceIn(0.4f, 1.6f)
        mutableStateOf((8 * initialScale).dp)
    }
    var horizontalMargin by remember { mutableStateOf(0.dp) }

    // Snapshot state for Resizer Cancel button
    var savedRowHeight by remember { mutableStateOf(initialRowHeight) }
    var savedChinPadding by remember { mutableStateOf(chinPadding) }
    var savedHorizontalMargin by remember { mutableStateOf(0.dp) }

    // Obey navigation bar insets: maximum of Compose WindowInsets and platform WindowInsetsCompat
    val composeNavBottom = if (applySystemInsets) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }
    val effectiveNavBarBottom = maxOf(navBarBottomInset, composeNavBottom)
    val totalBottomPadding = chinPadding + effectiveNavBarBottom

    // Reset toolbar options & expanded suggestions visibility when suggestions clear
    LaunchedEffect(suggestions.isEmpty()) {
        if (suggestions.isEmpty()) {
            isToolbarOptionsVisible = false
            isSuggestionsExpanded = false
        }
    }

    // When in sliding symbol mode from ?123, seamlessly switch display to Symbols layout
    val displayLayout = if (isSlidingSymbolMode) PredefinedLayouts.Symbols else layout

    // Clear key coordinate cache whenever active layout changes to prevent stale hits
    LaunchedEffect(isSlidingSymbolMode, displayLayout.id) {
        keyCoordinatesMap.clear()
        gestureTrail = emptyList()
    }

    val isKeysInteractive = isInteractive && !isResizerOpen
    val numberRowEffectiveHeight = if (showNumberRow && !displayLayout.id.startsWith("symbols")) (44 * scaleFactor).dp else 0.dp
    val keyboardContentHeight = (keyRowHeight * 4) + numberRowEffectiveHeight

    val context = LocalContext.current
    val handleToolbarAction: (ToolbarAction) -> Unit = { action ->
        when (action) {
            ToolbarAction.TEXT_EDIT -> {
                isTextEditOpen = true
                isToolsDrawerOpen = false
                isEmojiOpen = false
                isResizerOpen = false
                isSuggestionsExpanded = false
            }
            ToolbarAction.EMOJI -> {
                isEmojiOpen = !isEmojiOpen
                if (isEmojiOpen) {
                    isToolsDrawerOpen = false
                    isResizerOpen = false
                    isSuggestionsExpanded = false
                }
            }
            ToolbarAction.RESIZER -> {
                if (!isResizerOpen) {
                    savedRowHeight = keyRowHeight
                    savedChinPadding = chinPadding
                    savedHorizontalMargin = horizontalMargin
                    isEmojiOpen = false
                    isToolsDrawerOpen = false
                    isSuggestionsExpanded = false
                }
                isResizerOpen = !isResizerOpen
            }
            ToolbarAction.NUMBER_ROW_TOGGLE -> {
                showNumberRow = !showNumberRow
                onNumberRowToggle?.invoke(showNumberRow)
            }
            ToolbarAction.SECONDARY_KEYS_TOGGLE -> {
                showSecondaryKeys = !showSecondaryKeys
                onSecondaryKeysToggle?.invoke(showSecondaryKeys)
            }
            ToolbarAction.KEY_POPUP_TOGGLE -> {
                showKeyPopups = !showKeyPopups
                onKeyPopupsToggle?.invoke(showKeyPopups)
            }
            ToolbarAction.THEME_TOGGLE -> {
                onThemeToggle()
            }
            ToolbarAction.CLIPBOARD -> {
                isToolsDrawerOpen = false
                isEmojiOpen = false
                isResizerOpen = false
                isSuggestionsExpanded = false
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

    val isActionActive: (ToolbarAction) -> Boolean = { action ->
        when (action) {
            ToolbarAction.NUMBER_ROW_TOGGLE -> showNumberRow
            ToolbarAction.SECONDARY_KEYS_TOGGLE -> showSecondaryKeys
            ToolbarAction.KEY_POPUP_TOGGLE -> showKeyPopups
            ToolbarAction.ONE_HANDED -> oneHandedSide != "OFF"
            ToolbarAction.EMOJI -> isEmojiOpen
            ToolbarAction.RESIZER -> isResizerOpen
            ToolbarAction.CLIPBOARD -> isClipboardOpen
            ToolbarAction.TEXT_EDIT -> isTextEditOpen
            else -> false
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
                start = (4 * scaleFactor).dp + horizontalMargin + oneHandedStart,
                end = (4 * scaleFactor).dp + horizontalMargin + oneHandedEnd,
                top = 0.dp,
                bottom = totalBottomPadding
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isTextEditOpen) {
                // Text Editing panel covers the toolbar and replaces the expand icon with back icon (matching Borno Keyboard)
                TextEditPanelView(
                    onTextAction = { action, isSelecting -> onTextAction(action, isSelecting) },
                    onClose = { isTextEditOpen = false },
                    height = toolbarHeight + keyboardContentHeight
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
                    val isToolsExpanded = isToolsDrawerOpen || isToolbarOptionsVisible || isSuggestionsExpanded

                    // 1. Expand / Back / Toggle button on far left
                    Box(
                        modifier = Modifier
                            .size((36 * scaleFactor).coerceAtLeast(26f).dp)
                            .clip(CircleShape)
                            .background(
                                if (isToolsExpanded) Color(0x22FFFFFF) else Color.Transparent
                            )
                            .clickable {
                                when {
                                    isSuggestionsExpanded -> {
                                        isSuggestionsExpanded = false
                                    }
                                    isToolsDrawerOpen || isToolbarOptionsVisible -> {
                                        isToolsDrawerOpen = false
                                        isToolbarOptionsVisible = false
                                    }
                                    else -> {
                                        isToolsDrawerOpen = true
                                        isToolbarOptionsVisible = true
                                    }
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
                                isSuggestionsExpanded -> "Collapse Suggestions"
                                isToolsDrawerOpen -> "Close Tools"
                                isToolbarOptionsVisible -> "Show Suggestions"
                                else -> "All Tools"
                            },
                            tint = if (isToolsExpanded) colorScheme.keyForeground else colorScheme.keyForeground.copy(alpha = 0.85f),
                            modifier = Modifier.size((22 * scaleFactor).coerceAtLeast(16f).dp)
                        )
                    }

                    Spacer(modifier = Modifier.width((4 * scaleFactor).coerceAtLeast(2f).dp))

                    // 2. Main content: Suggestion Strip INSTEAD OF Toolbar Options in the exact same row
                    if (suggestions.isNotEmpty() && !isToolbarOptionsVisible && !isToolsDrawerOpen) {
                        SuggestionStripView(
                            candidates = suggestions,
                            onCandidateClick = { candidate ->
                                isSuggestionsExpanded = false
                                onCandidateSelect(candidate)
                            },
                            onCandidateLongClick = { candidate ->
                                unlearnCandidateTarget = candidate
                            },
                            isExpanded = isSuggestionsExpanded,
                            onExpandToggle = {
                                isSuggestionsExpanded = !isSuggestionsExpanded
                                if (isSuggestionsExpanded) {
                                    isToolsDrawerOpen = false
                                    isClipboardOpen = false
                                    isEmojiOpen = false
                                    isResizerOpen = false
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    } else {
                        ReorderableToolbar(
                            pinnedTools = pinnedTools,
                            onPinnedToolsChange = { updateTools(it, drawerTools) },
                            onInterchangeWithDrawer = { toolbarAct, drawerAct ->
                                interchangeTools(toolbarAct, drawerAct)
                            },
                            onMoveToDrawer = { toolbarAct ->
                                moveToolbarToDrawer(toolbarAct)
                            },
                            onActionClick = handleToolbarAction,
                            rootCoordinates = rootCoordinates,
                            onReportItemBounds = { act, bounds -> toolbarItemBounds[act] = bounds },
                            hoveredDrawerAction = hoveredDrawerAction,
                            isOverDrawerBlankSpace = isOverDrawerBlankSpace,
                            hoveredByDrawerAction = hoveredToolbarAction,
                            isActionActive = isActionActive,
                            isEmojiActive = isEmojiOpen,
                            isResizerActive = isResizerOpen,
                            isNumberRowActive = showNumberRow,
                            isSecondaryKeysActive = showSecondaryKeys,
                            isOneHandedActive = oneHandedSide != "OFF",
                            isTextEditActive = isTextEditOpen,
                            isClipboardActive = isClipboardOpen,
                            isCustomizeMode = isToolsDrawerOpen,
                            scaleFactor = scaleFactor,
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

                // Switch between Expanded Suggestions Panel, Tools Drawer, Clipboard Panel, Emoji Panel, and Keyboard Keys
                if (isSuggestionsExpanded && suggestions.isNotEmpty()) {
                    ExpandedSuggestionsPanelView(
                        candidates = suggestions,
                        onCandidateClick = { candidate ->
                            isSuggestionsExpanded = false
                            onCandidateSelect(candidate)
                        },
                        onCandidateLongClick = { candidate ->
                            unlearnCandidateTarget = candidate
                        },
                        onDelete = {
                            onKeyTap(KeyDef(type = KeyType.DELETE))
                        },
                        onEnter = {
                            isSuggestionsExpanded = false
                            onKeyTap(KeyDef(type = KeyType.ACTION))
                        },
                        height = keyboardContentHeight
                    )
                } else if (isToolsDrawerOpen) {
                    ToolsDrawerView(
                        drawerTools = drawerTools,
                        onDrawerToolsChange = { updateTools(pinnedTools, it) },
                        onInterchangeWithToolbar = { drawerAct, toolbarAct ->
                            interchangeTools(drawerAct, toolbarAct)
                        },
                        onMoveToToolbar = { drawerAct ->
                            moveDrawerToToolbar(drawerAct)
                        },
                        onToolClick = handleToolbarAction,
                        height = keyboardContentHeight,
                        rootCoordinates = rootCoordinates,
                        onReportItemBounds = { act, bounds -> drawerItemBounds[act] = bounds },
                        onReportContainerBounds = { bounds -> drawerContainerBounds = bounds },
                        hoveredDrawerAction = hoveredDrawerAction,
                        isOverDrawerBlankSpace = isOverDrawerBlankSpace,
                        hoveredToolbarAction = hoveredToolbarAction,
                        isActionActive = isActionActive,
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
                        modifier = Modifier.height(keyboardContentHeight)
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
                        height = keyboardContentHeight
                    )
                } else {
                    // Shared MoreKeys & key press handlers for NumberRowBar and keyboard rows
                    val handleKeyPress: (KeyDef) -> Unit = { keyDef ->
                        if (moreKeysState != null) {
                            moreKeysState = null
                        }
                        onKeyPress(keyDef)
                    }

                    val handleShowMoreKeys: (KeyDef, LayoutCoordinates) -> Unit = { keyDef, keyCoords ->
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

                            val horizontalPaddingPx = with(density) { (6 * scaleFactor).dp.toPx() }
                            val colGapPx = with(density) { (3 * scaleFactor).dp.toPx() }
                            val itemWidthPx = with(density) { (42 * scaleFactor).dp.toPx() }
                            val totalWidthPx = maxCols * itemWidthPx + (maxCols - 1) * colGapPx + horizontalPaddingPx * 2

                            val minLeftPx = with(density) { (8 * scaleFactor).dp.toPx() }
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
                    }

                    val handleMoreKeysHover: (LayoutCoordinates, Offset) -> Unit = { keyCoords, touchPosInKey ->
                        val state = moreKeysState
                        val rCoords = rootCoordinates
                        if (state != null && rCoords != null && rCoords.isAttached && keyCoords.isAttached) {
                            val fingerPosInRoot = rCoords.localPositionOf(keyCoords, touchPosInKey)
                            val itemCount = state.items.size
                            val rowCount = if (itemCount > 6) 2 else 1
                            val itemsPerRow = (itemCount + rowCount - 1) / rowCount
                            val chunkedRows = state.items.chunked(itemsPerRow)
                            val maxCols = itemsPerRow

                            val horizontalPaddingPx = with(density) { (6 * scaleFactor).dp.toPx() }
                            val verticalPaddingPx = with(density) { (5 * scaleFactor).dp.toPx() }
                            val colGapPx = with(density) { (3 * scaleFactor).dp.toPx() }
                            val rowGapPx = with(density) { (4 * scaleFactor).dp.toPx() }
                            val itemWidthPx = with(density) { (42 * scaleFactor).dp.toPx() }
                            val itemHeightPx = with(density) { (42 * scaleFactor).dp.toPx() }

                            val totalWidthPx = maxCols * itemWidthPx + (maxCols - 1) * colGapPx + horizontalPaddingPx * 2
                            val totalHeightPx = rowCount * itemHeightPx + (rowCount - 1) * rowGapPx + verticalPaddingPx * 2

                            val minLeftPx = with(density) { (8 * scaleFactor).dp.toPx() }
                            val maxLeftPx = (rootWidthPx - totalWidthPx - minLeftPx).coerceAtLeast(minLeftPx)
                            val clampedLeft = (state.anchorRect.center.x - totalWidthPx / 2f).coerceIn(minLeftPx, maxLeftPx)

                            val gapDp = (12 * scaleFactor).coerceIn(8f, 16f).dp
                            val gapPx = with(density) { gapDp.toPx() }
                            val idealTop = state.anchorRect.top - totalHeightPx - gapPx
                            val minTopDp = 6.dp
                            val minTopPx = with(density) { minTopDp.toPx() }
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
                    }

                    val handleMoreKeysCommit: () -> Unit = {
                        val state = moreKeysState
                        if (state != null) {
                            val selectedChar = state.items.getOrNull(state.selectedIndex)
                                ?: state.items.firstOrNull()
                            if (!selectedChar.isNullOrEmpty()) {
                                onKeyTap(KeyDef(label = selectedChar, type = KeyType.CHARACTER))
                            }
                        }
                        moreKeysState = null
                    }

                    // Optional 5th Number Row at the top
                    if (showNumberRow && !displayLayout.id.startsWith("symbols")) {
                        NumberRowBar(
                            language = displayLayout.language,
                            height = (44 * scaleFactor).dp,
                            rowHeight = keyRowHeight,
                            showKeyBorders = showKeyBorders,
                            showKeyPopups = showKeyPopups,
                            isInteractive = isKeysInteractive,
                            onKeyPress = handleKeyPress,
                            onKeyTap = onKeyTap,
                            onKeyLongPress = onKeyLongPress,
                            onShowMoreKeys = handleShowMoreKeys,
                            onMoreKeysHover = handleMoreKeysHover,
                            onMoreKeysCommit = handleMoreKeysCommit
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
                                    .width((row.startPaddingDp * scaleFactor).dp)
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
                                showSecondaryKeys = if (!isInteractive) false else showSecondaryKeys,
                                showKeyPopups = showKeyPopups,
                                isInteractive = isKeysInteractive,
                                rowHeight = keyRowHeight,
                                gestureTypingEnabled = gestureTypingEnabled,
                                modifier = Modifier
                                    .weight(effectiveKey.widthWeight)
                                    .fillMaxHeight(),
                                onKeyPress = handleKeyPress,
                                onKeyTap = onKeyTap,
                                onKeyLongPress = { keyDef ->
                                    if (keyDef.type == KeyType.SWITCH_LANGUAGE) {
                                        isLanguagePickerOpen = true
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        } catch (_: Exception) {}
                                    } else {
                                        onKeyLongPress(keyDef)
                                    }
                                },
                                onDeleteSwipeMove = onDeleteSwipeMove,
                                onDeleteSwipeEnd = onDeleteSwipeEnd,
                                onDeleteSwipeCancel = onDeleteSwipeCancel,
                                onSpacebarSwipeMove = { deltaX ->
                                    isSpacebarSwiping = true
                                    spacebarSwipeDeltaX = deltaX
                                },
                                onSpacebarSwipeEnd = { finalDeltaX ->
                                    if (isSpacebarSwiping && n > 1) {
                                        val finalTarget = if (finalDeltaX >= commitThresholdPx) {
                                            (currentLayoutIndex + 1) % n
                                        } else if (finalDeltaX <= -commitThresholdPx) {
                                            (currentLayoutIndex - 1 + n) % n
                                        } else {
                                            currentLayoutIndex
                                        }
                                        val newLayoutId = effectiveLayoutList.getOrNull(finalTarget)
                                        if (newLayoutId != null && newLayoutId != layout.id) {
                                            onSwitchToLayout(newLayoutId)
                                            try {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } catch (_: Exception) {}
                                        }
                                        isSpacebarSwiping = false
                                        spacebarSwipeDeltaX = 0f
                                    }
                                },
                                onSpacebarDwellReady = {
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}
                                },
                                onKeyPositioned = { k, coords ->
                                    keyCoordinatesMap[k] = coords
                                },
                                onSpacebarTrackpad = { direction ->
                                    val didMove = onTextAction(
                                        if (direction > 0) TextAction.MOVE_RIGHT else TextAction.MOVE_LEFT,
                                        false
                                    )
                                    if (didMove) {
                                        try {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        } catch (_: Exception) {}
                                    }
                                },
                                onShowMoreKeys = handleShowMoreKeys,
                                onMoreKeysHover = handleMoreKeysHover,
                                onMoreKeysCommit = handleMoreKeysCommit,
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
                                    if (!gestureTypingEnabled) return@KeycapView
                                    val rCoords = rootCoordinates
                                    if (rCoords != null && rCoords.isAttached && keyCoords.isAttached) {
                                        val rootPos = rCoords.localPositionOf(keyCoords, posInKey)
                                        gestureTrail = listOf(rootPos)
                                    }
                                },
                                onGestureMove = { keyCoords, posInKey ->
                                    if (!gestureTypingEnabled) return@KeycapView
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
                                    if (!gestureTypingEnabled) {
                                        gestureTrail = emptyList()
                                        return@KeycapView
                                    }
                                    val trail = gestureTrail
                                    gestureTrail = emptyList()
                                    // Require intentional swipe path: at least 4 points and minimum length of 48.dp
                                    var totalDist = 0f
                                    for (i in 1 until trail.size) {
                                        totalDist += (trail[i] - trail[i - 1]).getDistance()
                                    }
                                    val minGestureDistPx = with(density) { 48.dp.toPx() }
                                    if (trail.size >= 4 && totalDist >= minGestureDistPx) {
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
                                    .width((row.endPaddingDp * scaleFactor).dp)
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
            keyboardWidthPx = if (rootWidthPx > 0f) rootWidthPx else 1080f,
            scaleFactor = scaleFactor
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
                    keyRowHeight = 58.dp
                    chinPadding = 8.dp
                    horizontalMargin = 0.dp
                    onHeightScaleChange?.invoke(1.0f)
                },
                onDone = {
                    val scale = (keyRowHeight.value / 58f).coerceIn(0.7f, 1.3f)
                    onHeightScaleChange?.invoke(scale)
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

        // 4. Single-colored Gesture Trail Overlay
        if (gestureTypingEnabled && showGestureTrail) {
            GestureTrailOverlay(
                trail = gestureTrail,
                accentColor = colorScheme.accentColor,
                modifier = Modifier.matchParentSize()
            )
        }

        // 5. Spacebar Layout Slider Floating Popup Overlay
        if (isSpacebarSwiping && effectiveLayoutList.size > 1) {
            SpacebarLayoutSliderOverlay(
                activeLayouts = effectiveLayoutList,
                currentIndex = currentLayoutIndex,
                swipeDeltaX = spacebarSwipeDeltaX,
                commitThresholdPx = commitThresholdPx,
                modifier = Modifier.matchParentSize()
            )
        }

        // 6. Language Selection Modal Dialog (Long-press on globe key)
        if (isLanguagePickerOpen) {
            LanguageSelectionDialog(
                currentLayoutId = layout.id,
                enabledLayoutIds = effectiveLayoutList,
                onSelectLayout = { selectedId ->
                    isLanguagePickerOpen = false
                    onSwitchToLayout(selectedId)
                },
                onMoreLayouts = {
                    isLanguagePickerOpen = false
                    onOpenLanguagesSettings()
                },
                onOtherKeyboards = {
                    isLanguagePickerOpen = false
                    onOpenInputMethodPicker()
                },
                onDismiss = { isLanguagePickerOpen = false },
                modifier = Modifier.matchParentSize()
            )
        }

        // 7. Unlearn Learned Word Confirmation Dialog
        if (unlearnCandidateTarget != null) {
            val candidateToUnlearn = unlearnCandidateTarget!!
            UnlearnWordConfirmationDialog(
                word = candidateToUnlearn.text,
                onConfirm = {
                    onUnlearnWord(candidateToUnlearn.text)
                    unlearnCandidateTarget = null
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) {}
                },
                onDismiss = { unlearnCandidateTarget = null },
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/**
 * Floating rounded pill overlay displayed when swiping horizontally on the spacebar
 * to slide through active keyboard layouts. Displays a single layout name at a time,
 * smoothly translating in 1:1 direct tracking with the thumb swipe, with clear commitment
 * threshold and gentle rubber-band clamping.
 */
@Composable
fun SpacebarLayoutSliderOverlay(
    activeLayouts: List<String>,
    currentIndex: Int,
    swipeDeltaX: Float,
    commitThresholdPx: Float,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    val density = LocalDensity.current
    val slotPx = with(density) { 140.dp.toPx() }
    val n = activeLayouts.size
    if (n <= 1) return

    val absDelta = Math.abs(swipeDeltaX)
    val clampedAbs = if (absDelta <= slotPx) {
        absDelta
    } else {
        slotPx + (absDelta - slotPx) * 0.22f
    }
    val signedOffset = if (swipeDeltaX >= 0f) clampedAbs else -clampedAbs
    val isCommitted = absDelta >= commitThresholdPx

    val leftIndex = (currentIndex + 1) % n
    val centerIndex = currentIndex
    val rightIndex = (currentIndex - 1 + n) % n

    val leftTranslationX = -slotPx + signedOffset
    val centerTranslationX = signedOffset
    val rightTranslationX = slotPx + signedOffset

    val targetIndex = when {
        swipeDeltaX >= commitThresholdPx -> leftIndex
        swipeDeltaX <= -commitThresholdPx -> rightIndex
        else -> centerIndex
    }

    Box(
        modifier = modifier.padding(bottom = 74.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(colorScheme.containerBackground.copy(alpha = 0.96f))
                .border(
                    width = 1.dp,
                    color = colorScheme.accentColor.copy(alpha = if (isCommitted) 0.85f else 0.45f),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .widthIn(min = 190.dp, max = 240.dp)
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = if (swipeDeltaX <= -commitThresholdPx) {
                    colorScheme.accentColor
                } else {
                    colorScheme.keyForeground.copy(alpha = 0.35f)
                },
                modifier = Modifier.size(18.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                // Item incoming from left (selected when swiping right)
                val leftName = PredefinedLayouts.displayName(activeLayouts[leftIndex])
                val leftDist = Math.abs(leftTranslationX)
                val leftProgress = (leftDist / slotPx).coerceIn(0f, 1f)
                val leftAlpha = (1f - leftProgress * 0.75f).coerceIn(0.15f, 1f)
                val leftScale = (1f - leftProgress * 0.12f).coerceIn(0.88f, 1f)
                val isLeftTarget = targetIndex == leftIndex
                Text(
                    text = leftName,
                    color = if (isLeftTarget) colorScheme.accentColor else colorScheme.keyForeground.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    fontWeight = if (isLeftTarget) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = KiebordTheme.fontFamily,
                    maxLines = 1,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = leftTranslationX
                            alpha = leftAlpha
                            scaleX = leftScale
                            scaleY = leftScale
                        }
                )

                // Current center item
                val centerName = PredefinedLayouts.displayName(activeLayouts[centerIndex])
                val centerDist = Math.abs(centerTranslationX)
                val centerProgress = (centerDist / slotPx).coerceIn(0f, 1f)
                val centerAlpha = (1f - centerProgress * 0.75f).coerceIn(0.15f, 1f)
                val centerScale = (1f - centerProgress * 0.12f).coerceIn(0.88f, 1f)
                val isCenterTarget = targetIndex == centerIndex
                Text(
                    text = centerName,
                    color = if (isCenterTarget) colorScheme.accentColor else colorScheme.keyForeground.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    fontWeight = if (isCenterTarget) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = KiebordTheme.fontFamily,
                    maxLines = 1,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = centerTranslationX
                            alpha = centerAlpha
                            scaleX = centerScale
                            scaleY = centerScale
                        }
                )

                // Item incoming from right (selected when swiping left)
                val rightName = PredefinedLayouts.displayName(activeLayouts[rightIndex])
                val rightDist = Math.abs(rightTranslationX)
                val rightProgress = (rightDist / slotPx).coerceIn(0f, 1f)
                val rightAlpha = (1f - rightProgress * 0.75f).coerceIn(0.15f, 1f)
                val rightScale = (1f - rightProgress * 0.12f).coerceIn(0.88f, 1f)
                val isRightTarget = targetIndex == rightIndex
                Text(
                    text = rightName,
                    color = if (isRightTarget) colorScheme.accentColor else colorScheme.keyForeground.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    fontWeight = if (isRightTarget) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = KiebordTheme.fontFamily,
                    maxLines = 1,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = rightTranslationX
                            alpha = rightAlpha
                            scaleX = rightScale
                            scaleY = rightScale
                        }
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = if (swipeDeltaX >= commitThresholdPx) {
                    colorScheme.accentColor
                } else {
                    colorScheme.keyForeground.copy(alpha = 0.35f)
                },
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Single-colored, smoothly tapering and fading gesture trail overlay.
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
            // Quadratic easing gives that slender comet tail and rounded fingertip
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

/**
 * Confirmation dialog popup shown when a learned suggestion candidate is long-pressed,
 * prompting the user to unlearn and remove it from the personalized dictionary.
 */
@Composable
fun UnlearnWordConfirmationDialog(
    word: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme

    Box(
        modifier = modifier
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colorScheme.containerBackground)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {} // consume touch events
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Remove learned word?",
                color = colorScheme.keyForeground,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = KiebordTheme.fontFamily,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Do you want to remove \"$word\" from your learned suggestions?",
                color = colorScheme.keyForeground.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontFamily = KiebordTheme.fontFamily,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        color = colorScheme.keyForeground.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = KiebordTheme.fontFamily
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.accentColor)
                        .clickable { onConfirm() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Remove",
                        color = colorScheme.actionKeyForeground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = KiebordTheme.fontFamily
                    )
                }
            }
        }
    }
}

