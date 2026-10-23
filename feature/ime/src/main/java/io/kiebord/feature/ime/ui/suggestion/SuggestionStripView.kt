package io.kiebord.feature.ime.ui.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
            .fillMaxWidth()
            .height(38.dp)
            .background(colorScheme.containerBackground)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        candidates.forEach { candidate ->
            val isPrimary = candidate.isPrimary
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isPrimary) colorScheme.accentColor.copy(alpha = 0.18f) else Color.Transparent
                    )
                    .clickable { onCandidateClick(candidate) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = candidate.text,
                    color = if (isPrimary) colorScheme.accentColor else colorScheme.keyForeground,
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
