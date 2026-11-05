package io.kiebord.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.kiebord.data.prefs.KiebordPreferences

@Composable
fun SmartGesturesScreen(
    preferences: KiebordPreferences,
    onUpdateFastDeleteEnabled: (Boolean) -> Unit,
    onUpdateSelectiveDeleteMode: (String) -> Unit,
    onUpdateCursorTrackpad: (Boolean) -> Unit,
    onBack: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 32.dp)
    ) {
        SettingsCategoryHeader(title = "Delete Gestures")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Fast Delete",
                subtitle = "Delete all text before cursor by sliding up from the backspace key to the top row",
                checked = preferences.fastDeleteEnabled,
                position = GroupPosition.TOP,
                onCheckedChange = onUpdateFastDeleteEnabled
            )

            SettingsRadioOption(
                title = "Swipe Delete (Selective): By Word",
                subtitle = "Swiping left from backspace selects and deletes text word-by-word",
                selected = preferences.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_WORD,
                position = GroupPosition.MIDDLE,
                onSelect = { onUpdateSelectiveDeleteMode(KiebordPreferences.DELETE_MODE_WORD) }
            )

            SettingsRadioOption(
                title = "Swipe Delete (Selective): By Character",
                subtitle = "Swiping left from backspace selects and deletes text character-by-character",
                selected = preferences.selectiveDeleteMode == KiebordPreferences.DELETE_MODE_CHARACTER,
                position = GroupPosition.BOTTOM,
                onSelect = { onUpdateSelectiveDeleteMode(KiebordPreferences.DELETE_MODE_CHARACTER) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        SettingsCategoryHeader(title = "Spacebar Gestures")

        SettingsGroup(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsToggleItem(
                title = "Spacebar Cursor Trackpad",
                subtitle = "Slide thumb horizontally across spacebar to position cursor",
                checked = preferences.cursorTrackpadEnabled,
                position = GroupPosition.ALONE,
                onCheckedChange = onUpdateCursorTrackpad
            )
        }
    }
}

