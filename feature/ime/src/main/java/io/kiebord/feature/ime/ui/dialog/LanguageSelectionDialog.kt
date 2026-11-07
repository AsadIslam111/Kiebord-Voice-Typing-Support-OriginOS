package io.kiebord.feature.ime.ui.dialog

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.layout.PredefinedLayouts
import kotlinx.coroutines.launch
import io.kiebord.theme.KiebordTheme
import io.kiebord.theme.ThemeColorUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LanguageSelectionDialog(
    currentLayoutId: String,
    enabledLayoutIds: List<String>,
    onSelectLayout: (String) -> Unit,
    onMoreLayouts: () -> Unit,
    onOtherKeyboards: () -> Unit,
    onDismiss: () -> Unit,
    bottomOffsetDp: Dp = 68.dp,
    horizontalOffsetDp: Dp = 0.dp,
    maxAvailableHeightDp: Dp = 260.dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    val dialogBg = colorScheme.containerBackground
    val candidateColor = when {
        colorScheme.accentColor != Color.Unspecified -> colorScheme.accentColor
        colorScheme.shiftActiveColor != Color.Unspecified -> colorScheme.shiftActiveColor
        else -> colorScheme.keyForeground
    }
    val activeAccent = ThemeColorUtils.adjustContrast(
        color = candidateColor,
        background = dialogBg,
        minContrast = 2.8f
    )
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val contentPaddingVertical = if (isLandscape) 5.dp else 8.dp
    val itemPaddingVertical = if (isLandscape) 4.dp else 7.dp
    val actionPaddingVertical = if (isLandscape) 4.dp else 6.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = bottomOffsetDp)
                .then(
                    if (horizontalOffsetDp != 0.dp) {
                        Modifier.offset(x = horizontalOffsetDp)
                    } else Modifier
                )
                .widthIn(min = if (isLandscape) 280.dp else 255.dp, max = if (isLandscape) 340.dp else 300.dp)
                .heightIn(max = maxAvailableHeightDp)
                .clip(RoundedCornerShape(18.dp))
                .background(colorScheme.containerBackground)
                .border(
                    width = 1.dp,
                    color = colorScheme.keyForeground.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* Consume clicks to prevent dismissing dialog */ }
                .padding(vertical = contentPaddingVertical, horizontal = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = activeAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Layout",
                    color = colorScheme.keyForeground,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = KiebordTheme.fontFamily
                )
            }

            Spacer(modifier = Modifier.height(3.dp))
            HorizontalDivider(
                color = colorScheme.keyForeground.copy(alpha = 0.12f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(3.dp))

            // Enabled Layouts list - Scrollable so it never overflows
            val layoutsToShow = enabledLayoutIds.ifEmpty { listOf("en_qwerty") }
            val coroutineScope = rememberCoroutineScope()
            val bringIntoViewRequester = remember { BringIntoViewRequester() }
            val density = LocalDensity.current
            var hasScrolledToActive by remember(currentLayoutId) { mutableStateOf(false) }

            LaunchedEffect(currentLayoutId) {
                bringIntoViewRequester.bringIntoView()
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                layoutsToShow.forEach { layoutId ->
                    val isSelected = layoutId == currentLayoutId
                    val displayName = PredefinedLayouts.displayName(layoutId)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) activeAccent.copy(alpha = 0.12f) else Color.Transparent)
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .bringIntoViewRequester(bringIntoViewRequester)
                                        .onGloballyPositioned { coords ->
                                            if (!hasScrolledToActive) {
                                                hasScrolledToActive = true
                                                val h = coords.size.height.toFloat()
                                                val w = coords.size.width.toFloat()
                                                val margin = with(density) { 20.dp.toPx() }
                                                coroutineScope.launch {
                                                    bringIntoViewRequester.bringIntoView(
                                                        Rect(0f, -margin, w, h + margin)
                                                    )
                                                }
                                            }
                                        }
                                } else Modifier
                            )
                            .clickable {
                                onSelectLayout(layoutId)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = itemPaddingVertical),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = displayName,
                            color = if (isSelected) activeAccent else colorScheme.keyForeground,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = KiebordTheme.fontFamily
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(activeAccent.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Active",
                                    tint = activeAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))
            HorizontalDivider(
                color = colorScheme.keyForeground.copy(alpha = 0.12f),
                thickness = 0.5.dp
            )
            Spacer(modifier = Modifier.height(3.dp))

            // Actions row: More Layouts... & Other Keyboards side-by-side
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Action: More Layouts...
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.keyForeground.copy(alpha = 0.05f))
                        .clickable {
                            onDismiss()
                            onMoreLayouts()
                        }
                        .padding(horizontal = 6.dp, vertical = actionPaddingVertical),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = colorScheme.keyForeground.copy(alpha = 0.75f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "More Layouts...",
                        color = colorScheme.keyForeground.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontFamily = KiebordTheme.fontFamily
                    )
                }

                // Action: Other Keyboards
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.keyForeground.copy(alpha = 0.05f))
                        .clickable {
                            onDismiss()
                            onOtherKeyboards()
                        }
                        .padding(horizontal = 6.dp, vertical = actionPaddingVertical),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = null,
                        tint = colorScheme.keyForeground.copy(alpha = 0.75f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Other Keyboards",
                        color = colorScheme.keyForeground.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontFamily = KiebordTheme.fontFamily
                    )
                }
            }
        }
    }
}
