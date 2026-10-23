package io.kiebord.feature.ime.ui.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme

data class SuggestionCandidate(
    val text: String,
    val isPrimary: Boolean = false,
    val rawInput: String? = null
)

@Composable
fun SuggestionStripView(
    candidates: List<SuggestionCandidate>,
    onCandidateClick: (SuggestionCandidate) -> Unit,
    modifier: Modifier = Modifier
) {
    if (candidates.isEmpty()) return

    val colorScheme = KiebordTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        candidates.forEachIndexed { index, candidate ->
            val isPrimary = candidate.isPrimary

            // Clean vertical divider between candidate slots
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(1.dp)
                        .background(colorScheme.keyForeground.copy(alpha = 0.15f))
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCandidateClick(candidate) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = candidate.text,
                    color = if (isPrimary) colorScheme.keyForeground else colorScheme.keyForeground.copy(alpha = 0.72f),
                    fontSize = if (isPrimary) 15.sp else 14.sp,
                    fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = KiebordTheme.fontFamily,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
