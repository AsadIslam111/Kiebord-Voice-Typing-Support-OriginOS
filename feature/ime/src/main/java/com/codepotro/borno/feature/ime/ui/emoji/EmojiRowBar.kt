package com.codepotro.borno.feature.ime.ui.emoji

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codepotro.borno.theme.KeyboardColorScheme
import com.codepotro.borno.theme.effectiveToolbarBackground
import com.codepotro.borno.theme.effectiveToolbarForeground
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val DEFAULT_EMOJI_ROW_ITEMS: List<String> = listOf("😊", "😁", "😂", "❤️", "😍", "🙁", "😎", "👍", "🔥", "🎉")

@Composable
fun EmojiRowBar(
    recentEmojis: List<String>,
    onEmojiClick: (String) -> Unit,
    onHideClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRecordRecentEmoji: ((String) -> Unit)? = null,
    height: Dp = 43.dp,
    scaleFactor: Float = 1.0f,
    colorScheme: KeyboardColorScheme,
    vibrationEnabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val performHaptic: () -> Unit = {
        if (vibrationEnabled) {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            } catch (_: Exception) {}
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val currentOnRecordRecentEmoji by rememberUpdatedState(onRecordRecentEmoji)

    val initialList = remember {
        (recentEmojis.filter { it.isNotBlank() } + DEFAULT_EMOJI_ROW_ITEMS).distinct()
    }
    var displayedEmojis by remember { mutableStateOf(initialList) }
    var pendingTappedEmojis by remember { mutableStateOf<List<String>>(emptyList()) }
    var debounceJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(recentEmojis) {
        if (debounceJob?.isActive != true) {
            val updated = (recentEmojis.filter { it.isNotBlank() } + DEFAULT_EMOJI_ROW_ITEMS).distinct()
            if (updated != displayedEmojis) {
                displayedEmojis = updated
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            debounceJob?.cancel()
            if (pendingTappedEmojis.isNotEmpty()) {
                pendingTappedEmojis.reversed().forEach { e ->
                    currentOnRecordRecentEmoji?.invoke(e)
                }
            }
        }
    }

    val handleEmojiTap: (String) -> Unit = { emoji ->
        performHaptic()
        onEmojiClick(emoji)

        pendingTappedEmojis = (listOf(emoji) + pendingTappedEmojis.filter { it != emoji })

        debounceJob?.cancel()
        debounceJob = coroutineScope.launch {
            delay(2000L)
            displayedEmojis = (pendingTappedEmojis + displayedEmojis).distinct()
            val toPersist = pendingTappedEmojis
            pendingTappedEmojis = emptyList()
            toPersist.reversed().forEach { e ->
                currentOnRecordRecentEmoji?.invoke(e)
            }
        }
    }

    val handleHideClick: () -> Unit = {
        performHaptic()
        debounceJob?.cancel()
        if (pendingTappedEmojis.isNotEmpty()) {
            val toPersist = pendingTappedEmojis
            pendingTappedEmojis = emptyList()
            toPersist.reversed().forEach { e ->
                currentOnRecordRecentEmoji?.invoke(e)
            }
        }
        onHideClick()
    }

    val cellHeight = (height - 6.dp).coerceAtLeast(26.dp)
    val cellWidth = (cellHeight * 1.05f).coerceAtLeast(30.dp)
    val emojiFontSize = (20.5f * scaleFactor).coerceIn(16f, 26f).sp
    val hideBtnSize = (34f * scaleFactor).coerceIn(26f, 40f).dp
    val closeIconSize = (19f * scaleFactor).coerceIn(15f, 23f).dp
    val dividerColor = colorScheme.effectiveToolbarForeground.copy(alpha = 0.15f)

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.effectiveToolbarBackground)
            .drawWithContent {
                drawContent()
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2f
                drawLine(
                    color = dividerColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            },
        contentPadding = PaddingValues(start = 6.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy((5 * scaleFactor).coerceAtLeast(3f).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(displayedEmojis, key = { it }) { emoji ->
            Box(
                modifier = Modifier
                    .size(width = cellWidth, height = cellHeight)
                    .clip(RoundedCornerShape((8 * scaleFactor).coerceIn(4f, 12f).dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        handleEmojiTap(emoji)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = emojiFontSize
                )
            }
        }

        item(key = "emoji_row_hide_btn") {
            Box(
                modifier = Modifier
                    .size(hideBtnSize)
                    .clip(CircleShape)
                    .background(
                        if (colorScheme.keyBackground != Color.Unspecified) {
                            colorScheme.keyBackground.copy(alpha = 0.5f)
                        } else {
                            Color(0x18FFFFFF)
                        }
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        handleHideClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Hide Emoji Row",
                    tint = colorScheme.effectiveToolbarForeground,
                    modifier = Modifier.size(closeIconSize)
                )
            }
        }
    }
}
