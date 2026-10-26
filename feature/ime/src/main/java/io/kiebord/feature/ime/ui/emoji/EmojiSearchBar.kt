package io.kiebord.feature.ime.ui.emoji

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KeyboardColorScheme

/**
 * CardView containing 2-row horizontally scrollable emoji search results on top,
 * and the search input bar stacked at the bottom of the card inside.
 */
@Composable
fun EmojiSearchCardView(
    query: String,
    onQueryChange: (String) -> Unit,
    onEmojiSelected: (String) -> Unit,
    onClose: () -> Unit,
    colorScheme: KeyboardColorScheme,
    modifier: Modifier = Modifier
) {
    val cardBg = if (colorScheme.moreKeysBackground != Color.Unspecified && colorScheme.moreKeysBackground != colorScheme.containerBackground) {
        colorScheme.moreKeysBackground
    } else if (colorScheme.actionKeyBackground != Color.Unspecified && colorScheme.actionKeyBackground != colorScheme.containerBackground) {
        colorScheme.actionKeyBackground
    } else {
        colorScheme.keyBackground.copy(alpha = 0.85f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = if (colorScheme.keyBorder.alpha > 0.05f) {
            BorderStroke(1.dp, colorScheme.keyBorder.copy(alpha = 0.35f))
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // 1. Emoji search results: 2 rows horizontally scrollable on top
            EmojiSearchResultsGrid(
                query = query,
                onEmojiSelected = onEmojiSelected,
                colorScheme = colorScheme,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 2. Search input stacked bottom of the card inside
            EmojiSearchBarRow(
                query = query,
                onQueryChange = onQueryChange,
                onClose = onClose,
                colorScheme = colorScheme,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * 2-row horizontally scrollable emoji search results grid displayed on top of the search bar.
 */
@Composable
fun EmojiSearchResultsGrid(
    query: String,
    onEmojiSelected: (String) -> Unit,
    colorScheme: KeyboardColorScheme,
    modifier: Modifier = Modifier
) {
    val candidates = remember(query) {
        EmojiSearchCatalog.search(query)
    }

    if (candidates.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No emoji found",
                color = colorScheme.keyForeground.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
        }
    } else {
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(candidates) { emoji ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(colorScheme.keyBackground.copy(alpha = 0.5f))
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
}

/**
 * Backward compatibility alias for emoji search results.
 */
@Composable
fun EmojiSearchResultsStrip(
    query: String,
    onEmojiSelected: (String) -> Unit,
    colorScheme: KeyboardColorScheme,
    modifier: Modifier = Modifier
) {
    EmojiSearchResultsGrid(
        query = query,
        onEmojiSelected = onEmojiSelected,
        colorScheme = colorScheme,
        modifier = modifier
    )
}

/**
 * Dedicated Emoji Search Bar placed at the bottom inside the search card.
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
            .height(40.dp)
            .padding(horizontal = 6.dp, vertical = 2.dp),
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
