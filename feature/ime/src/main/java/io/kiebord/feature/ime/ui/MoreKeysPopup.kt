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
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

private const val HOVER_FOLLOW = 0.35f

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

    val isRectangularHover = dims.itemWidthPx > dims.itemHeightPx * 1.15f
    val insetPx = 4f * density.density
    val hoverHalfWidth = if (isRectangularHover) {
        (dims.itemWidthPx * 0.5f - insetPx).coerceAtLeast(1f)
    } else {
        (min(dims.itemWidthPx, dims.itemHeightPx) * 0.5f).roundToInt() - 1f
    }
    val hoverHalfHeight = if (isRectangularHover) {
        (dims.itemHeightPx * 0.5f - insetPx).coerceAtLeast(1f)
    } else {
        (min(dims.itemWidthPx, dims.itemHeightPx) * 0.5f).roundToInt() - 1f
    }

    fun keyLeft(visualCol: Int): Float =
        dims.horizontalPaddingPx + visualCol * (dims.itemWidthPx + dims.colGapPx)

    fun keyTop(visualRow: Int): Float =
        dims.verticalPaddingPx + visualRow * (dims.itemHeightPx + dims.rowGapPx)

    fun keyCenterX(visualCol: Int): Float =
        keyLeft(visualCol) + dims.itemWidthPx * 0.5f

    fun keyCenterY(visualRow: Int): Float =
        keyTop(visualRow) + dims.itemHeightPx * 0.5f

    val selectedPos = layoutResult.itemPositions[currentSelectedIndex]
        ?: layoutResult.itemPositions[layoutResult.defaultItemIndex]
        ?: Pair(0, 0)

    val hoverTargetX = keyCenterX(selectedPos.second)
    val hoverTargetY = keyCenterY(selectedPos.first)

    var hoverVisible by remember(state.anchorKey, state.anchorRect) { mutableStateOf(false) }
    var hoverCenterX by remember(state.anchorKey, state.anchorRect) { mutableFloatStateOf(hoverTargetX) }
    var hoverCenterY by remember(state.anchorKey, state.anchorRect) { mutableFloatStateOf(hoverTargetY) }

    LaunchedEffect(state.anchorKey, state.anchorRect, hoverTargetX, hoverTargetY) {
        if (!hoverVisible) {
            hoverVisible = true
            hoverCenterX = hoverTargetX
            hoverCenterY = hoverTargetY
            return@LaunchedEffect
        }

        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dtFrames = if (lastFrameNanos == 0L) {
                    1f
                } else {
                    ((frameNanos - lastFrameNanos) / 16_666_667f).coerceIn(0.25f, 3.0f)
                }
                lastFrameNanos = frameNanos

                val stepFactor = 1f - (1f - HOVER_FOLLOW).pow(dtFrames)
                val dx = hoverTargetX - hoverCenterX
                val dy = hoverTargetY - hoverCenterY
                hoverCenterX += dx * stepFactor
                hoverCenterY += dy * stepFactor
            }

            val remDx = hoverTargetX - hoverCenterX
            val remDy = hoverTargetY - hoverCenterY
            if (abs(remDx) < 0.75f && abs(remDy) < 0.75f) {
                hoverCenterX = hoverTargetX
                hoverCenterY = hoverTargetY
                break
            }
        }
    }

    fun isUnderHover(visualRow: Int, visualCol: Int): Boolean {
        if (!hoverVisible) return false
        val left = keyLeft(visualCol)
        val top = keyTop(visualRow)
        return hoverCenterX >= left && hoverCenterX < left + dims.itemWidthPx &&
            hoverCenterY >= top && hoverCenterY < top + dims.itemHeightPx
    }

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
                    .drawBehind {
                        if (hoverVisible) {
                            if (isRectangularHover) {
                                val cornerRadius = min(hoverHalfWidth, hoverHalfHeight)
                                drawRoundRect(
                                    color = colorScheme.accentColor,
                                    topLeft = Offset(
                                        hoverCenterX - hoverHalfWidth,
                                        hoverCenterY - hoverHalfHeight
                                    ),
                                    size = Size(hoverHalfWidth * 2f, hoverHalfHeight * 2f),
                                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                                )
                            } else {
                                drawCircle(
                                    color = colorScheme.accentColor,
                                    radius = hoverHalfWidth,
                                    center = Offset(hoverCenterX, hoverCenterY)
                                )
                            }
                        }
                    }
                    .padding(horizontal = horizontalPaddingDp, vertical = verticalPaddingDp)
            ) {
                // Keys Grid
                Column(
                    verticalArrangement = Arrangement.spacedBy(rowGapDp),
                    horizontalAlignment = Alignment.Start
                ) {
                    layoutResult.grid.forEachIndexed { visualRow, rowItems ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(colGapDp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEachIndexed { visualCol, originalItemIndex ->
                                if (originalItemIndex == null) {
                                    androidx.compose.foundation.layout.Spacer(
                                        modifier = Modifier.size(itemWidthDp, itemHeightDp)
                                    )
                                } else {
                                    val item = state.items[originalItemIndex]
                                    val isFocused = isUnderHover(visualRow, visualCol)

                                    MoreKeyCell(
                                        item = item,
                                        isSelected = isFocused,
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
    val contentColor = if (isSelected) {
        val preferredFg = if (colorScheme.accentForeground != Color.Unspecified) {
            colorScheme.accentForeground
        } else {
            colorScheme.actionKeyForeground
        }
        io.kiebord.theme.ThemeColorUtils.ensureContrast(
            foreground = preferredFg,
            background = colorScheme.accentColor,
            minContrast = 3.0f
        )
    } else {
        io.kiebord.theme.ThemeColorUtils.ensureContrast(
            foreground = colorScheme.keyForeground,
            background = colorScheme.moreKeysBackground,
            minContrast = 3.0f
        )
    }

    Box(
        modifier = Modifier
            .size(widthDp, heightDp)
            .clip(CircleShape),
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
            "ACTION_NEW_LINE", "ACTION_NEWLINE", "action:newline", "NEWLINE" -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardReturn,
                    contentDescription = "New Line",
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
