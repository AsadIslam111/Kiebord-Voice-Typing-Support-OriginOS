package io.kiebord.app.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.kiebord.data.prefs.KiebordPreferences

private data class LayoutVariant(
    val id: String,
    val name: String,
    val badge: String,
    val description: String,
    val previewKeys: List<List<String>>
)

@Composable
fun KeyboardLayoutsScreen(
    preferences: KiebordPreferences,
    onUpdatePrimaryLayout: (String) -> Unit,
    onToggleLayoutEnabled: (String, Boolean) -> Unit,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    val englishLayout = LayoutVariant(
        id = KiebordPreferences.LAYOUT_EN_QWERTY,
        name = "English (QWERTY)",
        badge = "EN",
        description = "Standard QWERTY layout with numeric flick keys and glide typing",
        previewKeys = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("z", "x", "c", "v", "b", "n", "m")
        )
    )

    val banglaLayouts = listOf(
        LayoutVariant(
            id = KiebordPreferences.LAYOUT_BN_BORNO,
            name = "Bengali (Borno Phonetic)",
            badge = "বর্ণ",
            description = "Natural English-to-Bangla phonetic transliteration (e.g. 'ami' -> 'আমি')",
            previewKeys = listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m")
            )
        ),
        LayoutVariant(
            id = KiebordPreferences.LAYOUT_BN_PROBHAT,
            name = "Bengali (Probhat)",
            badge = "প্রভাত",
            description = "Probhat national standard dual-shift mapping for direct Bengali input",
            previewKeys = listOf(
                listOf("দ", "ড", "প", "ট", "চ", "ত", "জ", "ক", "স", "হ"),
                listOf("ি", "ু", "ী", "ূ", "া", "ে", "ো", "য়", "র", "ল"),
                listOf("্", "ং", "ঃ", "ঁ", "ব", "ম", "ন", "ণ", "শ", "য")
            )
        )
    )

    // Current active selection in Bangla sub-pager
    var banglaIndex by remember {
        mutableIntStateOf(
            if (preferences.primaryLayoutId == KiebordPreferences.LAYOUT_BN_PROBHAT) 1 else 0
        )
    }

    var selectedLanguage by remember {
        mutableStateOf(
            if (preferences.primaryLayoutId.startsWith("bn")) "bn" else "en"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Languages & Primary Input")

        // 1. English Language Card
        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            val isEnglishSelected = preferences.primaryLayoutId == englishLayout.id

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedLanguage = "en"
                        onUpdatePrimaryLayout(englishLayout.id)
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
                        text = "QWERTY layout",
                        fontSize = 12.sp,
                        color = Color(0xFF9499A8)
                    )
                }

                RadioButton(
                    selected = isEnglishSelected,
                    onClick = {
                        selectedLanguage = "en"
                        onUpdatePrimaryLayout(englishLayout.id)
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Color(0xFF7AA2F7),
                        unselectedColor = Color(0xFF6B7280)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Bangla Language Card (Groups Borno Phonetic and Probhat)
        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            val isBanglaPrimary = preferences.primaryLayoutId.startsWith("bn")
            val currentBangla = banglaLayouts[banglaIndex]

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedLanguage = "bn"
                            onUpdatePrimaryLayout(currentBangla.id)
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
                            text = "Borno Phonetic • Probhat",
                            fontSize = 12.sp,
                            color = Color(0xFF9499A8)
                        )
                    }

                    RadioButton(
                        selected = isBanglaPrimary,
                        onClick = {
                            selectedLanguage = "bn"
                            onUpdatePrimaryLayout(currentBangla.id)
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF7AA2F7),
                            unselectedColor = Color(0xFF6B7280)
                        )
                    )
                }

                // If Bangla is selected/clicked: Show live keyboard preview with radio dot on bottom and layout name
                if (selectedLanguage == "bn") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF131417))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                AnimatedContent(
                                    targetState = banglaIndex,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "banglaKeyPreview"
                                ) { idx ->
                                    val layout = banglaLayouts[idx]
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        layout.previewKeys.forEach { rowKeys ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                rowKeys.forEach { keyStr ->
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(28.dp)
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(Color(0xFF25272B))
                                                            .border(0.5.dp, Color(0xFF383A40), RoundedCornerShape(4.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = keyStr,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color(0xFFE0E2E8)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Radio dots on bottom for selecting Bangla layout variant
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    banglaLayouts.forEachIndexed { idx, layout ->
                                        val isDotSelected = banglaIndex == idx
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    width = 2.dp,
                                                    color = if (isDotSelected) Color(0xFF7AA2F7) else Color(0xFF6B7280),
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    banglaIndex = idx
                                                    onUpdatePrimaryLayout(layout.id)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isDotSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF7AA2F7))
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Layout Name below the radio dots
                                Text(
                                    text = banglaLayouts[banglaIndex].name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF7AA2F7)
                                )
                                Text(
                                    text = banglaLayouts[banglaIndex].description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF9499A8),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        SettingsCategoryHeader(title = "Language Switcher Rotation")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            val allLayouts = listOf(englishLayout) + banglaLayouts
            allLayouts.forEachIndexed { index, layout ->
                val isEnabled = preferences.enabledLayoutIds.contains(layout.id)
                val isLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnabled

                SettingsToggleItem(
                    title = layout.name,
                    subtitle = if (isLastRemaining) "At least one layout must remain active" else "Include in globe switch rotation",
                    icon = Icons.Outlined.Keyboard,
                    checked = isEnabled,
                    showDivider = index < allLayouts.size - 1,
                    onCheckedChange = { checked ->
                        if (!isLastRemaining || checked) {
                            onToggleLayoutEnabled(layout.id, checked)
                        }
                    }
                )
            }
        }
    }
}
