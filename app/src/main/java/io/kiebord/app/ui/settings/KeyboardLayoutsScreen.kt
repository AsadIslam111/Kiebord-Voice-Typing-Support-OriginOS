package io.kiebord.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.prefs.KiebordPreferences

private data class BanglaLayoutCardData(
    val id: String,
    val name: String,
    val badge: String,
    val description: String,
    val previewRows: List<List<String>>,
    val spaceLabel: String,
    val punctuation: String
)

@Composable
fun KeyboardLayoutsScreen(
    preferences: KiebordPreferences,
    onUpdatePrimaryLayout: (String) -> Unit,
    onToggleLayoutEnabled: (String, Boolean) -> Unit,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    val isAnyBanglaEnabled = preferences.enabledLayoutIds.any { it.startsWith("bn") }
    var isBanglaExpanded by remember {
        mutableStateOf(isAnyBanglaEnabled)
    }

    val englishLayoutId = KiebordPreferences.LAYOUT_EN_QWERTY

    val banglaCards = listOf(
        BanglaLayoutCardData(
            id = KiebordPreferences.LAYOUT_BN_BORNO,
            name = "Borno Phonetic",
            badge = "বর্ণ",
            description = "Natural English-to-Bangla transliteration (ami → আমি)",
            previewRows = listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m")
            ),
            spaceLabel = "বাংলা",
            punctuation = "."
        ),
        BanglaLayoutCardData(
            id = KiebordPreferences.LAYOUT_BN_PROBHAT,
            name = "Probhat",
            badge = "প্রভাত",
            description = "Probhat national standard layout for direct Bengali typing",
            previewRows = listOf(
                listOf("দ", "ড", "প", "ট", "চ", "ত", "জ", "ক", "স", "হ"),
                listOf("ি", "ু", "ী", "ূ", "া", "ে", "ো", "য়", "র", "ল"),
                listOf("্", "ং", "ঃ", "ঁ", "ব", "ম", "ন", "ণ", "শ", "য")
            ),
            spaceLabel = "বাংলা (প্রভাত)",
            punctuation = "।"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Available Keyboard Layouts")

        // 1. English (QWERTY) Card with Checkbox
        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            val isEnglishEnabled = preferences.enabledLayoutIds.contains(englishLayoutId)
            val isLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnglishEnabled

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (!isLastRemaining || !isEnglishEnabled) {
                            onToggleLayoutEnabled(englishLayoutId, !isEnglishEnabled)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2B2D31)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7AA2F7)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "English",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE0E2E8)
                    )
                    Text(
                        text = if (isLastRemaining) "Active (at least one layout required)" else "QWERTY layout",
                        fontSize = 12.sp,
                        color = Color(0xFF9499A8)
                    )
                }

                Checkbox(
                    checked = isEnglishEnabled,
                    onCheckedChange = { checked ->
                        if (!isLastRemaining || checked) {
                            onToggleLayoutEnabled(englishLayoutId, checked)
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF7AA2F7),
                        uncheckedColor = Color(0xFF6B7280),
                        checkmarkColor = Color(0xFF0F141C)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Bangla Language Group Card (Expandable into separate real keyboard cards)
        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            val activeBanglaCount = banglaCards.count { preferences.enabledLayoutIds.contains(it.id) }

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isBanglaExpanded = !isBanglaExpanded
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2B2D31)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "বাং",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7AA2F7)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bangla Language",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE0E2E8)
                        )
                        Text(
                            text = if (isBanglaExpanded) "Borno Phonetic • Probhat" else "$activeBanglaCount active layout(s) • tap to expand",
                            fontSize = 12.sp,
                            color = Color(0xFF9499A8)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (activeBanglaCount > 0) Color(0xFF233047) else Color(0xFF25272B))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isBanglaExpanded) "Expanded" else "$activeBanglaCount Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (activeBanglaCount > 0) Color(0xFF7AA2F7) else Color(0xFF9499A8)
                        )
                    }
                }

                // Expanded Section: Separate Real Keyboard Cards with Checkboxes (NO shift/delete symbols)
                AnimatedVisibility(
                    visible = isBanglaExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        banglaCards.forEach { cardData ->
                            val isEnabled = preferences.enabledLayoutIds.contains(cardData.id)
                            val isLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnabled

                            RealKeyboardPreviewCard(
                                cardData = cardData,
                                isEnabled = isEnabled,
                                onToggle = {
                                    if (!isLastRemaining || !isEnabled) {
                                        onToggleLayoutEnabled(cardData.id, !isEnabled)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealKeyboardPreviewCard(
    cardData: BanglaLayoutCardData,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val borderColor = if (isEnabled) Color(0xFF7AA2F7) else Color(0xFF2A2D33)
    val cardBackground = Color(0xFF14161A)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBackground)
            .border(
                width = if (isEnabled) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onToggle)
            .padding(12.dp)
    ) {
        // Real Keyboard Surface
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0F1013))
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Row 1 (10 keys)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                cardData.previewRows[0].forEach { keyStr ->
                    PreviewKeycap(
                        label = keyStr,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Row 2 (9 or 10 keys)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                val row2Keys = cardData.previewRows[1]
                if (row2Keys.size == 9) {
                    Spacer(modifier = Modifier.weight(0.5f))
                }
                row2Keys.forEach { keyStr ->
                    PreviewKeycap(
                        label = keyStr,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row2Keys.size == 9) {
                    Spacer(modifier = Modifier.weight(0.5f))
                }
            }

            // Row 3: Shift + Keys + Delete (NO shift or delete symbols)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shift keycap (blank, no symbol)
                PreviewKeycap(
                    label = "",
                    modifier = Modifier.weight(1.3f),
                    backgroundColor = Color(0xFF1E2026)
                )

                cardData.previewRows[2].forEach { keyStr ->
                    PreviewKeycap(
                        label = keyStr,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Delete keycap (blank, no symbol)
                PreviewKeycap(
                    label = "",
                    modifier = Modifier.weight(1.3f),
                    backgroundColor = Color(0xFF1E2026)
                )
            }

            // Row 4: ?123, comma, spacebar, punctuation, enter (compact enter key)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ?123
                PreviewKeycap(
                    label = "?123",
                    modifier = Modifier.weight(1.2f),
                    backgroundColor = Color(0xFF1E2026),
                    textColor = Color(0xFF9499A8),
                    fontSize = 9.sp
                )

                // Comma
                PreviewKeycap(
                    label = ",",
                    modifier = Modifier.weight(0.9f)
                )

                // Spacebar
                PreviewKeycap(
                    label = cardData.spaceLabel,
                    modifier = Modifier.weight(4.0f),
                    fontSize = 10.sp,
                    textColor = Color(0xFF9499A8)
                )

                // Punctuation (. or ।)
                PreviewKeycap(
                    label = cardData.punctuation,
                    modifier = Modifier.weight(0.9f)
                )

                // Enter Key (Compact, weight 1.2f)
                PreviewKeycap(
                    label = "⏎",
                    modifier = Modifier.weight(1.2f),
                    backgroundColor = Color(0xFF7AA2F7),
                    textColor = Color(0xFF0F141C),
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Bar: Badge, Name, Description, and Checkbox selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF22252B))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = cardData.badge,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF7AA2F7)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cardData.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isEnabled) Color(0xFF7AA2F7) else Color(0xFFE0E2E8)
                )
                Text(
                    text = cardData.description,
                    fontSize = 11.sp,
                    color = Color(0xFF9499A8),
                    maxLines = 1
                )
            }

            Checkbox(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF7AA2F7),
                    uncheckedColor = Color(0xFF6B7280),
                    checkmarkColor = Color(0xFF0F141C)
                )
            )
        }
    }
}

@Composable
private fun PreviewKeycap(
    label: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF23252A),
    textColor: Color = Color(0xFFE0E2E8),
    fontSize: TextUnit = 11.sp
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(0.5.dp, Color(0xFF33363F), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}
