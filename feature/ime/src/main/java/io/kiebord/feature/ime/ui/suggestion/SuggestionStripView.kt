package io.kiebord.feature.ime.ui.suggestion

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    isExpanded: Boolean = false,
    onExpandToggle: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (candidates.isEmpty()) return

    val colorScheme = KiebordTheme.colorScheme
    val visibleCandidates = candidates.take(3)
    val showExpandBtn = onExpandToggle != null && (candidates.size > 3 || isExpanded)

    Row(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Suggestion slots
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            visibleCandidates.forEachIndexed { index, candidate ->
                val isPrimary = candidate.isPrimary
                val isRaw = candidate.rawInput != null && candidate.text == candidate.rawInput

                // Clean vertical divider between candidate slots
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .height(15.dp)
                            .width(1.dp)
                            .background(colorScheme.keyForeground.copy(alpha = 0.15f))
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .combinedClickable(
                            onClick = { onCandidateClick(candidate) },
                            onLongClick = if (candidate.isLearned && onCandidateLongClick != null) {
                                { onCandidateLongClick(candidate) }
                            } else null
                        )
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
                        fontSize = if (isPrimary) 17.sp else 16.sp,
                        fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = KiebordTheme.fontFamily,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Down expand button when candidates exceed strip capacity or expanded
        if (showExpandBtn) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onExpandToggle?.invoke() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse suggestions" else "Expand suggestions",
                    tint = if (isExpanded) colorScheme.accentColor else colorScheme.keyForeground.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
