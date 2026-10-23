package io.kiebord.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.layout.PredefinedLayouts
import io.kiebord.data.prefs.KiebordPreferences

private data class LayoutInfo(
    val id: String,
    val name: String,
    val language: String,
    val badge: String,
    val description: String,
    val previewKeys: List<List<String>>
)

@Composable
fun KeyboardLayoutsScreen(
    preferences: KiebordPreferences,
    onUpdatePrimaryLayout: (String) -> Unit,
    onToggleLayoutEnabled: (String, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    val availableLayouts = listOf(
        LayoutInfo(
            id = KiebordPreferences.LAYOUT_EN_QWERTY,
            name = "English (QWERTY)",
            language = "English",
            badge = "EN",
            description = "Standard staggered QWERTY with gesture typing and flick numbers",
            previewKeys = listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m")
            )
        ),
        LayoutInfo(
            id = KiebordPreferences.LAYOUT_BN_BORNO,
            name = "Bengali (Borno Phonetic)",
            language = "Bangla",
            badge = "বর্ণ",
            description = "Streamlined English-to-Bangla transliteration (e.g. 'ami' -> 'আমি')",
            previewKeys = listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("z", "x", "c", "v", "b", "n", "m")
            )
        ),
        LayoutInfo(
            id = KiebordPreferences.LAYOUT_BN_PROBHAT,
            name = "Bengali (Probhat)",
            language = "Bangla",
            badge = "প্রভাত",
            description = "Probhat national standard layout with dual-shift vowel & consonant layers",
            previewKeys = listOf(
                listOf("দ", "ড", "প", "ট", "চ", "ত", "জ", "ক", "স", "হ"),
                listOf("ি", "ু", "ী", "ূ", "া", "ে", "ো", "য়", "র", "ল"),
                listOf("্", "ং", "ঃ", "ঁ", "ব", "ম", "ন", "ণ", "শ", "য")
            )
        )
    )

    var previewLayoutId by remember { mutableStateOf(preferences.primaryLayoutId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsTopAppBar(
            title = "Keyboard Layouts",
            subtitle = "Active languages, primary layout and visual maps",
            onBack = onBack
        )

        SettingsCategoryHeader(title = "Primary Default Layout")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            availableLayouts.forEachIndexed { index, layout ->
                val isSelected = preferences.primaryLayoutId == layout.id
                SettingsRadioOption(
                    title = layout.name,
                    subtitle = layout.description,
                    selected = isSelected,
                    showDivider = index < availableLayouts.size - 1,
                    onSelect = {
                        onUpdatePrimaryLayout(layout.id)
                        previewLayoutId = layout.id
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Enabled Layout Rotation")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            availableLayouts.forEachIndexed { index, layout ->
                val isEnabled = preferences.enabledLayoutIds.contains(layout.id)
                val isLastRemaining = preferences.enabledLayoutIds.size == 1 && isEnabled

                SettingsToggleItem(
                    title = layout.name,
                    subtitle = if (isLastRemaining) "At least one layout must remain active" else "Include in globe / spacebar language rotation",
                    icon = Icons.Outlined.Keyboard,
                    checked = isEnabled,
                    showDivider = index < availableLayouts.size - 1,
                    onCheckedChange = { checked ->
                        if (!isLastRemaining || checked) {
                            onToggleLayoutEnabled(layout.id, checked)
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Live Layout Preview")

        val currentPreview = availableLayouts.find { it.id == previewLayoutId } ?: availableLayouts.first()

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2E3440))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentPreview.badge,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7AA2F7)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = currentPreview.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE0E2E8)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Miniature Keyboard Keycap Matrix
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF131417))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentPreview.previewKeys.forEach { rowKeys ->
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
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Bengali Phonetic Quick Reference")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = Color(0xFF7AA2F7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Borno Phonetic Transliteration Rules",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE0E2E8)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Vowels: a → অ, aa / A → আ, i → ই, ee / I → ঈ, u → উ, oo / U → ঊ, e → এ, o → ও\n" +
                            "• Consonants: k → ক, kh → খ, g → গ, gh → ঘ, ng → ঙ, ch → চ, cch → ছ, j → জ, jh → ঝ\n" +
                            "• Conjuncts: kkh → ক্ষ, jyo → জ্ঞ, nch → ঞ্চ, njo → ঞ্জ, tto → ত্ত, bbo → ব্ব\n" +
                            "• Special: `rri` → ঋ, `h` after consonants adds mahaprana aspiration.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF9499A8)
                )
            }
        }
    }
}
