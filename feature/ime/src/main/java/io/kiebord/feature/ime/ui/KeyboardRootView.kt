package io.kiebord.feature.ime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.kiebord.common.model.KeyDef
import io.kiebord.common.model.KeyType
import io.kiebord.common.model.KeyboardLayoutDef
import io.kiebord.common.model.ShiftState
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.theme.KiebordTheme

@Composable
fun KeyboardRootView(
    layout: KeyboardLayoutDef,
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    keyRowHeight: Dp = 60.dp,
    showNumberRow: Boolean = false,
    onKeyTap: (KeyDef) -> Unit,
    onKeyLongPress: (KeyDef) -> Unit = {}
) {
    val colorScheme = KiebordTheme.colorScheme

    var rootWindowBounds by remember { mutableStateOf(Rect.Zero) }
    val keyBoundsMap = remember { mutableStateMapOf<KeyDef, Rect>() }

    var moreKeysState by remember { mutableStateOf<MoreKeysPopupState?>(null) }
    var quickDragState by remember { mutableStateOf<QuickSymbolDragState?>(null) }

    // While dragging from ?123, render the Symbols layer under the finger
    val effectiveLayout = if (quickDragState != null) {
        PredefinedLayouts.Symbols
    } else {
        layout
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                rootWindowBounds = coords.boundsInWindow()
            }
            .background(colorScheme.containerBackground)
            .navigationBarsPadding()
            .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            onKeyLongPress = { longPressedKey ->
                                if (longPressedKey.moreKeys.isNotEmpty()) {
                                    val localBounds = keyBoundsMap[longPressedKey] ?: Rect.Zero
                                    moreKeysState = MoreKeysPopupState(
                                        anchorKey = longPressedKey,
                                        anchorBounds = localBounds,
                                        items = longPressedKey.moreKeys,
                                        selectedIndex = 0
                                    )
                                } else {
                                    onKeyLongPress(longPressedKey)
                                }
                            },
                            onKeyPositioned = { k, windowRect ->
                                if (rootWindowBounds != Rect.Zero) {
                                    val localRect = Rect(
                                        windowRect.left - rootWindowBounds.left,
                                        windowRect.top - rootWindowBounds.top,
                                        windowRect.right - rootWindowBounds.left,
                                        windowRect.bottom - rootWindowBounds.top
                                    )
                                    keyBoundsMap[k] = localRect
                                }
                            },
                            onSlidingDragStart = { _, _ ->
                                val startLocal = keyBoundsMap[key]?.center ?: Offset.Zero
                                quickDragState = QuickSymbolDragState(
                                    startOffset = startLocal,
                                    currentOffset = startLocal
                                )
                            },
                            onSlidingDragMove = { windowPos ->
                                if (rootWindowBounds != Rect.Zero) {
                                    val localPos = Offset(
                                        windowPos.x - rootWindowBounds.left,
                                        windowPos.y - rootWindowBounds.top
                                    )
                                    val hoveredEntry = keyBoundsMap.entries.firstOrNull { it.value.contains(localPos) }
                                    quickDragState = quickDragState?.copy(
                                        currentOffset = localPos,
                                        hoveredKeyBounds = hoveredEntry?.value
                                    )
                                }
                            },
                            onSlidingDragEnd = { windowPos ->
                                if (rootWindowBounds != Rect.Zero) {
                                    val localPos = Offset(
                                        windowPos.x - rootWindowBounds.left,
                                        windowPos.y - rootWindowBounds.top
                                    )
                                    val hoveredKey = keyBoundsMap.entries.firstOrNull { it.value.contains(localPos) }?.key
                                    if (hoveredKey != null && hoveredKey.type != KeyType.SYMBOLS) {
                                        onKeyTap(hoveredKey)
                                    }
                                }
                                quickDragState = null
                            }
                        )
                    }
                }
            }
        }

        // MoreKeys floating bubble overlay
        MoreKeysPopup(
            state = moreKeysState,
            modifier = Modifier.fillMaxWidth()
        )

        // Neon laser guideline overlay for sliding symbol input
        QuickSymbolDragOverlay(
            state = quickDragState
        )
    }
}
