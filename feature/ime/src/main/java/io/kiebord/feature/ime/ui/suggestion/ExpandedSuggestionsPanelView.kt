package io.kiebord.feature.ime.ui.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardReturn
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme
import kotlinx.coroutines.launch

@Composable
fun ExpandedSuggestionsPanelView(
    candidates: List<SuggestionCandidate>,
    onCandidateClick: (SuggestionCandidate) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Standard keyboard has 4 rows; allocate row height so 4 rows cleanly fit the view height
    val rowHeight = (height / 4).coerceAtLeast(48.dp)
    val scrollStepPx = with(density) { rowHeight.toPx() }

    val candidateRows = candidates.chunked(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(colorScheme.containerBackground)
    ) {
        // Subtle divider on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(colorScheme.keyForeground.copy(alpha = 0.12f))
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // 1. Candidate Grid area (3 columns per row, with dividers between cells, no button-like background)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
            ) {
                candidateRows.forEachIndexed { rowIndex, rowCandidates ->
                    // Horizontal divider between rows
                    if (rowIndex > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colorScheme.keyForeground.copy(alpha = 0.12f))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(rowHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (slotIndex in 0 until 3) {
                            // Vertical divider between candidate columns
                            if (slotIndex > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .background(colorScheme.keyForeground.copy(alpha = 0.12f))
                                )
                            }

                            if (slotIndex < rowCandidates.size) {
                                val candidate = rowCandidates[slotIndex]
                                val isPrimary = candidate.isPrimary
                                val isRaw = candidate.rawInput != null && candidate.text == candidate.rawInput

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clickable { onCandidateClick(candidate) }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = candidate.text,
                                        color = when {
                                            isPrimary -> colorScheme.accentColor
                                            isRaw -> colorScheme.keyForeground.copy(alpha = 0.9f)
                                            else -> colorScheme.keyForeground.copy(alpha = 0.78f)
                                        },
                                        fontSize = if (isPrimary) 20.sp else 19.sp,
                                        fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
                                        fontFamily = KiebordTheme.fontFamily,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                // Empty slot in row
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }

            // Vertical divider between candidate area and right action keys
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(colorScheme.keyForeground.copy(alpha = 0.15f))
            )

            // 2. Right action keys column (Up, Down, Delete, Enter)
            Column(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Key 1 (Top): Up Arrow to scroll vertically
                val canScrollUp = scrollState.value > 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.actionKeyBackground)
                        .clickable(enabled = canScrollUp) {
                            coroutineScope.launch {
                                val target = (scrollState.value - scrollStepPx.toInt()).coerceAtLeast(0)
                                scrollState.animateScrollTo(target)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "Scroll Up",
                        tint = colorScheme.keyForeground.copy(alpha = if (canScrollUp) 0.85f else 0.3f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Key 2: Down Arrow to scroll vertically
                val canScrollDown = scrollState.value < scrollState.maxValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.actionKeyBackground)
                        .clickable(enabled = canScrollDown) {
                            coroutineScope.launch {
                                val target = (scrollState.value + scrollStepPx.toInt()).coerceAtMost(scrollState.maxValue)
                                scrollState.animateScrollTo(target)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Scroll Down",
                        tint = colorScheme.keyForeground.copy(alpha = if (canScrollDown) 0.85f else 0.3f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Key 3: Delete / Backspace key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.actionKeyBackground)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Delete",
                        tint = colorScheme.keyForeground,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Key 4 (Bottom): Enter / Action key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(colorScheme.accentColor)
                        .clickable { onEnter() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardReturn,
                        contentDescription = "Enter",
                        tint = colorScheme.actionKeyForeground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
