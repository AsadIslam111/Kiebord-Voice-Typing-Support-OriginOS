package io.kiebord.feature.ime.ui.suggestion

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.prefs.KiebordPreferences
import io.kiebord.theme.KiebordTheme

data class SuggestionCandidate(
    val text: String,
    val isPrimary: Boolean = false,
    val rawInput: String? = null,
    val isLearned: Boolean = false
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuggestionStripView(
    candidates: List<SuggestionCandidate>,
    onCandidateClick: (SuggestionCandidate) -> Unit,
    onCandidateLongClick: ((SuggestionCandidate) -> Unit)? = null,
    mode: String = KiebordPreferences.SUGGESTION_BAR_MODE_NORMAL,
    isExpanded: Boolean = false,
    onExpandToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (candidates.isEmpty()) return

    val colorScheme = KiebordTheme.colorScheme
    val fontFamily = KiebordTheme.fontFamily
    val isScrollableMode = mode == KiebordPreferences.SUGGESTION_BAR_MODE_SCROLLABLE

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 2.dp)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()

        val expandBtnWidthPx = with(density) { 38.dp.toPx() }
        val dividerWidthPx = with(density) { 1.dp.toPx() }
        val chipPaddingPx = with(density) { 12.dp.toPx() }

        // Dynamically determine visible candidates:
        // Try 3 candidates -> if they do not fit, try 2 -> if they do not fit, show 1.
        // Never truncate any candidate.
        val visibleCandidates = remember(candidates, totalWidthPx, isScrollableMode, isExpanded, onExpandToggle != null, fontFamily) {
            fun requiredWidth(candidate: SuggestionCandidate): Float {
                val isPrimary = candidate.isPrimary
                val fontSize = if (isPrimary) 18.5.sp else 17.5.sp
                val fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(candidate.text),
                    style = TextStyle(
                        fontSize = fontSize,
                        fontWeight = fontWeight,
                        fontFamily = fontFamily,
                        letterSpacing = 0.sp
                    ),
                    maxLines = 1
                )
                return textLayoutResult.size.width.toFloat() + chipPaddingPx
            }

            if (isScrollableMode || candidates.isEmpty()) {
                candidates
            } else if (candidates.size == 1) {
                candidates
            } else {
                val availableWithBtn = (totalWidthPx - expandBtnWidthPx).coerceAtLeast(0f)
                val availableWithoutBtn = totalWidthPx.coerceAtLeast(0f)

                // 1. Try 3 candidates
                if (candidates.size >= 3) {
                    val pool3 = candidates.take(3)
                    val needsBtn = candidates.size > 3 || isExpanded
                    val availWidth = if (needsBtn && onExpandToggle != null) availableWithBtn else availableWithoutBtn
                    val slotWidth3 = (availWidth - 2 * dividerWidthPx) / 3f
                    if (slotWidth3 > 0f && pool3.all { requiredWidth(it) <= slotWidth3 }) {
                        return@remember pool3
                    }
                }

                // 2. Try 2 candidates
                val pool2 = if (candidates.size >= 3 && candidates[2].isPrimary) {
                    listOf(candidates[0], candidates[2])
                } else {
                    candidates.take(2)
                }
                val needsBtn2 = candidates.size > 2 || isExpanded
                val availWidth2 = if (needsBtn2 && onExpandToggle != null) availableWithBtn else availableWithoutBtn
                val slotWidth2 = (availWidth2 - dividerWidthPx) / 2f
                if (slotWidth2 > 0f && pool2.all { requiredWidth(it) <= slotWidth2 }) {
                    return@remember pool2
                }

                // 3. Fallback to 1 candidate (always prioritize primary candidate)
                val primary = candidates.firstOrNull { it.isPrimary } ?: candidates.first()
                listOf(primary)
            }
        }

        val showExpandBtn = !isScrollableMode && onExpandToggle != null &&
            (candidates.size > visibleCandidates.size || isExpanded)

        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isScrollableMode) {
                // Mode 2: Scrollable candidate chips horizontally
                val scrollState = rememberScrollState()
                LaunchedEffect(candidates) {
                    scrollState.scrollTo(0)
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    candidates.forEachIndexed { index, candidate ->
                        key(candidate.text) {
                            val isPrimary = candidate.isPrimary
                            val isRaw = candidate.rawInput != null && candidate.text == candidate.rawInput
                            val isLearnedWithAction = candidate.isLearned && onCandidateLongClick != null

                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(1.dp)
                                        .background(colorScheme.keyForeground.copy(alpha = 0.15f))
                                )
                            }

                            val chipClickModifier = if (isLearnedWithAction) {
                                Modifier.combinedClickable(
                                    onClick = { onCandidateClick(candidate) },
                                    onLongClick = { onCandidateLongClick(candidate) }
                                )
                            } else {
                                Modifier.clickable { onCandidateClick(candidate) }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(chipClickModifier)
                                    .padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = candidate.text,
                                    color = when {
                                        isPrimary -> colorScheme.accentColor
                                        isRaw -> colorScheme.keyForeground.copy(alpha = 0.88f)
                                        else -> colorScheme.keyForeground.copy(alpha = 0.72f)
                                    },
                                    fontSize = if (isPrimary) 18.5.sp else 17.5.sp,
                                    fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
                                    fontFamily = fontFamily,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            } else {
                // Mode 1: Normal mode (1 to 3 slots without truncation, with expand button when needed)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visibleCandidates.forEachIndexed { index, candidate ->
                        key(candidate.text) {
                            val isPrimary = candidate.isPrimary
                            val isRaw = candidate.rawInput != null && candidate.text == candidate.rawInput
                            val isLearnedWithAction = candidate.isLearned && onCandidateLongClick != null

                            // Clean vertical divider between candidate slots
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(1.dp)
                                        .background(colorScheme.keyForeground.copy(alpha = 0.15f))
                                )
                            }

                            val chipClickModifier = if (isLearnedWithAction) {
                                Modifier.combinedClickable(
                                    onClick = { onCandidateClick(candidate) },
                                    onLongClick = { onCandidateLongClick(candidate) }
                                )
                            } else {
                                Modifier.clickable { onCandidateClick(candidate) }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .then(chipClickModifier)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = candidate.text,
                                    color = when {
                                        isPrimary -> colorScheme.accentColor
                                        isRaw -> colorScheme.keyForeground.copy(alpha = 0.88f)
                                        else -> colorScheme.keyForeground.copy(alpha = 0.72f)
                                    },
                                    fontSize = if (isPrimary) 18.5.sp else 17.5.sp,
                                    fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
                                    fontFamily = fontFamily,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Clip
                                )
                            }
                        }
                    }
                }
            }

            // Down expand button when candidates exceed strip capacity or expanded (Normal mode only)
            if (showExpandBtn) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onExpandToggle?.invoke() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse suggestions" else "Expand suggestions",
                        tint = if (isExpanded) colorScheme.accentColor else colorScheme.keyForeground.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
