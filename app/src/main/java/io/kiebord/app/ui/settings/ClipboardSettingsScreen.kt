package io.kiebord.app.ui.settings

import android.content.ClipData
import android.content.ClipboardManager as AndroidClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.data.prefs.KiebordPreferencesRepository
import io.kiebord.feature.ime.clipboard.ClipboardEntry
import io.kiebord.feature.ime.clipboard.SandboxedClipboardManager
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class SettingsClipboardFilter {
    ALL,
    RECENT,
    PINNED
}

@Composable
fun ClipboardSettingsScreen(
    clipboardManager: SandboxedClipboardManager,
    preferences: KiebordPreferences? = null,
    onUpdateAutoDeleteRecentClips: ((Boolean) -> Unit)? = null,
    onUpdateAutoDeleteRecentClipsDurationHours: ((Int) -> Unit)? = null,
    onNavigateToManageClips: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalSettingsColors.current
    val scope = rememberCoroutineScope()
    val preferencesRepo = remember { KiebordPreferencesRepository.getInstance(context) }
    val preferencesState by preferencesRepo.preferencesFlow.collectAsState(
        initial = preferences ?: preferencesRepo.getInitialPreferences()
    )

    val autoDeleteEnabled = preferences?.autoDeleteRecentClips ?: preferencesState.autoDeleteRecentClips
    val durationHours = preferences?.autoDeleteRecentClipsDurationHours ?: preferencesState.autoDeleteRecentClipsDurationHours

    LaunchedEffect(Unit) {
        clipboardManager.syncFromSystemClipboard(context)
    }

    fun setAutoDelete(enabled: Boolean) {
        if (onUpdateAutoDeleteRecentClips != null) {
            onUpdateAutoDeleteRecentClips(enabled)
        } else {
            scope.launch { preferencesRepo.updateAutoDeleteRecentClips(enabled) }
        }
        if (enabled) {
            clipboardManager.purgeExpiredRecentClips(durationHours)
        }
    }

    fun setDuration(hours: Int) {
        if (onUpdateAutoDeleteRecentClipsDurationHours != null) {
            onUpdateAutoDeleteRecentClipsDurationHours(hours)
        } else {
            scope.launch { preferencesRepo.updateAutoDeleteRecentClipsDurationHours(hours) }
        }
        if (autoDeleteEnabled) {
            clipboardManager.purgeExpiredRecentClips(hours)
        }
    }

    val allClips = clipboardManager.clips

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToManageClips)
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Manage Clips",
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (allClips.isEmpty()) "No saved clips" else "${allClips.size} clips saved",
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground),
            border = BorderStroke(1.dp, colors.cardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Text(
                            text = "Delete Recent Clips",
                            color = colors.textPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (autoDeleteEnabled) {
                                "Deletes unpinned clips older than $durationHours ${if (durationHours == 1) "hour" else "hours"}"
                            } else {
                                "Automatically remove unpinned clips after a set time"
                            },
                            color = colors.textSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Switch(
                        checked = autoDeleteEnabled,
                        onCheckedChange = { isChecked -> setAutoDelete(isChecked) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.iconBackground
                        )
                    )
                }

                AnimatedVisibility(
                    visible = autoDeleteEnabled,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        HorizontalDivider(
                            color = colors.divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Time duration",
                                color = colors.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(colors.accent.copy(alpha = 0.16f))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (durationHours == 1) "1 hour" else "$durationHours hours",
                                    color = colors.accent,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Slider(
                            value = durationHours.toFloat(),
                            onValueChange = { setDuration(it.roundToInt().coerceIn(1, 48)) },
                            valueRange = 1f..48f,
                            steps = 46,
                            colors = SliderDefaults.colors(
                                thumbColor = colors.accent,
                                activeTrackColor = colors.accent,
                                inactiveTrackColor = colors.iconBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "1h", color = colors.textSecondary, fontSize = 11.sp)
                            Text(text = "24h", color = colors.textSecondary, fontSize = 11.sp)
                            Text(text = "48h", color = colors.textSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClipboardManagerScreen(
    clipboardManager: SandboxedClipboardManager,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalSettingsColors.current

    LaunchedEffect(Unit) {
        clipboardManager.syncFromSystemClipboard(context)
    }

    var searchQuery by remember { mutableStateOf("") }
    var currentFilter by remember { mutableStateOf(SettingsClipboardFilter.ALL) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        isSelectionMode = false
        selectedIds = emptySet()
    }

    val allClips = clipboardManager.clips
    val searchedClips = if (searchQuery.isBlank()) {
        allClips.toList()
    } else {
        allClips.filter { it.text.contains(searchQuery, ignoreCase = true) }
    }

    val filteredClips = when (currentFilter) {
        SettingsClipboardFilter.ALL -> searchedClips
        SettingsClipboardFilter.PINNED -> searchedClips.filter { it.isPinned }
        SettingsClipboardFilter.RECENT -> searchedClips.filter { !it.isPinned }
    }

    val recentClips = filteredClips.filter { !it.isPinned }
    val pinnedClips = filteredClips.filter { it.isPinned }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp),
                placeholder = {
                    Text(
                        text = "Search clipboard…",
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = if (searchQuery.isNotEmpty()) colors.accent else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = "Clear search",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.cardBorder,
                    focusedContainerColor = colors.cardBackground,
                    unfocusedContainerColor = colors.cardBackground,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accent
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterPill(
                    label = "All",
                    selected = currentFilter == SettingsClipboardFilter.ALL,
                    onClick = { currentFilter = SettingsClipboardFilter.ALL },
                    colors = colors
                )
                FilterPill(
                    label = "Recent",
                    selected = currentFilter == SettingsClipboardFilter.RECENT,
                    onClick = { currentFilter = SettingsClipboardFilter.RECENT },
                    colors = colors
                )
                FilterPill(
                    label = "Pinned",
                    selected = currentFilter == SettingsClipboardFilter.PINNED,
                    onClick = { currentFilter = SettingsClipboardFilter.PINNED },
                    colors = colors,
                    icon = if (currentFilter == SettingsClipboardFilter.PINNED) Icons.Filled.PushPin else Icons.Outlined.PushPin
                )
            }

            if (filteredClips.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.cardBackground)
                                .border(BorderStroke(1.dp, colors.cardBorder), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (searchQuery.isNotEmpty()) Icons.Outlined.Search else Icons.Outlined.ContentPaste,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No matching clips"
                            } else if (currentFilter == SettingsClipboardFilter.PINNED) {
                                "No pinned clips yet"
                            } else {
                                "No copied clips yet"
                            },
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "Try a different search query"
                            } else {
                                "Copied text will appear here automatically"
                            },
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                ) {
                    if (currentFilter == SettingsClipboardFilter.ALL) {
                        if (recentClips.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = "Recent",
                                    color = colors.textSecondary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
                                )
                            }
                            items(recentClips, key = { it.id }) { entry ->
                                SettingsClipCard(
                                    entry = entry,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = entry.id in selectedIds,
                                    colors = colors,
                                    onSelectToggle = {
                                        selectedIds = if (entry.id in selectedIds) {
                                            selectedIds - entry.id
                                        } else {
                                            selectedIds + entry.id
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            selectedIds = setOf(entry.id)
                                        }
                                    },
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedIds = if (entry.id in selectedIds) {
                                                selectedIds - entry.id
                                            } else {
                                                selectedIds + entry.id
                                            }
                                        } else {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("Kiebord", entry.text))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onTogglePin = { clipboardManager.togglePin(entry) },
                                    onDelete = { clipboardManager.deleteClip(entry) }
                                )
                            }
                        }

                        if (pinnedClips.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Text(
                                    text = "Pinned",
                                    color = colors.accent,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp)
                                )
                            }
                            items(pinnedClips, key = { it.id }) { entry ->
                                SettingsClipCard(
                                    entry = entry,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = entry.id in selectedIds,
                                    colors = colors,
                                    onSelectToggle = {
                                        selectedIds = if (entry.id in selectedIds) {
                                            selectedIds - entry.id
                                        } else {
                                            selectedIds + entry.id
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            isSelectionMode = true
                                            selectedIds = setOf(entry.id)
                                        }
                                    },
                                    onClick = {
                                        if (isSelectionMode) {
                                            selectedIds = if (entry.id in selectedIds) {
                                                selectedIds - entry.id
                                            } else {
                                                selectedIds + entry.id
                                            }
                                        } else {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("Kiebord", entry.text))
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onTogglePin = { clipboardManager.togglePin(entry) },
                                    onDelete = { clipboardManager.deleteClip(entry) }
                                )
                            }
                        }
                    } else {
                        items(filteredClips, key = { it.id }) { entry ->
                            SettingsClipCard(
                                entry = entry,
                                isSelectionMode = isSelectionMode,
                                isSelected = entry.id in selectedIds,
                                colors = colors,
                                onSelectToggle = {
                                    selectedIds = if (entry.id in selectedIds) {
                                        selectedIds - entry.id
                                    } else {
                                        selectedIds + entry.id
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        isSelectionMode = true
                                        selectedIds = setOf(entry.id)
                                    }
                                },
                                onClick = {
                                    if (isSelectionMode) {
                                        selectedIds = if (entry.id in selectedIds) {
                                            selectedIds - entry.id
                                        } else {
                                            selectedIds + entry.id
                                        }
                                    } else {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as AndroidClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Kiebord", entry.text))
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onTogglePin = { clipboardManager.togglePin(entry) },
                                onDelete = { clipboardManager.deleteClip(entry) }
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = filteredClips.isNotEmpty() || isSelectionMode,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.cardBackground,
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 6.dp
            ) {
                if (isSelectionMode) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.accent)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${selectedIds.size} selected",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                selectedIds = if (selectedIds.size == filteredClips.size) {
                                    emptySet()
                                } else {
                                    filteredClips.map { it.id }.toSet()
                                }
                            },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.iconBackground,
                                contentColor = colors.textPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = if (selectedIds.size == filteredClips.size) "Deselect" else "All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    val count = selectedIds.size
                                    clipboardManager.deleteClips(selectedIds)
                                    selectedIds = emptySet()
                                    isSelectionMode = false
                                    Toast.makeText(context, "$count clips deleted", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = selectedIds.isNotEmpty(),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC3545),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0x22DC3545),
                                disabledContentColor = Color(0x55DC3545)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.iconBackground)
                                .clickable {
                                    isSelectionMode = false
                                    selectedIds = emptySet()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Cancel selection",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { isSelectionMode = true },
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.iconBackground,
                                contentColor = colors.textPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Select",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (recentClips.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(20.dp)
                                    .background(colors.divider)
                            )

                            Button(
                                onClick = { showClearConfirmation = true },
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0x1FFF5252),
                                    contentColor = Color(0xFFFF5252)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Clear Recent",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showClearConfirmation) {
            AlertDialog(
                onDismissRequest = { showClearConfirmation = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = colors.cardBackground,
                titleContentColor = colors.textPrimary,
                textContentColor = colors.textSecondary,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0x22DC3545)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = Color(0xFFDC3545),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Clear Recent Clips?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "All unpinned clips will be permanently removed. Your pinned clips will stay safe.",
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearConfirmation = false
                            clipboardManager.clearAllUnpinned()
                            Toast.makeText(context, "Recent clips cleared", Toast.LENGTH_SHORT).show()
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC3545),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Clear", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearConfirmation = false },
                        shape = CircleShape
                    ) {
                        Text("Cancel", color = colors.textSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    colors: SettingsColors,
    icon: ImageVector? = null
) {
    Box(
        modifier = Modifier
            .height(34.dp)
            .clip(CircleShape)
            .background(if (selected) colors.accent else colors.cardBackground)
            .border(
                BorderStroke(
                    1.dp,
                    if (selected) colors.accent else colors.cardBorder
                ),
                CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.5.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsClipCard(
    entry: ClipboardEntry,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    colors: SettingsColors,
    onSelectToggle: () -> Unit,
    onLongClick: () -> Unit,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) colors.accent.copy(alpha = 0.12f) else colors.cardBackground)
            .border(
                BorderStroke(
                    if (isSelected) 1.5.dp else 1.dp,
                    if (isSelected) colors.accent else colors.cardBorder
                ),
                RoundedCornerShape(20.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.accent else Color.Transparent)
                            .border(
                                BorderStroke(1.5.dp, if (isSelected) colors.accent else colors.textSecondary.copy(alpha = 0.45f)),
                                CircleShape
                            )
                            .clickable { onSelectToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                } else if (entry.isOtp) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accent.copy(alpha = 0.16f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OTP",
                            color = colors.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onTogglePin() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (entry.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (entry.isPinned) "Unpin" else "Pin",
                            tint = if (entry.isPinned) colors.accent else colors.textSecondary.copy(alpha = 0.45f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (!isSelectionMode) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { onDelete() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Delete",
                                tint = colors.textSecondary.copy(alpha = 0.45f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = entry.text,
                color = colors.textPrimary,
                fontSize = 13.5.sp,
                lineHeight = 18.5.sp,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatRelativeTime(entry.timestamp),
                color = colors.textSecondary.copy(alpha = 0.6f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        diff < 60_000L -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}
