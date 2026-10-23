package io.kiebord.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Gesture
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.SpaceBar
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kiebord.data.prefs.KiebordPreferences

@Composable
fun CorrectionsSuggestionsScreen(
    preferences: KiebordPreferences,
    onUpdateSuggestionsEnabled: (Boolean) -> Unit,
    onUpdateNextWordPrediction: (Boolean) -> Unit,
    onUpdateBengaliSmartPhonetic: (Boolean) -> Unit,
    onUpdateGestureTyping: (Boolean) -> Unit,
    onUpdateShowGestureTrail: (Boolean) -> Unit,
    onUpdateAutoCapitalization: (Boolean) -> Unit,
    onUpdateDoubleSpacePeriod: (Boolean) -> Unit,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Suggestion Strip & Intelligence")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Display Suggestion Strip",
                subtitle = "Show candidate words and quick tools in the top bar",
                icon = Icons.Outlined.Spellcheck,
                checked = preferences.suggestionsEnabled,
                onCheckedChange = onUpdateSuggestionsEnabled
            )

            if (preferences.suggestionsEnabled) {
                SettingsToggleItem(
                    title = "Next-Word Prediction",
                    subtitle = "Predict subsequent words using bigram language models",
                    icon = Icons.Outlined.AutoAwesome,
                    checked = preferences.nextWordPrediction,
                    onCheckedChange = onUpdateNextWordPrediction
                )

                SettingsToggleItem(
                    title = "Bengali Frequency Ranking",
                    subtitle = "Score and rank phonetic Bengali transliterations by usage frequency",
                    icon = Icons.Outlined.Translate,
                    checked = preferences.bengaliSmartPhonetic,
                    showDivider = false,
                    onCheckedChange = onUpdateBengaliSmartPhonetic
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Glide & Gesture Typing")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Enable Glide Typing",
                subtitle = "Type words fluidly by swiping finger across keys without lifting",
                icon = Icons.Outlined.Gesture,
                checked = preferences.gestureTypingEnabled,
                onCheckedChange = onUpdateGestureTyping
            )

            if (preferences.gestureTypingEnabled) {
                SettingsToggleItem(
                    title = "Show Gesture Vapor Trail",
                    subtitle = "Draw glowing dynamic comet trail tracing your glide trajectory",
                    icon = Icons.Outlined.Route,
                    checked = preferences.showGestureTrail,
                    showDivider = false,
                    onCheckedChange = onUpdateShowGestureTrail
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Text Corrections & Shortcuts")

        SettingsCard(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Auto-Capitalization",
                subtitle = "Automatically capitalize the first letter of each sentence",
                icon = Icons.Outlined.TextFields,
                checked = preferences.autoCapitalization,
                onCheckedChange = onUpdateAutoCapitalization
            )

            SettingsToggleItem(
                title = "Double-Space Period",
                subtitle = "Quickly double-tap the spacebar to insert a period followed by a space",
                icon = Icons.Outlined.SpaceBar,
                checked = preferences.doubleSpacePeriod,
                showDivider = false,
                onCheckedChange = onUpdateDoubleSpacePeriod
            )
        }
    }
}
