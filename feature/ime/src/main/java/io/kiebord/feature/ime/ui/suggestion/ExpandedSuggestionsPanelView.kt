package io.kiebord.feature.ime.ui.suggestion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.theme.KiebordTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpandedSuggestionsPanelView(
    candidates: List<SuggestionCandidate>,
    onCandidateClick: (SuggestionCandidate) -> Unit,
    onClose: () -> Unit,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = KiebordTheme.colorScheme

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
                .background(colorScheme.keyForeground.copy(alpha = 0.1f))
        )

        // Header bar with count badge and collapse chevron
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Suggestions",
                color = colorScheme.keyForeground.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = KiebordTheme.fontFamily
            )

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colorScheme.keyForeground.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${candidates.size}",
                    color = colorScheme.keyForeground.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable { onClose() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = "Collapse Suggestions",
                    tint = colorScheme.keyForeground.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Staggered / Flow layout of suggestion chips
        FlowRow(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 10.dp, end = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            candidates.forEach { candidate ->
                val isPrimary = candidate.isPrimary
                val isRaw = candidate.rawInput != null && candidate.text == candidate.rawInput

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isPrimary -> colorScheme.accentColor.copy(alpha = 0.16f)
                                isRaw -> colorScheme.keyBackground.copy(alpha = 0.85f)
                                else -> colorScheme.keyBackground
                            }
                        )
                        .then(
                            if (isPrimary) {
                                Modifier.border(
                                    1.dp,
                                    colorScheme.accentColor.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                            } else {
                                Modifier.border(
                                    0.5.dp,
                                    colorScheme.keyForeground.copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                            }
                        )
                        .clickable { onCandidateClick(candidate) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = candidate.text,
                            color = if (isPrimary) colorScheme.accentColor else colorScheme.keyForeground,
                            fontSize = if (isPrimary) 16.sp else 15.sp,
                            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Normal,
                            fontFamily = KiebordTheme.fontFamily
                        )
                        if (isRaw) {
                            Text(
                                text = "abc",
                                color = colorScheme.keyForeground.copy(alpha = 0.45f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}
