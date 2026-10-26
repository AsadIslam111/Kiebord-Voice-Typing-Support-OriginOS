package io.kiebord.feature.ime.ui.emoji

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.emoji2.emojipicker.EmojiPickerView
import io.kiebord.theme.KiebordTheme

@Composable
fun EmojiPanelView(
    onEmojiSelected: (String) -> Unit,
    onDelete: () -> Unit,
    onSpace: () -> Unit,
    onClose: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 250.dp
) {
    val colorScheme = KiebordTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
    ) {
        // 1. AndroidX EmojiPicker View (occupies the main area above the bottom bar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { context ->
                    EmojiPickerView(context).apply {
                        emojiGridColumns = 9
                        setOnEmojiPickedListener { item ->
                            onEmojiSelected(item.emoji)
                        }

                        // Seamlessly integrate BackArrow and SearchIcon into the category bar
                        post {
                            try {
                                val header = findViewById<View>(androidx.emoji2.emojipicker.R.id.emoji_picker_header)
                                val parent = header?.parent as? ViewGroup
                                if (header != null && parent != null && parent.findViewWithTag<View>("kiebord_emoji_nav") == null) {
                                    val headerIdx = parent.indexOfChild(header)
                                    parent.removeView(header)

                                    val topNavRow = LinearLayout(context).apply {
                                        tag = "kiebord_emoji_nav"
                                        orientation = LinearLayout.HORIZONTAL
                                        gravity = Gravity.CENTER_VERTICAL
                                        layoutParams = LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            header.layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                    }

                                    val composeNavButtons = ComposeView(context).apply {
                                        setContent {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(start = 2.dp)
                                            ) {
                                                // Back to keyboard button
                                                IconButton(
                                                    onClick = onClose,
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                                        contentDescription = "Back to keyboard",
                                                        tint = colorScheme.keyForeground,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                // Search icon button
                                                IconButton(
                                                    onClick = onOpenSearch,
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Search,
                                                        contentDescription = "Search emojis",
                                                        tint = colorScheme.keyForeground,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    topNavRow.addView(
                                        composeNavButtons,
                                        LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.WRAP_CONTENT,
                                            ViewGroup.LayoutParams.WRAP_CONTENT
                                        )
                                    )

                                    header.layoutParams = LinearLayout.LayoutParams(
                                        0,
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        1f
                                    )
                                    topNavRow.addView(header)

                                    parent.addView(topNavRow, headerIdx)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().fillMaxHeight()
            )
        }

        // 2. Bottom Action Bar: Spacebar, Delete, and ABC return button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "ABC" return key (left)
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.actionKeyBackground)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ABC",
                    color = colorScheme.actionKeyForeground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Spacebar keycap (center)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.keyBackground)
                    .clickable { onSpace() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "",
                    color = colorScheme.keyForeground.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            }

            // Delete / Backspace keycap (right)
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colorScheme.actionKeyBackground)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = colorScheme.actionKeyForeground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
