package io.kiebord.app.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.app.ui.settings.LocalSettingsColors
import io.kiebord.data.prefs.KiebordPreferences

private data class LayoutOption(
    val id: String,
    val title: String,
    val subtitle: String
)

@Composable
fun OnboardingLayoutSelectionStep(
    initialEnabledLayouts: Set<String>,
    initialPrimaryLayout: String,
    onNext: (enabledLayouts: Set<String>, primaryLayout: String) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalSettingsColors.current

    val layoutOptions = remember {
        listOf(
            LayoutOption(
                id = KiebordPreferences.LAYOUT_EN_QWERTY,
                title = "English (QWERTY)",
                subtitle = "Standard Latin layout with gesture typing"
            ),
            LayoutOption(
                id = KiebordPreferences.LAYOUT_BN_BORNO,
                title = "Borno Phonetic",
                subtitle = "Smart transliteration: type 'ami' -> 'আমি'"
            ),
            LayoutOption(
                id = KiebordPreferences.LAYOUT_BN_PROBHAT,
                title = "Probhat",
                subtitle = "Popular fixed Bengali layout"
            ),
            LayoutOption(
                id = KiebordPreferences.LAYOUT_BN_NATIONAL,
                title = "National (Jatiyo)",
                subtitle = "Official national standard Bengali layout"
            )
        )
    }

    var enabledLayouts by remember { mutableStateOf(initialEnabledLayouts) }
    var primaryLayout by remember { mutableStateOf(initialPrimaryLayout) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // Step progress pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accent.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Step 3 of 4",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Make it yours",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Choose the layouts you'd like to use. You can easily switch between them anytime while typing.",
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Layout options list
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            layoutOptions.forEach { opt ->
                val isChecked = enabledLayouts.contains(opt.id)
                val isPrimary = primaryLayout == opt.id

                val cardBg by animateColorAsState(
                    targetValue = if (isChecked) colors.cardBackground else colors.cardBackground.copy(alpha = 0.6f),
                    label = "LayoutCardBg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isPrimary) colors.accent else if (isChecked) colors.cardBorder else Color.Transparent,
                    label = "LayoutBorder"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            width = if (isPrimary) 1.8.dp else 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            if (isChecked) {
                                // Don't allow unchecking if it's the only one enabled
                                if (enabledLayouts.size > 1) {
                                    val updated = enabledLayouts - opt.id
                                    enabledLayouts = updated
                                    if (primaryLayout == opt.id) {
                                        primaryLayout = updated.first()
                                    }
                                }
                            } else {
                                enabledLayouts = enabledLayouts + opt.id
                            }
                        },
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    enabledLayouts = enabledLayouts + opt.id
                                } else if (enabledLayouts.size > 1) {
                                    val updated = enabledLayouts - opt.id
                                    enabledLayouts = updated
                                    if (primaryLayout == opt.id) {
                                        primaryLayout = updated.first()
                                    }
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.accent,
                                checkmarkColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = opt.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isChecked) colors.textPrimary else colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = opt.subtitle,
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }

                        if (isChecked) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isPrimary) colors.accent else colors.iconBackground
                                    )
                                    .clickable { primaryLayout = opt.id }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isPrimary) "Primary" else "Set Primary",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPrimary) Color.White else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Action Bar: Skip & Next
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onSkip,
                shape = CircleShape,
                modifier = Modifier.height(46.dp)
            ) {
                Text(
                    text = "Skip",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
            }

            Button(
                onClick = { onNext(enabledLayouts, primaryLayout) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .height(46.dp)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "Next",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
