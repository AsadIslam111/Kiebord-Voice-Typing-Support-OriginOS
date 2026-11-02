package io.kiebord.feature.ime.ui.clipboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.feature.ime.clipboard.ClipboardEntry
import io.kiebord.theme.KiebordTheme

private enum class ClipboardFilter {
    ALL,
    PINNED
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardPanelView(
    clips: List<ClipboardEntry>,
    onClipClick: (ClipboardEntry) -> Unit,
    onTogglePin: (ClipboardEntry) -> Unit,
    onDeleteClip: (ClipboardEntry) -> Unit,
    onClearAll: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier,
    onSyncRequest: (() -> Unit)? = null
) {
    LaunchedEffect(Unit) {
        onSyncRequest?.invoke()
    }

    val colorScheme = KiebordTheme.colorScheme
    var currentFilter by remember { mutableStateOf(ClipboardFilter.ALL) }
    var previewEntry by remember { mutableStateOf<ClipboardEntry?>(null) }

    val pinnedClips = clips.filter { it.isPinned }
    val recentClips = clips.filter { !it.isPinned }

    val hasBackdrop = colorScheme.themeSpec?.background?.image != null || colorScheme.themeSpec?.background?.gradient != null
    val panelBg = if (hasBackdrop) Color.Transparent else colorScheme.containerBackground

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(panelBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // 1. Header: Back button | "Clipboard" | Pin/Recent toggle group btn | Edit btn
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.keyForeground,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "Clipboard",
                    color = colorScheme.keyForeground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.weight(1f))

                // Pin / Recent toggle group button (fully rounded pill)
                Row(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(colorScheme.keyBackground.copy(alpha = 0.7f))
                        .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), CircleShape)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // All segment
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (currentFilter == ClipboardFilter.ALL) colorScheme.accentColor else Color.Transparent
                            )
                            .clickable { currentFilter = ClipboardFilter.ALL }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentFilter == ClipboardFilter.ALL) Color.White else colorScheme.keyForeground.copy(alpha = 0.75f)
                        )
                    }

                    // Pinned segment
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (currentFilter == ClipboardFilter.PINNED) colorScheme.accentColor else Color.Transparent
                            )
                            .clickable { currentFilter = ClipboardFilter.PINNED }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PushPin,
                                contentDescription = null,
                                tint = if (currentFilter == ClipboardFilter.PINNED) Color.White else colorScheme.keyForeground.copy(alpha = 0.75f),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Pinned",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (currentFilter == ClipboardFilter.PINNED) Color.White else colorScheme.keyForeground.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Edit button (launches settings view)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .clickable(onClick = onOpenSettings),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Clipboard settings",
                        tint = colorScheme.keyForeground,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Main Content: 2-column Staggered Grid (Recent above Pinned)
            if (clips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Copied text will appear here",
                        color = colorScheme.keyForeground.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else if (currentFilter == ClipboardFilter.PINNED && pinnedClips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No pinned clips yet",
                        color = colorScheme.keyForeground.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalItemSpacing = 8.dp,
                    contentPadding = PaddingValues(bottom = 8.dp, top = 2.dp)
                ) {
                    // Recent section FIRST (above)
                    if (currentFilter == ClipboardFilter.ALL && recentClips.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "Recent",
                                color = colorScheme.keyForeground.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp)
                            )
                        }
                        items(recentClips, key = { it.id }) { entry ->
                            ClipboardCard(
                                entry = entry,
                                onClick = { onClipClick(entry) },
                                onLongClick = { previewEntry = entry },
                                onTogglePin = { onTogglePin(entry) }
                            )
                        }
                    }

                    // Pinned section SECOND (below)
                    if (pinnedClips.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Text(
                                text = "Pinned",
                                color = colorScheme.accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 2.dp)
                            )
                        }
                        items(pinnedClips, key = { it.id }) { entry ->
                            ClipboardCard(
                                entry = entry,
                                onClick = { onClipClick(entry) },
                                onLongClick = { previewEntry = entry },
                                onTogglePin = { onTogglePin(entry) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Long-Press Preview & Vertical Action Window (Overlay)
        if (previewEntry != null) {
            val entry = previewEntry!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.containerBackground.copy(alpha = 0.88f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        previewEntry = null
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}, // Prevent dismiss when tapping dialog itself
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Full clip scrollable card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colorScheme.keyBackground)
                            .border(BorderStroke(1.dp, Color(0x26FFFFFF)), RoundedCornerShape(18.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = entry.text,
                                color = colorScheme.keyForeground,
                                fontSize = 14.sp,
                                lineHeight = 19.sp
                            )

                            if (entry.isOtp) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "OTP Code",
                                    color = colorScheme.accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Right Column: Three buttons stacked vertically (same color as card, icon and text side by side)
                    Column(
                        modifier = Modifier
                            .width(116.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Paste Button (Topmost: rounded top edge radii)
                        Button(
                            onClick = {
                                onClipClick(entry)
                                previewEntry = null
                            },
                            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.keyBackground,
                                contentColor = colorScheme.keyForeground
                            ),
                            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = null,
                                    tint = colorScheme.keyForeground,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Paste", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.keyForeground)
                            }
                        }

                        // 2. Pin / Unpin Button (Middle: subtle uniform radii)
                        Button(
                            onClick = {
                                onTogglePin(entry)
                                previewEntry = entry.copy(isPinned = !entry.isPinned)
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.keyBackground,
                                contentColor = colorScheme.keyForeground
                            ),
                            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PushPin,
                                    contentDescription = null,
                                    tint = if (entry.isPinned) colorScheme.accentColor else colorScheme.keyForeground,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (entry.isPinned) "Unpin" else "Pin",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.keyForeground
                                )
                            }
                        }

                        // 3. Delete Button (Bottommost: rounded bottom edge radii)
                        Button(
                            onClick = {
                                onDeleteClip(entry)
                                previewEntry = null
                            },
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.keyBackground,
                                contentColor = colorScheme.keyForeground
                            ),
                            border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = colorScheme.keyForeground,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colorScheme.keyForeground)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ClipboardCard(
    entry: ClipboardEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onTogglePin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.keyBackground)
            .border(BorderStroke(1.dp, Color(0x18FFFFFF)), RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Vertically centered clip text with padding on the right for the pin icon
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp, top = if (entry.isOtp) 14.dp else 0.dp)
                .align(Alignment.CenterStart),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = entry.text,
                color = colorScheme.keyForeground,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (entry.isOtp) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(colorScheme.accentColor.copy(alpha = 0.18f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "OTP",
                    color = colorScheme.accentColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top-right Pin icon button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .clip(CircleShape)
                .background(if (entry.isPinned) colorScheme.accentColor.copy(alpha = 0.15f) else Color.Transparent)
                .clickable { onTogglePin() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PushPin,
                contentDescription = if (entry.isPinned) "Unpin" else "Pin",
                tint = if (entry.isPinned) colorScheme.accentColor else colorScheme.keyForeground.copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
