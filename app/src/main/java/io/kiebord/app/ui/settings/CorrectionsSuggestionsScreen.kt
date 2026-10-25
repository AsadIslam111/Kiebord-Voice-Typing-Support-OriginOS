package io.kiebord.app.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kiebord.data.prefs.KiebordPreferences

@Composable
fun CorrectionsSuggestionsScreen(
    preferences: KiebordPreferences,
    onUpdateSuggestionsEnabled: (Boolean) -> Unit,
    onUpdateNextWordPrediction: (Boolean) -> Unit,
    onUpdateBengaliSmartPhonetic: (Boolean) -> Unit,
    onUpdateBengaliShowRawInputCandidate: (Boolean) -> Unit = {},
    onUpdateLearnedWordsEnabled: (Boolean) -> Unit = {},
    onDeleteLearnedWords: () -> Unit = {},
    onUpdateGestureTyping: (Boolean) -> Unit,
    onUpdateShowGestureTrail: (Boolean) -> Unit,
    onUpdateAutoCapitalization: (Boolean) -> Unit,
    onUpdateDoubleSpacePeriod: (Boolean) -> Unit,
    onUpdateAutoSpaceAfterSuggestion: (Boolean) -> Unit = {},
    onUpdateAutoSpaceAfterPunctuation: (Boolean) -> Unit = {},
    onUpdateRemoveSpaceBeforePunctuation: (Boolean) -> Unit = {},
    onUpdateSuggestionBarMode: (String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val colors = LocalSettingsColors.current

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
        SettingsCategoryHeader(title = "Suggestion Strip & Intelligence")

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
                    title = "Show Proximity Suggestions in Phonetic Layout",
                    subtitle = "Suggest dictionary words and proximity corrections while typing phonetically",
                    checked = preferences.bengaliSmartPhonetic,
                    position = GroupPosition.MIDDLE,
                    onCheckedChange = onUpdateBengaliSmartPhonetic
                )

                SettingsToggleItem(
                    title = "Show English Input Candidate",
                    subtitle = "Display raw typed English word as first candidate in Borno Phonetic layout",
                    checked = preferences.bengaliShowRawInputCandidate,
                    position = GroupPosition.BOTTOM,
                    onCheckedChange = onUpdateBengaliShowRawInputCandidate
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
        SettingsCategoryHeader(title = "Text Corrections & Shortcuts")

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

            SettingsToggleItem(
                title = "Auto-Space after Suggestions",
                subtitle = "Automatically insert a space after committing a word from the candidate strip",
                checked = preferences.autoSpaceAfterSuggestion,
                position = GroupPosition.MIDDLE,
                onCheckedChange = onUpdateAutoSpaceAfterSuggestion
            )

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
