package io.kiebord.feature.ime.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.common.model.KeyDef
import io.kiebord.feature.ime.R
import io.kiebord.theme.KeyboardColorScheme
import io.kiebord.theme.KiebordTheme
import kotlin.math.roundToInt

data class MoreKeysPopupState(
    val anchorKey: KeyDef,
    val anchorRect: Rect,
    val items: List<String>,
    val dims: MoreKeysDimensions = MoreKeysLayoutHelper.calculateDimensions(1f, 1f, 1080f),
    val layoutResult: MoreKeysLayoutResult = MoreKeysLayoutHelper.computeLayout(items.size, anchorKey.moreKeysColumns, anchorRect.center.x, 1080f, 100f),
    val clampedLeft: Float = 0f,
    val popupTop: Float = 0f,
    val pivotFractionX: Float = 0.5f,
    val allowedLeft: Float = 0f,
    val allowedRight: Float = 1080f,
    val allowedTop: Float = 0f,
    val allowedBottom: Float = 1920f,
    val selectedIndex: Int = 0
)

@Composable
fun MoreKeysPopup(
    state: MoreKeysPopupState?,
    selectedIndexProvider: () -> Int = { 0 },
    keyboardWidthPx: Float = 0f,
    keyboardLeftPx: Float = 0f,
    keyboardRightPx: Float = keyboardWidthPx,
    scaleFactor: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    if (state == null || state.items.isEmpty()) return

    val density = LocalDensity.current
    val colorScheme = KiebordTheme.colorScheme

    val dims = state.dims
    val layoutResult = state.layoutResult

    val itemWidthDp = with(density) { dims.itemWidthPx.toDp() }
    val itemHeightDp = with(density) { dims.itemHeightPx.toDp() }
    val colGapDp = with(density) { dims.colGapPx.toDp() }
    val rowGapDp = with(density) { dims.rowGapPx.toDp() }
    val horizontalPaddingDp = with(density) { dims.horizontalPaddingPx.toDp() }
    val verticalPaddingDp = with(density) { dims.verticalPaddingPx.toDp() }

    val isLight = colorScheme == KeyboardColorScheme.DefaultLight
    val currentSelectedIndex = selectedIndexProvider()

    // Outer Box holds the FIXED precomputed position so there is ZERO jumping or re-layout
    Box(
        modifier = modifier
            .offset { IntOffset(state.clampedLeft.roundToInt(), state.popupTop.roundToInt()) }
            .wrapContentSize()
    ) {
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(
                initialScale = 0.85f,
                transformOrigin = TransformOrigin(state.pivotFractionX, 1.0f),
                animationSpec = tween(durationMillis = 80, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 80)),
            exit = scaleOut(
                targetScale = 0.85f,
                transformOrigin = TransformOrigin(state.pivotFractionX, 1.0f),
                animationSpec = tween(durationMillis = 50)
            ) + fadeOut(animationSpec = tween(durationMillis = 50))
        ) {
            // Material 3 Borderless Floating Capsule with Soft Shadow
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .shadow(
                        elevation = (4 * scaleFactor).dp,
                        shape = RoundedCornerShape(20.dp),
                        ambientColor = Color(0x38000000),
                        spotColor = Color(0x30000000)
                    )
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorScheme.moreKeysBackground)
                    .then(
                        if (!isLight && colorScheme.keyBorder.alpha > 0.01f) {
                            Modifier.border(
                                width = 0.5.dp,
                                color = Color(0x22FFFFFF),
                                shape = RoundedCornerShape(20.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = horizontalPaddingDp, vertical = verticalPaddingDp)
            ) {
                // Keys Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(rowGapDp),
                    horizontalAlignment = Alignment.Start
                ) {
                    layoutResult.grid.forEach { rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(colGapDp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEach { originalItemIndex ->
                                if (originalItemIndex == null) {
                                    androidx.compose.foundation.layout.Spacer(
                                        modifier = Modifier.size(itemWidthDp, itemHeightDp)
                                    )
                                } else {
                                    val item = state.items[originalItemIndex]
                                    val isSelected = originalItemIndex == currentSelectedIndex

                                    MoreKeyCell(
                                        item = item,
                                        isSelected = isSelected,
                                        widthDp = itemWidthDp,
                                        heightDp = itemHeightDp,
                                        fontSizeSp = dims.fontSizeSp,
                                        iconSizeDp = dims.iconSizeDp,
                                        colorScheme = colorScheme
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoreKeysPopup(
    state: MoreKeysPopupState?,
    selectedIndex: Int,
    keyboardWidthPx: Float = 0f,
    keyboardLeftPx: Float = 0f,
    keyboardRightPx: Float = keyboardWidthPx,
    scaleFactor: Float = 1.0f,
    modifier: Modifier = Modifier
) = MoreKeysPopup(
    state = state,
    selectedIndexProvider = { selectedIndex },
    keyboardWidthPx = keyboardWidthPx,
    keyboardLeftPx = keyboardLeftPx,
    keyboardRightPx = keyboardRightPx,
    scaleFactor = scaleFactor,
    modifier = modifier
)

@Composable
private fun MoreKeyCell(
    item: String,
    isSelected: Boolean,
    widthDp: Dp,
    heightDp: Dp,
    fontSizeSp: Float,
    iconSizeDp: Float,
    colorScheme: KeyboardColorScheme
) {
    val contentColor = if (isSelected) Color.White else colorScheme.keyForeground
    val cellBackground = if (isSelected) colorScheme.accentColor else Color.Transparent

    Box(
        modifier = Modifier
            .size(widthDp, heightDp)
            .clip(CircleShape)
            .background(cellBackground),
        contentAlignment = Alignment.Center
    ) {
        val actionIconSize = iconSizeDp.dp
        when (item) {
            "ACTION_EMOJI", "action:emoji", "EMOJI" -> {
                Icon(
                    imageVector = Icons.Outlined.Mood,
                    contentDescription = "Emoji",
                    tint = contentColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            "ACTION_SELECT_ALL", "action:select_all", "SELECT_ALL" -> {
                Icon(
                    imageVector = Icons.Outlined.SelectAll,
                    contentDescription = "Select All",
                    tint = contentColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            "ACTION_CUT", "action:cut", "CUT" -> {
                Icon(
                    imageVector = Icons.Outlined.ContentCut,
                    contentDescription = "Cut",
                    tint = contentColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            "ACTION_COPY", "action:copy", "COPY" -> {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy",
                    tint = contentColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            "ACTION_PASTE", "action:paste", "PASTE" -> {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = "Paste",
                    tint = contentColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            "ACTION_UNDO", "action:undo", "UNDO" -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_undo),
                    contentDescription = "Undo",
                    tint = contentColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            "ACTION_REDO", "action:redo", "REDO" -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_redo),
                    contentDescription = "Redo",
                    tint = contentColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            "ACTION_SETTINGS", "action:settings", "SETTINGS" -> {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = contentColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            "ACTION_ONE_HANDED", "action:one_handed", "ONE_HANDED" -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_one_handed),
                    contentDescription = "One-Handed",
                    tint = contentColor,
                    modifier = Modifier.size(actionIconSize)
                )
            }
            else -> {
                val isBangla = isBanglaText(item)
                val finalFontSize = if (isBangla) fontSizeSp * 0.88f else fontSizeSp
                Text(
                    text = item,
                    color = contentColor,
                    fontSize = finalFontSize.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}
