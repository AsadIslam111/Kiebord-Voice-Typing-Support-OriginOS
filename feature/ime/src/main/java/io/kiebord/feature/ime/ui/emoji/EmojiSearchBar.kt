package io.kiebord.feature.ime.ui.emoji

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KeyboardColorScheme

/**
 * Dedicated Emoji Search Bar placed directly ABOVE the toolbar.
 */
@Composable
fun EmojiSearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    colorScheme: KeyboardColorScheme,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 1. Back button (ArrowBack)
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = colorScheme.keyForeground,
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Search query pill
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(colorScheme.keyBackground)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colorScheme.keyForeground.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = query.ifEmpty { "Search emojis" },
                    color = if (query.isEmpty()) colorScheme.keyForeground.copy(alpha = 0.45f) else colorScheme.keyForeground,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear",
                        tint = colorScheme.keyForeground.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onQueryChange("") }
                    )
                }
            }
        }
    }
}

/**
 * Horizontal strip of matching emoji results displayed on top (in the toolbar slot).
 */
@Composable
fun EmojiSearchResultsStrip(
    query: String,
    onEmojiSelected: (String) -> Unit,
    colorScheme: KeyboardColorScheme,
    modifier: Modifier = Modifier
) {
    val candidates = remember(query) {
        EmojiSearchCatalog.search(query)
    }

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(candidates) { emoji ->
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colorScheme.keyBackground.copy(alpha = 0.45f))
                    .clickable { onEmojiSelected(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 20.sp
                )
            }
        }
    }
}
