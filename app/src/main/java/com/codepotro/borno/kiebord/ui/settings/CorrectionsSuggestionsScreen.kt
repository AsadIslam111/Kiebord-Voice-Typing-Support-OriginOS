package com.codepotro.borno.kiebord.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codepotro.borno.data.layout.LayoutRepository
import com.codepotro.borno.data.prefs.KiebordPreferences

@Composable
fun CorrectionsSuggestionsScreen(
    preferences: KiebordPreferences,
    onUpdateSuggestionsEnabled: (Boolean) -> Unit,
    onUpdateNextWordPrediction: (Boolean) -> Unit,
    onUpdatePhoneticAutoCorrect: (Boolean) -> Unit = {},
    onUpdateBengaliSmartPhonetic: (Boolean) -> Unit,
    onUpdateBengaliShowRawInputCandidate: (Boolean) -> Unit = {},
    onUpdateBornoRememberSuggestionChoice: (Boolean) -> Unit = {},
    onUpdateLearnedWordsEnabled: (Boolean) -> Unit = {},
    onDeleteLearnedWords: () -> Unit = {},
    onUpdateGestureTyping: (Boolean) -> Unit,
    onUpdateShowGestureTrail: (Boolean) -> Unit,
    onUpdateAutoCapitalization: (Boolean) -> Unit,
    onUpdateDoubleSpacePeriod: (Boolean) -> Unit,
    onUpdateAutoSpaceAfterSuggestion: (Boolean) -> Unit = {},
    onToggleAutoSpaceAfterSuggestionLanguage: (String, Boolean) -> Unit = { _, _ -> },
    onUpdateAutoSpaceAfterPunctuation: (Boolean) -> Unit = {},
    onUpdateRemoveSpaceBeforePunctuation: (Boolean) -> Unit = {},
    onUpdateSuggestionBarMode: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = LocalSettingsColors.current
    val layoutRepo = remember(context) { LayoutRepository.getInstance(context) }

    val activeLanguages = remember(preferences.enabledLayoutIds) {
        preferences.enabledLayoutIds
            .mapNotNull { id ->
                try {
                    layoutRepo.getLayout(id)
                } catch (_: Exception) {
                    null
                }
            }
            .map { layout ->
                val langCode = layout.language.trim().ifBlank {
                    if (layout.id.startsWith("bn")) "bn"
                    else if (layout.id.startsWith("en")) "en"
                    else layout.id.substringBefore('_').lowercase()
                }.lowercase()
                val title = when (langCode) {
                    "en" -> "English"
                    "bn" -> "Bangla"
                    else -> try {
                        val loc = java.util.Locale.forLanguageTag(langCode)
                        val display = loc.getDisplayLanguage(java.util.Locale.ENGLISH)
                        if (display.isNotBlank() && !display.equals(langCode, ignoreCase = true)) {
                            display
                        } else {
                            langCode.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.ENGLISH) else it.toString() }
                        }
                    } catch (_: Exception) {
                        langCode.uppercase()
                    }
                }
                langCode to title
            }
            .distinctBy { it.first }
            .ifEmpty { listOf("en" to "English", "bn" to "Bangla") }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Learned Words?",
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will erase all custom words, next-word prediction ngrams, and personalized frequency data learned from your typing.",
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteLearnedWords()
                        Toast.makeText(context, "Learned words deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = Color(0xFFFF5555), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            },
            containerColor = colors.dialogBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Suggestion Strip")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            val stripPosition = if (preferences.suggestionsEnabled) GroupPosition.TOP else GroupPosition.ALONE
            SettingsToggleItem(
                title = "Display Suggestion Strip",
                subtitle = "Show candidate words and quick tools in the top bar",
                checked = preferences.suggestionsEnabled,
                position = stripPosition,
                onCheckedChange = onUpdateSuggestionsEnabled
            )

            if (preferences.suggestionsEnabled) {
                SettingsToggleItem(
                    title = "Next-Word Prediction",
                    subtitle = "Predict subsequent words using bigram language models",
                    checked = preferences.nextWordPrediction,
                    position = GroupPosition.MIDDLE,
                    onCheckedChange = onUpdateNextWordPrediction
                )

                SettingsToggleItem(
                    title = "Phonetic Auto Correct",
                    subtitle = "Automatically correct common phonetic misspellings",
                    checked = preferences.phoneticAutoCorrect,
                    position = GroupPosition.BOTTOM,
                    onCheckedChange = onUpdatePhoneticAutoCorrect
                )
            }
        }

        if (preferences.suggestionsEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            SettingsCategoryHeader(title = "Suggestion Bar Mode")

            SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
                SettingsRadioOption(
                    title = "Normal (Default)",
                    subtitle = "Shows candidates with an expand button for the expanded view",
                    selected = preferences.suggestionBarMode == KiebordPreferences.SUGGESTION_BAR_MODE_NORMAL,
                    position = GroupPosition.TOP,
                    onSelect = { onUpdateSuggestionBarMode(KiebordPreferences.SUGGESTION_BAR_MODE_NORMAL) }
                )

                SettingsRadioOption(
                    title = "Scrollable",
                    subtitle = "Candidates can be scrolled horizontally instead of expanded view",
                    selected = preferences.suggestionBarMode == KiebordPreferences.SUGGESTION_BAR_MODE_SCROLLABLE,
                    position = GroupPosition.BOTTOM,
                    onSelect = { onUpdateSuggestionBarMode(KiebordPreferences.SUGGESTION_BAR_MODE_SCROLLABLE) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Borno Phonetic")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Show Proximity Suggestions in Borno Phonetic Layout",
                subtitle = "Suggest dictionary words and proximity corrections while typing in Borno Phonetic",
                checked = preferences.bengaliSmartPhonetic,
                position = GroupPosition.TOP,
                onCheckedChange = onUpdateBengaliSmartPhonetic
            )

            SettingsToggleItem(
                title = "Show English Input Candidate",
                subtitle = "Display raw typed English word as first candidate in Borno Phonetic layout",
                checked = preferences.bengaliShowRawInputCandidate,
                position = GroupPosition.MIDDLE,
                onCheckedChange = onUpdateBengaliShowRawInputCandidate
            )

            SettingsToggleItem(
                title = "Remember Suggestion Choice",
                subtitle = "Remember chosen suggestions and commit directly next time",
                checked = preferences.bornoRememberSuggestionChoice,
                position = GroupPosition.BOTTOM,
                onCheckedChange = onUpdateBornoRememberSuggestionChoice
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Personalized Learning")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Learn from Typing",
                subtitle = "Remember frequently typed words, valid candidate selections, and next-word ngrams",
                checked = preferences.learnedWordsEnabled,
                position = GroupPosition.TOP,
                onCheckedChange = onUpdateLearnedWordsEnabled
            )

            SettingsNavigationItem(
                title = "Delete Learned Words",
                subtitle = "Clear all personalized learned words, bigrams, and prediction history",
                position = GroupPosition.BOTTOM,
                onClick = { showDeleteDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Glide & Gesture Typing")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            val glidePosition = if (preferences.gestureTypingEnabled) GroupPosition.TOP else GroupPosition.ALONE
            SettingsToggleItem(
                title = "Enable Glide Typing",
                subtitle = "Type words fluidly by swiping finger across keys without lifting",
                icon = Icons.Outlined.Gesture,
                checked = preferences.gestureTypingEnabled,
                position = glidePosition,
                onCheckedChange = onUpdateGestureTyping
            )

            if (preferences.gestureTypingEnabled) {
                SettingsToggleItem(
                    title = "Show Gesture Vapor Trail",
                    subtitle = "Draw glowing dynamic comet trail tracing your glide trajectory",
                    icon = Icons.Outlined.Route,
                    checked = preferences.showGestureTrail,
                    position = GroupPosition.BOTTOM,
                    onCheckedChange = onUpdateShowGestureTrail
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Text Corrections")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Auto-Capitalization",
                subtitle = "Automatically capitalize the first letter of each sentence",
                checked = preferences.autoCapitalization,
                position = GroupPosition.TOP,
                onCheckedChange = onUpdateAutoCapitalization
            )

            SettingsToggleItem(
                title = "Double-Space Period",
                subtitle = "Quickly double-tap the spacebar to insert a period followed by a space",
                checked = preferences.doubleSpacePeriod,
                position = GroupPosition.MIDDLE,
                onCheckedChange = onUpdateDoubleSpacePeriod
            )

            val autoSpaceShape = remember { getGroupItemShape(GroupPosition.MIDDLE) }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(autoSpaceShape),
                shape = autoSpaceShape,
                colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateAutoSpaceAfterSuggestion(!preferences.autoSpaceAfterSuggestion) }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Space after Suggestions",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Automatically insert a space after committing a word from the candidate strip",
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Switch(
                            checked = preferences.autoSpaceAfterSuggestion,
                            onCheckedChange = onUpdateAutoSpaceAfterSuggestion,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = colors.accent,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.iconBackground,
                                uncheckedBorderColor = colors.inputBorder
                            )
                        )
                    }

                    AnimatedVisibility(
                        visible = preferences.autoSpaceAfterSuggestion,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            HorizontalDivider(
                                color = colors.divider,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                            )

                            activeLanguages.forEach { (langCode, langTitle) ->
                                val isLangEnabled = preferences.isAutoSpaceAfterSuggestionLanguageEnabled(langCode)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onToggleAutoSpaceAfterSuggestionLanguage(langCode, !isLangEnabled)
                                        }
                                        .padding(start = 28.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = langTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary
                                        )
                                        if (!isLangEnabled) {
                                            Text(
                                                text = "Keeps composing buffer active without trailing space",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = isLangEnabled,
                                        onCheckedChange = { isChecked ->
                                            onToggleAutoSpaceAfterSuggestionLanguage(langCode, isChecked)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = colors.accent,
                                            uncheckedThumbColor = colors.textSecondary,
                                            uncheckedTrackColor = colors.iconBackground,
                                            uncheckedBorderColor = colors.inputBorder
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SettingsToggleItem(
                title = "Auto-Space after Punctuation",
                subtitle = "Automatically insert a space after comma, period, and sentence terminators",
                checked = preferences.autoSpaceAfterPunctuation,
                position = GroupPosition.MIDDLE,
                onCheckedChange = onUpdateAutoSpaceAfterPunctuation
            )

            SettingsToggleItem(
                title = "Remove Space Before Punctuation",
                subtitle = "Automatically remove space before punctuation marks (except URL and code fields)",
                checked = preferences.removeSpaceBeforePunctuation,
                position = GroupPosition.BOTTOM,
                onCheckedChange = onUpdateRemoveSpaceBeforePunctuation
            )
        }
    }
}
