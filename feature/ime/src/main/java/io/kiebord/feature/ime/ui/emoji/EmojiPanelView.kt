package io.kiebord.feature.ime.ui.emoji

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme

data class EmojiCategory(
    val name: String,
    val icon: String,
    val emojis: List<String>
)

object EmojiCatalog {
    val categories = listOf(
        EmojiCategory(
            name = "Smileys",
            icon = "😀",
            emojis = listOf(
                "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃", "😉", "😊", "😇", "🥰",
                "😍", "🤩", "😘", "😗", "😚", "😙", "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭",
                "🤫", "🤔", "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄", "😬", "🤥", "😌", "😔",
                "😪", "🤤", "😴", "😷", "🤒", "🤕", "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯",
                "🤠", "🥳", "😎", "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯", "😲", "😳", "🥺", "😦",
                "😧", "😨", "😰", "😥", "😢", "😭", "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱",
                "😤", "😡", "😠", "🤬", "😈", "👿", "💀", "☠️", "💩", "🤡", "👹", "👺", "👻", "👽", "🤖"
            )
        ),
        EmojiCategory(
            name = "Gestures & Hearts",
            icon = "❤️",
            emojis = listOf(
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓",
                "💗", "💖", "💘", "💝", "👍", "👎", "👊", "✊", "🤛", "🤜", "👏", "🙌", "👐", "🤲",
                "🤝", "🙏", "✍️", "💅", "🤳", "💪", "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
                "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👀", "👁️", "👅", "👄"
            )
        ),
        EmojiCategory(
            name = "Animals & Nature",
            icon = "🐶",
            emojis = listOf(
                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸",
                "🐵", "🐔", "🐧", "🐦", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛",
                "🦋", "🐌", "🐞", "🐜", "🦟", "🦗", "🕷️", "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙",
                "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳", "🦈", "🐊", "🐅", "🐆", "🦓",
                "🦍", "🦧", "🐘", "🦛", "🦏", "🐪", "🐫", "🦒", "🦘", "🌲", "🌳", "🌴", "🌵", "🌷", "🌸", "🌹", "🌺", "🌻", "🌼", "🌾", "🌿", "🍀", "🍁", "🍂", "🍃"
            )
        ),
        EmojiCategory(
            name = "Food & Drink",
            icon = "🍔",
            emojis = listOf(
                "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑", "🥭", "🍍", "🥥",
                "🥝", "🍅", "🍆", "🥑", "🥦", "🥒", "🌶️", "🌽", "🥕", "🥔", "🥐", "🍞", "🥖", "🥨",
                "🧀", "🥚", "🍳", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕", "🥪",
                "🌮", "🌯", "🥗", "🥘", "🍝", "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🍤", "🍦", "🍩", "🍪", "🎂", "🍰", "🍫", "🍿", "☕", "🍵", "🧃", "🥤", "🧋", "🍺", "🍻", "🥂", "🍷"
            )
        ),
        EmojiCategory(
            name = "Travel & Objects",
            icon = "🚀",
            emojis = listOf(
                "🚗", "🚕", "🚙", "🚌", "🚓", "🚑", "🚒", "🚚", "🚜", "🚲", "🛵", "🏍️", "🚨", "✈️",
                "🛫", "🚀", "🛸", "🚁", "⛵", "🚤", "🛳️", "🚢", "🗺️", "🗽", "🗼", "🏰", "🏖️", "🏝️",
                "🏜️", "🌋", "⛰️", "🏠", "🏡", "🏢", "🏫", "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🥊",
                "🏆", "🥇", "🥈", "🥉", "🎯", "🎮", "🎰", "📱", "💻", "🖥️", "📷", "💡", "📚", "💵", "💰", "💳", "💎", "🔑", "🔒", "🔔", "🔥", "✨", "🎉", "🎊", "🇧🇩", "🇺🇸", "🇬🇧", "🇨🇦", "🇯🇵"
            )
        )
    )
}

@Composable
fun EmojiPanelView(
    onEmojiSelected: (String) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 250.dp
) {
    val colorScheme = KiebordTheme.colorScheme
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val allEmojis = remember { EmojiCatalog.categories.flatMap { it.emojis } }
    val displayedEmojis = remember(selectedCategoryIndex, searchQuery) {
        if (searchQuery.isNotBlank()) {
            allEmojis.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
        } else {
            EmojiCatalog.categories[selectedCategoryIndex].emojis
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        // 1. Search Bar & Action Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Return to ABC button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.actionKeyBackground)
                    .clickable { onClose() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ABC",
                    color = colorScheme.actionKeyForeground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Search pill
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colorScheme.keyBackground)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Search Emojis",
                        tint = colorScheme.keyForeground.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = colorScheme.keyForeground,
                            fontSize = 14.sp
                        ),
                        cursorBrush = SolidColor(colorScheme.accentColor),
                        modifier = Modifier.weight(1.0f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Clear",
                            tint = colorScheme.keyForeground.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            // Quick backspace button
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = colorScheme.keyForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Category Tab Row (Icons)
        if (searchQuery.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EmojiCatalog.categories.forEachIndexed { index, category ->
                    val isSelected = index == selectedCategoryIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) colorScheme.accentColor.copy(alpha = 0.25f) else colorScheme.containerBackground
                            )
                            .clickable { selectedCategoryIndex = index }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.icon,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // 3. Emoji Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 42.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            items(displayedEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .clickable { onEmojiSelected(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = emoji,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}
